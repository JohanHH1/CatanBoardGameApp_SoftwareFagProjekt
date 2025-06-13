package org.example.catanboardgameapp;

import javafx.scene.paint.Color;
import java.util.*;
import org.example.catanboardgameapp.DevelopmentCard.DevelopmentCardType;

public class Player {

    private static final boolean DEBUG_MODE = false; // Set to false for normal game

    private final Gameplay gameplay;
    private int playerId;
    private final Color color;
    // The resources a player has
    private final HashMap<String, Integer> resources;
    // The development cards a player has
    private Map<DevelopmentCardType, Integer> developmentCards = new HashMap<>();
    private final List<Vertex> settlements;
    private final List<Edge> roads;
    private final List<Vertex> cities;
    private Vertex secondSettlement; // Used to validate the second free road connection
    private int playerScore;
    private int playedKnights;
    private int longestRoad;

    //_____________________________Constructor_____________________________//
    public Player(int playerId, Color color, Gameplay gameplay) {
        this.playerId = playerId;
        this.color = color;
        this.gameplay = gameplay;
        this.resources = new HashMap<>();
        this.developmentCards = new HashMap<>();
        this.settlements = new ArrayList<>();
        this.roads = new ArrayList<>();
        this.cities = new ArrayList<>();
        this.playerScore = 0;
        this.playedKnights = 0;
        this.longestRoad = 0;

        initializeResources();
        initializeDevelopmentCards();
    }

    // Initial resources -> 0 of every resource
    private void initializeResources() {
        List<String> resourceTypes = Arrays.asList("Brick", "Wood", "Ore", "Grain", "Wool");
        for (String resource : resourceTypes) {
            resources.put(resource, DEBUG_MODE ? 10 : 0);
        }
    }

    // Initial development cards -> 0 of each
    private void initializeDevelopmentCards() {
        for (DevelopmentCardType cardType : DevelopmentCardType.values()) {
            developmentCards.put(cardType, DEBUG_MODE ? 2 : 0);
        }
    }

    //______________________________Robber Logic____________________________//
    // for AI use and for auto-discard for human players

    public Map<String, Integer> chooseDiscardCards() {
        Map<String, Integer> resources = new HashMap<>(getResources());
        int total = resources.values().stream().mapToInt(Integer::intValue).sum();
        int toDiscard = total / 2;

        if (toDiscard == 0) return null;

        Map<String, Integer> discardMap = new HashMap<>();

        // Simple heuristic: discard from most abundant resource
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(resources.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue())); // highest first

        for (Map.Entry<String, Integer> entry : sorted) {
            if (toDiscard == 0) break;

            String res = entry.getKey();
            int available = entry.getValue();
            int discard = Math.min(available, toDiscard);
            if (discard > 0) {
                discardMap.put(res, discard);
                toDiscard -= discard;
            }
        }

        // Log discards
        if (gameplay != null && gameplay.getCatanBoardGameView() != null) {
            StringBuilder log = new StringBuilder("AI Player " + getPlayerId() + " discarded: ");
            discardMap.forEach((res, amt) -> log.append(amt).append(" ").append(res).append(", "));
            if (!discardMap.isEmpty()) {
                log.setLength(log.length() - 2); // remove trailing comma
                gameplay.getCatanBoardGameView().logToGameLog(log.toString());
            }
        }

        return discardMap;
    }

    //_____________________________Functions_____________________________//

    public void playerScorePlusOne() {
        playerScore += 1;
    }

    public void playerScoreMinusOne() {
        playerScore -= 1;
    }

    public void increasePlayedKnights() { playedKnights += 1; }

    public void setLongestRoad(int longestRoad) { this.longestRoad = longestRoad; }

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

    public Map<DevelopmentCardType, Integer> getDevelopmentCards() {
        return developmentCards;
    }
    public DevelopmentCardType getFirstDevelopmentCard() {
        for (Map.Entry<DevelopmentCard.DevelopmentCardType, Integer> entry : developmentCards.entrySet()) {
            if (entry.getValue() > 0) {
                return entry.getKey(); // Return the first card type without modifying the map
            }
        }
        return null;
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
    public List<Vertex> getSettlementsAndCities() {
        List<Vertex> all = new ArrayList<>();
        all.addAll(settlements);
        all.addAll(cities);
        return all;
    }
    public boolean isBlocked(Vertex vertex, Gameplay gameplay) {
        for (Player player : gameplay.getPlayerList()) {
            if (player != this && player.getSettlementsAndCities().contains(vertex)) {
                return true;
            }
        }
        return false;
    }

    public void setPlayerId(int id) {
        this.playerId = id;
    }
    public int getPlayerScore() { return playerScore; }

    public int getPlayedKnights() { return playedKnights; }

    public int getLongestRoad() { return longestRoad; }

    public Vertex getSecondSettlement() {
        return secondSettlement;
    }

    public boolean hasNoDevelopmentCards() {
        if (developmentCards == null || developmentCards.isEmpty()) {
            return true;
        }
        return developmentCards.values().stream()
                .allMatch(count -> count == 0);
    }


    //_________________________toString method_________________________________//

    @Override
    public String toString() {
        return "Player " + playerId;
    }

}
