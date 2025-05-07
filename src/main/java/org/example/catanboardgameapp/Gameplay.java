package org.example.catanboardgameapp;

import javafx.scene.paint.Color;
import org.example.catanboardgameviews.CatanBoardGameView;

import java.util.ArrayList;
import java.util.*;


public class Gameplay {
private final List<Player> playerList = new ArrayList<>();
private int currentPlayerIndex;
private Player currentPlayer;
private boolean secondFreeSettelment=false;
private Robber robber;
private boolean robberNeedsToMove = false;



    public void initializePlayers (int numberOfPlayers) {
        List<Color> colorList = List.of(Color.RED, Color.GREEN, Color.BLUE, Color.DARKORANGE, Color.PURPLE, Color.YELLOW);

        for (int i = 0; i < numberOfPlayers && i < colorList.size(); i++) {
            Player player = new Player(i+1, colorList.get(i));
            playerList.add(player);
        }
        currentPlayerIndex = 0; // Determines which player starts
        currentPlayer = playerList.get(currentPlayerIndex);
    }

    public Player getCurrentPlayer() {
        return playerList.get(currentPlayerIndex);

    }
    public void nextPlayerTurn() {
        // Determine if we are in the special condition
        int oneCount = 0; // have less than one settlement and one road
        int twoCount = 0; // has less than 2 settlements and 2 roads
        for (Player player : playerList) {
            int settlements = player.getSettlements().size();
            if (settlements == 1) {
                oneCount++;
            } else if (settlements == 2) {
                twoCount++;
            }
        }

        // Special condition: either all players have 1, or some have 1 and the rest have 2.
        boolean specialCondition1 = (oneCount == playerList.size());
        boolean specialCondition2 = (oneCount >= 1 && (oneCount + twoCount == playerList.size()));
        if (specialCondition1 ) {
            currentPlayer = getCurrentPlayer();
            secondFreeSettelment=true;
        }
        else if (specialCondition2) {
            // Count backwards: decrement the index with wrap-around
            currentPlayerIndex = (currentPlayerIndex - 1 + playerList.size()) % playerList.size();
        } else {
            // Normal turn: count forward
            currentPlayerIndex = (currentPlayerIndex + 1) % playerList.size();
        }

        currentPlayer = getCurrentPlayer();
        //CatanBoardGameView.updatePlayerHighlight(currentPlayer, currentPlayerIndex);

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

    private boolean isNotValidCityPlacement(Vertex vertex) {
        if (vertex.hasSettlement() && vertex.getOwner()==currentPlayer) {
            return false; // settlement already in place at the vertex
        }
        return true;
    }

    // Checking if a road placement is valid
    private boolean isValidRoadPlacement(Edge edge) {
        // Rule 1: Can't place a road where one already exists
        for (Player player : playerList) {
            if (player.getRoads().contains(edge)) {
                return false;
            }
        }

        // Rule 2: Can't build through another player's settlement -- needs adjustment
        /*for (Player player : playerList) {
            if (player != currentPlayer) {
                if ((player.getSettlements().contains(edge.getVertex1()) && existingRoad.isConnectedTo(edge.getVertex1())) || player.getSettlements().contains(edge.getVertex2())) {
                    return false;
                }
            }
        }*/
        for (Player player : playerList) {
            if (player != currentPlayer) {
                // Check vertex1
                if (player.getSettlements().contains(edge.getVertex1())) {
                    for (Edge existingRoad : currentPlayer.getRoads()) {
                        if (existingRoad.isConnectedTo(edge.getVertex1())) {
                            return false; // Can't build through another player's settlement
                        }
                    }
                }

                // Check vertex2
                if (player.getSettlements().contains(edge.getVertex2())) {
                    for (Edge existingRoad : currentPlayer.getRoads()) {
                        if (existingRoad.isConnectedTo(edge.getVertex2())) {
                            return false;
                        }
                    }
                }
            }
        }
        // Rule 3: Road must connect to player's settlement or another road
        boolean connectsToSettlement = currentPlayer.getSettlements().contains(edge.getVertex1()) || currentPlayer.getSettlements().contains(edge.getVertex2());

        boolean connectsToRoad = currentPlayer.getRoads().stream().anyMatch(existingRoad ->
                existingRoad.isConnectedTo(edge.getVertex1()) || existingRoad.isConnectedTo(edge.getVertex2())
        );

        return connectsToSettlement || connectsToRoad;
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

        // Special case for initial placement (first two settlements)
        if (currentPlayer.getSettlements().isEmpty()) { // first settlement
            currentPlayer.getSettlements().add(vertex);
            vertex.setOwner(currentPlayer);
            addScore();
            vertex.makeSettlement();
            return true;
        } else if (currentPlayer.getSettlements().size() == 1 && !(currentPlayer.getRoads().isEmpty())) { // second settlement
            currentPlayer.getSettlements().add(vertex);
            vertex.setOwner(currentPlayer);
            addScore();
            for (Tile tile : vertex.getAdjacentTiles()){
                if (!tile.getResourcetype().getName().equals("Desert")){
                int currentAmount = currentPlayer.getResources().getOrDefault(tile.getResourcetype().getName(), 0);
                currentPlayer.getResources().put(tile.getResourcetype().getName(),currentAmount  + 1);
            }}
            currentPlayer.setSecondSettlement(vertex);
            vertex.makeSettlement();
            return true;
        }

        // Normal settlement building (after initial placement)
        // Check if player has required resources
        if (canRemoveResource("Brick", 1) && canRemoveResource("Wood", 1) &&
                canRemoveResource("Grain", 1) && canRemoveResource("Wool", 1)) {
            removeResource("Brick", 1);
            removeResource("Wood", 1);
            removeResource("Grain", 1);
            removeResource("Wool", 1);
            currentPlayer.getSettlements().add(vertex);
            vertex.setOwner(currentPlayer);
            addScore();
            vertex.makeSettlement();
            return true;
        }
        return false;
    }

    // Upgrade to city
    public boolean buildCity(Vertex vertex) {
        if (isNotValidCityPlacement(vertex)) {
            return false; // Invalid placement
        }
        // Normal city upgrade
        // Check if player has required resources
        if (canRemoveResource("Ore", 3) && canRemoveResource("Grain", 2)) {
            removeResource("Ore", 3);
            removeResource("Grain", 2);
            currentPlayer.getSettlements().remove(vertex);
            currentPlayer.getCities().add(vertex);
            vertex.setOwner(currentPlayer);
            addScore();
            vertex.makeCity();
            System.out.println(currentPlayer.getplayerScore());
            return true;
        }
        return false;
    }

    // Build road if enough resources and connected to other road/settlement
    public boolean buildRoad(Edge edge) {
        if (!isValidRoadPlacement(edge)) {
            return false; // Invalid placement
        }
        // Special case for initial placement (first two roads)
        if (currentPlayer.getRoads().isEmpty()) {
            currentPlayer.getRoads().add(edge); // initial road added. Can only be added to the first settlement
            return true;
        }  else if ( currentPlayer.getRoads().size() == 1) { // second free road // Only allow the second road if it's connected to the second settlement
        Vertex secondSettlement = currentPlayer.getSecondSettlement();
        if (!edge.isConnectedTo(secondSettlement)) {
            return false;
        }
        currentPlayer.getRoads().add(edge);

        if (currentPlayer.getPlayerId() == (1) && currentPlayer.getRoads().size() == 2) {
            CatanBoardGameView.showDiceButton();
            System.out.println("is in here");
        }
        return true;

        } else { // Placing rest of the roads
        // Normal road building (after initial placement)
        // Check if player has required resources

        if (canRemoveResource("Brick", 1) && canRemoveResource("Wood", 1)) {
            removeResource("Brick", 1);
            removeResource("Wood", 1);
            currentPlayer.getRoads().add(edge);
            return true;
        }
        return false;
    }}
    public void distributeResource (int diceNumber) {
        List<Tile> tiles = Board.getTiles();
        if (diceNumber == 7){return;
        }else {
            for (Tile tile : tiles) {
                if (tile.getTileDiceNumber() == diceNumber) {
                    for (Vertex vertex : tile.getVertices()) {
                        if (vertex.getOwner() != null) {
                            String resourceType = tile.getResourcetype().getName();
                            Player owner = vertex.getOwner();
                            int currentAmount = owner.getResources().getOrDefault(resourceType, 0);
                            System.out.println(vertex.hasCity() +" is here");
                            if (vertex.getTypeOf().equals("City")) {
                                System.out.println("a city has been rolled");
                                owner.getResources().put(resourceType, currentAmount + 2);
                            } else {
                                System.out.println("a settlement has been rolled");
                                owner.getResources().put(resourceType, currentAmount + 1);
                            }
                            System.out.println(owner + "gets " + resourceType + " " + currentAmount);
                        }
                    }

                }
            }
        }
    }

    public boolean tradeWithBank(String giveResource, String receiveResource) {
        if (!canRemoveResource(giveResource, 4)) {
            return false;
        }

        removeResource(giveResource,4);
        addResource(receiveResource, 1);
        System.out.println("traded 4" + giveResource + " for 1 " + receiveResource);
        return true;
    }


// Update playerScore by adding 1
    public void addScore() {
        currentPlayer.increasePlayerScore();
        if (currentPlayer.getSettlements().size() >= 10) {
            System.out.print("player" + currentPlayer.getPlayerId() + "is the winner");
        }
    }

    public int rollDice() {
        Random random = new Random();
        int diceRoll= (random.nextInt(6) + 1)+(random.nextInt(6) + 1);
        System.out.println("Dice rolled: " + diceRoll);
        return diceRoll;
    }

    public void reset() {
        playerList.clear();            // clear all players
        currentPlayerIndex = 0;        // reset turn counter
        currentPlayer = null;          // reset player reference
    }


    public void decreasePlayerScore(){
            currentPlayer.decreasePlayerScore();
    }

    public List<Player> getPlayerList() {
        return playerList;
    }

    public boolean stealResourceFrom(Player victim) {
        Map<String, Integer> victimResources = victim.getResources();
        List<String> availableResources = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : victimResources.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                availableResources.add(entry.getKey());
            }
        }

        if (availableResources.isEmpty()) {
            return false;
        }
        Collections.shuffle(availableResources);
        String stolenResource = availableResources.get(0);

        //transfer resource
        victimResources.put(stolenResource, victimResources.get(stolenResource) - 1);
        getCurrentPlayer().getResources().put(stolenResource, getCurrentPlayer().getResources().getOrDefault(stolenResource, 0) + 1);
        System.out.println("Player " + getCurrentPlayer().getPlayerId() + " stole 1 " + stolenResource + " from Player " + victim.getPlayerId());
        return true;
    }

    public void initializeRobber(Tile desertTile) {
        this.robber = new Robber(desertTile);
    }

    public void setRobber(Robber robber) {
        this.robber = robber;
    }

    public Robber getRobber() {
        return robber;
    }
    public void requireRobberMove() {
        robberNeedsToMove = true;
    }
    public void robberHasMoved() {
        robberNeedsToMove = false;
    }
    public boolean isRobberMovementRequired() {
        return robberNeedsToMove;
    }
}

