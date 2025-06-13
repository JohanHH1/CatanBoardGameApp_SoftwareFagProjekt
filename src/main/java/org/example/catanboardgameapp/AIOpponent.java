package org.example.catanboardgameapp;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import org.example.catanboardgameviews.CatanBoardGameView;
import org.example.catanboardgameviews.CatanBoardGameView;
import org.example.catanboardgameviews.MenuView;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final MenuView menuView;
    private final EnumMap<Strategy, Integer> strategyUsageMap = new EnumMap<>(Strategy.class);
    private ThinkingSpeed thinkingSpeed = ThinkingSpeed.EXTREME; // Default
    private static final int MAX_STRATEGY_ATTEMPTS = 20;

    //__________________________________CONSTRUCTOR___________________________________________//
    public AIOpponent(int playerId, Color color, StrategyLevel level, Gameplay gameplay) {
        super(playerId, color, gameplay);
        this.strategyLevel = level;
        this.drawOrDisplay = gameplay.getDrawOrDisplay();
        this.gameplay = gameplay;
        this.menuView = gameplay.getMenuView();
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

        if (hasLessThanMaxAllowedCities() && canUpgradeToCityNow()) {
            selected = Strategy.CITYUPGRADER;
        }
        else if (hasLessThanMaxAllowedCities() &&
                hasAlmostEnoughResourcesForCityUpgrade() &&
                hasSettlementThatCanBeUpgradedToCity() &&
                hasOreAndWheatTiles()) {
            selected = Strategy.CITYUPGRADER;
        }
        else if (hasLessThanMaxAllowedSettlements() && !getValidSettlementSpots(gameplay).isEmpty()) {
            selected = Strategy.SETTLEMENTPLACER;
        }
        else if (hasLessThanMaxAllowedRoads() &&
                gameplay.getBoard().getEdges().stream().anyMatch(gameplay::isValidRoadPlacement)) {
            selected = Strategy.ROADBUILDER;
        }
        else {
            selected = Strategy.NONE;
        }

        strategyUsageMap.merge(selected, 1, Integer::sum);
        return selected;
    }

    private Strategy determineMediumStrategy(Gameplay gameplay) {
        Strategy selected;

        if (hasLessThanMaxAllowedCities() && canUpgradeToCityNow()) {
            selected = Strategy.CITYUPGRADER;
        }
        else if (hasLessThanMaxAllowedCities() &&
                hasAlmostEnoughResourcesForCityUpgrade() &&
                hasSettlementThatCanBeUpgradedToCity()
        ){
            selected = Strategy.CITYUPGRADER;
        }
        else if (hasLessThanMaxAllowedSettlements() && !getValidSettlementSpots(gameplay).isEmpty()) {
            selected = Strategy.SETTLEMENTPLACER;
        }
        else if (shouldUseResources(gameplay, 10)) {
            selected = Strategy.USERESOURCES;
        }
        else if (hasLessThanMaxAllowedRoads() &&
                gameplay.getBoard().getEdges().stream().anyMatch(gameplay::isValidRoadPlacement)) {
            selected = Strategy.ROADBUILDER;
        }
        else {
            selected = Strategy.NONE;
        }
        strategyUsageMap.merge(selected, 1, Integer::sum);
        return selected;
    }

    private Strategy determineHardStrategy(Gameplay gameplay) {
        Strategy selected = Strategy.NONE;
        if (gameplay.getCurrentPlayer().getPlayerScore() == 9) {
            System.out.println("Do anything to just WIN now");
            // Build city to instant win if possible
            if (hasLessThanMaxAllowedCities() && canUpgradeToCityNow()) {
                tryBuildCity(gameplay, gameplay.getCatanBoardGameView().getBoardGroup(), selected);
            }
            // Build settlement to instant win if possible
            else if (hasLessThanMaxAllowedSettlements() && !getValidSettlementSpots(gameplay).isEmpty() && canAffordSettlement()) {
                tryBuildSettlement(gameplay, gameplay.getCatanBoardGameView().getBoardGroup(), selected);
            }
            // make this canGetLongestRoad function
//            else if (canGetLongestRoad) {
//                tryBuildConnectedRoad(gameplay, gameplay.getCatanBoardGameView().getBoardGroup());
//            }
        }
        if (!earlyGame() && shouldGoForLongestRoad(gameplay)) {
            selected = Strategy.LONGESTROAD;
        }
        else if (!earlyGame() && shouldGoForBiggestArmy(gameplay)) {
            selected = Strategy.BIGGESTARMY;
        }
        else if (hasLessThanMaxAllowedCities() && canUpgradeToCityNow()) {
            selected = Strategy.CITYUPGRADER;
        } else if (
                hasLessThanMaxAllowedCities() &&
                        hasAlmostEnoughResourcesForCityUpgrade() &&
                        hasSettlementThatCanBeUpgradedToCity() &&
                        hasOreAndWheatTiles() &&
                        !shouldUseResources(gameplay, 13)
        ) {
            selected = Strategy.CITYUPGRADER;
        } else if (hasLessThanMaxAllowedSettlements() && !getValidSettlementSpots(gameplay).isEmpty()) {
            selected = Strategy.SETTLEMENTPLACER;
        } else if (canAffordDevCard() && !gameplay.getShuffledDevelopmentCards().isEmpty()) {
            selected = Strategy.DEVELOPMENTCARDBYER;
        } else if (shouldUseResources(gameplay, 10)) {
            selected = Strategy.USERESOURCES;
        } else if (hasLessThanMaxAllowedRoads() && gameplay.getBoard().getEdges().stream().anyMatch(gameplay::isValidRoadPlacement)) {
            selected = Strategy.ROADBUILDER;
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
                case CITYUPGRADER -> moveMade = tryBuildCity(gameplay, boardGroup, strategy);
                case SETTLEMENTPLACER -> moveMade = tryBuildSettlement(gameplay, boardGroup, strategy);
                case ROADBUILDER -> moveMade = tryBuildRoad(gameplay, boardGroup);
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
            view.runOnFX(() -> view.logToGameLog("\n" + gameplay.getCurrentPlayer() + " (MEDIUM) is using strategy: " + strategy));
            moveMade = false;
            switch (strategy) {
                case CITYUPGRADER -> moveMade = tryBuildCity(gameplay, boardGroup, strategy);
                case SETTLEMENTPLACER -> moveMade = tryBuildSettlement(gameplay, boardGroup, strategy);
                case USERESOURCES -> {
                    moveMade = tryBankTrade(gameplay, strategy);
                    if (!moveMade) {
                        System.out.println("I CHOSE USE RESOURCE STRATEGY BUT I COULD NOT TRADE WITH BANK AT ALL");
                        // fallback: try other useful actions
                        moveMade = tryBuildCity(gameplay, boardGroup, strategy)
                                || tryBuildSettlement(gameplay, boardGroup, strategy)
                                || tryBuildRoad(gameplay, boardGroup);
                        System.out.println("I managed to make a different kind of move: " + moveMade);
                    }
                }
                case ROADBUILDER -> moveMade = tryBuildRoad(gameplay, boardGroup);
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
            view.runOnFX(() -> view.logToGameLog("\n" + gameplay.getCurrentPlayer() + " (HARD) is using strategy: " + strategy));
            moveMade = false;
            switch (strategy) {
                case CITYUPGRADER -> moveMade = tryBuildCity(gameplay, boardGroup, strategy);
                case SETTLEMENTPLACER -> moveMade = tryBuildSettlement(gameplay, boardGroup, strategy);
                case DEVELOPMENTCARDBYER, BIGGESTARMY -> moveMade = tryBuyDevCard(gameplay);
                case USERESOURCES -> {
                    moveMade = tryBankTrade(gameplay, strategy);
                    if (!moveMade) {
                        System.out.println("I CHOSE USE RESOURCE STRATEGY BUT I COULD NOT TRADE WITH BANK AT ALL");
                        // fallback: try other useful actions
                        moveMade = tryBuildCity(gameplay, boardGroup, strategy)
                                || tryBuildSettlement(gameplay, boardGroup, strategy)
                                || tryBuildRoad(gameplay, boardGroup)
                                || tryBuyDevCard(gameplay);
                        System.out.println("I managed to make a different kind of move: " + moveMade);
                    }
                }                case LONGESTROAD -> moveMade = tryBuildConnectedRoad(gameplay, boardGroup);
                case ROADBUILDER -> moveMade = tryBuildRoad(gameplay, boardGroup);
            }
        } while (moveMade && --attempts > 0);

        // before finishing their turn, always try to play development card if they have any
        tryPlayDevCard(gameplay, boardGroup);
        view.runOnFX(() -> {
            view.logToGameLog(gameplay.getCurrentPlayer() + " (" + strategyLevel.name() + ") has ended their turn.");
            gameplay.nextPlayerTurn();
        });
    }

    private boolean tryBuildCity(Gameplay gameplay, Group boardGroup, Strategy strategy) {
        // Most important is to have a settlement that can be upgraded
        if (!hasSettlementThatCanBeUpgradedToCity()) {
            if (strategy != Strategy.USERESOURCES && strategy != Strategy.NONE) {
                System.out.println("ERROR THIS SHOULD NEVER HAPPENS, WRONG (CITY) STRATEGY CHOSEN PAL!!!!!");
            }
            return false;
        }
        // Then check if you have enough resources
        if (!canAffordCity()) {
            // Attempt trade to fix the missing resources
            boolean successfulTrade = tryBankTrade(gameplay, Strategy.CITYUPGRADER);
            if (!successfulTrade) return false;
            // Retry after trade
            if (!canAffordCity()) return false;
        }
        Vertex best = null;
        int bestScore = Integer.MIN_VALUE;

        for (Vertex v : getSettlements()) {
            int score = getSettlementDiceValue(v, gameplay);
            if (score > bestScore) {
                best = v;
                bestScore = score;
            }
        }
        if (best != null) {
            BuildResult result = gameplay.buildCity(best);
            if (result == BuildResult.UPGRADED_TO_CITY) {
                String msg = gameplay.getCurrentPlayer() + " UPGRADED TO A CITY";
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

    private boolean tryBuildSettlement(Gameplay gameplay, Group boardGroup, Strategy strategy) {
        // Most important: Check if there is a spot available to build a settlement
        if (getValidSettlementSpots(gameplay).isEmpty()) {
            if (strategy != Strategy.USERESOURCES && strategy != Strategy.NONE) {
                System.out.println("ERROR THIS SHOULD NEVER HAPPENS, WRONG (SETTLEMENT) STRATEGY CHOSEN PAL!!!!!");
            }
            return false;
        }
        // Check if having all the resources
        if (!canAffordSettlement()) {
            // Attempt trade to fix the missing resources

            boolean successfulTrade = tryBankTrade(gameplay, Strategy.SETTLEMENTPLACER);
            if (!successfulTrade) return false;
            // Retry after trade
            if (!canAffordSettlement()) return false;
        }
        Vertex bestSpot = null;
        int bestScore = Integer.MIN_VALUE;

        for (Vertex v : getValidSettlementSpots(gameplay)) {
            int score = getSmartSettlementScore(v, gameplay);
            if (score > bestScore) {
                bestScore = score;
                bestSpot = v;
            }
        }
        if (bestSpot != null) {
            if (!gameplay.isValidSettlementPlacement(bestSpot)) {
                System.out.println("Warning: Best spot became invalid before build");
                return false;
            }
            BuildResult result = gameplay.buildSettlement(bestSpot);
            if (result == BuildResult.SUCCESS) {
                String msg = gameplay.getCurrentPlayer() + " successfully built a Settlement";
                Circle circle = new Circle(bestSpot.getX(), bestSpot.getY(), 16.0 / gameplay.getBoardRadius());
                Vertex finalBestSpot = bestSpot;
                gameplay.getCatanBoardGameView().runOnFX(() -> {
                    drawOrDisplay.drawSettlement(circle, finalBestSpot, boardGroup);
                    gameplay.getCatanBoardGameView().logToGameLog(msg);
                });
                return true;
            }
        }
        System.out.println("Failed to build settlement ");
        return false;
    }

    private boolean tryBuildRoad(Gameplay gameplay, Group boardGroup) {
        // Check if there is any place to build a Road available
        List<Edge> validRoads = gameplay.getBoard().getEdges().stream()
                .filter(gameplay::isValidRoadPlacement)
                .toList();
        if (validRoads.isEmpty()) return false;

        boolean hasRoadResources = hasResources("Wood", 1) && hasResources("Brick", 1);
        if (!hasRoadResources) {
            if (!tryBankTrade(gameplay, Strategy.ROADBUILDER)) return false;
            if (!hasResources("Brick", 1) || !hasResources("Wood", 1)) return false;
        }

        Edge bestEdge = null;
        int bestScore = Integer.MIN_VALUE;

        for (Edge edge : validRoads) {
            Vertex source = (getSettlements().contains(edge.getVertex1())) ? edge.getVertex1()
                    : (getSettlements().contains(edge.getVertex2())) ? edge.getVertex2()
                    : edge.getVertex1(); // fallback

            int score = getSmartRoadScore(edge, source, gameplay);
            if (score > bestScore) {
                bestScore = score;
                bestEdge = edge;
            }
        }

        if (bestEdge != null) {
            BuildResult result = gameplay.buildRoad(bestEdge);
            if (result == BuildResult.SUCCESS) {
                String msg = gameplay.getCurrentPlayer() + " successfully built a road";
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

    private boolean tryBuildConnectedRoad(Gameplay gameplay, Group boardGroup) {
        if (!hasResources("Brick", 1) || !hasResources("Wood", 1)) return false;

        Set<Vertex> myEndpoints = getRoads().stream()
                .flatMap(edge -> Stream.of(edge.getVertex1(), edge.getVertex2()))
                .collect(Collectors.toSet());
        List<Edge> candidateEdges = gameplay.getBoard().getEdges().stream()
                .filter(gameplay::isValidRoadPlacement)
                .filter(e -> myEndpoints.contains(e.getVertex1()) || myEndpoints.contains(e.getVertex2()))
                .toList();

        if (candidateEdges.isEmpty()) return false;

        Edge bestEdge = null;
        int bestScore = Integer.MIN_VALUE;

        for (Edge edge : candidateEdges) {
            Vertex source = myEndpoints.contains(edge.getVertex1()) ? edge.getVertex1() : edge.getVertex2();
            int score = getSmartRoadScore(edge, source, gameplay);

           // if (extendsLongestChain(edge)) score += 10;

            if (score > bestScore) {
                bestScore = score;
                bestEdge = edge;
            }
        }
        if (bestEdge != null) {
            BuildResult result = gameplay.buildRoad(bestEdge);
            if (result == BuildResult.SUCCESS) {
                String msg = this + " built a Longest Road-aimed road";
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

                if (target == null || give == null || target.equals(give)) return false;

                int ratio = getBestTradeRatio(give, this);
                int owned = resources.getOrDefault(give, 0);

                if (ratio > 0 && owned >= ratio + 1) {
                    return executeBankTrade(gameplay, give, target, strategy);
                }
                return false;
            }
            default -> {
                return false;
            }
        }

        // Try to trade for any missing resource from targetCost
        for (String need : targetCost.keySet()) {
            int owned = resources.getOrDefault(need, 0);
            int required = targetCost.get(need);
            if (owned >= required) continue;

            for (String give : allTypes) {
                if (give.equals(need)) continue;

                int ratio = getBestTradeRatio(give, this);
                if (ratio <= 0) continue;

                int available = resources.getOrDefault(give, 0);
                if (available >= ratio) {
                    if (executeBankTrade(gameplay, give, need, strategy)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
    private boolean executeBankTrade(Gameplay gameplay, String give, String receive, Strategy strategy) {
        int ratio = getBestTradeRatio(give, this); // Get trade ratio (e.g., 4:1 or 3:1)
        // Check if player has enough of the resource to trade
        if (ratio <= 0 || getResources().getOrDefault(give, 0) < ratio) {
            return false;
        }
        // Deduct given resources
        getResources().put(give, getResources().get(give) - ratio);

        // Add received resource
        getResources().put(receive, getResources().getOrDefault(receive, 0) + 1);

        // Log the trade to the UI via JavaFX thread
        String msg = gameplay.getCurrentPlayer() +
                " traded " + ratio + " " + give +
                " for 1 " + receive + " (Strategy: " + strategy + ")";
        gameplay.getCatanBoardGameView().runOnFX(() -> {
            gameplay.increaseTradeCounter();
            gameplay.getCatanBoardGameView().logToGameLog(msg);
        });
        return true;
    }

    public int getBestTradeRatio(String resource, Player player) {
        int bestRatio = 4;
        for (Harbor harbor : gameplay.getBoard().getHarbors()) {
            if (harbor.usableBy(player)) {
                if (harbor.getType() == Harbor.HarborType.GENERIC) {
                    bestRatio = Math.min(bestRatio, 3);
                } else if (harbor.getType().specific.getName().equals(resource)) {
                    bestRatio = Math.min(bestRatio, 2);
                }
            }
        }
        return bestRatio;
    }

    //_____________________________DEVELOPMENT CARD LOGIC________________________________//
    private boolean tryBuyDevCard(Gameplay gameplay) {
        if (!hasResources("Wool", 1) || !hasResources("Grain", 1)|| !hasResources("Ore", 1) || !gameplay.hasRolledDice() || gameplay.getShuffledDevelopmentCards().isEmpty() ) {return false;}
        gameplay.buyDevelopmentCard();
        return true;
    }

    private boolean tryPlayDevCard(Gameplay gameplay, Group boardGroup) {
        if (hasNoDevelopmentCards()) {
            return false;
        }
        else {
            DevelopmentCard.DevelopmentCardType devCardType = getFirstDevelopmentCard();
            if (devCardType != null) {
                gameplay.getCatanBoardGameView().runOnFX(() ->
                        gameplay.getCatanBoardGameView().logToGameLog(gameplay.getCurrentPlayer() +" just played development card of type: " + devCardType)
                );
                // Call the enum-based AI method
                devCardType.playAsAI(this, gameplay.getDevelopmentCard(), gameplay);

                // Remove it from inventory AFTER play
                getDevelopmentCards().computeIfPresent(devCardType, (k, v) -> (v > 1) ? v - 1 : 0);

                return true;
            }
        }
        return false;
    }

    //__________________________________LONGEST ROAD AND BIGGEST ARMY_________________________________//
    private boolean shouldGoForLongestRoad(Gameplay gameplay) {
        if (earlyGame()) {
            return false;   // dont go for longest road in the early game, it can ruin your game
        }
        boolean closeEnough;
        boolean hasRoadResources = hasResources("Wood", 1) && hasResources("Brick", 1);
        Player currentHolder = gameplay.getLongestRoadManager().getCurrentHolder();
        int myLongest = gameplay.getLongestRoadManager().calculateLongestRoad(this, gameplay.getPlayerList());

        // if no one is LongestRoadManager, try to get longest road right away if already have 4
        if (currentHolder == null) {
            return myLongest == 4 && hasRoadResources;
        }

        int holderLength = gameplay.getLongestRoadManager().calculateLongestRoad(currentHolder, gameplay.getPlayerList());

        // if current longestRoadManager is almost winning - Try sabotage them by overtaking longest road
        if (gameplay.getLongestRoadManager().getCurrentHolder().getPlayerScore() >= 8) {
            gameplay.getCatanBoardGameView().logToGameLog("current Longest Road manager is a big thread, i want to steal it from them!");
            closeEnough = myLongest + 1 >= holderLength;
            return closeEnough;
        }

        // Out-commented for now since it might fuck up the AI logic
//        // If lateGame (i have 8 points or someone else have 7 points)
//        if (lateGame() && getPlayerScore()>=8) {
//            System.out.println("If i get longest Road right now i win the game!!");
//            closeEnough = myLongest + 2 >= holderLength;
//            return closeEnough;
//        }

        // Otherwise, only go for it if you can overtake longest road immediately
        closeEnough = myLongest == holderLength;
        return currentHolder != this && closeEnough && hasRoadResources;
    }

    private boolean shouldGoForBiggestArmy(Gameplay gameplay) {
        if (earlyGame()) {
            return false; // Early game: don’t chase biggest army
        }
        Player currentHolder = gameplay.getBiggestArmy().getCurrentHolder();
        int myKnights = getPlayedKnights();

        // If no one has it, go for it if we're at 2 and can buy a card
        if (currentHolder == null) {
            return myKnights == 2 && canAffordDevCard(); // Setting up for the 3rd knight
        }

        // If I already hold it, no need to "go for it"
        if (currentHolder == this) {
            // But if someone else is threatening my title, maybe play more
            int maxOpponentKnights = gameplay.getPlayerList().stream()
                    .filter(p -> p != this)
                    .mapToInt(Player::getPlayedKnights)
                    .max().orElse(0);

            // If someone else is close to stealing it, defend it
            return maxOpponentKnights + 1 > myKnights && canAffordDevCard();
        }

        int holderKnights = currentHolder.getPlayedKnights();

        // Case: current holder is near victory – steal for 2 points
        if (currentHolder.getPlayerScore() >= 8) {
            return myKnights + 1 > holderKnights && canAffordDevCard();
        }

        // Case: late game, and stealing biggest army could win the game
        if (lateGame() && gameplay.getCurrentPlayer().getPlayerScore() >= 8) {
            System.out.println("Trying to win the game RIGHT NOW by buying DEV CARD!");
            return myKnights + 1 > holderKnights && canAffordDevCard();
        }
        // General case: don't go for it unless you can beat the holder right away
        boolean canOvertake = myKnights + 1 > holderKnights;
        return canOvertake && canAffordDevCard();
    }

    //_____________________________________ROBBER LOGIC_____________________________________//
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

            // Production score: total dice weight from settlements
            int productionScore = getProductionScore(res);

            // Base score: more owned = more discardable
            int score = amountOwned * 3;

            // Subtract based on production (more produced → less discardable)
            score -= productionScore * 2;

            // Penalize if resource is needed for strategy
            if (neededResources.contains(res)) {
                score -= 8;
            }
            // Penalize if resource is easy to trade (2:1 or 3:1 harbor = valuable)
            int ratio = getBestTradeRatio(res, this);
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
                gameplay.getCatanBoardGameView().runOnFX(() -> gameplay.getCatanBoardGameView().logToGameLog(log.toString()));
            }
        }
        return discardMap;
    }

    //__________________________________HELPER / CALCULATION FUNCTIONS____________________________________//
    private int getProductionScore(String resourceType) {
        int productionScore = 0;
        for (Vertex v : getSettlements()) {
            for (Tile t : gameplay.getBoard().getTiles()) {
                if (t.getVertices().contains(v) &&
                        t.getResourcetype().toString().equals(resourceType)) {
                    productionScore += getDiceProbabilityValue(t.getTileDiceNumber());
                }
            }
        }
        return productionScore;
    }

    private int getSmartSettlementScore(Vertex vertex, Gameplay gameplay) {
        int diceValue = getSettlementDiceValue(vertex, gameplay);     // Primary: total dice probability
        int diversity = getResourceDiversityScore(vertex, gameplay);  // Secondary: unique resource types
        int newResources = countMissingResourcesCovered(vertex, gameplay); // Bonus: new types for player
        boolean blocked = isBlocked(vertex, gameplay);

        // Weighted scoring (Denne kan måske ændres eller gøres bedre)
        int score = (diceValue * 8) + (diversity * 3) + (newResources * 2);

        // Heavily penalize blocked vertices (should never pick them)
        if (blocked) score -= 100;
        return score;
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

    private int getResourceDiversityScore(Vertex vertex, Gameplay gameplay) {
        Set<Resource.ResourceType> resourceTypes = new HashSet<>();
        for (Tile tile : gameplay.getBoard().getTiles()) {
            if (tile.getVertices().contains(vertex)) {
                resourceTypes.add(tile.getResourcetype());
            }
        }
        return resourceTypes.size();
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

    private boolean isBlocked(Vertex vertex, Gameplay gameplay) {
        for (Player player : gameplay.getPlayerList()) {
            if (player != this && player.getSettlements().contains(vertex)) {
                return true;
            }
        }
        return false;
    }

    // Denne function skal forbedres så AI altid forsøger at bevæge sig et sted hen hvor de kan bygge settlement.
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

    private boolean shouldUseResources(Gameplay gameplay, int maxResources) {
        Map<String, Integer> resources = getResources();
        // Total resources must be at least 10
        int totalResources = resources.values().stream().mapToInt(Integer::intValue).sum();
        return totalResources >= maxResources;
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
            case DEVELOPMENTCARDBYER -> {
                if (getResources().getOrDefault("Ore", 0) < 1) needed.add("Ore");
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
        // if current player has 8 points or more, its lateGame
        if (getPlayerScore() >= 8) return true;
        // or if someone else has 7 points or more
        return gameplay.getPlayerList().stream()
                .anyMatch(p -> p != this && p.getPlayerScore() >= 7);
    }

    public boolean earlyGame() {
        // Current player has 4 or fewer points → early game
        if (getPlayerScore() <= 4) return true;

        // No other player has more than 4 points → still early game
        return gameplay.getPlayerList().stream()
                .filter(p -> p != this)
                .noneMatch(p -> p.getPlayerScore() > 4);
    }

    private boolean hasAlmostEnoughResourcesForCityUpgrade() {
        int ore = getResources().getOrDefault("Ore", 0);
        int grain = getResources().getOrDefault("Grain", 0);
        int total = ore + grain;
        // Already has full resources → return true
        if (ore >= 3 && grain >= 2) return true;
        // Has 3 out of 5 needed resources → consider planning
        return total >= 3;
    }

    private boolean hasSettlementThatCanBeUpgradedToCity() {
        return getSettlements().stream().anyMatch(v -> !v.isCity());
    }

    public boolean canUpgradeToCityNow() {
        if (!hasSettlementThatCanBeUpgradedToCity()) {return false;}
        return canAffordCity();
    }

    public boolean hasOreAndWheatTiles() {
        boolean hasOre = false;
        boolean hasGrain = false;

        // Combine settlements and cities into a single list of owned vertices
        List<Vertex> ownedStructures = new ArrayList<>();
        ownedStructures.addAll(getSettlements());
        ownedStructures.addAll(getCities());

        for (Vertex vertex : ownedStructures) {
            for (Tile tile : vertex.getAdjacentTiles()) {
                if (tile.isSea()) continue;

                Resource.ResourceType resource = tile.getResourcetype();
                if (resource == Resource.ResourceType.ORE) {
                    hasOre = true;
                } else if (resource == Resource.ResourceType.GRAIN) {
                    hasGrain = true;
                }
                if (hasOre && hasGrain) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasResources(String type, int amount) {
        return getResources().getOrDefault(type, 0) >= amount;
    }

    // this function might or might not work as intended, needs more testing...
    private List<Vertex> getValidSettlementSpots(Gameplay gameplay) {
        return gameplay.getBoard().getVertices().stream()
                .filter(gameplay::isValidSettlementPlacement)
                .collect(Collectors.toList());
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

                int totalProd = getProductionScore(res);
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
                    int ratio = getBestTradeRatio(res, this);
                    return owned >= ratio + 1; // Keep at least 1
                })
                .collect(Collectors.toList());

        if (tradable.isEmpty()) return null; // No viable option

        // Prioritize based on abundance minus production value
        Map<String, Integer> productionScore = new HashMap<>();
        for (String res : tradable) {
            int score = getProductionScore(res);
            productionScore.put(res, score);
        }

        // Rank: abundance first, then lowest production value
        tradable.sort(Comparator
                .comparingInt((String res) -> -resourceAmounts.get(res))  // Most owned first
                .thenComparingInt(productionScore::get));                 // Least useful production

        return tradable.get(0);
    }
    public String chooseSmartResourceToMonopoly(Gameplay gameplay){
        // by
        // settlement
        // hvad den kan bytte frest til via havn eller 4:1

        List<Player> opponents = gameplay.getPlayerList().stream()
                .filter(p -> p != this)
                .toList();

        Map<String, Integer> totalOpponentResources = new HashMap<>();

        for (Player p : opponents) {
            for (Map.Entry<String, Integer> entry : p.getResources().entrySet()) {
                totalOpponentResources.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }
        // --- Step 1: Check if we can steal enough to build a full city ---
        int oreHave = this.getResources().getOrDefault("Ore", 0);
        int grainHave = this.getResources().getOrDefault("Grain", 0);
        int woolHave = this.getResources().getOrDefault("Wool", 0);
        int woodHave = this.getResources().getOrDefault("Wood", 0);
        int brickHave = this.getResources().getOrDefault("Brick", 0);

        int oreFromOpponents = totalOpponentResources.getOrDefault("Ore", 0);
        int grainFromOpponents = totalOpponentResources.getOrDefault("Grain", 0);
        int woolFromOpponents = totalOpponentResources.getOrDefault("Wool", 0);
        int woodFromOpponents = totalOpponentResources.getOrDefault("Wood", 0);
        int brickFromOpponents = totalOpponentResources.getOrDefault("Brick", 0);

        System.out.println("ore to steal: " +oreFromOpponents );
        System.out.println("Grain to steal: " +grainFromOpponents );
        System.out.println("Wool to steal: " +woolFromOpponents );
        System.out.println("Wood to steal: " +woodFromOpponents );
        System.out.println("Brick to steal: " +brickFromOpponents );

        // enough to build a city
        int oreNeed = Math.max(0, 3 - oreHave);
        int grainNeed = Math.max(0, 2 - grainHave);

        if ((oreFromOpponents >= oreNeed) && grainNeed==0 ){
            System.out.println("steals ore for city");
            return "Ore";
        } else if (grainFromOpponents >= grainNeed && oreNeed==0) {
            System.out.println("steals grain for city");
            return"Grain";
        }

        // enought to build a settelment:
        grainNeed = Math.max(0, 1 - grainHave);
        int woodNeed = Math.max(0, 1 - woodHave);
        int woolNeed = Math.max(0, 1 - woolHave);
        int brickNeed = Math.max(0, 1 - brickHave);
        if (grainNeed > 0 && grainNeed <= grainFromOpponents
                && woodNeed == 0 && woolNeed == 0 && brickNeed == 0) {
            System.out.println("steals grain for settlement");
            return "Grain";
        } else if (woodNeed > 0 && woodNeed <= woodFromOpponents
                && grainNeed == 0 && woolNeed == 0 && brickNeed == 0) {
            System.out.println("steals Wood for settlement");
            return "Wood";
        } else if (woolNeed > 0 && woolNeed <= woolFromOpponents
                && grainNeed == 0 && woodNeed == 0 && brickNeed == 0) {
            System.out.println("steals Wool for settlement");
            return "Wool";
        } else if (brickNeed > 0 && brickNeed <= brickFromOpponents
                && grainNeed == 0 && woolNeed == 0 && woodNeed == 0) {
            System.out.println("steals Brick for settlement");
            return "Brick";
        }
        Map.Entry<String, Integer> mostCommon = getMostCommonOpponentResourceWithCount(totalOpponentResources);
        String mostCommonResourceName = mostCommon.getKey();
        int count = mostCommon.getValue();
        List<Harbor.HarborType> harborTypes = new ArrayList<>();
        for (Harbor harbor : gameplay.getBoard().getHarbors()) {
            if (harbor.usableBy(this)) {
                harborTypes.add(harbor.getType());
            }
        }
        String bestTradeResource = null;
        int bestTradeValue = 0;

        for (String resource : List.of("Ore", "Grain", "Wool", "Wood", "Brick")) {
            int available = totalOpponentResources.getOrDefault(resource, 0);
            int tradeRatio = 4; // default (not tradable)

            // Check for specific 2:1 harbor
            for (Harbor.HarborType type : harborTypes) {
                if (type.specific != null && type.specific.name().equalsIgnoreCase(resource)) {
                    tradeRatio = 2;
                    break;
                }
            }

            // If no 2:1 harbor, check for generic 3:1
            if (tradeRatio == 4) {
                for (Harbor.HarborType type : harborTypes) {
                    if (type == Harbor.HarborType.GENERIC) {
                        tradeRatio = 3;
                        break;
                    }
                }
            }

            // If we have a usable harbor (2:1 or 3:1), compute trades
            if (tradeRatio < 4) {
                int tradeCount = available / tradeRatio;
                if (tradeCount > bestTradeValue) {
                    bestTradeValue = tradeCount;
                    bestTradeResource = resource;
                }
            }
        }
        System.out.println("steals the best resource: " + bestTradeResource);
        return bestTradeResource;
    }
    private Map.Entry<String, Integer> getMostCommonOpponentResourceWithCount(Map<String, Integer> totalOpponentResources) {
        return totalOpponentResources.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(Map.entry("Grain", 0)); // fallback
    }

    public Map<String, Integer> chooseResourcesForYearOfPlenty() {
        Strategy currentStrategy = determineStrategy();
        Set<String> needed = getNeededResourcesForStrategy(currentStrategy);

        Map<String, Integer> priorityMap = new HashMap<>();
        for (String res : needed) {
            int owned = getResources().getOrDefault(res, 0);

            int prodScore = getProductionScore(res);
            int tradeRatio = getBestTradeRatio(res, this);
            int score = (5 - owned) * 3 + (5 - prodScore) * 2 + tradeRatio * 2;
            priorityMap.put(res, score);
        }

        List<String> topTwo = priorityMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(2)
                .map(Map.Entry::getKey)
                .toList();

        Map<String, Integer> result = new HashMap<>();
        for (String res : topTwo) {
            result.put(res, result.getOrDefault(res, 0) + 1);
        }

        // If only 1 resource chosen, double it
        if (result.size() == 1) {
            String key = topTwo.get(0);
            result.put(key, 2);
        } else if (result.size() < 2) {
            // fallback to random if nothing found
            result.put("Ore", 1);
            result.put("Grain", 1);
        }

        return result;
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

    public boolean canAffordDevCard() {
        return hasResources("Wool", 1) && hasResources("Grain", 1) && hasResources("Ore", 1);
    }
    
    public boolean canAffordSettlement() {
        return (hasResources("Brick", 1) || hasResources("Wood", 1) ||
                hasResources("Wool", 1) || hasResources("Grain", 1));
    }
    public boolean canAffordCity() {
        return hasResources("Ore", 3) && hasResources("Grain", 2);
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

    private boolean hasLessThanMaxAllowedCities() {
        return getCities().size() < menuView.getMaxCities();
    }
    private boolean hasLessThanMaxAllowedSettlements() {
        return getSettlements().size() < menuView.getMaxSettlements();
    }
    private boolean hasLessThanMaxAllowedRoads() {
        return getRoads().size() < menuView.getMaxRoads();
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

}
