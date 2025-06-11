package org.example.catanboardgameapp;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import org.example.catanboardgameviews.CatanBoardGameView;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class AIOpponent extends Player {

    //___________________________STRATEGY AND STRATEGY LEVEL ENUMS___________________________//
    public enum StrategyLevel {
        EASY, MEDIUM, HARD
    }

    public enum Strategy {
        ROADBUILDER, CITYUPGRADER, SETTLEMENTPLACER, LONGESTROAD, BIGGESTARMY, DEVELOPMENTCARDBYER, USERESOURCES, NONE
    }

    public enum ThinkingSpeed {
        SLOW, MEDIUM, FAST, EXTREME
    }

    //___________________________________FIELDS______________________________________________//
    private final Gameplay gameplay;
    private final StrategyLevel strategyLevel;
    private final Random random = new Random();
    private final DrawOrDisplay drawOrDisplay;
    private final EnumMap<Strategy, Integer> strategyUsageMap = new EnumMap<>(Strategy.class);
    private ThinkingSpeed thinkingSpeed = ThinkingSpeed.EXTREME; // Default
    private static final int MAX_STRATEGY_ATTEMPTS = 20;

    //__________________________________CONSTRUCTOR___________________________________________//
    public AIOpponent(int playerId, Color color, StrategyLevel level, Gameplay gameplay) {
        super(playerId, color, gameplay);
        this.strategyLevel = level;
        this.drawOrDisplay = gameplay.getDrawOrDisplay();
        this.gameplay = gameplay;
        for (Strategy strategy : Strategy.values()) {
            strategyUsageMap.put(strategy, 0);
        }
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
                        view.logToGameLog(gameplay.getCurrentPlayer() +  " (" + strategyLevel.name() + ") finished placing their initial settlement and road.");
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
        Strategy selected;

        boolean hasCityResources = hasResources("Ore", 3) && hasResources("Grain", 2);
        boolean canUpgrade = getSettlements().stream().anyMatch(v -> !v.isCity());
        if (hasCityResources && canUpgrade) {
            selected = Strategy.CITYUPGRADER;
        } else if (!getValidSettlementSpots(gameplay).isEmpty()) {
            selected = Strategy.SETTLEMENTPLACER;
        } else if (gameplay.getBoard().getEdges().stream().anyMatch(gameplay::isValidRoadPlacement)) {
            selected = Strategy.ROADBUILDER;
        } else {
            selected = Strategy.NONE;
        }

        strategyUsageMap.merge(selected, 1, Integer::sum);
        return selected;
    }
    private Strategy determineMediumStrategy(Gameplay gameplay) {
        Strategy selected;

        boolean hasCityResources = hasResources("Ore", 3) && hasResources("Grain", 2);
        boolean canUpgrade = getSettlements().stream().anyMatch(v -> !v.isCity());
        if (hasCityResources && canUpgrade) {
            selected = Strategy.CITYUPGRADER;
        } else if (!getValidSettlementSpots(gameplay).isEmpty()) {
            selected = Strategy.SETTLEMENTPLACER;
        } else if (gameplay.getBoard().getEdges().stream().anyMatch(gameplay::isValidRoadPlacement)) {
            selected = Strategy.ROADBUILDER;
        } else if (shouldUseResources(gameplay)) {
            selected = Strategy.USERESOURCES;
        }
        else {
            selected = Strategy.NONE;
        }

        strategyUsageMap.merge(selected, 1, Integer::sum);
        return selected;
    }
    private Strategy determineHardStrategy(Gameplay gameplay) {
        Strategy selected;

        boolean hasCityResources = hasResources("Ore", 3) && hasResources("Grain", 2);
        boolean canUpgrade = getSettlements().stream().anyMatch(v -> !v.isCity());
        if (hasCityResources && canUpgrade) {
            selected = Strategy.CITYUPGRADER;
        } else if (!getValidSettlementSpots(gameplay).isEmpty()) {
            selected = Strategy.SETTLEMENTPLACER;
        } else if (hasResources("Ore", 1) && hasResources("Grain", 1) && hasResources("Wool", 1)
                && !gameplay.getShuffledDevelopmentCards().isEmpty()) {
            selected = Strategy.DEVELOPMENTCARDBYER;
        } else if (shouldUseResources(gameplay)) {
            selected = Strategy.USERESOURCES;
        }else if (gameplay.getBoard().getEdges().stream().anyMatch(gameplay::isValidRoadPlacement)) {
                selected = Strategy.ROADBUILDER;
        } else {
            selected = Strategy.NONE;
        }

        strategyUsageMap.merge(selected, 1, Integer::sum);
        return selected;
    }
    private void makeEasyLevelMove(Gameplay gameplay, Group boardGroup) {
        CatanBoardGameView view = gameplay.getCatanBoardGameView();
        int attempts = getMaxStrategyAttempts();
        boolean moveMade;

        do {
            Strategy strategy = determineStrategy();
            view.runOnFX(() -> view.logToGameLog("\n" + gameplay.getCurrentPlayer() + " (EASY) is using strategy: " + strategy));

            moveMade = false;

            switch (strategy) {
                case CITYUPGRADER -> moveMade = tryBuildCity(gameplay, boardGroup);
                case SETTLEMENTPLACER -> moveMade = tryBuildSettlement(gameplay, boardGroup);
                case ROADBUILDER -> moveMade = tryBuildRoad(gameplay, boardGroup);
                case USERESOURCES -> moveMade = tryBankTrade(gameplay, strategy);
            }
        } while (moveMade && --attempts > 0);
        view.runOnFX(() -> {
            view.logToGameLog(gameplay.getCurrentPlayer() + " (" + strategyLevel.name() + ") has ended their turn.");
            gameplay.nextPlayerTurn();
        });
    }
    private void makeMediumLevelMove(Gameplay gameplay, Group boardGroup) {
        CatanBoardGameView view = gameplay.getCatanBoardGameView();
        int attempts = getMaxStrategyAttempts();
        boolean moveMade;

        do {
            Strategy strategy = determineStrategy();
            view.runOnFX(() -> view.logToGameLog(gameplay.getCurrentPlayer() + " (MEDIUM) is using strategy: " + strategy));
            moveMade = false;
            switch (strategy) {
                case CITYUPGRADER -> moveMade = tryBuildCity(gameplay, boardGroup);
                case SETTLEMENTPLACER -> moveMade = tryBuildSettlement(gameplay, boardGroup);
                case ROADBUILDER -> moveMade = tryBuildRoad(gameplay, boardGroup);
                case USERESOURCES -> moveMade = tryBankTrade(gameplay, strategy);
            }
        } while (moveMade && --attempts > 0);
        view.runOnFX(() -> {
            view.logToGameLog(gameplay.getCurrentPlayer() + " (" + strategyLevel.name() + ") has ended their turn.");
            gameplay.nextPlayerTurn();
        });
    }
    private void makeHardLevelMove(Gameplay gameplay, Group boardGroup) {
        CatanBoardGameView view = gameplay.getCatanBoardGameView();
        int attempts = getMaxStrategyAttempts();
        boolean moveMade;

        do {
            Strategy strategy = determineStrategy();
            view.runOnFX(() -> view.logToGameLog(gameplay.getCurrentPlayer() + " (HARD) is using strategy: " + strategy));

            moveMade = false;

            switch (strategy) {
                case CITYUPGRADER -> moveMade = tryBuildCity(gameplay, boardGroup);
                case SETTLEMENTPLACER -> moveMade = tryBuildSettlement(gameplay, boardGroup);
                case ROADBUILDER -> moveMade = tryBuildRoad(gameplay, boardGroup);
                case DEVELOPMENTCARDBYER -> moveMade = tryBuyDevCard(gameplay);
                case USERESOURCES -> moveMade = tryBankTrade(gameplay, strategy);
            }
        } while (moveMade && --attempts > 0);

        tryPlayDevCard(gameplay, boardGroup);
        view.runOnFX(() -> {
            view.logToGameLog(gameplay.getCurrentPlayer() + " (" + strategyLevel.name() + ") has ended their turn.");
            gameplay.nextPlayerTurn();
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
                String msg = gameplay.getCurrentPlayer() +  "UPGRADED TO A CITY";
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
                String msg = gameplay.getCurrentPlayer() + " succesfully built a Settlement";
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
            gameplay.getCatanBoardGameView().logToGameLog(gameplay.getCurrentPlayer() +  " has bought a development card");
        return true;
    }

    private boolean tryPlayDevCard(Gameplay gameplay, Group boardGroup){
        if (hasNoDevelopmentCards()) {
            return false;
        } else {
            DevelopmentCard.DevelopmentCardType devCard = removeFirstDevelopmentCard();
            if (devCard != null) {
                System.out.println(getDevelopmentCards().toString());
                gameplay.getCatanBoardGameView().logToGameLog(gameplay.getCurrentPlayer() +  " has played a development card !!!!!!!!!!!!!!!!!!!!!!!!!");
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
                String msg = gameplay.getCurrentPlayer() + " succesfully built a road";
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

    public boolean tryBankTrade(Gameplay gameplay, Strategy strategy) {
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
            case USERESOURCES -> {
                String target = chooseSmartResourceToReceive(gameplay);
                String give = chooseSmartResourceToGive(gameplay);

                if (give == null || give.equals(target)) return false;

                int ratio = gameplay.getBestTradeRatio(give, this);
                int owned = resources.getOrDefault(give, 0);

                if (owned >= ratio + 1) {
                    int usedRatio = gameplay.tradeWithBank(give, target, this);
                    if (usedRatio > 0) {
                        String msg = gameplay.getCurrentPlayer() +
                                " traded " + usedRatio + " " + give +
                                " for 1 " + target + " (Strategy: USERESOURCES)";
                        gameplay.getCatanBoardGameView().runOnFX(() ->
                                gameplay.getCatanBoardGameView().logToGameLog(msg)
                        );
                        return true;
                    }
                }
                return false;
            }
            default -> { return false; }
        }

        // 💡 Don't restrict to trades that complete the full cost – allow partial help
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
                        String msg = gameplay.getCurrentPlayer() +
                                " traded " + usedRatio + " " + give +
                                " for 1 " + need + " (Strategy: " + strategy + ")";
                        gameplay.getCatanBoardGameView().runOnFX(() ->
                                gameplay.getCatanBoardGameView().logToGameLog(msg)
                        );
                        return true;
                    }
                }
            }
        }
        return false;
    }
    //______________________________ROBBER LOGIC____________________________//
    // for AI use and for auto-discard for human players
    public Map<String, Integer> chooseDiscardCards() {
        Map<String, Integer> resources = new HashMap<>(getResources());
        int total = resources.values().stream().mapToInt(Integer::intValue).sum();
        int toDiscard = total / 2;

        if (toDiscard == 0) return null;

        Map<String, Integer> discardMap = new HashMap<>();
        Map<String, Integer> priorityScores = new HashMap<>();

        // Resources AI needs for next strategy (e.g. city/settlement/dev card)
        Set<String> neededResources = getNeededResourcesForStrategy(determineStrategy());

        for (String res : resources.keySet()) {
            int amountOwned = resources.getOrDefault(res, 0);
            int productionScore = 0;

            // Production score: total dice weight from settlements
            for (Vertex v : getSettlements()) {
                for (Tile t : gameplay.getBoard().getTiles()) {
                    if (t.getVertices().contains(v) &&
                            t.getResourcetype().toString().equals(res)) {
                        productionScore += getDiceProbabilityValue(t.getTileDiceNumber());
                    }
                }
            }

            // Base score: more owned = more discardable
            int score = amountOwned * 3;

            // Subtract based on production (more produced → less discardable)
            score -= productionScore * 2;

            // Penalize if resource is needed for strategy
            if (neededResources.contains(res)) {
                score -= 8;
            }

            // Penalize if resource is easy to trade (2:1 or 3:1 harbor = valuable)
            int ratio = gameplay.getBestTradeRatio(res, this);
            if (ratio <= 2) score -= 6;
            else if (ratio == 3) score -= 3;

            priorityScores.put(res, score);
        }

        // Sort by lowest score (most discardable first)
        List<String> discardOrder = priorityScores.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .toList();

        for (String res : discardOrder) {
            if (toDiscard == 0) break;

            int available = resources.get(res);
            int discard = Math.min(available, toDiscard);
            if (discard > 0) {
                discardMap.put(res, discard);
                toDiscard -= discard;
            }
        }

        // Optional log
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

    private boolean shouldUseResources(Gameplay gameplay) {
        Map<String, Integer> resources = getResources();

        // Step 1: Total resources must be at least 10
        int totalResources = resources.values().stream().mapToInt(Integer::intValue).sum();
        if (totalResources < 10) return false;

        // Step 2: Check if any resource can be traded without fully depleting it
        for (Map.Entry<String, Integer> entry : resources.entrySet()) {
            String resource = entry.getKey();
            int owned = entry.getValue();

            int ratio = gameplay.getBestTradeRatio(resource, this); // e.g., 3:1 or 2:1
            if (owned >= ratio + 1) {
                return true;
            }
        }

        return false;
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

    public String chooseSmartResourceToReceive(Gameplay gameplay) {
        List<String> allTypes = List.of("Brick", "Wood", "Ore", "Grain", "Wool");
        List<String> priority = List.of("Ore", "Grain", "Brick", "Wood", "Wool");

        Map<String, Integer> resources = getResources();

        // 1. Find lowest-count resources
        int minCount = resources.values().stream().min(Integer::compareTo).orElse(0);
        List<String> lowestResources = allTypes.stream()
                .filter(res -> resources.getOrDefault(res, 0) == minCount)
                .collect(Collectors.toList());

        // 2. Break tie with lowest production (based on dice potential)
        if (lowestResources.size() > 1) {
            Map<String, Integer> productionMap = new HashMap<>();
            for (String res : lowestResources) {
                int totalProd = 0;
                for (Vertex v : getSettlements()) {
                    for (Tile tile : gameplay.getBoard().getTiles()) {
                        if (tile.getVertices().contains(v) &&
                                tile.getResourcetype().toString().equals(res)) {
                            totalProd += getDiceProbabilityValue(tile.getTileDiceNumber());
                        }
                    }
                }
                productionMap.put(res, totalProd);
            }

            int minProd = productionMap.values().stream().min(Integer::compareTo).orElse(0);
            lowestResources = productionMap.entrySet().stream()
                    .filter(e -> e.getValue() == minProd)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }

        // 3. Final fallback: fixed priority order
        lowestResources.sort(Comparator.comparingInt(priority::indexOf));
        return lowestResources.get(0); // Most needed resource
    }
    public String chooseSmartResourceToGive(Gameplay gameplay) {
        List<String> allTypes = List.of("Brick", "Wood", "Ore", "Grain", "Wool");

        // Prefer to give away resources that are abundant and least useful
        Map<String, Integer> resourceAmounts = new HashMap<>();
        for (String res : allTypes) {
            resourceAmounts.put(res, getResources().getOrDefault(res, 0));
        }

        // Filter out resources we can't trade
        List<String> tradable = allTypes.stream()
                .filter(res -> {
                    int owned = resourceAmounts.getOrDefault(res, 0);
                    int ratio = gameplay.getBestTradeRatio(res, this);
                    return owned >= ratio + 1; // Keep at least 1
                })
                .collect(Collectors.toList());

        if (tradable.isEmpty()) return null; // No viable option

        // Prioritize based on abundance minus production value
        Map<String, Integer> productionScore = new HashMap<>();
        for (String res : tradable) {
            int score = 0;
            for (Vertex v : getSettlements()) {
                for (Tile tile : gameplay.getBoard().getTiles()) {
                    if (tile.getVertices().contains(v)
                            && tile.getResourcetype().toString().equals(res)) {
                        score += getDiceProbabilityValue(tile.getTileDiceNumber());
                    }
                }
            }
            productionScore.put(res, score);
        }

        // Rank: abundance first, then lowest production value
        tradable.sort(Comparator
                .comparingInt((String res) -> -resourceAmounts.get(res))  // Most owned first
                .thenComparingInt(productionScore::get));                 // Least useful production

        return tradable.get(0);
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
        if (Platform.isFxApplicationThread()) {
            System.err.println("AI pause called on JavaFX Application Thread!");
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
    public EnumMap<Strategy, Integer> getStrategyUsageMap() {
        return strategyUsageMap;
    }
    public int getMaxStrategyAttempts() {
        return MAX_STRATEGY_ATTEMPTS;
    }

}
