package org.example.catanboardgameapp;

import org.example.catanboardgameviews.CatanBoardGameView;
import javafx.scene.Group;
import org.example.controller.TradeController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DevelopmentCard {

    private final List<Player> playerList;
    private final CatanBoardGameView view;
    private final TradeController tradeController;

    private boolean placingFreeRoads = false;
    private boolean movingKnight = false;
    private int freeRoadsLeft = 0;

    public DevelopmentCard(List<Player> playerList, CatanBoardGameView view, TradeController tradeController) {
        this.playerList = playerList;
        this.view = view;
        this.tradeController = tradeController;
    }

    public enum DevelopmentCardType {
        MONOPOLY("Monopoly") {
            @Override
            public void play(Player player, DevelopmentCard handler) {
                handler.tradeController.playMonopolyCardFromButton();
                handler.log("Player " + player.getPlayerId() + " played a monopoly development card");
            }
        },
        KNIGHT("Knight") {
            @Override
            public void play(Player player, DevelopmentCard handler) {
                handler.view.hideDiceButton();
                handler.view.showTurnButton();
                Group boardGroup = handler.view.getBoardGroup();
                handler.view.getNextTurnButton().setDisable(true);
                handler.startMovingKnight();
                handler.view.getRobber().showRobberTargets(boardGroup);
                handler.log("Player " + player.getPlayerId() + " played a knight development card");
            }
        },
        ROADBUILDING("Road Building") {
            @Override
            public void play(Player player, DevelopmentCard handler) {
                handler.startPlacingFreeRoads(2);
                handler.log("Player " + player.getPlayerId() + " played a road building development card");
            }
        },
        YEAROFPLENTY("Year Of Plenty") {
            @Override
            public void play(Player player, DevelopmentCard handler) {
                handler.tradeController.playYearOfPlentyCardFromButton();
                handler.log("Player " + player.getPlayerId() + " played a year of plenty development card");
            }
        },
        VICTORYPOINT("Victory Point") {
            @Override
            public void play(Player player, DevelopmentCard handler) {
                player.increasePlayerScore();
                handler.log("Player " + player.getPlayerId() + " played a victory point development card");
            }
        };

        private final String name;

        DevelopmentCardType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public abstract void play(Player player, DevelopmentCard handler);

        public static DevelopmentCardType fromName(String name) {
            return Arrays.stream(values())
                    .filter(card -> card.name.equalsIgnoreCase(name))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown card: " + name));
        }
    }

    // ------------------------
    // Utility Methods
    // ------------------------

    public void log(String message) {
        if (view != null) {
            view.logToGameLog(message);
        } else {
            System.err.println("LOG FAIL (view=null): " + message); // optional fallback
        }
    }
    public void startMovingKnight() {
        this.movingKnight = true;
    }

    public void finishMovingKnight() {
        this.movingKnight = false;
    }

    public boolean isMovingKnight() {
        return movingKnight;
    }

    public void startPlacingFreeRoads(int count) {
        this.placingFreeRoads = true;
        this.freeRoadsLeft = count;
    }

    public void finishFreeRoadPlacement() {
        this.placingFreeRoads = false;
        this.freeRoadsLeft = 0;
    }

    public boolean isPlacingFreeRoads() {
        return placingFreeRoads;
    }

    public int getFreeRoadsLeft() {
        return freeRoadsLeft;
    }

    public void decrementFreeRoads() {
        if (freeRoadsLeft > 0) freeRoadsLeft--;
        if (freeRoadsLeft == 0) placingFreeRoads = false;
    }

    public void addResourcesToPlayer(Player player, Map<String, Integer> added) {
        added.forEach((res, amt) -> {
            int current = player.getResources().getOrDefault(res, 0);
            player.getResources().put(res, current + amt);
        });
    }

    public int monopolizeResource(String resource, Player player) {
        int totalTaken = 0;
        for (Player other : playerList) {
            if (!other.equals(player)) {
                int amount = other.getResources().getOrDefault(resource, 0);
                if (amount > 0) {
                    other.getResources().put(resource, 0);
                    totalTaken += amount;
                }
            }
        }
        player.getResources().merge(resource, totalTaken, Integer::sum);
        return totalTaken;
    }
}
