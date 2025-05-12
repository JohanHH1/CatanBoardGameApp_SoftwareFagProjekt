package org.example.catanboardgameapp;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.control.Alert;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.example.catanboardgameviews.CatanBoardGameView;
import java.util.*;
import org.example.catanboardgameviews.MenuView;

public class Gameplay {

    //__________________________CONFIG & VIEWS_____________________________//
    private final int boardRadius;
    private final DrawOrDisplay drawOrDisplay;
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

    //__________________________BOARD & GAME DATA_____________________________//
    private Board board;
    private Vertex lastInitialSettlement = null;

    //__________________________DICE ROLL TRACKING_____________________________//
    private int lastRolledDie1;
    private int lastRolledDie2;


    //__________________________CONSTRUCTOR_____________________________//
    public Gameplay(Stage primaryStage, int boardRadius) {
        this.drawOrDisplay = new DrawOrDisplay(boardRadius);
        this.boardRadius = boardRadius;
    }

    //________________________INITIALIZE_______________________________//

    // Initialize players and any chosen AI players
    public void initializeAllPlayers(int humanCount, int aiEasy, int aiMedium, int aiHard) {
        playerList.clear();
        List<Color> colors = new ArrayList<>(List.of(
                Color.RED, Color.BLUE, Color.GREEN, Color.DARKORANGE, Color.PURPLE, Color.YELLOW
        ));

        int idCounter = 1;

        for (int i = 0; i < humanCount && !colors.isEmpty(); i++) {
            playerList.add(new Player(idCounter++, colors.remove(0)));
        }
        for (int i = 0; i < aiEasy && !colors.isEmpty(); i++) {
            playerList.add(new AIOpponent(idCounter++, colors.remove(0), AIOpponent.StrategyLevel.EASY, this));
        }
        for (int i = 0; i < aiMedium && !colors.isEmpty(); i++) {
            playerList.add(new AIOpponent(idCounter++, colors.remove(0), AIOpponent.StrategyLevel.MEDIUM, this));
        }
        for (int i = 0; i < aiHard && !colors.isEmpty(); i++) {
            playerList.add(new AIOpponent(idCounter++, colors.remove(0), AIOpponent.StrategyLevel.HARD, this));
        }

        if (!playerList.isEmpty()) {
            currentPlayerIndex = 0;
            currentPlayer = playerList.get(0);
        }
    }

    //____________________________TURN MANAGEMENT______________________________//

    public void nextPlayerTurn() {
        hasRolledThisTurn = false;

        // ---------------- INITIAL PHASE (Setup Turns) ----------------
        if (initialPhase && currentPlayer instanceof AIOpponent ai) {
            ai.placeInitialSettlementAndRoad(this, catanBoardGameView.getBoardGroup());

            if (!waitingForInitialRoad && !currentPlayer.getSettlements().isEmpty()) {
                currentPlayerIndex = forwardOrder ? currentPlayerIndex + 1 : currentPlayerIndex - 1;

                if (forwardOrder && currentPlayerIndex >= playerList.size()) {
                    currentPlayerIndex = playerList.size() - 1;
                    forwardOrder = false;
                } else if (!forwardOrder && currentPlayerIndex < 0) {
                    currentPlayerIndex = 0;
                    initialPhase = false;
                    forwardOrder = true;
                }

                waitingForInitialRoad = false;
                lastInitialSettlement = null;
                currentPlayer = playerList.get(currentPlayerIndex);

                if (initialPhase && currentPlayer instanceof AIOpponent nextAI) {
                    nextAI.placeInitialSettlementAndRoad(this, catanBoardGameView.getBoardGroup());
                }
            }
            return;
        }

        // ---------------- PHASE TRANSITION ----------------
        if (initialPhase) {
            currentPlayerIndex = forwardOrder ? currentPlayerIndex + 1 : currentPlayerIndex - 1;

            if (forwardOrder && currentPlayerIndex >= playerList.size()) {
                currentPlayerIndex = playerList.size() - 1;
                forwardOrder = false;
            } else if (!forwardOrder && currentPlayerIndex < 0) {
                currentPlayerIndex = 0;
                initialPhase = false;
                forwardOrder = true;
            }
        } else {
            currentPlayerIndex = (currentPlayerIndex + 1) % playerList.size();
        }
        waitingForInitialRoad = false;
        lastInitialSettlement = null;
        currentPlayer = playerList.get(currentPlayerIndex);

        // fix this for AI...
        if (!initialPhase && currentPlayer instanceof AIOpponent ai) {
            catanBoardGameView.showDiceButton();
            new Thread(() -> ai.makeMoveAI(this)).start();
        } else if (!initialPhase) {
            catanBoardGameView.showDiceButton();
            catanBoardGameView.hideAllVertexClickCircles();
        }
    }

