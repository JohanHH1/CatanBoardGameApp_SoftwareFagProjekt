package org.example.catanboardgameapp;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class AIOpponent extends Player {

    //___________________________STRATEGY AND STRATEGY LEVEL ENUMS___________________________//
    public enum StrategyLevel {
        EASY, MEDIUM, HARD
    }
    public enum Strategy {
        ROADBUILDER, CITYUPGRADER, SETTLEMENTPLACER, LONGESTROAD, BIGGESTARMY, NONE
    }
    //___________________________________FIELDS______________________________________________//
    private final Gameplay gameplay;
    private final StrategyLevel strategyLevel;
    private final Random random = new Random();
    private final DrawOrDisplay drawOrDisplay;

    //__________________________________CONSTRUCTOR___________________________________________//
    public AIOpponent(int playerId, Color color, StrategyLevel level, Gameplay gameplay) {
        super(playerId, color);
        this.strategyLevel = level;
        this.drawOrDisplay = new DrawOrDisplay(gameplay.getBoardRadius());
        this.gameplay = gameplay;
    }

    //___________________________________INITIAL PHASE_________________________________________//
    public void placeInitialSettlementAndRoad(Gameplay gameplay, Group boardGroup) {
        pauseBeforeMove();
        //System.out.println("placeInitialSettlementAndRoad");

        List<Vertex> candidates = new ArrayList<>(gameplay.getBoard().getVertices());

        // STEP 1: Pick a settlement vertex
        Vertex chosenSettlement = null;
        int chosenSettlementScore = 0;

        if (strategyLevel == StrategyLevel.EASY) {
            Collections.shuffle(candidates);
            for (Vertex v : candidates) {
                if (gameplay.isValidSettlementPlacement(v)) {
                    chosenSettlement = v;
                    break;
                }
            }
        } else {
            // MEDIUM or HARD - pick best scored settlement
            int bestScore = Integer.MIN_VALUE;

            for (Vertex v : candidates) {
                if (!gameplay.isValidSettlementPlacement(v)) continue;

                int score = getSmartSettlementScore(v, gameplay);
                if (score > bestScore) {
                    bestScore = score;
                    chosenSettlement = v;
                    chosenSettlementScore = score;
                }
            }

        }

        if (chosenSettlement == null) {
            gameplay.getCatanBoardGameView().logToGameLog("AI " + getPlayerId() + " could not find a valid initial settlement.");
            return;
        }

        // STEP 2: Attempt to place the settlement
        BuildResult settlementResult = gameplay.buildInitialSettlement(chosenSettlement);
        if (settlementResult != BuildResult.SUCCESS) {
            gameplay.getCatanBoardGameView().logToGameLog("AI " + getPlayerId() + " failed to place initial settlement.");
            return;
        }
        System.out.println("AI Player " + getPlayerId() + " (" + strategyLevel.name() + ") chose settlement with score: " + chosenSettlementScore);
        Circle circle = new Circle(chosenSettlement.getX(), chosenSettlement.getY(), 16.0 / gameplay.getBoardRadius());

        chosenSettlement.setOwner(gameplay.getCurrentPlayer());
        drawOrDisplay.drawSettlement(circle, chosenSettlement, boardGroup);


        // STEP 3: Choose best connecting road
        Edge chosenEdge = null;
        int chosenRoadScore = 0;
        List<Edge> edges = gameplay.getBoard().getEdges();

        if (strategyLevel == StrategyLevel.EASY) {
            Collections.shuffle(edges);
            for (Edge edge : edges) {
                if (edge.isConnectedTo(chosenSettlement) && gameplay.isValidRoadPlacement(edge)) {
                    chosenEdge = edge;
                    break;
                }
            }
        } else {
            // Medium/Hard – score by avoiding sea & maximizing future options
            int bestEdgeScore = Integer.MIN_VALUE;
            for (Edge edge : edges) {
                if (!edge.isConnectedTo(chosenSettlement) || !gameplay.isValidRoadPlacement(edge)) continue;

                int score = getSmartRoadScore(edge, chosenSettlement, gameplay);
                if (score > bestEdgeScore) {
                    bestEdgeScore = score;
                    chosenEdge = edge;
                    chosenRoadScore = score;
                }
            }

        }

        if (chosenEdge != null) {
            BuildResult roadResult = gameplay.buildRoad(chosenEdge);
            if (roadResult == BuildResult.SUCCESS) {
                Line line = new Line(
                        chosenEdge.getVertex1().getX(), chosenEdge.getVertex1().getY(),
                        chosenEdge.getVertex2().getX(), chosenEdge.getVertex2().getY()
                );
                System.out.println("AI Player " + getPlayerId() + " (" + strategyLevel.name() + ") placed road with score: " + chosenRoadScore);
                drawOrDisplay.drawRoad(line, this, boardGroup);

                // Done! Advance turn
                Platform.runLater(() -> {
                    gameplay.getCatanBoardGameView().logToGameLog(
                            "AI Player " + getPlayerId() + " (" + strategyLevel.name() + ") finished placing their initial settlement and road."
                    );
                    gameplay.nextPlayerTurn();
                });
                return;
            }
        }
        // Cleanup if road failed (Should not happen)
        System.out.println("SOMETHING WRONG WITH AI BRAIN");
        chosenSettlement.setOwner(null);
        getSettlements().remove(chosenSettlement);
        boardGroup.getChildren().remove(circle);
        gameplay.getCatanBoardGameView().logToGameLog("AI " + getPlayerId() + " failed to place road. Resetting.");
    }

    //__________________________________MAKE MOVE LOGIC________________________________________//
    public void makeMoveAI(Gameplay gameplay) {
        // Simulate thinking delay on background thread
        pauseBeforeMove();

        // Defer UI interaction to JavaFX Application Thread
        Platform.runLater(() -> {
            gameplay.rollDice();

            switch (strategyLevel) {
                case EASY -> {
                    gameplay.getCatanBoardGameView().logToGameLog("Strategy: EASY");
                    makeEasyLevelMove(gameplay);
                }
                case MEDIUM -> {
                    gameplay.getCatanBoardGameView().logToGameLog("Strategy: MEDIUM (EASY FOR NOW)");
                    makeEasyLevelMove(gameplay); // Temporarily use EASY logic
                    // makeMediumLevelMove(gameplay);
                }
                case HARD -> {
                    gameplay.getCatanBoardGameView().logToGameLog("Strategy: HARD (EASY FOR NOW)");
                    makeEasyLevelMove(gameplay); // Temporarily use EASY logic
                    // makeHardLevelMove(gameplay);
                }
            }
        });
    }

    private void makeEasyLevelMove(Gameplay gameplay) {
        Strategy strategy = determineStrategy();
        gameplay.getCatanBoardGameView().logToGameLog("AI Player " + getPlayerId() + " is using strategy: " + strategy);

        boolean moveMade = false;

        // First, try the intended build
        switch (strategy) {
            case CITYUPGRADER -> moveMade = tryBuildCity(gameplay);
            case SETTLEMENTPLACER -> moveMade = tryBuildSettlement(gameplay);
            case ROADBUILDER -> moveMade = tryBuildRoad(gameplay);
        }

        // If build failed, try to trade toward that build
        if (!moveMade) {
            boolean traded = tryBankTrade(gameplay, strategy);
            if (traded) {
                // Try the build again after trading
                switch (strategy) {
                    case CITYUPGRADER -> moveMade = tryBuildCity(gameplay);
                    case SETTLEMENTPLACER -> moveMade = tryBuildSettlement(gameplay);
                    case ROADBUILDER -> moveMade = tryBuildRoad(gameplay);
                }
            }
        }

        // If no action could be completed this turn, just move on
        gameplay.nextPlayerTurn();
        gameplay.getCatanBoardGameView().logToGameLog("AI Player " + getPlayerId() + " has ended its turn.");
    }

    //______________________________CHOSING STRATEGY LOGIC_______________________________________//
    public Strategy determineStrategy() {
        return switch (strategyLevel) {
            case EASY -> determineEasyStrategy(gameplay);
            case MEDIUM -> determineMediumStrategy(gameplay);
            case HARD -> determineHardStrategy(gameplay);
        };
    }


    private Strategy determineEasyStrategy(Gameplay gameplay) {
        // Priority: CITYUPGRADER > SETTLEMENTPLACER > ROADBUILDER

        // 1. City upgrade
        boolean hasCityResources = hasResources("Ore", 3) && hasResources("Grain", 2);
        boolean canUpgrade = !getSettlements().isEmpty();
        if (hasCityResources && canUpgrade) {
            return Strategy.CITYUPGRADER;
        }

        // 2. Settlement
        boolean hasSettlementResources = hasResources("Brick", 1)
                && hasResources("Wood", 1)
                && hasResources("Wool", 1)
                && hasResources("Grain", 1);

        if (hasSettlementResources && !getValidSettlementSpots(gameplay).isEmpty()) {
            return Strategy.SETTLEMENTPLACER;
        }

        // 3. Road
        boolean hasRoadResources = hasResources("Brick", 1) && hasResources("Wood", 1);
        boolean hasValidRoadSpots = gameplay.getBoard().getEdges().stream()
                .anyMatch(e -> gameplay.isValidRoadPlacement(e));
        if (hasRoadResources && hasValidRoadSpots) {
            return Strategy.ROADBUILDER;
        }

        // 4. Wait
        System.out.println(gameplay.getCurrentPlayer() + ": "+ strategyLevel + ": Cant find proper strategy right now");
        return Strategy.NONE;
    }



    private Strategy determineMediumStrategy(Gameplay gameplay) {
        // NOT YET IMPLEMENTED
        return Strategy.NONE;
    }

    private Strategy determineHardStrategy(Gameplay gameplay) {
        // NOT YET IMPLEMENTED
        return Strategy.NONE;
    }

    private int getSmartRoadScore(Edge edge, Vertex source, Gameplay gameplay) {
        Vertex target = edge.getVertex1().equals(source) ? edge.getVertex2() : edge.getVertex1();

        // 1. Base: Settlement potential at target
        int settlementScore = getSmartSettlementScore(target, gameplay);

        // 2. Blocked by enemy?
        boolean blocked = target.hasSettlement() && target.getOwner() != this;
        if (blocked) {
            return -100; // avoid completely
        }

        // 3. Number of adjacent tiles (prefer more connected areas)
        int tileCount = target.getAdjacentTiles().size();

        // 4. Existing road connections to target (don't waste effort)
        long friendlyRoads = getRoads().stream()
                .filter(r -> r.isConnectedTo(target))
                .count();

        // Total score (weights can be tweaked)
        int score = (settlementScore * 3) + (tileCount * 2) - (int)(friendlyRoads * 2);
        return score;
    }

    private void makeMediumLevelMove(Gameplay gameplay) {
        tryBuildCity(gameplay);
        tryBuildSettlement(gameplay);
        tryBuildRoad(gameplay);
    }

    private void makeHardLevelMove(Gameplay gameplay) {
        //check makeEASY move for implementation
    }

    private boolean tryBuildCity(Gameplay gameplay) {
        if (hasResources("Ore", 3) && hasResources("Grain", 2)) {
            List<Vertex> settlements = getSettlements();
            if (!settlements.isEmpty()) {
                Vertex chosenVertex = settlements.get(random.nextInt(settlements.size()));
                BuildResult result = gameplay.buildCity(chosenVertex);
                if (result == BuildResult.UPGRADED_TO_CITY) {
                    gameplay.getCatanBoardGameView().logToGameLog("AI Player " + getPlayerId() + " upgraded to a city.");
                    return true;
                }
            }
        }
        return false;
    }

    private boolean tryBuildSettlement(Gameplay gameplay) {
        // Check if AI has enough resources for a settlement
        if (!hasResources("Brick", 1) || !hasResources("Wood", 1)
                || !hasResources("Wool", 1) || !hasResources("Grain", 1)) {
            return false;
        }

        // Get all valid build spots connected to current roads
        List<Vertex> validSpots = getValidSettlementSpots(gameplay);
        if (validSpots.isEmpty()) return false;

        // Pick the best scoring vertex
        Vertex bestSpot = null;
        int bestScore = Integer.MIN_VALUE;

        for (Vertex v : validSpots) {
            int score = getSmartSettlementScore(v, gameplay);
            if (score > bestScore) {
                bestScore = score;
                bestSpot = v;
            }
        }

        if (bestSpot != null) {
            BuildResult result = gameplay.buildSettlement(bestSpot);
            if (result == BuildResult.SUCCESS) {
                gameplay.getCatanBoardGameView().logToGameLog("AI Player " + getPlayerId() + " built a settlement at vertex " + bestSpot.getIdOrCoords());
                return true;
            }
        }

        return false;
    }

    private boolean tryBuildRoad(Gameplay gameplay) {
        // Ensure resources for road
        if (!hasResources("Brick", 1) || !hasResources("Wood", 1)) return false;

        List<Edge> validRoads = new ArrayList<>();
        for (Edge edge : gameplay.getBoard().getEdges()) {
            if (gameplay.isValidRoadPlacement(edge)) {
                validRoads.add(edge);
            }
        }

        if (validRoads.isEmpty()) return false;

        // Score and select the best road
        Edge bestEdge = null;
        int bestScore = Integer.MIN_VALUE;

        for (Edge edge : validRoads) {
            // Get connected settlement as a source (owned end)
            Vertex source = (getSettlements().contains(edge.getVertex1())) ? edge.getVertex1() :
                    (getSettlements().contains(edge.getVertex2())) ? edge.getVertex2() : null;

            // If source is null, fallback to either endpoint for scoring
            if (source == null) {
                source = edge.getVertex1();
            }

            int score = getSmartRoadScore(edge, source, gameplay);
            if (score > bestScore) {
                bestScore = score;
                bestEdge = edge;
            }
        }

        if (bestEdge != null) {
            BuildResult result = gameplay.buildRoad(bestEdge);
            if (result == BuildResult.SUCCESS) {
                gameplay.getCatanBoardGameView().logToGameLog("AI Player " + getPlayerId() +
                        " built a road between " +
                        bestEdge.getVertex1().getIdOrCoords() + " and " +
                        bestEdge.getVertex2().getIdOrCoords());
                return true;
            }
        }

        return false;
    }

    private boolean tryBankTrade(Gameplay gameplay, Strategy strategy) {
        Map<String, Integer> resources = getResources();
        List<String> allTypes = List.of("Brick", "Wood", "Ore", "Grain", "Wool");

        Map<String, Integer> targetCost = new HashMap<>();

        switch (strategy) {
            case CITYUPGRADER -> {
                targetCost.put("Ore", 3);
                targetCost.put("Grain", 2);
            }
            case SETTLEMENTPLACER -> {
                targetCost.put("Brick", 1);
                targetCost.put("Wood", 1);
                targetCost.put("Wool", 1);
                targetCost.put("Grain", 1);
            }
            case ROADBUILDER -> {
                targetCost.put("Brick", 1);
                targetCost.put("Wood", 1);
            }
            default -> {
                return false;
            }
        }

        // Check if we're just one resource short from building
        if (!canCompleteBuildWithOneTrade(resources, targetCost)) {
            return false;
        }

        // Find missing resource(s)
        for (String need : targetCost.keySet()) {
            int owned = resources.getOrDefault(need, 0);
            int required = targetCost.get(need);
            if (owned >= required) continue; // Already have enough

            // Need this resource – try to find something to trade for it
            for (String give : allTypes) {
                if (!give.equals(need) && resources.getOrDefault(give, 0) >= 4) {
                    boolean traded = gameplay.tradeWithBank(give, need);
                    if (traded) {
                        gameplay.getCatanBoardGameView().logToGameLog("AI Player " + getPlayerId() +
                                " traded 4 " + give + " for 1 " + need + " (Strategy: " + strategy + ")");
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean canCompleteBuildWithOneTrade(Map<String, Integer> currentResources, Map<String, Integer> targetCost) {
        int missingCount = 0;
        for (Map.Entry<String, Integer> entry : targetCost.entrySet()) {
            int owned = currentResources.getOrDefault(entry.getKey(), 0);
            if (owned < entry.getValue()) {
                missingCount += (entry.getValue() - owned);
            }
        }
        return missingCount == 1;
    }


    //______________________________ROBBER LOGIC____________________________//
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


    public Set<String> getNeededResourcesForStrategy(Strategy strategy) {
        Set<String> needed = new HashSet<>();

        switch (strategy) {
            case CITYUPGRADER -> {
                if (getResources().getOrDefault("Ore", 0) < 3) needed.add("Ore");
                if (getResources().getOrDefault("Grain", 0) < 2) needed.add("Grain");
            }
            case SETTLEMENTPLACER -> {
                if (getResources().getOrDefault("Brick", 0) < 1) needed.add("Brick");
                if (getResources().getOrDefault("Wood", 0) < 1) needed.add("Wood");
                if (getResources().getOrDefault("Wool", 0) < 1) needed.add("Wool");
                if (getResources().getOrDefault("Grain", 0) < 1) needed.add("Grain");
            }
            case ROADBUILDER -> {
                if (getResources().getOrDefault("Brick", 0) < 1) needed.add("Brick");
                if (getResources().getOrDefault("Wood", 0) < 1) needed.add("Wood");
            }
            case NONE -> {
                for (Map.Entry<String, Integer> entry : getResources().entrySet()) {
                    if (entry.getValue() == 0) {
                        needed.add(entry.getKey());
                    }
                }
            }
        }

        return needed;
    }
    public int countHelpfulCards(Player victim, Set<String> neededResources) {
        int count = 0;
        for (String res : neededResources) {
            count += victim.getResources().getOrDefault(res, 0);
        }
        return count;
    }

    public Player chooseBestRobberyTargetForHardAI(AIOpponent ai, List<Player> victims) {
        Strategy currentStrategy = determineStrategy();
        Set<String> neededResources = getNeededResourcesForStrategy(currentStrategy);

        return victims.stream()
                .max(Comparator.comparingInt(v -> countHelpfulCards(v, neededResources)))
                .orElse(null);
    }

    public boolean lateGame() {
        if (getPlayerScore() >= 8) return true;
        return gameplay.getPlayerList().stream()
                .anyMatch(p -> p != this && p.getPlayerScore() >= 7);
    }

    public void robberPlacement() {
        // Robber placement logic to be implemented
    }

    public boolean canAlmostUpgradeToCity() {
        return true;
    }

    private boolean hasResources(String type, int amount) {
        return getResources().getOrDefault(type, 0) >= amount;
    }

    private boolean isBlocked(Vertex vertex, Gameplay gameplay) {
        for (Player player : gameplay.getPlayerList()) {
            if (player != this && player.getSettlements().contains(vertex)) {
                return true;
            }
        }
        return false;
    }

    private boolean canAfford(Map<String, Integer> cost) {
        for (Map.Entry<String, Integer> entry : cost.entrySet()) {
            if (getResources().getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private List<Vertex> getValidSettlementSpots(Gameplay gameplay) {
        Set<Vertex> validSpots = new HashSet<>();
        for (Edge road : getRoads()) {
            Vertex v1 = road.getVertex1();
            Vertex v2 = road.getVertex2();
            if (gameplay.isValidSettlementPlacement(v1)) validSpots.add(v1);
            if (gameplay.isValidSettlementPlacement(v2)) validSpots.add(v2);
        }
        return new ArrayList<>(validSpots);
    }

    private int getResourceDiversityScore(Vertex vertex, Gameplay gameplay) {
        Set<String> resourceTypes = new HashSet<>();
        for (Tile tile : gameplay.getBoard().getTiles()) {
            if (tile.getVertices().contains(vertex)) {
                resourceTypes.add(tile.getResourcetype().toString());
            }
        }
        return resourceTypes.size();
    }

    private int getSmartSettlementScore(Vertex vertex, Gameplay gameplay) {
        int diceValue = getSettlementDiceValue(vertex, gameplay);     // Sum of dice probabilities
        int diversity = getResourceDiversityScore(vertex, gameplay);  // Unique resource types
        int newResources = countMissingResourcesCovered(vertex, gameplay);
        boolean blocked = isBlocked(vertex, gameplay);

        // 🆕 New: count adjacent land tiles (max 3)
        long landTileCount = vertex.getAdjacentTiles().stream()
                .filter(t -> !t.isSea())
                .count();

        // New weight: reward more adjacent land
        int score = (int)((diceValue * 2) + (diversity * 3) +(newResources * 4) +(landTileCount * 3));

        if (blocked) score -= 100;

        return score;
    }

    private int countMissingResourcesCovered(Vertex vertex, Gameplay gameplay) {
        Set<String> ownedTypes = new HashSet<>();
        for (Vertex settlement : getSettlements()) {
            for (Tile tile : gameplay.getBoard().getTiles()) {
                if (tile.getVertices().contains(settlement)) {
                    ownedTypes.add(tile.getResourcetype().toString());
                }
            }
        }

        Set<String> newTypes = new HashSet<>();
        for (Tile tile : gameplay.getBoard().getTiles()) {
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

    public int getSettlementDiceValue(Vertex v, Gameplay gameplay) {
        int total = 0;
        for (Tile tile : gameplay.getBoard().getTiles()) {
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
    public StrategyLevel getStrategyLevel() {
        return strategyLevel;
    }

    private void pauseBeforeMove() {
        //System.out.println("AI is thinking for about a second before making a move");
        try {
            //int delay = ThreadLocalRandom.current().nextInt(3000, 10000);
            //int delay = ThreadLocalRandom.current().nextInt(700, 900); // ~0.7 to 0.9 sec
            int delay = ThreadLocalRandom.current().nextInt(7, 11); // ~0.2 to 0.6 sec
            Thread.sleep(delay);
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
