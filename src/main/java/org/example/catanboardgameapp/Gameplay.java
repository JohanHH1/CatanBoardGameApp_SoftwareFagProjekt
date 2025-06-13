package org.example.catanboardgameapp;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.catanboardgameviews.CatanBoardGameView;
import java.util.*;

import org.example.catanboardgameviews.MenuView;
import org.example.controller.GameController;

import static org.example.catanboardgameapp.DevelopmentCard.DevelopmentCardType.*;

public class Gameplay {

    //__________________________CONFIG & VIEWS_____________________________//
    private final GameController gameController;
    private final int boardRadius;
    private DrawOrDisplay drawOrDisplay;
    private CatanBoardGameView catanBoardGameView;
    private MenuView menuView;

    //__________________________PLAYER STATE_____________________________//
    private final List<Player> playerList = new ArrayList<>();
    private int currentPlayerIndex;
    private Player currentPlayer;

    //__________________________TURN & PHASE CONTROL_____________________________//
    private boolean initialPhase = true;
    private boolean forwardOrder = true;
    private boolean hasRolledThisTurn = false;
    private boolean waitingForInitialRoad = false;
    private volatile boolean gamePaused = false;
    private Thread activeAIThread;
    private boolean isRobberMoveRequired = false;
    private boolean gameOver = false;

    //__________________________BOARD & GAME DATA_____________________________//
    private Board board;
    private Vertex lastInitialSettlement = null;
    private DevelopmentCard developmentCard;
    private final DevelopmentCard.DevelopmentCardType[] developmentCardTypes = {
            MONOPOLY, MONOPOLY,
            YEAROFPLENTY, YEAROFPLENTY,
            ROADBUILDING, ROADBUILDING,
            VICTORYPOINT, VICTORYPOINT, VICTORYPOINT, VICTORYPOINT, VICTORYPOINT,
            KNIGHT, KNIGHT, KNIGHT, KNIGHT, KNIGHT, KNIGHT,
            KNIGHT, KNIGHT, KNIGHT, KNIGHT, KNIGHT, KNIGHT, KNIGHT
    };

    private List<DevelopmentCard.DevelopmentCardType> shuffledDevelopmentCards;

    private final LongestRoadManager longestRoadManager;
    private final BiggestArmyManager biggestArmy;

    private int lastRolledDie1;
    private int lastRolledDie2;
    private int tradeCounter;
    private int turnCounter = 0;

    //__________________________CONSTRUCTOR_____________________________//
    public Gameplay(Stage primaryStage, int boardRadius, GameController gameController) {
        this.drawOrDisplay = new DrawOrDisplay(boardRadius);
        this.boardRadius = boardRadius;
        this.gameController = gameController;
        this.longestRoadManager = new LongestRoadManager(this);
        this.biggestArmy = new BiggestArmyManager(this);
    }
    //________________________INITIALIZE_______________________________//
    public void initializeDevelopmentCards() {
        if (catanBoardGameView == null) {
            throw new IllegalStateException("CatanBoardGameView must be set before initializing development cards.");
        }

        // Create the development card handler (now that view is valid)
        this.developmentCard = new DevelopmentCard(
                this,
                playerList,
                catanBoardGameView,
                gameController.getTradeController()
        );

        // Shuffle the development card deck using enum values directly
        List<DevelopmentCard.DevelopmentCardType> shuffledDevCards =
                new ArrayList<>(Arrays.asList(developmentCardTypes));
        Collections.shuffle(shuffledDevCards);
        this.shuffledDevelopmentCards = shuffledDevCards;
    }