    //_____________________________DICE________________________________//

    public int rollDice() {
        Random rand = new Random();
        lastRolledDie1 = rand.nextInt(6) + 1;
        lastRolledDie2 = rand.nextInt(6) + 1;
        int roll = lastRolledDie1 + lastRolledDie2;
        catanBoardGameView.logToGameLog("Dice rolled: " + lastRolledDie1 + " + " + lastRolledDie2 + " = " + roll);
        return roll;
    }

    public int rollDiceAndDistributeResources() {
        int roll = rollDice();
        if (roll == 7) {
            catanBoardGameView.getRobber().requireRobberMove();
        } else {
            distributeResource(roll);
        }
        return roll;
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

    public boolean tradeWithBank(String give, String receive) {
        if (!canRemoveResource(give, 4)) return false;
        removeResource(give, 4);
        addResource(receive, 1);
        catanBoardGameView.logToGameLog("Traded 4 " + give + " for 1 " + receive);
        return true;
    }

    //_____________________________BUILDING FUNCTIONS____________________________//

    public boolean buildInitialSettlement(Vertex vertex) {
        if (vertex == null || !isValidSettlementPlacement(vertex)) return false;
        if (currentPlayer.getSettlements().contains(vertex)) return false;
        if (initialPhase && waitingForInitialRoad) return false;

        currentPlayer.getSettlements().add(vertex);
        vertex.setOwner(currentPlayer);
        vertex.makeSettlement();
        increasePlayerScore();

        waitingForInitialRoad = true;
        lastInitialSettlement = vertex;

        if (currentPlayer.getSettlements().size() == 2) {
            currentPlayer.setSecondSettlement(vertex);
            for (Tile tile : vertex.getAdjacentTiles()) {
                String type = tile.getResourcetype().getName();
                if (!type.equals("Desert")) {
                    currentPlayer.getResources().merge(type, 1, Integer::sum);
                }
            }
        }
        return true;
    }

    public boolean buildRoad(Edge edge) {
        if (initialPhase && waitingForInitialRoad) {
            if (!edge.isConnectedTo(lastInitialSettlement)) return false;
            if (!isValidRoadPlacement(edge)) return false;
            currentPlayer.getRoads().add(edge);
            waitingForInitialRoad = false;
            lastInitialSettlement = null;
            return true;
        }

        if (initialPhase) return false;

        if (!isValidRoadPlacement(edge)) return false;

        if (currentPlayer.getRoads().isEmpty()) {
            currentPlayer.getRoads().add(edge);
            return true;
        }

        if (canRemoveResource("Brick", 1) && canRemoveResource("Wood", 1)) {
            removeResource("Brick", 1);
            removeResource("Wood", 1);
            currentPlayer.getRoads().add(edge);
            return true;
        }

        return false;
    }

    public boolean buildSettlement(Vertex vertex) {
        if (vertex == null || !isValidSettlementPlacement(vertex)) return false;
        if (currentPlayer.getSettlements().contains(vertex)) return false;

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
            return true;
        }

        return false;
    }

    public boolean buildCity(Vertex vertex) {
        if (isNotValidCityPlacement(vertex)) return false;

        if (canRemoveResource("Ore", 3) && canRemoveResource("Grain", 2)) {
            removeResource("Ore", 3);
            removeResource("Grain", 2);
            currentPlayer.getSettlements().remove(vertex);
            currentPlayer.getCities().add(vertex);
            vertex.setOwner(currentPlayer);
            increasePlayerScore();
            vertex.makeCity();
            System.out.println(currentPlayer.getPlayerScore());
            return true;
        }

        return false;
    }

    //______________________BOOLEANS___________________________//

    public boolean isInitialPlacementDone() {
        int totalRoads = playerList.stream().mapToInt(p -> p.getRoads().size()).sum();
        return totalRoads >= (playerList.size() * 2 - 1);
    }

