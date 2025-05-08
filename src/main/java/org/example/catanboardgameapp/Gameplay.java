package org.example.catanboardgameapp;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.example.catanboardgameviews.CatanBoardGameView;

import java.util.*;

import static org.example.catanboardgameapp.Robber.robberDeNiro;

public class Gameplay {
    private final List<Player> playerList = new ArrayList<>();
    private int currentPlayerIndex;
    private Player currentPlayer;
    private boolean initialPhase = true;
    private boolean forwardOrder = true;
    private Board board;
    private int lastRolledDie1;
    private int lastRolledDie2;

    // -------------------- Initialization --------------------

    public void setBoard(Board board) {
        this.board = board;
    }

    public Board getBoard() {
        return board;
    }

    public void initializePlayers(int numberOfPlayers) {
        List<Color> colors = List.of(Color.RED, Color.GREEN, Color.BLUE, Color.DARKORANGE, Color.PURPLE, Color.YELLOW);
        for (int i = 0; i < numberOfPlayers && i < colors.size(); i++) {
            playerList.add(new Player(i + 1, colors.get(i)));
        }
        currentPlayerIndex = 0;
        currentPlayer = playerList.get(currentPlayerIndex);
    }

    public void initializeAis(int amountOfAi) {
        List<Color> colors = new ArrayList<>(List.of(Color.RED, Color.GREEN, Color.BLUE, Color.DARKORANGE, Color.PURPLE, Color.YELLOW));
        for (int i = 0; i < playerList.size(); i++) {
            if (!colors.isEmpty()) colors.remove(0);
        }
        for (int i = 0; i < amountOfAi && i < colors.size(); i++) {
            int id = playerList.size() + 1;
            playerList.add(new AIOpponent(id, colors.get(i), AIOpponent.StrategyLevel.EASY));
        }
        if (currentPlayer == null && !playerList.isEmpty()) {
            currentPlayerIndex = 0;
            currentPlayer = playerList.get(currentPlayerIndex);
        }
    }

    public void reset() {
        playerList.clear();
        currentPlayerIndex = 0;
        currentPlayer = null;
    }

    // -------------------- Turn Management --------------------

    public void nextPlayerTurn() {
        // Handle AI initial placement (and visual drawing)
        if (initialPhase && currentPlayer instanceof AIOpponent ai) {
            ai.placeInitialSettlementAndRoad(this, CatanBoardGameView.getBoardGroup());

            // Immediately move to next player if AI placed both settlement and road
            if (currentPlayer.getSettlements().size() == 2 && currentPlayer.getRoads().size() == 2) {
                currentPlayerIndex = forwardOrder ? currentPlayerIndex + 1 : currentPlayerIndex - 1;

                if (forwardOrder && currentPlayerIndex >= playerList.size()) {
                    currentPlayerIndex = playerList.size() - 1;
                    forwardOrder = false;
                } else if (!forwardOrder && currentPlayerIndex < 0) {
                    currentPlayerIndex = 0;
                    initialPhase = false;
                    forwardOrder = true;
                }

                currentPlayer = playerList.get(currentPlayerIndex);

                // Handle next AI if in initial phase
                if (initialPhase && currentPlayer instanceof AIOpponent nextAi) {
                    nextAi.placeInitialSettlementAndRoad(this, CatanBoardGameView.getBoardGroup());
                }

                return;
            }
        }

        // Regular turn handling
        if (initialPhase) {
            if (forwardOrder) {
                currentPlayerIndex++;
                if (currentPlayerIndex >= playerList.size()) {
                    currentPlayerIndex = playerList.size() - 1;
                    forwardOrder = false;
                }
            } else {
                currentPlayerIndex--;
                if (currentPlayerIndex < 0) {
                    currentPlayerIndex = 0;
                    initialPhase = false;
                    forwardOrder = true;
                }
            }
        } else {
            currentPlayerIndex = (currentPlayerIndex + 1) % playerList.size();
        }

        currentPlayer = playerList.get(currentPlayerIndex);

        if (!initialPhase && currentPlayer instanceof AIOpponent ai) {
            new Thread(() -> ai.makeMoveAI(this)).start();
        }
    }




    public String getCurrentPhaseName() {
        return initialPhase ? "INITIAL PHASE" : "REGULAR PHASE";
    }

    // -------------------- Dice --------------------

    public int rollDice() {
        Random rand = new Random();
        lastRolledDie1 = rand.nextInt(6) + 1;
        lastRolledDie2 = rand.nextInt(6) + 1;
        int roll = lastRolledDie1 + lastRolledDie2;
        System.out.println("Dice rolled: " + lastRolledDie1 + " + " + lastRolledDie2 + " = " + roll);
        return roll;
    }