    // Initialize players and any chosen AI players
    public void initializeAllPlayers(int humanCount, int aiEasy, int aiMedium, int aiHard) {
        playerList.clear();
        List<Color> colors = new ArrayList<>(List.of(
                Color.RED, Color.BLUE, Color.GREEN, Color.DARKORANGE, Color.PURPLE, Color.YELLOW
        ));

        int idCounter = 1;

        for (int i = 0; i < humanCount && !colors.isEmpty(); i++) {
            playerList.add(new Player(idCounter++, colors.remove(0), this));
        }

        AIOpponent.ThinkingSpeed selectedSpeed = menuView.getSelectedAISpeed(); // <- retrieve selected speed

        for (int i = 0; i < aiEasy && !colors.isEmpty(); i++) {
            AIOpponent ai = new AIOpponent(idCounter++, colors.remove(0), AIOpponent.StrategyLevel.EASY, this);
            ai.setThinkingSpeed(selectedSpeed);
            playerList.add(ai);
        }
        for (int i = 0; i < aiMedium && !colors.isEmpty(); i++) {
            AIOpponent ai = new AIOpponent(idCounter++, colors.remove(0), AIOpponent.StrategyLevel.MEDIUM, this);
            ai.setThinkingSpeed(selectedSpeed);
            playerList.add(ai);
        }
        for (int i = 0; i < aiHard && !colors.isEmpty(); i++) {
            AIOpponent ai = new AIOpponent(idCounter++, colors.remove(0), AIOpponent.StrategyLevel.HARD, this);
            ai.setThinkingSpeed(selectedSpeed);
            playerList.add(ai);
        }

        if (!playerList.isEmpty()) {
            currentPlayerIndex = 0;
            currentPlayer = playerList.get(0);
        }
    }

    //____________________________TURN MANAGEMENT______________________________//
    public void nextPlayerTurn() {
        stopAllAIThreads(); // Stop any in-progress AI thread before advancing
        crashGameIfMaxTurnsExceeded(500, turnCounter);
        startOfTurnEffects();

        // ------------------- INITIAL PLACEMENT PHASE -------------------
        if (initialPhase) {
            if (waitingForInitialRoad) {
                catanBoardGameView.logToGameLog("Player " + currentPlayer.getPlayerId() + " must place a road.");
                return;
            }

            currentPlayerIndex = forwardOrder ? currentPlayerIndex + 1 : currentPlayerIndex - 1;

            if (forwardOrder && currentPlayerIndex >= playerList.size()) {
                currentPlayerIndex = playerList.size() - 1;
                forwardOrder = false;
            } else if (!forwardOrder && currentPlayerIndex < 0) {
                // Transition from initial to main phase
                initialPhase = false;
                forwardOrder = true;
                currentPlayerIndex = 0;
                currentPlayer = playerList.get(currentPlayerIndex);
                waitingForInitialRoad = false;
                lastInitialSettlement = null;

                // Safe to start turn
                Platform.runLater(() -> {
                    catanBoardGameView.logToGameLog("All initial placements complete. Starting first turn...");
                    if (currentPlayer instanceof AIOpponent ai) {
                        startAIThread(ai); // Safe AI startup
                    } else {
                        catanBoardGameView.showDiceButton();
                    }
                });
                return;
            }

            currentPlayer = playerList.get(currentPlayerIndex);
            waitingForInitialRoad = false;
            lastInitialSettlement = null;

            if (currentPlayer instanceof AIOpponent ai) {
                startAIThread(ai); //always call through here
            } else {
                catanBoardGameView.prepareForHumanInitialPlacement(currentPlayer);
            }
            return;
        }

        // ------------------- MAIN GAME PHASE -------------------
        waitingForInitialRoad = false;
        lastInitialSettlement = null;

        if (currentPlayer instanceof AIOpponent ai) {
            startAIThread(ai); // Safe thread init
        } else {
            catanBoardGameView.showDiceButton();
        }
    }

    // Helper function for nextPlayerTurn function above
    private void startOfTurnEffects() {
        if (!initialPhase) {
            catanBoardGameView.logToGameLog(getCurrentPlayer() +  " has ended their turn.");
            // Rotate player index only in main game phase
            currentPlayerIndex = (currentPlayerIndex + 1) % playerList.size();
            currentPlayer = playerList.get(currentPlayerIndex);
        }

        // UI refresh applies in both phases
        catanBoardGameView.runOnFX(() -> catanBoardGameView.refreshSidebar());
        catanBoardGameView.hideTurnButton();
        setHasRolledThisTurn(false);

        if (hasHumanPlayers()) {
            catanBoardGameView.centerBoard(
                    catanBoardGameView.getBoardGroup(),
                    menuView.getGAME_WIDTH(),
                    menuView.getGAME_HEIGHT()
            );
        }
    }