    public boolean isValidSettlementPlacement(Vertex vertex) {
        if (vertex.hasSettlement()) return false;
        for (Vertex neighbor : vertex.getNeighbors()) {
            if (neighbor.hasSettlement()) return false;
        }
        return true;
    }

    public boolean isNotValidCityPlacement(Vertex vertex) {
        return !(vertex.hasSettlement() && vertex.getOwner() == currentPlayer);
    }

    public boolean isValidRoadPlacement(Edge edge) {
        for (Player player : playerList) {
            if (player.getRoads().contains(edge)) return false;
        }

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

        boolean connectsToSettlement = currentPlayer.getSettlements().contains(edge.getVertex1()) ||
                currentPlayer.getSettlements().contains(edge.getVertex2());

        boolean connectsToRoad = currentPlayer.getRoads().stream().anyMatch(r ->
                r.isConnectedTo(edge.getVertex1()) || r.isConnectedTo(edge.getVertex2()));

        return connectsToSettlement || connectsToRoad;
    }


    //___________________________SCORE MANAGEMENT_____________________________//

    public void increasePlayerScore() {
        currentPlayer.increasePlayerScore();

        if (currentPlayer.getPlayerScore() >= 10) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Game Over");
                alert.setHeaderText("We have a winner!");
                alert.setContentText("Player " + currentPlayer.getPlayerId() + " has won the game!");
                alert.setOnHidden(e -> menuView.showMainMenu());
                alert.show();
            });
        }
    }

    public void decreasePlayerScore() {
        currentPlayer.decreasePlayerScore();
        currentPlayer.decreasePlayerScore();
    }

    //___________________________HELPER FUNCTIONS_____________________________//

    public void rollDiceAndDistribute(Gameplay gameplay, ImageView dice1, ImageView dice2, BorderPane root, Group boardGroup, Board board) {
        int result = gameplay.rollDiceAndDistributeResources();
        int die1 = gameplay.getLastRolledDie1();
        int die2 = gameplay.getLastRolledDie2();

        dice1.setImage(drawOrDisplay.loadDiceImage(die1));
        dice2.setImage(drawOrDisplay.loadDiceImage(die2));
        dice1.setFitWidth(40);
        dice2.setFitWidth(40);
        dice1.setFitHeight(40);
        dice2.setFitHeight(40);

        if (result == 7) {
            catanBoardGameView.getNextTurnButton().setVisible(false);
            catanBoardGameView.getNextTurnButton().setDisable(true);
            catanBoardGameView.getRobber().showRobberTargets(boardGroup);
        } else {
            catanBoardGameView.getNextTurnButton().setVisible(false);
        }

        catanBoardGameView.refreshSidebar();
        catanBoardGameView.getRollDiceButton().setVisible(false);
    }

    public void distributeResource(int diceRoll) {
        if (diceRoll == 7) return;
        for (Tile tile : board.getTiles()) {
            if (tile.getTileDiceNumber() == diceRoll) {
                for (Vertex vertex : tile.getVertices()) {
                    Player owner = vertex.getOwner();
                    if (owner != null) {
                        String res = tile.getResourcetype().getName();
                        int current = owner.getResources().getOrDefault(res, 0);
                        owner.getResources().put(res, current + (vertex.getTypeOf().equals("City") ? 2 : 1));
                        catanBoardGameView.logToGameLog("Player " + owner.getPlayerId() + " gets " + res);
                    }
                }
            }
        }
    }

    public int getTotalSelectedCards(Map<String, Integer> selection) {
        return selection.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getSettlementDiceValue(Vertex v) {
        int total = 0;
        for (Tile tile : board.getTiles()) {
            if (tile.getVertices().contains(v)) {
                total += getDiceProbabilityValue(tile.getTileDiceNumber());
            }
        }
        return total;
    }

    private int getDiceProbabilityValue(int dice) {
        return switch (dice) {
            case 6, 8 -> 5;
            case 5, 9 -> 4;
            case 4, 10 -> 3;
            case 3, 11 -> 2;
            case 2, 12 -> 1;
            default -> 0;
        };
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

//__________________________GETTERS________________________//

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
        return !hasRolledThisTurn;
    }

    public boolean isWaitingForInitialRoad() {
        return !waitingForInitialRoad;
    }

    public DrawOrDisplay getDrawOrDisplay() {
        return drawOrDisplay;
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
}