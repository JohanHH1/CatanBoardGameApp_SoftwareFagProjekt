package org.example.catanboardgameapp;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import org.example.catanboardgameviews.CatanBoardGameView;
import org.example.catanboardgameviews.MenuView;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//___________________________AI OPPONENT FOR CATAN GAME___________________________//
public class AIOpponent extends Player {

    //___________________________STRATEGY AND STRATEGY LEVEL ENUMS___________________________//
    // Defines AI intelligence level
    public enum StrategyLevel {
        EASY, MEDIUM, HARD
    }

    // AI decision types
    public enum Strategy {
        ROADBUILDER, CITYUPGRADER, SETTLEMENTPLACER, LONGESTROAD, BIGGESTARMY, DEVELOPMENTCARDBUYER, USERESOURCES, NONE
    }

    // Controls AI delay for realism
    public enum ThinkingSpeed {
        SLOW, MEDIUM, FAST, EXTREME
    }

    //___________________________________FIELDS______________________________________________//
    private final Gameplay gameplay;
    private final StrategyLevel strategyLevel;
    private final DrawOrDisplay drawOrDisplay;          // UI drawing handler
    private final MenuView menuView;                    // View settings like max builds
    private final EnumMap<Strategy, Integer> strategyUsageMap = new EnumMap<>(Strategy.class); // Track strategy usage
    private ThinkingSpeed thinkingSpeed = ThinkingSpeed.EXTREME; // Default AI speed
    private static final int MAX_STRATEGY_ATTEMPTS = 20; // Max retries for making a move

