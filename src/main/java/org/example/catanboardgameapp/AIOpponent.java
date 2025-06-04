package org.example.catanboardgameapp;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class AIOpponent extends Player {
    //___________________________FIELDS___________________________//
    public enum StrategyLevel {
        EASY, MEDIUM, HARD
    }

    public enum Strategy {
        ROADBUILDER, CITYUPGRADER, SETTLEMENTPLACER, LONGESTROAD, BIGGESTARMY,
    }
    
    private final StrategyLevel strategyLevel;
    private final Random random = new Random();
    private final DrawOrDisplay drawOrDisplay;

    //___________________________CONSTRUCTOR___________________________//
    public AIOpponent(int playerId, Color color, StrategyLevel level, Gameplay gameplay) {
        super(playerId, color);
        this.strategyLevel = level;
        this.drawOrDisplay = new DrawOrDisplay(gameplay.getBoardRadius());
    }

    //___________________________FUNCTIONS___________________________//
    public void makeMoveAI(Gameplay gameplay) {
        System.out.println("makeMoveAI");
        pauseBeforeMove();

        gameplay.getCatanBoardGameView().logToGameLog("AI Player " + getPlayerId() + " (" + strategyLevel + ") is taking its turn...");

        switch (strategyLevel) {
            case EASY -> {
                gameplay.getCatanBoardGameView().logToGameLog("Strategy: EASY");
                makeEasyLevelMove(gameplay);
            }
            case MEDIUM -> {
                gameplay.getCatanBoardGameView().logToGameLog("Strategy: MEDIUM");
                makeMediumLevelMove(gameplay);
            }
            case HARD -> {
                gameplay.getCatanBoardGameView().logToGameLog("Strategy: HARD");
                makeHardLevelMove(gameplay);
            }
        }

        gameplay.getCatanBoardGameView().logToGameLog("AI Player " + getPlayerId() + " has ended its turn.");
    }


    public void placeInitialSettlementAndRoad(Gameplay gameplay, Group boardGroup) {
        System.out.println("placeInitialSettlementAndRoad");
        List<Vertex> allVertices = new ArrayList<>(gameplay.getBoard().getVertices());
        Collections.shuffle(allVertices);

        for (Vertex v : allVertices) {
            if (!gameplay.isValidSettlementPlacement(v)) continue;

            BuildResult result = gameplay.buildInitialSettlement(v);
            if (result == BuildResult.SUCCESS) {
                Circle circle = new Circle(v.getX(), v.getY(), 16.0 / gameplay.getBoardRadius());
                drawOrDisplay.drawPlayerSettlement(circle, v, boardGroup);

                for (Edge edge : gameplay.getBoard().getEdges()) {
                    if (edge.isConnectedTo(v) && gameplay.isValidRoadPlacement(edge)) {
                        BuildResult roadResult = gameplay.buildRoad(edge);
                        if (roadResult == BuildResult.SUCCESS) {
                            Line line = new Line(
                                    edge.getVertex1().getX(), edge.getVertex1().getY(),
                                    edge.getVertex2().getX(), edge.getVertex2().getY()
                            );
                            drawOrDisplay.drawPlayerRoad(line, this, boardGroup);

                            // After successfully placing both settlement and road:
                            Platform.runLater(() -> {
                                gameplay.getCatanBoardGameView().logToGameLog("AI Player " + getPlayerId() + " finished initial placement.");
                                gameplay.nextPlayerTurn(); // <<< THIS controls turn flow
                            });
                            return;
                        }
                    }
                }

                // Cleanup if road fails
                v.setOwner(null);
                getSettlements().remove(v);
                boardGroup.getChildren().remove(circle);
            }
        }

        gameplay.getCatanBoardGameView().logToGameLog("AI " + getPlayerId() + " could not place initial settlement + road.");
    }


    private Strategy chooseStrategy() {
        return null;
    }

    private void pauseBeforeMove() {
        try {
            //int delay = ThreadLocalRandom.current().nextInt(3000, 10000);
            int delay = ThreadLocalRandom.current().nextInt(200, 600); // ~0.2 to 0.6 sec

            Thread.sleep(delay);
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void makeEasyLevelMove(Gameplay gameplay) {
        tryBuildCity(gameplay);
        tryBuildSettlement(gameplay);
        tryBuildRoad(gameplay);
    }

    private void makeMediumLevelMove(Gameplay gameplay) {
        tryBuildCity(gameplay);
        tryBuildBestSettlement(gameplay);
        tryBuildRoad(gameplay);
    }

    private void makeHardLevelMove(Gameplay gameplay) {
        tryBankTrade(gameplay);
        tryBuildCity(gameplay);
        tryBuildBestSettlement(gameplay);
        tryBuildRoad(gameplay);
    }

    private void tryBuildCity(Gameplay gameplay) {
        if (hasResources("Ore", 3) && hasResources("Grain", 2)) {
            List<Vertex> settlements = getSettlements();
            if (!settlements.isEmpty()) {
                Vertex chosenVertex = settlements.get(random.nextInt(settlements.size()));
                gameplay.getCatanBoardGameView().logToGameLog("AI Player " + getPlayerId() + " tries upgrading to a city at vertex " + chosenVertex.getIdOrCoords());
                BuildResult result = gameplay.buildCity(chosenVertex);
                if (result == BuildResult.UPGRADED_TO_CITY) {
                    gameplay.getCatanBoardGameView().logToGameLog("AI Player " + getPlayerId() + " upgraded to a city.");
                }
            }
        }
    }


    private void tryBuildSettlement(Gameplay gameplay) {
        if (!hasResources("Brick", 1) || !hasResources("Wood", 1)
                || !hasResources("Wool", 1) || !hasResources("Grain", 1)) return;

        List<Vertex> validSpots = getValidSettlementSpots(gameplay);
        if (!validSpots.isEmpty()) {
            Collections.shuffle(validSpots);
            gameplay.buildSettlement(validSpots.get(0));
            gameplay.getCatanBoardGameView().logToGameLog("AI Player " + getPlayerId() + " built a settlement.");
        }
    }

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
        gameplay.getCatanBoardGameView().logToGameLog("AI Player " + getPlayerId() + " built a settlement.");
    }

    private void tryBuildRoad(Gameplay gameplay) {
        if (!hasResources("Brick", 1) || !hasResources("Wood", 1)) return;

        List<Edge> validRoads = new ArrayList<>();
        for (Edge edge : gameplay.getBoard().getEdges()) {
            if (gameplay.isValidRoadPlacement(edge)) validRoads.add(edge);
        }

        if (!validRoads.isEmpty()) {
            Collections.shuffle(validRoads);
            gameplay.buildRoad(validRoads.get(0));
        }
    }

    private void tryBankTrade(Gameplay gameplay) {
        Map<String, Integer> resources = getResources();
        List<String> allTypes = List.of("Brick", "Wood", "Ore", "Grain", "Wool");

        for (String need : allTypes) {
            if (resources.getOrDefault(need, 0) == 0) {
                for (String give : allTypes) {
                    if (!give.equals(need) && resources.getOrDefault(give, 0) >= 4) {
                        gameplay.tradeWithBank(give, need);
                        gameplay.getCatanBoardGameView().logToGameLog("AI Player " + getPlayerId() + " traded with the bank.");
                        return;
                    }
                }
            }
        }
    }

    public boolean lateGame() {
        return false;
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
        int diceValue = getSettlementDiceValue(vertex, gameplay);
        int diversity = getResourceDiversityScore(vertex, gameplay);
        boolean blocked = isBlocked(vertex, gameplay);
        int resourceNeedMatch = countMissingResourcesCovered(vertex, gameplay);

        int score = (diceValue * 2) + (diversity * 3) + (resourceNeedMatch * 4);

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
}
