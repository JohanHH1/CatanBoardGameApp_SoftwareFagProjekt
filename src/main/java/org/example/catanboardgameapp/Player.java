package org.example.catanboardgameapp;

import javafx.scene.paint.Color;
import java.util.*;
import org.example.catanboardgameapp.DevelopmentCard.DevelopmentCardType;

public class Player {

    private static final boolean DEBUG_MODE = true; // Set to false for normal game

    private final Gameplay gameplay;
    private final int playerId;
    private final Color color;
    private final HashMap<String, Integer> resources;
    private Map<DevelopmentCardType, Integer> developmentCards = new HashMap<>();
    private final List<Vertex> settlements;
    private final List<Edge> roads;
    private final List<Vertex> cities;
    private Vertex secondSettlement; // Used to validate the second free road connection
    private int playerScore;
    private int playedKnights;

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
            developmentCards.put(cardType, DEBUG_MODE ? 2 : 0);
        }
    }
    //______________________________ROBBER LOGIC____________________________//
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

    public void increasePlayerScore() {
        playerScore += 1;
    }

    public void decreasePlayerScore() {
        playerScore -= 1;
    }

    public void increasePlayedKnights() { playedKnights += 1; }

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

    public List<Vertex> getSettlements() {
        return settlements;
    }

    public List<Edge> getRoads() {
        return roads;
    }

    public List<Vertex> getCities() {
        return cities;
    }

    public int getPlayerScore() { return playerScore; }

    public int getPlayedKnights() { return playedKnights; }

    public Vertex getSecondSettlement() {
        return secondSettlement;
    }

    //_________________________toString method_________________________________//

    @Override
    public String toString() {
        return "Player " + playerId;
    }
}