    //__________________________________CONSTRUCTOR___________________________________________//
    public AIOpponent(int playerId, Color color, StrategyLevel level, Gameplay gameplay) {
        super(playerId, color, gameplay);
        this.strategyLevel = level;
        this.drawOrDisplay = gameplay.getDrawOrDisplay();
        this.gameplay = gameplay;
        this.menuView = gameplay.getMenuView();
        for (Strategy strategy : Strategy.values()) {
            strategyUsageMap.put(strategy, 0); // Init usage counts to zero
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

        // Define the logic in a separate thread-safe task
        Runnable logic = () -> {
            // Pause before AI acts
            pauseBeforeMove();

            // Gather all vertex candidates on the board
            List<Vertex> candidates = new ArrayList<>(gameplay.getBoard().getVertices());
            Vertex chosenSettlement = null;

            // Choose a random valid settlement spot for easy
            if (strategyLevel == StrategyLevel.EASY) {
                Collections.shuffle(candidates);
                for (Vertex v : candidates) {
                    if (gameplay.isValidSettlementPlacement(v)) {
                        chosenSettlement = v;
                        break;
                    }
                }
            } else {
                // Score each valid vertex and choose the best
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

            // If no settlement spot was found, abort
            if (chosenSettlement == null) {
                view.runOnFX(() -> {
                    view.logToGameLog("AI " + getPlayerId() + " could not find a valid initial settlement.");
                    view.hideAITurnOverlay();
                });
                return;
            }

            // Try to build settlement
            BuildResult settlementResult = gameplay.buildInitialSettlement(chosenSettlement);
            if (settlementResult != BuildResult.SUCCESS) {
                view.runOnFX(() -> {
                    view.logToGameLog("AI " + getPlayerId() + " failed to place initial settlement.");
                    view.hideAITurnOverlay();
                });
                return;
            }

            // Draw settlement
            Circle circle = new Circle(chosenSettlement.getX(), chosenSettlement.getY(), 16.0 / gameplay.getBoardRadius());
            Vertex finalSettlement = chosenSettlement;

            view.runOnFX(() -> {
                finalSettlement.setOwner(gameplay.getCurrentPlayer());
                drawOrDisplay.drawSettlement(circle, finalSettlement, boardGroup);
            });

            //_________________________CHOOSE INITIAL ROAD_________________________//
            Edge chosenEdge = null;
            List<Edge> edges = gameplay.getBoard().getEdges();

            // Pick a random valid edge connected to settlement for easy
            if (strategyLevel == StrategyLevel.EASY) {
                Collections.shuffle(edges);
                for (Edge edge : edges) {
                    if (edge.isConnectedTo(chosenSettlement) && gameplay.isValidRoadPlacement(edge)) {
                        chosenEdge = edge;
                        break;
                    }
                }
            } else {
                // Score each valid edge and choose the best (worst, since go away from other people)
                int worstEdgeScore = Integer.MAX_VALUE;
                for (Edge edge : edges) {
                    if (!edge.isConnectedTo(chosenSettlement) || !gameplay.isValidRoadPlacement(edge)) continue;
                    int score = getSmartRoadScore(edge, chosenSettlement, gameplay, true);
                    if (score < worstEdgeScore) {
                        worstEdgeScore = score;
                        chosenEdge = edge;
                    }
                }
            }

            // Try to place road
            if (chosenEdge != null) {
                BuildResult roadResult = gameplay.buildRoad(chosenEdge);
                if (roadResult == BuildResult.SUCCESS) {
                    Line line = new Line(
                            chosenEdge.getVertex1().getX(), chosenEdge.getVertex1().getY(),
                            chosenEdge.getVertex2().getX(), chosenEdge.getVertex2().getY()
                    );

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

        // Start logic in background thread if currently on FX thread
        if (Platform.isFxApplicationThread()) {
            new Thread(logic).start();
        } else {
            logic.run();  // Already in background thread
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

    //______________________________CHOOSING STRATEGY LOGIC_______________________________________//
    public Strategy determineStrategy(boolean aiMakingMove) {
        return switch (strategyLevel) {
            case EASY -> determineEasyStrategy(gameplay);
            case MEDIUM -> determineMediumStrategy(gameplay);
            case HARD -> determineHardStrategy(gameplay, aiMakingMove);
        };
    }
    private Strategy determineEasyStrategy(Gameplay gameplay) {
        Strategy selected;

        if (hasLessThanMaxAllowedCities() && canUpgradeToCityNow()) {
            selected = Strategy.CITYUPGRADER;
        }
        else if (canAffordSettlement() && hasLessThanMaxAllowedSettlements() && !getValidSettlementSpots(gameplay).isEmpty()) {
            selected = Strategy.SETTLEMENTPLACER;
        }
        else if (hasLessThanMaxAllowedCities() &&
                hasAlmostEnoughResourcesForCityUpgrade() &&
                hasSettlementThatCanBeUpgradedToCity())
        {
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
        else if (canAffordSettlement() && hasLessThanMaxAllowedSettlements() && !getValidSettlementSpots(gameplay).isEmpty()) {
            selected = Strategy.SETTLEMENTPLACER;
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
        else if (shouldUseResources(10)) {
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

    private Strategy determineHardStrategy(Gameplay gameplay, boolean aiMakingMove) {
        Strategy selected = Strategy.NONE;
        // If 9 points, TRY WIN NOW!
        if (aiMakingMove && gameplay.getCurrentPlayer().getPlayerScore() == 9) {
            // Build city to instant win if possible
            if (hasLessThanMaxAllowedCities() && canUpgradeToCityNow()) {
                gameplay.getCatanBoardGameView().logToGameLog(gameplay.getCurrentPlayer() + " is winning the game right now by upgrading to a City!");
                tryBuildCity(gameplay, gameplay.getCatanBoardGameView().getBoardGroup());
                gameplay.stopAllAIThreads();
                return Strategy.CITYUPGRADER;
            }
            // Build settlement to instant win if possible
            else if (hasLessThanMaxAllowedSettlements() && !getValidSettlementSpots(gameplay).isEmpty() && canAffordSettlement()) {
                gameplay.getCatanBoardGameView().logToGameLog(gameplay.getCurrentPlayer() + " is winning the game right now by building a Settlement!");
                tryBuildSettlement(gameplay, gameplay.getCatanBoardGameView().getBoardGroup());
                gameplay.stopAllAIThreads();
                return Strategy.SETTLEMENTPLACER;
            }
            else if (canAffordRoad() && (gameplay.getCurrentPlayer() != gameplay.getLongestRoadManager().getCurrentHolder()) && canGetLongestRoad(gameplay)) {
                gameplay.getCatanBoardGameView().logToGameLog(gameplay.getCurrentPlayer() + " is winning the game right now by becoming LongestRoadManager!");
                tryBuildLongestRoad(gameplay, gameplay.getCatanBoardGameView().getBoardGroup());
                gameplay.stopAllAIThreads();
                return Strategy.LONGESTROAD;
            }
        }
        // Stop AI thread if the AI just won the game
        if (gameplay.isGameOver()) {
            gameplay.stopAllAIThreads();
            return Strategy.NONE;
        }
        // ELSE - Check and evaluate all possible strategies

        // 1. Longest Road and Biggest Army priority
        if (!earlyGame() && shouldGoForLongestRoad(gameplay)) {
            selected = Strategy.LONGESTROAD;
        }
        else if (!earlyGame() && shouldGoForBiggestArmy(gameplay) && !gameplay.getShuffledDevelopmentCards().isEmpty()) {
            selected = Strategy.BIGGESTARMY;
        }

        // 2. Upgrade to city right now if you can
        else if (hasLessThanMaxAllowedCities() && canUpgradeToCityNow()) {
            selected = Strategy.CITYUPGRADER;
        // 3. Place a Settlement right now if you can
        } else if (canAffordSettlement() && !getValidSettlementSpots(gameplay).isEmpty()) {
                selected = Strategy.SETTLEMENTPLACER;
        // 4. If you cant build a new settlement anywhere, focus on roads now
        } else if (canAffordRoad() && earlyGame() && getValidSettlementSpots(gameplay).isEmpty() && hasLessThanMaxAllowedRoads() && gameplay.getBoard().getEdges().stream().anyMatch(gameplay::isValidRoadPlacement)) {
            selected = Strategy.ROADBUILDER;
        // 5. If you can almost upgrade to city, wait until you can do it
        } else if (
                hasLessThanMaxAllowedCities() &&
                        hasAlmostEnoughResourcesForCityUpgrade() &&
                        hasSettlementThatCanBeUpgradedToCity()
        ) {
            selected = Strategy.CITYUPGRADER;
        // 6. If you have a valid spot to build a settlement, do it or wait till you can.
        } else if (shouldWaitForSettlementPlacement() && !getValidSettlementSpots(gameplay).isEmpty()) {
            selected = Strategy.SETTLEMENTPLACER;
        // 7. Buy a developmentcard if you can
        } else if (canAffordDevCard() && !gameplay.getShuffledDevelopmentCards().isEmpty()) {
            selected = Strategy.DEVELOPMENTCARDBUYER;
        // 8. Get rid of resource before you just lose them by Robber discard
        } else if (shouldUseResources(10)) {
            selected = Strategy.USERESOURCES;
        // 9. Last resort if no other good strategy, build a road
        } else if (hasLessThanMaxAllowedRoads() && gameplay.getBoard().getEdges().stream().anyMatch(gameplay::isValidRoadPlacement)) {
            selected = Strategy.ROADBUILDER;
        }
        strategyUsageMap.merge(selected, 1, Integer::sum);
        return selected;
    }

    //_______________________________AI MOVE MAKING LOGIC______________________________//
    private void makeEasyLevelMove(Gameplay gameplay, Group boardGroup) {
        CatanBoardGameView view = gameplay.getCatanBoardGameView();
        int attempts = getMaxStrategyAttempts();
        boolean moveMade;
        do {
            Strategy strategy = determineStrategy(true);
            view.logToGameLog("\n" + gameplay.getCurrentPlayer() + " (EASY) is using strategy: " + strategy);
            moveMade = false;
            switch (strategy) {
                case CITYUPGRADER -> moveMade = tryBuildCity(gameplay, boardGroup);
                case SETTLEMENTPLACER -> moveMade = tryBuildSettlement(gameplay, boardGroup);
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
            Strategy strategy = determineStrategy(true);
            view.logToGameLog("\n" + gameplay.getCurrentPlayer() + " (MEDIUM) is using strategy: " + strategy);
            moveMade = false;
            switch (strategy) {
                case CITYUPGRADER -> moveMade = tryBuildCity(gameplay, boardGroup);
                case SETTLEMENTPLACER -> moveMade = tryBuildSettlement(gameplay, boardGroup);
                case USERESOURCES -> {
                    moveMade = tryBankTrade(gameplay, strategy);
                    if (!moveMade) {
                        // fallback: try other useful actions
                        moveMade = tryBuildCity(gameplay, boardGroup)
                                || tryBuildSettlement(gameplay, boardGroup)
                                || tryBuildRoad(gameplay, boardGroup);
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
            Strategy strategy = determineStrategy(true);
            view.logToGameLog("\n" + gameplay.getCurrentPlayer() + " (HARD) is using strategy: " + strategy);
            moveMade = false;
            switch (strategy) {
                case CITYUPGRADER -> moveMade = tryBuildCity(gameplay, boardGroup);
                case SETTLEMENTPLACER -> moveMade = tryBuildSettlement(gameplay, boardGroup);
                case DEVELOPMENTCARDBUYER, BIGGESTARMY -> moveMade = tryBuyDevCard(gameplay);
                case USERESOURCES -> {
                    moveMade = tryBankTrade(gameplay, strategy);
                    if (!moveMade) {
                        // fallback: try other useful actions
                        moveMade = tryBuildCity(gameplay, boardGroup)
                                || tryBuildSettlement(gameplay, boardGroup)
                                || tryBuildRoad(gameplay, boardGroup)
                                || tryBuyDevCard(gameplay);
                    }
                }
                case LONGESTROAD -> moveMade = tryBuildLongestRoad(gameplay, boardGroup);
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

    //_______________________________ BUILDS AND TRADES ______________________________//
    private boolean tryBuildCity(Gameplay gameplay, Group boardGroup) {
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
            // Successful city build
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

    private boolean tryBuildSettlement(Gameplay gameplay, Group boardGroup) {
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
        return false;
    }

    private boolean tryBuildRoad(Gameplay gameplay, Group boardGroup) {
        // Check if there is any place to build a Road available
        List<Edge> validRoads = gameplay.getBoard().getEdges().stream()
                .filter(gameplay::isValidRoadPlacement)
                .toList();
        if (validRoads.isEmpty()) return false;

        if (!canAffordRoad()) {
            if (!tryBankTrade(gameplay, Strategy.ROADBUILDER)) return false;
            if (!canAffordRoad()) return false;
        }
        Edge bestEdge = null;
        int bestScore = Integer.MIN_VALUE;
        // Find the best road to build
        for (Edge edge : validRoads) {
            Vertex source = (getSettlementsAndCities().contains(edge.getVertex1())) ? edge.getVertex1()
                    : (getSettlementsAndCities().contains(edge.getVertex2())) ? edge.getVertex2()
                    : edge.getVertex1();
            int score = getSmartRoadScore(edge, source, gameplay, false);
            if (score > bestScore) {
                bestScore = score;
                bestEdge = edge;
            }
        }
        // Successful build the best road
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
    // Build a road with main focus of getting longest road.
    private boolean tryBuildLongestRoad(Gameplay gameplay, Group boardGroup) {
        // Step 1: Check if there are any valid road placements
        List<Edge> validRoads = gameplay.getBoard().getEdges().stream()
                .filter(gameplay::isValidRoadPlacement)
                .toList();
        if (validRoads.isEmpty()) return false;
        // Step 2: Check if we can afford at least one road
        if (!canAffordRoad()) {
            if (!tryBankTrade(gameplay, Strategy.ROADBUILDER)) return false;
            if (!canAffordRoad()) return false;
        }
        // Step 3: Calculate how many roads we can afford right now
        int maxRoads = Math.min(getResourceAmount("Brick"), getResourceAmount("Wood"));
        int currentLongest = gameplay.getLongestRoadManager().calculateLongestRoad(this);
        List<Edge> myRoads = new ArrayList<>(getRoads());
        Set<Edge> bestExtension = new HashSet<>();
        for (Edge firstCandidate : validRoads) {
            if (!connectsToMyNetwork(firstCandidate)) continue;
            List<Edge> simulated = new ArrayList<>(myRoads);
            simulated.add(firstCandidate);
            Set<Edge> visited = new HashSet<>();
            visited.add(firstCandidate);
            // Extend only up to maxRoads limit
            extendRoadPathLimited(simulated, visited, validRoads, maxRoads - 1);
            int newLength = gameplay.getLongestRoadManager().calculateLongestRoadFromEdges(simulated);
            if (newLength > currentLongest && visited.size() > bestExtension.size()) {
                bestExtension = new HashSet<>(visited);
            }
        }
        if (bestExtension.isEmpty()) return false;
        // Step 4: Build as many roads as we can afford from the bestExtension path
        for (Edge edge : bestExtension) {
            if (!gameplay.isValidRoadPlacement(edge)) continue;
            if (!canAffordRoad()) break;
            BuildResult result = gameplay.buildRoad(edge);
            if (result == BuildResult.SUCCESS) {
                gameplay.getCatanBoardGameView().runOnFX(() -> {
                    Line line = new Line(
                            edge.getVertex1().getX(), edge.getVertex1().getY(),
                            edge.getVertex2().getX(), edge.getVertex2().getY()
                    );
                    drawOrDisplay.drawRoad(line, this, boardGroup);
                    gameplay.getCatanBoardGameView().logToGameLog(this + " extended their road network.");
                    if (gameplay.isGameOver()) {gameplay.stopAllAIThreads();}
                });
                return true;
            }
        }
        return false;
    }
    // Helper method to recursively extend roads with a limit on steps
    private void extendRoadPathLimited(List<Edge> current, Set<Edge> visited, List<Edge> validRoads, int maxSteps) {
        int steps = 0;
        boolean added = true;
        while (added && steps < maxSteps) {
            added = false;
            for (Edge edge : validRoads) {
                if (visited.contains(edge)) continue;
                for (Edge built : current) {
                    if (edge.isConnectedTo(built.getVertex1()) || edge.isConnectedTo(built.getVertex2())) {
                        current.add(edge);
                        visited.add(edge);
                        added = true;
                        break;
                    }
                }
            }
            steps++;
        }
    }
    // Checks if AI can get the longest road right away.
    private boolean canGetLongestRoad(Gameplay gameplay) {
        LongestRoadManager roadManager = gameplay.getLongestRoadManager();
        Player currentHolder = roadManager.getCurrentHolder();
        int holderLength = (currentHolder != null) ? roadManager.calculateLongestRoad(currentHolder) : 0;
        // Step 1: Check if player has enough resources to build at least one road
        int maxRoads = Math.min(getResourceAmount("Brick"), getResourceAmount("Wood"));
        if (maxRoads <= 0) return false;
        // Step 2: Simulate all valid roads we can build from our current network
        List<Edge> validRoads = gameplay.getBoard().getEdges().stream()
                .filter(gameplay::isValidRoadPlacement)
                .filter(this::connectsToMyNetwork)
                .toList();
        if (validRoads.isEmpty()) return false;
        List<Edge> currentRoads = new ArrayList<>(getRoads());

        for (Edge startEdge : validRoads) {
            List<Edge> simulated = new ArrayList<>(currentRoads);
            simulated.add(startEdge);
            Set<Edge> visited = new HashSet<>();
            visited.add(startEdge);
            // Simulate further roads up to what we can afford
            extendRoadPathLimited(simulated, visited, validRoads, maxRoads - 1);
            int simulatedLength = roadManager.calculateLongestRoadFromEdges(simulated);
            if (simulatedLength > holderLength && simulatedLength >= 5) {
                return true; // Can win Longest Road this turn
            }
        }
        return false;
    }

    // Attempt to trade with the bank
    public boolean tryBankTrade(Gameplay gameplay, Strategy strategy) {
        Map<String, Integer> resources = getResources();
        List<String> allTypes = List.of("Brick", "Wood", "Ore", "Grain", "Wool");
        Map<String, Integer> targetCost = new HashMap<>();

        // Choose best possible trade depending on current strategy
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
            case DEVELOPMENTCARDBUYER -> {
                targetCost.put("Wool", 1);
                targetCost.put("Grain", 1);
                targetCost.put("Ore", 1);
            }
            case USERESOURCES -> {
                String target = chooseSmartResourceToReceive();
                String give = chooseSmartResourceToGive();

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
    // Bank trade helper function that executes the trade
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
            gameplay.getCatanBoardGameView().logToGameLog(msg);
        });
        return true;
    }

    // Helper function for Bank-trade that makes sure to check if the player gets Harbor discount on trades
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
        Player currentHolder = gameplay.getLongestRoadManager().getCurrentHolder();
        int myLongest = gameplay.getLongestRoadManager().calculateLongestRoad(this);

        // if no one is LongestRoadManager, try to get longest road right away if already have 4
        if (currentHolder == null) {
            gameplay.getCatanBoardGameView().logToGameLog("No One is Longest Road Manager yet, I'll take it!");
            return myLongest == 4 && canAffordRoad();
        }

        int holderLength = gameplay.getLongestRoadManager().calculateLongestRoad(currentHolder);

        // if current longestRoadManager is almost winning - Try sabotage them by overtaking longest road
        if (gameplay.getLongestRoadManager().getCurrentHolder().getPlayerScore() >= 8) {
            closeEnough = myLongest + 1 > holderLength;
            if (closeEnough && (hasResources("Wood",1) || hasResources("Brick",1))) {
                gameplay.getCatanBoardGameView().logToGameLog("current Longest Road manager is a big thread, i want to steal it from them!");
            }
            // Try steal longest road if you only need 1 road and maximum missing 1 resource to do it
            return closeEnough && (hasResources("Wood",1) || hasResources("Brick",1));
        }

        // Otherwise, only go for it if you can overtake longest road immediately
        closeEnough = myLongest == holderLength;
        return currentHolder != this && closeEnough && canAffordRoad();
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
        Strategy strategy = determineStrategy(false);
        Set<String> neededResources = getNeededResourcesForStrategy(strategy);

        for (String res : resources.keySet()) {
            int amountOwned = resources.getOrDefault(res, 0);
            int productionScore = 0;

            // Production score: total dice weight from settlements
            productionScore = getProductionScore(res);

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
    // How many new resources a certain Tile would give the player
    private int countMissingResourcesCovered(Vertex vertex, Gameplay gameplay) {
        Set<String> ownedTypes = new HashSet<>();
        for (Vertex settlement : getSettlementsAndCities()) {
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

    public int countHelpfulCards(Player victim, Set<String> neededResources) {
        int count = 0;
        for (String res : neededResources) {
            count += victim.getResources().getOrDefault(res, 0);
        }
        return count;
    }

    // Sleep the AI timer for real game simulation effect
    private void pauseBeforeMove() {
        if (Platform.isFxApplicationThread()) {
            System.err.println("AI pause called on JavaFX Application Thread!");
        }
        int delayMillis;
        switch (thinkingSpeed) {
            case SLOW -> delayMillis = ThreadLocalRandom.current().nextInt(3000, 7000);
            case FAST -> delayMillis = 200;
            case EXTREME -> delayMillis = 20;
            default -> delayMillis = 1000;
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

    //_________________________Choose Methods_______________________//
    public String chooseSmartResourceToReceive() {
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

    public String chooseSmartResourceToGive() {
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

    // Monopoly logic
    public String chooseSmartResourceToMonopoly(Gameplay gameplay){
        //getting all opponents
        List<Player> opponents = gameplay.getPlayerList().stream()
                .filter(p -> p != this)
                .toList();

        // finding there resources
        Map<String, Integer> totalOpponentResources = new HashMap<>();

        for (Player p : opponents) {
            for (Map.Entry<String, Integer> entry : p.getResources().entrySet()) {
                totalOpponentResources.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }
        // Calculate how much the player has of each resource
        int oreHave = this.getResources().getOrDefault("Ore", 0);
        int grainHave = this.getResources().getOrDefault("Grain", 0);
        int woolHave = this.getResources().getOrDefault("Wool", 0);
        int woodHave = this.getResources().getOrDefault("Wood", 0);
        int brickHave = this.getResources().getOrDefault("Brick", 0);

        // Calculate how much you can steal in total of each resource
        int oreFromOpponents = totalOpponentResources.getOrDefault("Ore", 0);
        int grainFromOpponents = totalOpponentResources.getOrDefault("Grain", 0);
        int woolFromOpponents = totalOpponentResources.getOrDefault("Wool", 0);
        int woodFromOpponents = totalOpponentResources.getOrDefault("Wood", 0);
        int brickFromOpponents = totalOpponentResources.getOrDefault("Brick", 0);

        // --- Step 1: Check if we can steal enough to build a full city ---
        int oreNeed = Math.max(0, 3 - oreHave);
        int grainNeed = Math.max(0, 2 - grainHave);
        if ((oreFromOpponents >= oreNeed) && grainNeed==0 ){
            return "Ore";
        } else if (grainFromOpponents >= grainNeed && oreNeed==0) {
            return"Grain";
        }

        // --- Step 2: Check if we can steal enough to build a settlement ---
        grainNeed = Math.max(0, 1 - grainHave);
        int woodNeed = Math.max(0, 1 - woodHave);
        int woolNeed = Math.max(0, 1 - woolHave);
        int brickNeed = Math.max(0, 1 - brickHave);
        if (grainNeed > 0 && grainNeed <= grainFromOpponents
                && woodNeed == 0 && woolNeed == 0 && brickNeed == 0) {
            return "Grain";
        } else if (woodNeed > 0 && woodNeed <= woodFromOpponents
                && grainNeed == 0 && woolNeed == 0 && brickNeed == 0) {
            return "Wood";
        } else if (woolNeed > 0 && woolNeed <= woolFromOpponents
                && grainNeed == 0 && woodNeed == 0 && brickNeed == 0) {
            return "Wool";
        } else if (brickNeed > 0 && brickNeed <= brickFromOpponents
                && grainNeed == 0 && woolNeed == 0 && woodNeed == 0) {
            return "Brick";
        }

        // If not, pick he one with the largest trading potential
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
        return bestTradeResource;
    }

    public Player chooseBestRobberTargetForHardAI(AIOpponent ai, List<Player> victims) {
        Strategy strategy = determineStrategy(false);
        Set<String> neededResources = getNeededResourcesForStrategy(strategy);

        return victims.stream()
                .max(Comparator.comparingInt(v -> countHelpfulCards(v, neededResources)))
                .orElse(null);
    }

    // Year of plenty logic
    public Map<String, Integer> chooseResourcesForYearOfPlenty() {
        Strategy strategy = determineStrategy(false);
        Set<String> neededResources = getNeededResourcesForStrategy(strategy);

        Map<String, Integer> priorityMap = new HashMap<>();
        for (String res : neededResources) {
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

    //__________________________BOOLEAN HELPERS (LEGAL MOVE CHECKS)__________________________//
    private boolean shouldUseResources(int maxResources) {
        Map<String, Integer> resources = getResources();
        // Total resources must be at least 10
        int totalResources = resources.values().stream().mapToInt(Integer::intValue).sum();
        return totalResources >= maxResources;}

    private boolean hasResources(String type, int amount) {
        return getResources().getOrDefault(type, 0) >= amount;
    }

    private boolean canAffordDevCard() {
        return hasResources("Wool", 1) && hasResources("Grain", 1) && hasResources("Ore", 1);}

    private boolean canAffordSettlement() {
        return (hasResources("Brick", 1) && hasResources("Wood", 1) &&
                hasResources("Wool", 1) && hasResources("Grain", 1));}

    private boolean canAffordCity() {return hasResources("Ore", 3) && hasResources("Grain", 2);}

    private boolean canAffordRoad() {return hasResources("Wood", 1) && hasResources("Brick", 1);}

    public boolean canUpgradeToCityNow() {
        if (!hasSettlementThatCanBeUpgradedToCity()) {return false;}
        return canAffordCity();
    }

    private boolean hasLessThanMaxAllowedCities() {
        return getCities().size() < menuView.getMaxCities();
    }

    private boolean hasLessThanMaxAllowedSettlements() { return getSettlements().size() < menuView.getMaxSettlements();}

    private boolean hasLessThanMaxAllowedRoads() {
        return getRoads().size() < menuView.getMaxRoads();
    }

    public boolean hasOreAndWheatTiles() {
        boolean hasOre = false;
        boolean hasGrain = false;

        for (Vertex vertex : getSettlementsAndCities()) {
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

    private boolean hasAlmostEnoughResourcesForCityUpgrade() {
        int ore = getResources().getOrDefault("Ore", 0);
        int grain = getResources().getOrDefault("Grain", 0);
        int total = ore + grain;
        // Already has full resources → return true
        if (ore >= 3 && grain >= 2) return true;
        // Has 3 out of 5 needed resources → consider planning
        return total >= 3;
    }

    private boolean shouldWaitForSettlementPlacement() {
        if (!hasLessThanMaxAllowedSettlements()) return false;

        int currentTotal = 0;
        currentTotal += Math.min(getResourceAmount("Brick"), 1);
        currentTotal += Math.min(getResourceAmount("Wood"), 1);
        currentTotal += Math.min(getResourceAmount("Grain"), 1);
        currentTotal += Math.min(getResourceAmount("Wool"), 1);
        // If you have at least 3 of 4 resources for build, wait for that
        if (currentTotal >= 3) return true;
        // Else, consider buying a dev card first if you can.
        return currentTotal == 2 && !canAffordDevCard();
    }

    private boolean hasSettlementThatCanBeUpgradedToCity() {
        return !getSettlements().isEmpty();
    }

    public boolean earlyGame() {
        // Current player has 5 or fewer points → early game
        if (getPlayerScore() <= 5) return true;

        // No other player has more than 4 points → still early game
        return gameplay.getPlayerList().stream()
                .filter(p -> p != this)
                .noneMatch(p -> p.getPlayerScore() > 4);
    }

    public boolean lateGame() {
        // if current player has 8 points or more, its lateGame
        if (getPlayerScore() >= 8) return true;
        // or if someone else has 7 points or more
        return gameplay.getPlayerList().stream()
                .anyMatch(p -> p != this && p.getPlayerScore() >= 7);
    }

    //__________________________CALCULATION FUNCTIONS__________________________//
    // Helper to calculate how much of a resource a player gets
    private int getProductionScore(String resourceType) {
        int productionScore = 0;
        //Loop settlements
        for (Vertex v : getSettlements()) {
            for (Tile t : gameplay.getBoard().getTiles()) {
                if (t.getVertices().contains(v) &&
                        t.getResourcetype().toString().equals(resourceType)) {
                    productionScore += getDiceProbabilityValue(t.getTileDiceNumber());
                }
            }
        }
        // Loop cities
        for (Vertex v : getCities()) {
            for (Tile t : gameplay.getBoard().getTiles()) {
                if (t.getVertices().contains(v) &&
                        t.getResourcetype().toString().equals(resourceType)) {
                    productionScore += 2 * (getDiceProbabilityValue(t.getTileDiceNumber()));
                }
            }
        }
        return productionScore;
    }

    // Helper to choose the best possible spot for a Settlement placement
    private int getSmartSettlementScore(Vertex vertex, Gameplay gameplay) {
        int diceValue = getSettlementDiceValue(vertex, gameplay);     // Primary: total dice probability
        int diversity = getResourceDiversityScore(vertex, gameplay);  // Secondary: unique resource types
        int newResources = countMissingResourcesCovered(vertex, gameplay); // Bonus: new types for player
        boolean blocked = isBlocked(vertex, gameplay);
        int score;
        // Weighted scoring
        if (getPlayerScore() == 0) { // First settlement
            score =  (diceValue * 4) + (diversity * 5);
        }
        else if (newResources == diversity && diversity != 1) { // AI tries to get all resource types
            score =  (diceValue * 11) + (diversity * 5);
        }
        // Standard scoring else
        else score = diceValue * 6 + newResources * 13 + diversity * 3;

        // Heavily penalize blocked vertices (should never pick them)
        if (blocked) score -= 1000;
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

    // Multiple different types of resources are typically better than
    private int getResourceDiversityScore(Vertex vertex, Gameplay gameplay) {
        Set<Resource.ResourceType> resourceTypes = new HashSet<>();
        for (Tile tile : gameplay.getBoard().getTiles()) {
            if (tile.getVertices().contains(vertex)) {
                resourceTypes.add(tile.getResourcetype());
            }
        }
        return resourceTypes.size();
    }

    // returning all valid spots to place a settlement
    private List<Vertex> getValidSettlementSpots(Gameplay gameplay) {
        return gameplay.getBoard().getVertices().stream()
                .filter(gameplay::isValidSettlementPlacement)
                .collect(Collectors.toList());
    }

    // Helper to decide which road is the best road to build
    private int getSmartRoadScore(Edge edge, Vertex source, Gameplay gameplay, boolean initialphase) {
        Vertex target;
        if (initialphase) {
            target = source;
        } else {
            target = edge.getOppositeVertex(source);
        }
        int roadScore = 0;

        // 1. Top priority! can build settlement at target
        if (gameplay.isValidSettlementPlacement(target, true) ||
                gameplay.isValidSettlementPlacement(source, true)) {
            roadScore += 2000;
        }
        // 2. Smart scoring for actually good settlement locations
        int settlementScore = getSmartSettlementScore(target, gameplay); // Already includes dice values, diversity etc.
        // 3. Favor well-connected tiles
        int tileCount = target.getAdjacentTiles().size();
        // 4. Penalize already connected vertices (avoid loops)
        long friendlyRoads = getRoads().stream()
                .filter(r -> r.isConnectedTo(target))
                .count();
        // 5. Final scoring formula taking everything into account
        roadScore += (settlementScore * 3) + (tileCount * 2) - (int)(friendlyRoads * 40);
        return roadScore;
    }

    // Helper to choose which resources are needed for different strategies
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
            case DEVELOPMENTCARDBUYER -> {
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

    // Calculates the probability of a Tile being triggered with the dices
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

    //__________________________SETTERS__________________________//
    public void setThinkingSpeed(ThinkingSpeed speed) {
        this.thinkingSpeed = speed;
    }

    //__________________________GETTERS__________________________//
    public EnumMap<Strategy, Integer> getStrategyUsageMap() {
        return strategyUsageMap;
    }

    public int getMaxStrategyAttempts() {
        return MAX_STRATEGY_ATTEMPTS;
    }

    public StrategyLevel getStrategyLevel() {
        return strategyLevel;
    }
}