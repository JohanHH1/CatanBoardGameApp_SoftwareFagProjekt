package org.example.catanboardgameapp;

import javafx.scene.paint.Color;
import java.util.*;
import org.example.catanboardgameapp.DevelopmentCard.DevelopmentCardType;

public class Player {

    private static final boolean DEBUG_MODE = true; // Set to false for normal game

    private final int playerId;
    private final Color color;
    private final HashMap<String, Integer> resources;
    private final HashMap<String, Integer> developmentCards;
    private final List<Vertex> settlements;
    private final List<Edge> roads;
    private final List<Vertex> cities;
    private Vertex secondSettlement; // Used to validate the second free road connection
    private int playerScore;

    //_____________________________Constructor_____________________________//
    public Player(int playerId, Color color) {
        this.playerId = playerId;
        this.color = color;

        this.resources = new HashMap<>();
        this.developmentCards = new HashMap<>();
        this.settlements = new ArrayList<>();
        this.roads = new ArrayList<>();
        this.cities = new ArrayList<>();
        this.playerScore = 0;

        initializeResources();
        initializeDevelopmentCards();
    }

    private void initializeResources() {
        List<String> resourceTypes = Arrays.asList("Brick", "Wood", "Ore", "Grain", "Wool");
        for (String resource : resourceTypes) {
            resources.put(resource, DEBUG_MODE ? 10 : 0);
        }
    }

    private void initializeDevelopmentCards() {
        for (DevelopmentCardType cardType : DevelopmentCardType.values()) {
            developmentCards.put(cardType.getName(), DEBUG_MODE ? 2 : 0);
        }
    }

    //_____________________________Functions_____________________________//

    public void increasePlayerScore() {
        playerScore += 1;
    }

    public void decreasePlayerScore() {
        playerScore -= 1;
    }

    public void setSecondSettlement(Vertex secondSettlement) {
        this.secondSettlement = secondSettlement;
    }

    public int getTotalResourceCount() {
        return resources.values().stream().mapToInt(Integer::intValue).sum();
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

    public HashMap<String, Integer> getDevelopmentCards() {
        return developmentCards;
    }

    public List<Vertex> getSettlements() {
        return settlements;
    }

    public List<Edge> getRoads() {
        return roads;
    }

    public List<Vertex> getCities() {
        return cities;
    }

    public int getPlayerScore() {
        return playerScore;
    }

    public Vertex getSecondSettlement() {
        return secondSettlement;
    }

    //_________________________toString method_________________________________//

    @Override
    public String toString() {
        return "Player " + playerId;
    }
}
