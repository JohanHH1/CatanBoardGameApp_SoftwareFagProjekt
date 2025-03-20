package org.example.catanboardgameapp;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import javafx.scene.paint.Color;

public class Player {
    private int playerId;
    private String name;
    private Color color;
    private HashMap<String, Integer> resources;
    private List<Vertex> settlements;
    private List<Edge> roads;
    private int playerScore;

    //_____________________________Constructor_____________________________//
    public Player(int playerId, String playerName, Color color) {
        // Player characterizations
        this.playerId = playerId;
        this.name = playerName;
        this.color = color;

        // Initializing game logic and arrays
        this.resources = new HashMap<>();
        this.settlements = new ArrayList<>();
        this.roads = new ArrayList<>();
        this.playerScore = 0;

        // Initialize each resource count to 0
        resources.put("Brick", 0);
        resources.put("Lumber", 0);
        resources.put("Ore", 0);
        resources.put("Grain", 0);
        resources.put("Wool", 0);
    }

    //_____________________________Functions_____________________________//

    // Checks if a settlement placement is valid
    private boolean isValidSettlementPlacement(Vertex vertex) {
        if (settlements.contains(vertex)) {
            return false; // settlement already in place at the vertex
        }
        // Check if any neighboring vertex already has a settlement
        for (Vertex neighbor : vertex.getNeighbors()) {
            if (settlements.contains(neighbor)) {
                return false; // Settlement too close
            }
        }
        return true;
    }

    // Checking if a road placement is valid
    private boolean isValidRoadPlacement(Edge edge) {
        if (roads.contains(edge)) {
            return false; // Prevent duplicate roads
        }
        // A road must be connected to an existing road or settlement owned by the player
        return settlements.contains(edge.getVertex1()) || settlements.contains(edge.getVertex2()) ||
                roads.stream().anyMatch(existingRoad ->
                        existingRoad.isConnectedTo(edge.getVertex1()) || existingRoad.isConnectedTo(edge.getVertex2()));
    }

    // Add resources
    public void addResource(String resource, int amount) {
        // "getOrDefault" returns the current count of given resource and returns 0
        // if player does not have any of that specific resource type.
        resources.put(resource, resources.getOrDefault(resource, 0) + amount);
    }

    // check if player has the needed resource amount
    public boolean canRemoveResource(String resource, int amount) {
        // Returns true if player has "amount" or more of "resource"
        return resources.getOrDefault(resource, 0) >= amount;
    }

    // Remove resources
    public void removeResource(String resource, int amount) {
        // if enough resources ->
        if (resources.getOrDefault(resource, 0) >= amount) {
            // remove those resources and return true
            resources.put(resource, resources.get(resource) - amount);
        }
    }

    // Build settlement if enough resources and no neighbors/duplicates
    public boolean buildSettlement(Vertex vertex) {
        if (!isValidSettlementPlacement(vertex)) {
            return false; // Invalid placement
        }
        // Check if player has required resources
        if (canRemoveResource("Brick", 1) && canRemoveResource("Lumber", 1) &&
                canRemoveResource("Grain", 1) && canRemoveResource("Wool", 1)) {
            removeResource("Brick",1);
            removeResource("Lumber",1);
            removeResource("Grain",1);
            removeResource("Wool",1);
            settlements.add(vertex);
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
        // Check if player has required resources
        if (canRemoveResource("Brick", 1) && canRemoveResource("Lumber", 1)) {
            removeResource("Brick",1);
            removeResource("Lumber",1);
            roads.add(edge);
            return true;
        }
        return false;
    }

    // Update playerScore by adding 1
    public void addScore() {
        this.playerScore++;
    }

    //_____________________________Getters_____________________________//
    public int getPlayerId() {
        return playerId;
    }

    public String getName() {
        return name;
    }

    public Color getColor() {
        return color;
    }

    public HashMap<String, Integer> getResources() {
        return resources;
    }

    public List<Vertex> getSettlements() {
        return settlements;
    }

    public List<Edge> getRoads() {
        return roads;
    }

    public int getplayerScore() {
        return playerScore;
    }

    //_________________________toString method_________________________________
    @Override
    public String toString() {
        return "Player{" +
                "id=" + playerId +
                ", name='" + name + '\'' +
                ", color=" + color +
                ", playerScore=" + playerScore +
                ", resources=" + resources +
                ", settlements=" + settlements.size() +
                ", roads=" + roads.size() +
                '}';
    }
}