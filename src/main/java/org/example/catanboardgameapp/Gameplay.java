package org.example.catanboardgameapp;

import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.*;

public class Gameplay {
private final List<Player> playerList = new ArrayList<>();
private int currentPlayerIndex;
private Player currentPlayer;

    public void initializePlayers (int numberOfPlayers) {
        List<Color> colorList = List.of(Color.RED, Color.GREEN, Color.BLUE, Color.DARKORANGE, Color.PURPLE, Color.YELLOW);

        for (int i = 0; i < numberOfPlayers && i < colorList.size(); i++) {
            Player player = new Player(i+1, colorList.get(i));
            playerList.add(player);
        }
        currentPlayerIndex = 0; // Determines which player starts
        currentPlayer = playerList.get(currentPlayerIndex);
    }

    public void playerTurn () {
        
        int dice = rollDice();
        distributeResource(dice);

        // display dice in catanBoardGameApp
        // roll dice
        // get resources
        // build settelments
        // next player
    }



    public Player getCurrentPlayer() {
        return playerList.get(currentPlayerIndex);

    }
    public void nextPlayerTurn () {
        currentPlayerIndex= (currentPlayerIndex+1) % playerList.size();
        currentPlayer = getCurrentPlayer();
    }


    private boolean isValidSettlementPlacement(Vertex vertex) {
        if (vertex.hasSettlement()) {
            return false; // settlement already in place at the vertex
        }
        // Check if any neighboring vertex already has a settlement
        for (Vertex neighbor : vertex.getNeighbors()) {
            if (neighbor.hasSettlement()) {
                return false; // Settlement too close
            }
        }
        return true;
    }

    // Checking if a road placement is valid
    private boolean isValidRoadPlacement(Edge edge) {
        if (currentPlayer.getRoads().contains(edge)) {
            return false; // Prevent duplicate roads
        }
        // A road must be connected to an existing road or settlement owned by the player
        return currentPlayer.getSettlements().contains(edge.getVertex1()) || currentPlayer.getSettlements().contains(edge.getVertex2()) ||
                currentPlayer.getRoads().stream().anyMatch(existingRoad ->
                        existingRoad.isConnectedTo(edge.getVertex1()) || existingRoad.isConnectedTo(edge.getVertex2()));
    }

    // Add resources
    public void addResource(String resource, int amount) {
        // "getOrDefault" returns the current count of given resource and returns 0
        // if player does not have any of that specific resource type.
        currentPlayer.getResources().put(resource, currentPlayer.getResources().getOrDefault(resource, 0) + amount);
    }

    // check if player has the needed resource amount
    public boolean canRemoveResource(String resource, int amount) {
        // Returns true if player has "amount" or more of "resource"
        return currentPlayer.getResources().getOrDefault(resource, 0) >= amount;
    }

    // Remove resources
    public void removeResource(String resource, int amount) {
        // if enough resources ->
        if (currentPlayer.getResources().getOrDefault(resource, 0) >= amount) {
            // remove those resources and return true
            currentPlayer.getResources().put(resource, currentPlayer.getResources().get(resource) - amount);
        }
    }

    // Build settlement if enough resources and no neighbors/duplicates
    public boolean buildSettlement(Vertex vertex) {
        if (!isValidSettlementPlacement(vertex)) {
            return false; // Invalid placement
        }

        if (currentPlayer.getSettlements().size() < 2) { // allows player to place 2 initial settlements
            currentPlayer.getSettlements().add(vertex);
            vertex.setOwner(currentPlayer);
            addScore();
            return true;
        }
        // Check if player has required resources
        if (canRemoveResource("Brick", 1) && canRemoveResource("Wood", 1) &&
                canRemoveResource("Grain", 1) && canRemoveResource("Wool", 1)) {
            removeResource("Brick",1);
            removeResource("Wood",1);
            removeResource("Grain",1);
            removeResource("Wool",1);
            currentPlayer.getSettlements().add(vertex);
            vertex.setOwner(currentPlayer);
            addScore();
            return true;
        }
        return false;
    }

    // Build road if enough resources and connected to other road/settlement
    public boolean buildRoad(Edge edge) {
        if (!isValidRoadPlacement(edge)) {
            return false; // Invalid placement
        }
        if (currentPlayer.getRoads().size() < 2 ) {
            for (Edge existingRoad : currentPlayer.getRoads()) {
                if (existingRoad.sharesVertexWith(edge)) {
                    return false;
                }
            }
            currentPlayer.getRoads().add(edge);
            return true;
        }
        // Check if player has required resources
        if (canRemoveResource("Brick", 1) && canRemoveResource("Wood", 1)) {
            removeResource("Brick",1);
            removeResource("Wood",1);
            currentPlayer.getRoads().add(edge);
            return true;
        }
        return false;
    }
    public void distributeResource (int diceNumber) {
        List<Tile> tiles = Board.getTiles();
        for (Tile tile : tiles) {
            if (tile.getTileDiceNumber() == diceNumber) {
                for (Vertex vertex : tile.getVertices()) {
                    if (vertex.getOwner() != null) {
                        String resourceType = tile.getResourcetype().getName();
                        Player owner = vertex.getOwner();
                        int currentAmount = owner.getResources().getOrDefault(resourceType, 0);
                        owner.getResources().put(resourceType, currentAmount + 1);
                        System.out.println(owner + "gets " + resourceType + " " + currentAmount);
                    }
                }

            }
        }
    }


    // Update playerScore by adding 1
    public void addScore() {currentPlayer.increasePlayerScore();
    }

    public int rollDice() {
        Random random = new Random();
        int diceRoll= (random.nextInt(6) + 1)+(random.nextInt(6) + 1);
        System.out.println("Dice rolled: " + diceRoll);
        return diceRoll;
    }

    public void decreasePlayerScore(){
            currentPlayer.decreasePlayerScore();
    }

    public List<Player> getPlayerList() {
        return playerList;
    }
}

