package org.example.catanboardgameapp;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import javafx.scene.paint.Color;

public class Player {
    private final int playerId;
    private final Color color;
    private final HashMap<String, Integer> resources;
    private final List<Vertex> settlements;
    private final List<Edge> roads;
    private int playerScore;
    private final List<Vertex> cities;
    private Vertex secondSettlement; // Used to validate the second free road connection

    //_____________________________Constructor_____________________________//
    public Player(int playerId, Color color) {
        // Player characterizations
        this.playerId = playerId;
        this.color = color;

        // Initializing game logic and arrays
        this.resources = new HashMap<>();
        this.settlements = new ArrayList<>();
        this.roads = new ArrayList<>();
        this.cities = new ArrayList<>();
        this.playerScore = 0;

        // Initialize each resource count to 0
        resources.put("Brick", 0);
        resources.put("Wood", 0);
        resources.put("Ore", 0);
        resources.put("Grain", 0);
        resources.put("Wool", 0);
    }

    //_____________________________Functions_____________________________//

    // Increase the player's score by 1
    public void increasePlayerScore() {
        playerScore += 1;
    }

    // Decrease the player's score by 1
    public void decreasePlayerScore() {
        playerScore -= 1;
    }

    // Store the second settlement to validate road placement
    public void setSecondSettlement(Vertex secondSettlement) {
        this.secondSettlement = secondSettlement;
    }

    //_____________________________Getters_____________________________//

    public int getPlayerId() {
        return playerId;
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

    public List<Vertex> getCities() {
        return cities;
    }

    public List<Edge> getRoads() {
        return roads;
    }

    public int getPlayerScore() {
        return playerScore;
    }

    public Vertex getSecondSettlement() {
        return secondSettlement;
    }

    //_________________________toString method_________________________________

    @Override
    public String toString() {
        return "Player " + playerId;
        /*
        return "Player{" +
                "id=" + playerId +
                ", color=" + color +
                ", playerScore=" + playerScore +
                ", resources=" + resources +
                ", settlements=" + settlements.size() +
                ", roads=" + roads.size() +
                '}';
        */
    }
}