    public void crashGameIfMaxTurnsExceeded(int MAX_TURNS, int turnCounter) {
        if (turnCounter > MAX_TURNS) {
            pauseGame();
            String error = "Fatal error: MAX_TURNS (" + MAX_TURNS + ") exceeded. Possible infinite loop or thread leak.";
            System.err.println(error);

            // Stop all running threads gracefully
            stopAllAIThreads();

            // Optional: alert the user before killing the app
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, error, ButtonType.OK);
                alert.setTitle("Game Crash");
                alert.setHeaderText("Too many turns! Game is terminating.");
                alert.showAndWait();

                // Now forcefully exit
                System.exit(1);
            });
        }
    }

    //_____________________________DICE________________________________//
    public void rollDice() {
        turnCounter++;
        setHasRolledThisTurn(true);

        // Logic part (no FX)
        Random rand = new Random();
        lastRolledDie1 = rand.nextInt(6) + 1;
        lastRolledDie2 = rand.nextInt(6) + 1;
        int roll = lastRolledDie1 + lastRolledDie2;

        catanBoardGameView.runOnFX(() -> {
            // Step 1: update visuals
            catanBoardGameView.updateDiceImages(lastRolledDie1, lastRolledDie2);
            catanBoardGameView.logToGameLog("\n" + currentPlayer + " ROLLED " + roll + "!");
            catanBoardGameView.hideDiceButton();
            catanBoardGameView.showTurnButton();
            catanBoardGameView.runOnFX(() -> catanBoardGameView.refreshSidebar());

            // Handle robber or distribute resources
            if (roll == 7) {
                catanBoardGameView.getRobber().requireRobberMove();
                catanBoardGameView.getRobber().showRobberTargets(catanBoardGameView.getBoardGroup());
                setRobberMoveRequired(true);
            } else {
                catanBoardGameView.logToGameLog("Distributing resources:");
                distributeResources(roll);
            }
            catanBoardGameView.runOnFX(() -> catanBoardGameView.refreshSidebar());
        });
    }

    public void distributeResources(int diceRoll) {
        for (Tile tile : board.getTiles()) {
            if (tile.getTileDiceNumber() == diceRoll) {
                Resource.ResourceType type = tile.getResourcetype();

                if (type == Resource.ResourceType.SEA || type == Resource.ResourceType.DESERT) continue;

                for (Vertex vertex : tile.getVertices()) {
                    Player owner = vertex.getOwner();
                    if (owner != null) {
                        String res = type.getName();
                        int amount = vertex.isCity() ? 2 : 1;
                        owner.getResources().merge(res, amount, Integer::sum);

                        String logMsg = "Player " + owner.getPlayerId() + " gets " + res;
                        catanBoardGameView.runOnFX(() -> catanBoardGameView.logToGameLog(logMsg));
                    }
                }
            }
        }
        catanBoardGameView.runOnFX(() -> catanBoardGameView.logToGameLog("\n"));
    }

    //_________________________________________ AI THREAD _____________________________________________//
    public void startAIThread(AIOpponent ai) {
        if (activeAIThread != null && activeAIThread.isAlive()) return;
        if (isGameOver()) return;

        activeAIThread = new Thread(() -> {
            while (isGamePaused()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            if (initialPhase) {
                ai.placeInitialSettlementAndRoad(this, catanBoardGameView.getBoardGroup());
            } else {
                ai.makeMoveAI(this, getCatanBoardGameView().getBoardGroup());
            }
        });
        activeAIThread.setDaemon(true);
        activeAIThread.start();
    }

    public void stopAllAIThreads() {
        if (activeAIThread != null && activeAIThread.isAlive()) {
            activeAIThread.interrupt();
        }
        activeAIThread = null;
    }

    //_________________________________BUY AND PLAY DEVELOPMENT CARDS_____________________________________//
    public void buyDevelopmentCard() {
        if (shuffledDevelopmentCards.isEmpty()) {
            catanBoardGameView.runOnFX(drawOrDisplay::showNoMoreDevelopmentCardToBuyPopup);
        } else if (!hasRolledDice()){
            catanBoardGameView.runOnFX(() -> drawOrDisplay.rollDiceBeforeActionPopup("You must roll the dice before taking any actions!"));
        } else if (canRemoveResource("Wool", 1) && canRemoveResource("Ore", 1) && canRemoveResource("Grain", 1)) {
            removeResource("Wool", 1);
            removeResource("Ore", 1);
            removeResource("Grain", 1);
            // Correctly remove and use the DevelopmentCardType directly
            DevelopmentCard.DevelopmentCardType cardType = shuffledDevelopmentCards.remove(0);

            // Store the card in the player's development card map
            currentPlayer.getDevelopmentCards().merge(cardType, 1, Integer::sum);
//            System.out.println("DEV CARDS AFTER MERGE: " + currentPlayer.getDevelopmentCards());// removed before hand in
//            System.out.println("Current player class: " + currentPlayer.getClass().getSimpleName());// removed before hand in
//            System.out.println("Current player ID: " + currentPlayer.getPlayerId()); // removed before hand in
//
            String log = currentPlayer + " bought a development card: " + cardType.getDisplayName(); // removed before hand in
            catanBoardGameView.runOnFX(() -> {
                catanBoardGameView.logToGameLog(log);
                catanBoardGameView.runOnFX(() -> catanBoardGameView.refreshSidebar());
            });
        } else {
            catanBoardGameView.runOnFX(drawOrDisplay::showFailToBuyDevelopmentCardPopup);
        }
    }

    public void playDevelopmentCard(Player player, DevelopmentCard.DevelopmentCardType type) {
        if (isActionBlockedByDevelopmentCard()) {
            drawOrDisplay.showFinishDevelopmentCardActionPopup();
            return;
        }
        // Play the card
        type.play(player, developmentCard);

        // Safely remove it from the player's collection
        player.getDevelopmentCards().computeIfPresent(type, (k, v) -> (v > 1) ? v - 1 : 0);

        catanBoardGameView.runOnFX(() -> {
//            catanBoardGameView.logToGameLog(player + " played " + type.getDisplayName() + " card.");
            catanBoardGameView.runOnFX(() -> catanBoardGameView.refreshSidebar());
        });
    }


    public DevelopmentCard getDevelopmentCard() {
        return developmentCard;
    }

    //_____________________________RESOURCES & TRADING_____________________________//

    public boolean canRemoveResource(String resource, int amount) {
        return currentPlayer.getResources().getOrDefault(resource, 0) >= amount;
    }

    public void removeResource(String resource, int amount) {
        int current = currentPlayer.getResources().getOrDefault(resource, 0);
        if (current >= amount) {
            currentPlayer.getResources().put(resource, current - amount);
        }
    }
    public void addResource(String resource, int amount) {
        currentPlayer.getResources().put(resource,
                currentPlayer.getResources().getOrDefault(resource, 0) + amount);
    }

    //_____________________________BUILDING FUNCTIONS____________________________//
    public BuildResult buildInitialSettlement(Vertex vertex) {
        if (vertex == null || !isValidSettlementPlacement(vertex)) return BuildResult.INVALID_VERTEX;
        if (currentPlayer.getSettlements().contains(vertex)) return BuildResult.INVALID_VERTEX;
        if (initialPhase && waitingForInitialRoad) return BuildResult.INVALID_VERTEX;

        currentPlayer.getSettlements().add(vertex);
        vertex.setOwner(currentPlayer);
        vertex.makeSettlement();
        increasePlayerScore();

        waitingForInitialRoad = true;
        lastInitialSettlement = vertex;

        if (currentPlayer.getSettlements().size() == 2) {
            currentPlayer.setSecondSettlement(vertex);
            for (Tile tile : vertex.getAdjacentTiles()) {
                Resource.ResourceType type = tile.getResourcetype();

                // Only collect valid resource types
                if (type != Resource.ResourceType.DESERT && type != Resource.ResourceType.SEA) {
                    currentPlayer.getResources().merge(type.getName(), 1, Integer::sum);
                }
            }
        }
        return BuildResult.SUCCESS;
    }

    public BuildResult buildRoad(Edge edge) {
        if (initialPhase && waitingForInitialRoad) {
            if (!edge.isConnectedTo(lastInitialSettlement)) return BuildResult.NOT_CONNECTED;
            if (!isValidRoadPlacement(edge)) return BuildResult.INVALID_EDGE;
            currentPlayer.getRoads().add(edge);
            waitingForInitialRoad = false;
            lastInitialSettlement = null;

            // update longest road for currentPlayer
            longestRoadManager.calculateAndUpdateLongestRoad(currentPlayer, playerList);
            catanBoardGameView.runOnFX(() -> catanBoardGameView.refreshSidebar());
            return BuildResult.SUCCESS;
        }

        if (initialPhase) return BuildResult.INVALID_EDGE;

        if (!isValidRoadPlacement(edge)) return BuildResult.INVALID_EDGE;

        if (currentPlayer.getRoads().size() >= menuView.getMaxRoads()) {
            return BuildResult.TOO_MANY_ROADS;
        }

        if (canRemoveResource("Brick", 1) && canRemoveResource("Wood", 1)) {
            removeResource("Brick", 1);
            removeResource("Wood", 1);
            currentPlayer.getRoads().add(edge);

            // update longest road for currentPlayer
            longestRoadManager.calculateAndUpdateLongestRoad(currentPlayer, playerList);
            catanBoardGameView.runOnFX(() -> catanBoardGameView.refreshSidebar());
            return BuildResult.SUCCESS;
        }

        return BuildResult.INSUFFICIENT_RESOURCES;
    }

    public BuildResult buildSettlement(Vertex vertex) {
        if (vertex == null || !isValidSettlementPlacement(vertex)) return BuildResult.INVALID_VERTEX;
        if (currentPlayer.getSettlements().contains(vertex)) return BuildResult.INVALID_VERTEX;
        if (currentPlayer.getSettlements().size() >= menuView.getMaxSettlements()) {
            return BuildResult.TOO_MANY_SETTLEMENTS;
        }

        if (canRemoveResource("Brick", 1) &&
                canRemoveResource("Wood", 1) &&
                canRemoveResource("Grain", 1) &&
                canRemoveResource("Wool", 1)) {

            removeResource("Brick", 1);
            removeResource("Wood", 1);
            removeResource("Grain", 1);
            removeResource("Wool", 1);

            currentPlayer.getSettlements().add(vertex);
            vertex.setOwner(currentPlayer);
            vertex.makeSettlement();
            increasePlayerScore();
            return BuildResult.SUCCESS;
        }
        return BuildResult.INSUFFICIENT_RESOURCES;
    }

    public BuildResult buildCity(Vertex vertex) {
        if (isNotValidCityPlacement(vertex)) return BuildResult.INVALID_VERTEX;
        if (currentPlayer.getCities().size() >= menuView.getMaxCities()) {
            return BuildResult.TOO_MANY_CITIES;
        }

        if (canRemoveResource("Ore", 3) && canRemoveResource("Grain", 2)) {
            removeResource("Ore", 3);
            removeResource("Grain", 2);
            currentPlayer.getSettlements().remove(vertex);
            currentPlayer.getCities().add(vertex);
            vertex.setOwner(currentPlayer);
            vertex.makeCity();
            increasePlayerScore();
            return BuildResult.UPGRADED_TO_CITY;
        }
        drawOrDisplay.notEnoughResourcesPopup("Not enough resources to build a city");
        return BuildResult.INSUFFICIENT_RESOURCES;
    }

    public BuildResult placeFreeRoad(Player player, Edge edge) {
        if (!isValidRoadPlacement(edge)) return BuildResult.INVALID_EDGE;
        if (player.getRoads().size() >= menuView.getMaxRoads()) return BuildResult.TOO_MANY_ROADS;
        catanBoardGameView.runOnFX(() -> {
            getGameController().getBuildController().buildRoad(edge, player);
        });
        player.getRoads().add(edge);
        // Recalculate longest road
        longestRoadManager.calculateAndUpdateLongestRoad(player, playerList);
        // Update sidebar/UI
        catanBoardGameView.runOnFX(() -> catanBoardGameView.refreshSidebar());
        return BuildResult.SUCCESS;
    }

    //______________________VALID BUILD CHECKS___________________________//
    public boolean isValidSettlementPlacement(Vertex vertex) {
        if (vertex.hasSettlement()) return false;

        // Must border at least one land tile
        boolean hasLand = vertex.getAdjacentTiles().stream()
                .anyMatch(tile -> !tile.isSea());
        if (!hasLand) return false;

        // Distance rule — no adjacent settlements
        for (Vertex neighbor : vertex.getNeighbors()) {
            if (neighbor.hasSettlement()) return false;
        }

        // In main phase: require an adjacent road that belongs to the current player
        if (!isInInitialPhase()) {
            boolean hasOwnAdjacentRoad = getCurrentPlayer().getRoads().stream()
                    .anyMatch(edge -> edge.isConnectedTo(vertex));
            if (!hasOwnAdjacentRoad) return false;
        }

        return true;
    }

    public boolean isValidRoadPlacement(Edge edge) {
        // Reject edges where either end is sea-only
        boolean vertex1HasLand = edge.getVertex1().getAdjacentTiles().stream().anyMatch(t -> !t.isSea());
        boolean vertex2HasLand = edge.getVertex2().getAdjacentTiles().stream().anyMatch(t -> !t.isSea());
        if (!vertex1HasLand || !vertex2HasLand) return false;

        // No duplicate road
        if (playerList.stream().anyMatch(p -> p.getRoads().contains(edge))) return false;

        // Block building through opponent's settlements
        for (Player player : playerList) {
            if (player != currentPlayer) {
                if (player.getSettlements().contains(edge.getVertex1()) &&
                        currentPlayer.getRoads().stream().anyMatch(r -> r.isConnectedTo(edge.getVertex1()))) {
                    return false;
                }
                if (player.getSettlements().contains(edge.getVertex2()) &&
                        currentPlayer.getRoads().stream().anyMatch(r -> r.isConnectedTo(edge.getVertex2()))) {
                    return false;
                }
            }
        }

        // Must connect to a player's own road or settlement
        boolean connectsToSettlement = currentPlayer.getSettlements().contains(edge.getVertex1()) ||
                currentPlayer.getSettlements().contains(edge.getVertex2());

        boolean connectsToRoad = currentPlayer.getRoads().stream().anyMatch(r ->
                r.isConnectedTo(edge.getVertex1()) || r.isConnectedTo(edge.getVertex2()));

        return connectsToSettlement || connectsToRoad;
    }

    public boolean isNotValidCityPlacement(Vertex vertex) {
        return !(vertex.hasSettlement() && vertex.getOwner() == currentPlayer);
    }

    //___________________________SCORE MANAGEMENT_____________________________//
    public void increasePlayerScore() {
        currentPlayer.playerScorePlusOne();
        if (currentPlayer.getPlayerScore() >= menuView.getMaxVictoryPoints()) {
            if (isGamePaused()) return;
            Player winner = currentPlayer;  // <- Freeze the winning player here
            endOfGameWinnerPopup(winner);       // <- pass it down
        }
    }

    // For Longest Road and Biggest Army
    public void decreasePlayerScoreByTwo(Player player) {
        player.playerScoreMinusOne();
        player.playerScoreMinusOne();
    }

    // For Longest Road and Biggest Army
    public void increasePlayerScoreByTwo(Player player) {
        player.playerScorePlusOne();
        player.playerScorePlusOne();
        if (player.getPlayerScore() >= menuView.getMaxVictoryPoints()) {
            if (isGamePaused()) return;
            endOfGameWinnerPopup(player);
        }
    }

    //___________________________HELPER FUNCTIONS_____________________________//

    public int getTotalSelectedCards(Map<String, Integer> selection) {
        return selection.values().stream().mapToInt(Integer::intValue).sum();
    }

    private void endOfGameWinnerPopup(Player winner) {
        if (gameOver) return;
        gameOver = true;

        Platform.runLater(() -> {
            System.out.println("FINAL TRADES WITH BANK IN THIS GAME: " + tradeCounter);
            Stage popup = new Stage();
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.initStyle(StageStyle.UNDECORATED);
            popup.setTitle("Game Over");

            VBox content = new VBox(15);
            content.setPadding(new Insets(15));
            content.setAlignment(Pos.TOP_CENTER);
            content.setStyle("""
            -fx-background-color: linear-gradient(to bottom, #f9ecd1, #d2a86e);
            -fx-border-color: #8c5b1a;
            -fx-border-width: 2;
            -fx-border-radius: 10;
            -fx-background-radius: 10;
        """);

            Label header = new Label("🏆 Player " + winner.getPlayerId() + " has won the game! (It took them " + turnCounter + " turns)");
            header.setFont(Font.font("Georgia", FontWeight.BOLD, 18));
            header.setTextFill(Color.DARKGREEN);

            VBox playerStats = new VBox(12);
            playerStats.setPadding(new Insets(10));

            // Sort by score, descending
            List<Player> sortedPlayers = playerList.stream()
                    .sorted((a, b) -> Integer.compare(b.getPlayerScore(), a.getPlayerScore()))
                    .toList();

            for (Player player : sortedPlayers) {
                VBox box = new VBox(6);
                box.setPadding(new Insets(10));
                box.setStyle("""
                -fx-background-color: linear-gradient(to bottom, #f3e2c7, #e0b97d);
                -fx-border-color: #a86c1f;
                -fx-border-width: 1.5;
                -fx-background-radius: 8;
                -fx-border-radius: 8;
            """);

                String displayName = (player instanceof AIOpponent ai)
                        ? "AI Player " + ai.getPlayerId() + " (" + ai.getStrategyLevel().name() + ")"
                        : "Player " + player.getPlayerId();

                Text name = new Text(displayName + " : " + player.getPlayerScore() + " points");
                name.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
                name.setFill(player.getColor());
                box.getChildren().add(name);

                // Base stats
                int resources = player.getResources().values().stream().mapToInt(Integer::intValue).sum();
                int devCards = player.getDevelopmentCards().values().stream().mapToInt(Integer::intValue).sum();

                Text resText = new Text("Current Resources: " + resources);
                Text devText = new Text("Current Development Cards: " + devCards);
                Text lroadText = new Text("Longest Road: " + player.getLongestRoad());
                Text knightText = new Text("Biggest Army: " + player.getPlayedKnights());

                Text cityText = new Text("Cities: " + player.getCities().size());
                Text settlementText = new Text("Settlements: " + player.getSettlements().size());
                Text roadText = new Text("Roads: " + player.getRoads().size());

                List<Text> baseStats = List.of(
                        resText, devText, lroadText, knightText,
                        cityText, settlementText, roadText
                );
                baseStats.forEach(stat -> stat.setFont(Font.font("Georgia", 12)));
                box.getChildren().addAll(baseStats);

                if (player instanceof AIOpponent ai) {
                    VBox strategyBox = new VBox(4);
                    for (Map.Entry<AIOpponent.Strategy, Integer> entry : ai.getStrategyUsageMap().entrySet()) {
                        Text stat = new Text("• " + entry.getKey().name() + ": " + entry.getValue() + " times");
                        stat.setFont(Font.font("Georgia", 12));
                        strategyBox.getChildren().add(stat);
                    }

                    TitledPane togglePane = new TitledPane("Strategy Usage", strategyBox);
                    togglePane.setExpanded(false);
                    togglePane.setFont(Font.font("Georgia", FontWeight.NORMAL, 12));
                    box.getChildren().add(togglePane);
                }

                playerStats.getChildren().add(box);
            }

            ScrollPane scrollPane = new ScrollPane(playerStats);
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefViewportHeight(500);
            scrollPane.setStyle("""
            -fx-background: transparent;
            -fx-border-color: #a86c1f;
            -fx-border-radius: 8;
        """);

            Button closeBtn = new Button("Back to Main Menu");
            closeBtn.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
            closeBtn.setStyle("""
            -fx-background-color: linear-gradient(to bottom, #d8b173, #a86c1f);
            -fx-text-fill: black;
        """);
            closeBtn.setOnAction(e -> {
                popup.close();
                menuView.showMainMenu();
            });

            content.getChildren().addAll(header, scrollPane, closeBtn);
            Scene scene = new Scene(content, menuView.getGAME_WIDTH(), menuView.getGAME_HEIGHT());
            popup.setScene(scene);
            popup.show();
        });
    }

    public boolean isActionBlockedByDevelopmentCard() {
        return developmentCard.isPlayingCard();
    }

    //__________________________SETTERS________________________//

    public void setBoard(Board board) {
        this.board = board;
    }

    public void setCatanBoardGameView(CatanBoardGameView view) {
        this.catanBoardGameView = view;
    }

    public void setMenuView(MenuView menuView) {
        this.menuView = menuView;
    }

    public void setHasRolledThisTurn(boolean b) {
        hasRolledThisTurn = b;
    }
    public void resetCounters() {
        turnCounter=0;
        drawOrDisplay.resetCounters();
        tradeCounter=0;
    }

    public BiggestArmyManager getBiggestArmy() {
        return biggestArmy;
    }

    public Player getCurrentBiggestArmyHolder() {
        return biggestArmy.getCurrentHolder();
    }

    public LongestRoadManager getLongestRoadManager() {
        return longestRoadManager;
    }

    //__________________________GETTERS________________________//
    public GameController getGameController() {
        return gameController;
    }
    public DrawOrDisplay getDrawOrDisplay() {
        return drawOrDisplay;
    }

    public void setDrawOrDisplay(DrawOrDisplay drawOrDisplay) {
        this.drawOrDisplay = drawOrDisplay;
    }

    public boolean isRobberMoveRequired() {
        return isRobberMoveRequired;
    }

    public void setRobberMoveRequired(boolean required) {
        isRobberMoveRequired = required;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public List<Player> getPlayerList() {
        return playerList;
    }

    public int getLastRolledDie1() {
        return lastRolledDie1;
    }

    public int getLastRolledDie2() {
        return lastRolledDie2;
    }

    public boolean isInInitialPhase() {
        return initialPhase;
    }

    public boolean hasRolledDice() {
        return hasRolledThisTurn;
    }

    public boolean isWaitingForInitialRoad() {
        return waitingForInitialRoad;
    }

    public MenuView getMenuView() {
        return menuView;
    }

    public int getBoardRadius() {
        return boardRadius;
    }

    public Board getBoard() {
        return board;
    }

    public CatanBoardGameView getCatanBoardGameView() {
        return catanBoardGameView;
    }

    public List<DevelopmentCard.DevelopmentCardType> getShuffledDevelopmentCards() {
        return shuffledDevelopmentCards;
    }

    public boolean isGamePaused() {
        return gamePaused;
    }

    public void pauseGame() {
        if (!gamePaused) {
            this.drawOrDisplay.pauseThinkingAnimation(this.drawOrDisplay);
            catanBoardGameView.logToGameLog("Game paused.");
            gamePaused = true;
            stopAllAIThreads();  // interrupt AI thread cleanly
        }
    }

    public void resumeGame() {
        if (!gamePaused) return; // prevent spamming or double-starting
        this.drawOrDisplay.resumeThinkingAnimation(this.drawOrDisplay);
        catanBoardGameView.logToGameLog("Game resumed.");
        gamePaused = false;
        if (currentPlayer instanceof AIOpponent ai) {
            startAIThread(ai);
        }
    }

    public boolean isGameOver() {
        return playerList.stream().anyMatch(p -> p.getPlayerScore() >= menuView.getMaxVictoryPoints());
    }

    public boolean isBlockedByAITurn() {
        if (gameController.getGameplay().getCurrentPlayer() instanceof AIOpponent) {
            drawOrDisplay.showAITurnPopup();
            return true;
        }
        return false;
    }

    public boolean hasHumanPlayers() {
        return playerList.stream().anyMatch(p -> !(p instanceof AIOpponent));
    }


    public void increaseTradeCounter() {
        tradeCounter++;
    }

    public int getTradeCounter() {
        return tradeCounter;
    }
    public void resetTradeCounter() {
        tradeCounter = 0;
    }

}