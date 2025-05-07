package org.example.catanboardgameapp;

import javafx.scene.paint.Color;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class AIOpponent extends Player {

    public enum StrategyLevel { EASY, MEDIUM, HARD }

    private StrategyLevel strategyLevel;
    private Random random = new Random();

    public AIOpponent(int playerId, Color color, StrategyLevel level) {
        super(playerId, color);
        this.strategyLevel = level;
    }

    // Main method to make AI move depending on difficulty level
    public void makeMoveAI(Gameplay gameplay) {
        pauseBeforeMove(); // Add delay for realism

        switch (strategyLevel) {
            case EASY -> makeEasyLevelMove(gameplay);
            case MEDIUM -> makeMediumLevelMove(gameplay);
            case HARD -> makeHardLevelMove(gameplay);
        }
    }

    // Pauses the AI move 3 to 10 seconds to simulate real player timing
    private void pauseBeforeMove() {
        try {
            int delay = ThreadLocalRandom.current().nextInt(3000, 10000); // 3 to 10 seconds
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Easy AI: performs almost random but legal actions
    private void makeEasyLevelMove(Gameplay gameplay) {
        tryBuildCity(gameplay);
        tryBuildSettlement(gameplay);
        tryBuildRoad(gameplay);
    }

    // Medium AI: adds smarter decision making
    private void makeMediumLevelMove(Gameplay gameplay) {
        tryBuildCity(gameplay);
        tryBuildBestSettlement(gameplay); // Chooses best based on dice value
        tryBuildRoad(gameplay);
    }

    // HARD AI: adds even smarter decision making
    private void makeHardLevelMove(Gameplay gameplay) {
        tryBankTrade(gameplay);
        tryBuildCity(gameplay);
        tryBuildBestSettlement(gameplay); // Chooses best based on dice value
        tryBuildRoad(gameplay);
    }

    // Tries to build a city on a random owned settlement
    private void tryBuildCity(Gameplay gameplay) {
        if (hasResources("Ore", 3) && hasResources("Grain", 2)) {
            List<Vertex> settlements = getSettlements();
            if (!settlements.isEmpty()) {
                gameplay.buildCity(settlements.get(random.nextInt(settlements.size())));
            }
        }
    }

    // Tries to build a settlement on a valid and random spot (EASY level strategy)
    private void tryBuildSettlement(Gameplay gameplay) {
        if (!hasResources("Brick", 1) || !hasResources("Wood", 1)
                || !hasResources("Wool", 1) || !hasResources("Grain", 1)) return;

        List<Vertex> validSpots = getValidSettlementSpots(gameplay);
        if (!validSpots.isEmpty()) {
            Collections.shuffle(validSpots);
            gameplay.buildSettlement(validSpots.get(0));
        }
    }

    // Medium/Hard AI uses settlement dice value to choose best spot
    private void tryBuildBestSettlement(Gameplay gameplay) {
        if (!hasResources("Brick", 1) || !hasResources("Wood", 1)
                || !hasResources("Wool", 1) || !hasResources("Grain", 1)) return;

        List<Vertex> validSpots = getValidSettlementSpots(gameplay);
        Vertex bestSpot = null;
        int bestScore = Integer.MIN_VALUE;

        for (Vertex v : validSpots) {
            int score = getSmartSettlementScore(v, gameplay);
            if (score > bestScore) {
                bestScore = score;
                bestSpot = v;
            }
        }

        if (bestSpot != null) gameplay.buildSettlement(bestSpot);
    }

    // Tries to build a road randomly on a valid edge
    private void tryBuildRoad(Gameplay gameplay) {
        if (!hasResources("Brick", 1) || !hasResources("Wood", 1)) return;

        List<Edge> validRoads = new ArrayList<>();
        for (Edge edge : Board.getEdges()) {
            if (gameplay.isValidRoadPlacement(edge)) validRoads.add(edge);
        }

        if (!validRoads.isEmpty()) {
            Collections.shuffle(validRoads);
            gameplay.buildRoad(validRoads.get(0));
        }
    }

    // Tries to trade with the bank when one resource is missing and another is overstocked
    private void tryBankTrade(Gameplay gameplay) {
        Map<String, Integer> resources = getResources();
        List<String> allTypes = List.of("Brick", "Wood", "Ore", "Grain", "Wool");

        for (String need : allTypes) {
            if (resources.getOrDefault(need, 0) == 0) {
                for (String give : allTypes) {
                    if (!give.equals(need) && resources.getOrDefault(give, 0) >= 4) {
                        gameplay.tradeWithBank(give, need); // Assumes this method exists
                        return;
                    }
                }
            }
        }
    }

    // Finds valid settlement positions connected to current roads
    private List<Vertex> getValidSettlementSpots(Gameplay gameplay) {
        List<Vertex> validSpots = new ArrayList<>();
        for (Edge road : getRoads()) {
            if (gameplay.isValidSettlementPlacement(road.getVertex1()) && !validSpots.contains(road.getVertex1())) {
                validSpots.add(road.getVertex1());
            }
            if (gameplay.isValidSettlementPlacement(road.getVertex2()) && !validSpots.contains(road.getVertex2())) {
                validSpots.add(road.getVertex2());
            }
        }
        return validSpots;
    }

    // Helper method to check if AI has enough resources of a given type
    private boolean hasResources(String type, int amount) {
        return getResources().getOrDefault(type, 0) >= amount;
    }

    // Checks if a vertex is blocked by an opponent settlement
    private boolean isBlocked(Vertex vertex, Gameplay gameplay) {
        for (Player player : gameplay.getPlayerList()) {
            if (player != this && player.getSettlements().contains(vertex)) {
                return true;
            }
        }
        return false;
    }

    // Calculates how many different resource types a vertex touches
    private int getResourceDiversityScore(Vertex vertex, Gameplay gameplay) {
        Set<String> resourceTypes = new HashSet<>();
        for (Tile tile : Board.getTiles()) {
            if (tile.getVertices().contains(vertex)) {
                resourceTypes.add(tile.getResourcetype().toString());
            }
        }
        return resourceTypes.size();
    }

    // Returns top N settlement spots sorted by dice value score
    private List<Vertex> getTopNSettlementSpots(Gameplay gameplay, int n) {
        List<Vertex> validSpots = getValidSettlementSpots(gameplay);
        validSpots.sort((v1, v2) -> Integer.compare(
                gameplay.getSettlementDiceValue(v2),
                gameplay.getSettlementDiceValue(v1)));
        return validSpots.subList(0, Math.min(n, validSpots.size()));
    }

    // Checks if AI can afford a structure by resource type and amount
    private boolean canAfford(Map<String, Integer> cost) {
        for (Map.Entry<String, Integer> entry : cost.entrySet()) {
            if (getResources().getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    // Calculates a combined settlement score based on dice value and resource diversity, adjusted for AI's current resource gaps
    private int getSmartSettlementScore(Vertex vertex, Gameplay gameplay) {
        int diceValue = gameplay.getSettlementDiceValue(vertex);
        int diversity = getResourceDiversityScore(vertex, gameplay);
        boolean blocked = isBlocked(vertex, gameplay);
        int resourceNeedMatch = countMissingResourcesCovered(vertex, gameplay);

        int score = (diceValue * 2) + (diversity * 3) + (resourceNeedMatch * 4);

        if (blocked) score -= 100;

        return score;
    }

    // Counts how many missing resource types the vertex would help cover
    private int countMissingResourcesCovered(Vertex vertex, Gameplay gameplay) {
        Set<String> ownedTypes = new HashSet<>();
        for (Vertex settlement : getSettlements()) {
            for (Tile tile : Board.getTiles()) {
                if (tile.getVertices().contains(settlement)) {
                    ownedTypes.add(tile.getResourcetype().toString());
                }
            }
        }

        Set<String> newTypes = new HashSet<>();
        for (Tile tile : Board.getTiles()) {
            if (tile.getVertices().contains(vertex)) {
                newTypes.add(tile.getResourcetype().toString());
            }
        }

        int missingCovered = 0;
        for (String type : newTypes) {
            if (!ownedTypes.contains(type)) {
                missingCovered++;
            }
        }

        return missingCovered;
    }
}
