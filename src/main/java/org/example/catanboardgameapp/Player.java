package org.example.catanboardgameapp;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import javafx.scene.paint.Color;

public class Player {
    private int playerId;
    private Color color;
    private HashMap<String, Integer> resources;
    private List<Vertex> settlements;
    private List<Edge> roads;
    private int playerScore;
    private List<Vertex> cities;
    private Vertex secondSettlment;

    //_____________________________Constructor_____________________________//
    public Player(int playerId, Color color) {
        // Player characterizations
        this.playerId = playerId;
        this.color = color;

        // Initializing game logic and arrays
        this.resources = new HashMap<>();
        this.settlements = new ArrayList<>();
        this.roads = new ArrayList<>();
        this.playerScore = 0;
        this.cities = new ArrayList<>();

        // Initialize each resource count to 0
        resources.put("Brick", 0);
        resources.put("Wood", 0);
        resources.put("Ore", 0);
        resources.put("Grain", 0);
        resources.put("Wool", 0);
    }

    //_____________________________Functions_____________________________//

    // Checks if a settlement placement is valid


    public void increasePlayerScore() {
        playerScore += 1;
    }
    public void decreasePlayerScore() { playerScore -= 1; }

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

    public int getplayerScore() {
        return playerScore;
    }
    public void setSecondSettlement(Vertex secondSettlement) {
        this.secondSettlment = secondSettlement;
    }
    public Vertex getSecondSettlement() {return secondSettlment;}

    //_________________________toString method_________________________________
    @Override
    public String toString() {
        return "Player " + playerId;
                /*"id=" + playerId +
                ", color=" + color +
                ", playerScore=" + playerScore +
                ", resources=" + resources +
                ", settlements=" + settlements.size() +
                ", roads=" + roads.size() +
                '}';*/
    }

}