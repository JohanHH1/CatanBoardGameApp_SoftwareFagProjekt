package org.example.catanboardgameapp;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import org.example.catanboardgameviews.CatanBoardGameView;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class AIOpponent extends Player {

    //___________________________STRATEGY AND STRATEGY LEVEL ENUMS___________________________//
    public enum StrategyLevel {
        EASY, MEDIUM, HARD
    }

    public enum Strategy {
        ROADBUILDER, CITYUPGRADER, SETTLEMENTPLACER, LONGESTROAD, BIGGESTARMY, DEVELOPMENTCARDBYER, NONE
    }

    public enum ThinkingSpeed {
        SLOW, MEDIUM, FAST, EXTREME
    }

    //___________________________________FIELDS______________________________________________//
    private final Gameplay gameplay;
    private final StrategyLevel strategyLevel;
    private final Random random = new Random();
    private final DrawOrDisplay drawOrDisplay;
    private int noneStrategyCount = 0;
    private ThinkingSpeed thinkingSpeed = ThinkingSpeed.EXTREME; // Default

    //__________________________________CONSTRUCTOR___________________________________________//
    public AIOpponent(int playerId, Color color, StrategyLevel level, Gameplay gameplay) {
        super(playerId, color, gameplay);
        this.strategyLevel = level;
        this.drawOrDisplay = gameplay.getDrawOrDisplay();
        this.gameplay = gameplay;
    }

    //___________________________________INITIAL PHASE_________________________________________//
    public void placeInitialSettlementAndRoad(Gameplay gameplay, Group boardGroup) {
        CatanBoardGameView view = gameplay.getCatanBoardGameView();

        // STEP 1: Show overlay if any human player exists
        boolean showOverlay = gameplay.hasHumanPlayers();
        if (showOverlay) {
            view.runOnFX(() -> view.showAITurnOverlay(this));
        }

        // STEP 2: Make sure this logic is inside a background thread — so wrap all logic here:
        Runnable logic = () -> {
            pauseBeforeMove();  // ← will now be safe

            List<Vertex> candidates = new ArrayList<>(gameplay.getBoard().getVertices());
            Vertex chosenSettlement = null;

            if (strategyLevel == StrategyLevel.EASY) {
                Collections.shuffle(candidates);
                for (Vertex v : candidates) {
                    if (gameplay.isValidSettlementPlacement(v)) {
                        chosenSettlement = v;
                        break;
                    }
                }
            } else {
                int bestScore = Integer.MIN_VALUE;
                for (Vertex v : candidates) {
                    if (!gameplay.isValidSettlementPlacement(v)) continue;
                    int score = getSmartSettlementScore(v, gameplay);
                    if (score > bestScore) {
                        bestScore = score;
                        chosenSettlement = v;
                    }
                }
            }

            if (chosenSettlement == null) {
                view.runOnFX(() -> {
                    view.logToGameLog("AI " + getPlayerId() + " could not find a valid initial settlement.");
                    view.hideAITurnOverlay();
                });
                return;
            }

            BuildResult settlementResult = gameplay.buildInitialSettlement(chosenSettlement);
            if (settlementResult != BuildResult.SUCCESS) {
                view.runOnFX(() -> {
                    view.logToGameLog("AI " + getPlayerId() + " failed to place initial settlement.");
                    view.hideAITurnOverlay();
                });
                return;
            }

            Circle circle = new Circle(chosenSettlement.getX(), chosenSettlement.getY(), 16.0 / gameplay.getBoardRadius());
            Vertex finalSettlement = chosenSettlement;

            view.runOnFX(() -> {
                finalSettlement.setOwner(gameplay.getCurrentPlayer());
                drawOrDisplay.drawSettlement(circle, finalSettlement, boardGroup);
            });

            // Select road
            Edge chosenEdge = null;
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
                int bestEdgeScore = Integer.MIN_VALUE;
                for (Edge edge : edges) {
                    if (!edge.isConnectedTo(chosenSettlement) || !gameplay.isValidRoadPlacement(edge)) continue;
                    int score = getSmartRoadScore(edge, chosenSettlement, gameplay);
                    if (score > bestEdgeScore) {
                        bestEdgeScore = score;
                        chosenEdge = edge;
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
                    Edge finalEdge = chosenEdge;

                    view.runOnFX(() -> {
                        drawOrDisplay.drawRoad(line, this, boardGroup);
                        view.logToGameLog("AI Player " + getPlayerId() + " (" + strategyLevel.name() + ") finished placing their initial settlement and road.");
                        gameplay.nextPlayerTurn();
                        view.refreshSidebar();
                        view.hideAITurnOverlay();
                    });
                    return;
                }
            }

            // fallback error
            view.runOnFX(() -> {
                view.logToGameLog("AI " + getPlayerId() + " failed to place road.");
                view.hideAITurnOverlay();
            });
        };

        // Execute AI logic in a background thread if not already on one
        if (Platform.isFxApplicationThread()) {
            new Thread(logic).start();
        } else {
            logic.run();  // already on background thread from startAIThread()
        }
    }

    //__________________________________MAKE MOVE LOGIC________________________________________//
    public void makeMoveAI(Gameplay gameplay, Group boardGroup) {
        // Wait until game is unpaused
        while (gameplay.isGamePaused()) {
            if (Thread.currentThread().isInterrupted()) return;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        if (gameplay.hasHumanPlayers()) {
            gameplay.getCatanBoardGameView().runOnFX(() -> gameplay.getCatanBoardGameView().showAITurnOverlay(this));
        }
        pauseBeforeMove();

        if (Thread.currentThread().isInterrupted()) return;

        // Background logic
        if (!gameplay.isGamePaused() && !gameplay.hasRolledDice()) {
            gameplay.rollDice();
        }

        // Capture result messages (if any)
        String logMsg = "Strategy: " + strategyLevel.name();
        gameplay.getCatanBoardGameView().runOnFX(() -> gameplay.getCatanBoardGameView().logToGameLog(logMsg));

        // Perform AI move (off FX thread!)
        switch (strategyLevel) {
            case EASY -> makeEasyLevelMove(gameplay, boardGroup);
            case MEDIUM -> makeMediumLevelMove(gameplay, boardGroup);
            case HARD -> makeHardLevelMove(gameplay, boardGroup);
        }

        // Done – UI update on FX thread
        gameplay.getCatanBoardGameView().runOnFX(() -> gameplay.getCatanBoardGameView().hideAITurnOverlay());
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
        // 1. City upgrade (only if has upgradeable settlement)
        boolean hasCityResources = hasResources("Ore", 3) && hasResources("Grain", 2);
        boolean canUpgrade = getSettlements().stream().anyMatch(v -> !v.isCity());
        if (hasCityResources && canUpgrade) {
            return Strategy.CITYUPGRADER;
        }

        // 2. Settlement
        if (!getValidSettlementSpots(gameplay).isEmpty()) {
            return Strategy.SETTLEMENTPLACER;
        }

        // 3. Road
        boolean hasValidRoadSpots = gameplay.getBoard().getEdges().stream()
                .anyMatch(gameplay::isValidRoadPlacement);
        if (hasValidRoadSpots) {
            return Strategy.ROADBUILDER;
        }

        // 4. No good option
        noneStrategyCount++;
        return Strategy.NONE;
    }

    private Strategy determineMediumStrategy(Gameplay gameplay) {
        // 1. City upgrade (only if has upgradeable settlement)
        boolean hasCityResources = hasResources("Ore", 3) && hasResources("Grain", 2);
        boolean canUpgrade = getSettlements().stream().anyMatch(v -> !v.isCity());
        if (hasCityResources && canUpgrade) {
            return Strategy.CITYUPGRADER;
        }

        // 2. Settlement
        if (!getValidSettlementSpots(gameplay).isEmpty()) {
            return Strategy.SETTLEMENTPLACER;
        }

        // 3. Road
        boolean hasValidRoadSpots = gameplay.getBoard().getEdges().stream()
                .anyMatch(gameplay::isValidRoadPlacement);
        if (hasValidRoadSpots) {
            return Strategy.ROADBUILDER;
        }

        // 4. No good option
        noneStrategyCount++;
        return Strategy.NONE;
    }

    private Strategy determineHardStrategy(Gameplay gameplay) {
        // 1. City upgrade (only if has upgradeable settlement)
        boolean hasCityResources = hasResources("Ore", 3) && hasResources("Grain", 2);
        boolean canUpgrade = getSettlements().stream().anyMatch(v -> !v.isCity());
        if (hasCityResources && canUpgrade) {
            return Strategy.CITYUPGRADER;
        }

        // 2. Settlement
        if (!getValidSettlementSpots(gameplay).isEmpty()) {
            return Strategy.SETTLEMENTPLACER;
        }

        // 3. development card
        boolean hasDevcardResources = hasResources("Ore", 1) && hasResources("Grain", 1) && hasResources("Wool", 1);
        if (hasDevcardResources && (!gameplay.getShuffledDevelopmentCards().isEmpty())){
            System.out.println("IS IN DEV CARDS STRATEGY");
            noneStrategyCount++;
            return Strategy.DEVELOPMENTCARDBYER;
        }
        // 4. Road
        boolean hasValidRoadSpots = gameplay.getBoard().getEdges().stream()
                .anyMatch(e -> gameplay.isValidRoadPlacement(e));
        if (hasValidRoadSpots) {
            return Strategy.ROADBUILDER;
        }

        // 4. No good option
        //noneStrategyCount++;
        return Strategy.NONE;
    }

    private void makeEasyLevelMove(Gameplay gameplay, Group boardGroup) {
        boolean moveMade;
        int safetyLimit = 10;
        CatanBoardGameView view = gameplay.getCatanBoardGameView();

        do {
            Strategy strategy = determineStrategy();

            view.runOnFX(() -> view.logToGameLog("AI Player " + getPlayerId() + " is using strategy: " + strategy));
            moveMade = false;

            switch (strategy) {
                case CITYUPGRADER -> moveMade = tryBuildCity(gameplay, boardGroup);
                case SETTLEMENTPLACER -> moveMade = tryBuildSettlement(gameplay, boardGroup);
                case ROADBUILDER -> moveMade = tryBuildRoad(gameplay, boardGroup);
            }

            if (!moveMade) {
                moveMade = tryBankTrade(gameplay, strategy);
                if (moveMade) {
                    // Retry original action after trade
                    switch (strategy) {
                        case CITYUPGRADER -> moveMade = tryBuildCity(gameplay, boardGroup);
                        case SETTLEMENTPLACER -> moveMade = tryBuildSettlement(gameplay, boardGroup);
                        case ROADBUILDER -> moveMade = tryBuildRoad(gameplay, boardGroup);
                    }
                }
            }

        } while (moveMade && --safetyLimit > 0);

        view.runOnFX(() -> {
            gameplay.nextPlayerTurn();
            view.logToGameLog("AI Player " + getPlayerId() + " has ended its turn.");
        });
    }

    private void makeMediumLevelMove(Gameplay gameplay, Group boardGroup) {
        boolean moveMade;
        int safetyLimit = 10;
        CatanBoardGameView view = gameplay.getCatanBoardGameView();

        do {
            Strategy strategy = determineStrategy();
            view.runOnFX(() -> view.logToGameLog("AI Player " + getPlayerId() + " is using strategy: " + strategy));
            moveMade = false;

            switch (strategy) {
                case CITYUPGRADER -> moveMade = tryBuildCity(gameplay, boardGroup);
                case SETTLEMENTPLACER -> moveMade = tryBuildSettlement(gameplay, boardGroup);
                case ROADBUILDER -> moveMade = tryBuildRoad(gameplay, boardGroup);
            }

            if (!moveMade) {
                moveMade = tryBankTrade(gameplay, strategy);
                if (moveMade) {
                    switch (strategy) {
                        case CITYUPGRADER -> moveMade = tryBuildCity(gameplay, boardGroup);
                        case SETTLEMENTPLACER -> moveMade = tryBuildSettlement(gameplay, boardGroup);
                        case ROADBUILDER -> moveMade = tryBuildRoad(gameplay, boardGroup);
                    }
                }
            }
        } while (moveMade && --safetyLimit > 0);

        view.runOnFX(() -> {
            gameplay.nextPlayerTurn();
            view.logToGameLog("AI Player " + getPlayerId() + " has ended its turn.");
        });
    }


    private void makeHardLevelMove(Gameplay gameplay, Group boardGroup) {
        boolean moveMade;
        int safetyLimit = 10;
        CatanBoardGameView view = gameplay.getCatanBoardGameView();

        do {
            Strategy strategy = determineStrategy();
            view.runOnFX(() -> view.logToGameLog("AI Player " + getPlayerId() + " is using strategy: " + strategy));
            moveMade = false;

            switch (strategy) {
                case CITYUPGRADER -> moveMade = tryBuildCity(gameplay, boardGroup);
                case SETTLEMENTPLACER -> moveMade = tryBuildSettlement(gameplay, boardGroup);
                case ROADBUILDER -> moveMade = tryBuildRoad(gameplay, boardGroup);
                case DEVELOPMENTCARDBYER -> moveMade = tryBuyDevCard(gameplay);
            }

            if (!moveMade) {
                moveMade = tryBankTrade(gameplay, strategy);
                if (moveMade) {
                    switch (strategy) {
                        case CITYUPGRADER -> moveMade = tryBuildCity(gameplay, boardGroup);
                        case SETTLEMENTPLACER -> moveMade = tryBuildSettlement(gameplay, boardGroup);
                        case ROADBUILDER -> moveMade = tryBuildRoad(gameplay, boardGroup);
                        case DEVELOPMENTCARDBYER -> moveMade = tryBuyDevCard(gameplay);
                    }
                }
            }
        } while (moveMade && --safetyLimit > 0);
        tryPlayDevCard(gameplay,boardGroup);

        //play dev card here in if statment
        view.runOnFX(() -> {
            gameplay.nextPlayerTurn();
            view.logToGameLog("AI Player " + getPlayerId() + " has ended its turn.");
        });
    }

    private boolean tryBuildCity(Gameplay gameplay, Group boardGroup) {
        if (!hasResources("Ore", 3) || !hasResources("Grain", 2)) return false;
        List<Vertex> settlements = getSettlements();
        if (settlements.isEmpty()) return false;

        Vertex best = null;
        int bestScore = Integer.MIN_VALUE;

        for (Vertex v : settlements) {
            int score = getSettlementDiceValue(v, gameplay);
            if (score > bestScore) {
                best = v;
                bestScore = score;
            }
        }

        if (best != null) {
            BuildResult result = gameplay.buildCity(best);
            if (result == BuildResult.UPGRADED_TO_CITY) {
                String msg = "AI Player " + getPlayerId() + " upgraded settlement at " + best.getIdOrCoords()
                        + " to a city (dice score: " + bestScore + ").";
                Vertex finalBest = best;
                gameplay.getCatanBoardGameView().runOnFX(() -> {
                    drawOrDisplay.drawCity(finalBest, boardGroup);
                    gameplay.getCatanBoardGameView().logToGameLog(msg);
                });

                return true;
            }
        }
        return false;
    }

    private boolean tryBuildSettlement(Gameplay gameplay, Group boardGroup) {
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
                String msg = "AI Player " + getPlayerId() + " built a settlement at vertex " + bestSpot.getIdOrCoords();
                Circle circle = new Circle(bestSpot.getX(), bestSpot.getY(), 16.0 / gameplay.getBoardRadius());
                Vertex finalBestSpot = bestSpot;

                gameplay.getCatanBoardGameView().runOnFX(() -> {
                    drawOrDisplay.drawSettlement(circle, finalBestSpot, boardGroup);
                    gameplay.getCatanBoardGameView().logToGameLog(msg);
                });

                return true;
            }
        }

        return false;
    }
    private boolean tryBuyDevCard(Gameplay gameplay) {
        if (!hasResources("Wool", 1) || !hasResources("Grain", 1)|| !hasResources("Ore", 1)) {return false;}
            gameplay.buyDevelopmentCard();
            gameplay.getCatanBoardGameView().logToGameLog("AI Player " + getPlayerId() + " has bought a development card");
        return true;
    }

    private boolean tryPlayDevCard(Gameplay gameplay, Group boardGroup){
        if (hasNoDevelopmentCards()) {
            return false;
        } else {
            DevelopmentCard.DevelopmentCardType devCard = removeFirstDevelopmentCard();
            if (devCard != null) {
                System.out.println(getDevelopmentCards().toString());
                gameplay.getCatanBoardGameView().logToGameLog("AI Player " + getPlayerId() + " has played a development card !!!!!!!!!!!!!!!!!!!!!!!!!");
                //devCard.play(this, gameplay.getDevelopmentCard());
                getDevelopmentCards().computeIfPresent(devCard, (k, v) -> (v > 1) ? v - 1 : null);
                return true;
            }
        }
        return false;
    }

    private boolean tryBuildRoad(Gameplay gameplay, Group boardGroup) {

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
            Vertex source = (getSettlements().contains(edge.getVertex1())) ? edge.getVertex1() :
                    (getSettlements().contains(edge.getVertex2())) ? edge.getVertex2() : null;

            if (source == null) {
                source = edge.getVertex1(); // fallback
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
                String msg = "AI Player " + getPlayerId() +
                        " built a road between " +
                        bestEdge.getVertex1().getIdOrCoords() + " and " +
                        bestEdge.getVertex2().getIdOrCoords();

                Edge finalBestEdge = bestEdge;
                gameplay.getCatanBoardGameView().runOnFX(() -> {
                    Line line = new Line(
                            finalBestEdge.getVertex1().getX(), finalBestEdge.getVertex1().getY(),
                            finalBestEdge.getVertex2().getX(), finalBestEdge.getVertex2().getY()
                    );
                    drawOrDisplay.drawRoad(line, this, boardGroup);
                    gameplay.getCatanBoardGameView().logToGameLog(msg);
                });
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
            case DEVELOPMENTCARDBYER -> {
                targetCost.put("Wool", 1);
                targetCost.put("Grain", 1);
                targetCost.put("Ore", 1);
            }
            default -> { return false; }
        }

        if (!canCompleteBuildWithOneTrade(resources, targetCost)) return false;

        for (String need : targetCost.keySet()) {
            int owned = resources.getOrDefault(need, 0);
            int required = targetCost.get(need);
            if (owned >= required) continue;

            for (String give : allTypes) {
                if (give.equals(need)) continue;

                int ratio = gameplay.getBestTradeRatio(give, this);
                if (resources.getOrDefault(give, 0) >= ratio) {
                    int usedRatio = gameplay.tradeWithBank(give, need, this);
                    if (usedRatio > 0) {
                        String msg = "AI Player " + getPlayerId() +
                                " traded " + usedRatio + " " + give +
                                " for 1 " + need + " (Strategy: " + strategy + ")";

                        gameplay.getCatanBoardGameView().runOnFX(() -> {
                            gameplay.getCatanBoardGameView().logToGameLog(msg);
                        });
                        return true;
                    }
                }
            }
        }
        return false;
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

    private boolean shouldPlanCityUpgrade() {
        int ore = getResources().getOrDefault("Ore", 0);
        int grain = getResources().getOrDefault("Grain", 0);
        int total = ore + grain;

        // Already has full resources → return true
        if (ore >= 3 && grain >= 2) return true;

        // Has 3 out of 5 needed resources → consider planning
        return total >= 3 && !getSettlements().isEmpty();
    }

    private boolean hasSettlementPath(Gameplay gameplay) {
        List<Vertex> validSpots = getValidSettlementSpots(gameplay);
        return !validSpots.isEmpty();
    }

    private boolean canAfford(Map<String, Integer> cost) {
        for (Map.Entry<String, Integer> entry : cost.entrySet()) {
            if (getResources().getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return false;
            }
        }
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

    // THIS FUNCTION NEEDS IMPROVEMENTS!!
    private int getSmartSettlementScore(Vertex vertex, Gameplay gameplay) {
        int diceValue = getSettlementDiceValue(vertex, gameplay);     // Sum of dice probabilities
        int diversity = getResourceDiversityScore(vertex, gameplay);  // Unique resource types
        int newResources = countMissingResourcesCovered(vertex, gameplay);
        boolean blocked = isBlocked(vertex, gameplay);

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
    public int getNoneStrategyCount() {
        return noneStrategyCount;
    }

    private void pauseBeforeMove() {
        if (Platform.isFxApplicationThread()) {
            System.err.println("⚠️ AI pause called on JavaFX Application Thread!");
        }
        int delayMillis;
        switch (thinkingSpeed) {
            case SLOW -> delayMillis = ThreadLocalRandom.current().nextInt(3000, 7000);
            case FAST -> delayMillis = 200;
            case EXTREME -> delayMillis = 20;
            default -> delayMillis = 1000; // MEDIUM SPEED = default
        }

        try {
            int step = 10;
            int steps = delayMillis / step;
            for (int i = 0; i < steps; i++) {
                if (Thread.currentThread().isInterrupted()) return;
                Thread.sleep(step);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void setThinkingSpeed(ThinkingSpeed speed) {
        this.thinkingSpeed = speed;
    }

}