    public int rollDiceAndDistributeResources() {
        int roll = rollDice();
        if (roll == 7) {
            robberDeNiro.requireRobberMove();
        } else {
            distributeResource(roll);
        }
        return roll;
    }

    // -------------------- Resource add/remove / Trading --------------------

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
        System.out.println("Traded 4 " + give + " for 1 " + receive);
        return true;
    }

    // -------------------- Building --------------------

    public boolean buildInitialSettlement(Vertex vertex) {
        // Prevent invalid or duplicate placement
        if (vertex == null || !isValidSettlementPlacement(vertex)) return false;
        if (currentPlayer.getSettlements().contains(vertex)) return false;

        // Add the settlement
        currentPlayer.getSettlements().add(vertex);
        vertex.setOwner(currentPlayer);
        vertex.makeSettlement();
        increasePlayerScore();

        // Store this settlement as the "second" for AI/road connection logic
        if (currentPlayer.getSettlements().size() == 2) {
            currentPlayer.setSecondSettlement(vertex);

            // Grant starting resources from adjacent tiles (excluding desert)
            for (Tile tile : vertex.getAdjacentTiles()) {
                String type = tile.getResourcetype().getName();
                if (!type.equals("Desert")) {
                    currentPlayer.getResources().merge(type, 1, Integer::sum);
                }
            }
        }

        return true;
    }

    public boolean buildSettlement(Vertex vertex) {
        // Validate and check duplicates
        if (vertex == null || !isValidSettlementPlacement(vertex)) return false;
        if (currentPlayer.getSettlements().contains(vertex)) return false;

        // Check and pay resources
        if (canRemoveResource("Brick", 1) &&
                canRemoveResource("Wood", 1) &&
                canRemoveResource("Grain", 1) &&
                canRemoveResource("Wool", 1)) {

            removeResource("Brick", 1);
            removeResource("Wood", 1);
            removeResource("Grain", 1);
            removeResource("Wool", 1);

            // Place settlement
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

    public boolean buildRoad(Edge edge) {
        if (!isValidRoadPlacement(edge)) return false;

        if (currentPlayer.getRoads().isEmpty()) {
            currentPlayer.getRoads().add(edge);
            return true;
        }

        if (currentPlayer.getRoads().size() == 1) {
            Vertex second = currentPlayer.getSecondSettlement();
            if (!edge.isConnectedTo(second)) return false;
            currentPlayer.getRoads().add(edge);
            if (currentPlayer.getPlayerId() == 1 && currentPlayer.getRoads().size() == 2) {
                CatanBoardGameView.showDiceButton();
            }
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

    // -------------------- Validation --------------------

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
        boolean connectsToRoad = currentPlayer.getRoads().stream().anyMatch(r -> r.isConnectedTo(edge.getVertex1()) || r.isConnectedTo(edge.getVertex2()));
        return connectsToSettlement || connectsToRoad;
    }

    // -------------------- Resource Distribution --------------------
    public void distributeResource(int diceRoll) {
        if (diceRoll == 7) return;
        for (Tile tile : Board.getTiles()) {
            if (tile.getTileDiceNumber() == diceRoll) {
                for (Vertex vertex : tile.getVertices()) {
                    Player owner = vertex.getOwner();
                    if (owner != null) {
                        String res = tile.getResourcetype().getName();
                        int current = owner.getResources().getOrDefault(res, 0);
                        // GIVE 100 RESOURCES SO PLAYER CAN WIN THE GAME FOR TESTING (CHANGE TO 1 FOR ACTUAL GAME)
                        owner.getResources().put(res, current + (vertex.getTypeOf().equals("City") ? 2 : 100));
                        System.out.println("Player " + owner.getPlayerId() + " gets " + res);
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
        for (Tile tile : Board.getTiles()) {
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

    // -------------------- Score Management --------------------

    public void increasePlayerScore() {
        currentPlayer.increasePlayerScore();

        if (currentPlayer.getPlayerScore() >= 10) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Game Over");
                alert.setHeaderText("We have a winner!");
                alert.setContentText("Player " + currentPlayer.getPlayerId() + " has won the game!");
                alert.setOnHidden(e -> CatanBoardGameView.returnToMainMenu());
                alert.show();
            });
        }
    }


    public void decreasePlayerScore() {
        currentPlayer.decreasePlayerScore();
    }

    // -------------------- Getters --------------------

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

}
