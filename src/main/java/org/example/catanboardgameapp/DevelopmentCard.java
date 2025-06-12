package org.example.catanboardgameapp;

import org.example.catanboardgameviews.CatanBoardGameView;
import javafx.scene.Group;
import org.example.controller.TradeController;


import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DevelopmentCard {
    private final Gameplay gameplay;
    private final List<Player> playerList;
    private final CatanBoardGameView view;
    private final TradeController tradeController;
    private final DrawOrDisplay drawOrDisplay;
    private CatanBoardGameView catanBoardGameView;
    private boolean placingFreeRoads = false;
    private boolean playingCard = false;
    private int freeRoadsLeft = 0;

    public DevelopmentCard(Gameplay gameplay, List<Player> playerList, CatanBoardGameView view, TradeController tradeController) {
        this.gameplay = gameplay;
        this.playerList = playerList;
        this.view = view;
        this.tradeController = tradeController;
        this.drawOrDisplay = gameplay.getDrawOrDisplay();
    }

    public enum DevelopmentCardType {
        MONOPOLY("Monopoly") {
            @Override
            public void play(Player player, DevelopmentCard devCard) {
                devCard.startPlayingCard();
                devCard.gameplay.playMonopolyCard();
                devCard.log("Player " + player.getPlayerId() + " played a monopoly development card");
            }
        },
        KNIGHT("Knight") {
            @Override
            public void play(Player player, DevelopmentCard devCard) {
                devCard.view.hideDiceButton();
                devCard.view.hideTurnButton();

                Group boardGroup = devCard.view.getBoardGroup();
                devCard.startPlayingCard();
                devCard.view.getRobber().showRobberTargets(boardGroup);
                devCard.gameplay.setRobberMoveRequired(true);
                player.increasePlayedKnights();
                devCard.gameplay.getBiggestArmy().calculateAndUpdateBiggestArmy(player);

                devCard.log("Player " + player.getPlayerId() + " played a knight development card");
            }
        },
        ROADBUILDING("Road Building") {
            @Override
            public void play(Player player, DevelopmentCard devCard) {
                devCard.startPlacingFreeRoads(2);
                devCard.log("Player " + player.getPlayerId() + " played a road building development card");
            }
        },
        YEAROFPLENTY("Year Of Plenty") {
            @Override
            public void play(Player player, DevelopmentCard devCard) {
                devCard.startPlayingCard();
                devCard.playYearOfPlentyCard();
                devCard.log("Player " + player.getPlayerId() + " played a year of plenty development card");
            }
        },
        VICTORYPOINT("Victory Point") {
            @Override
            public void play(Player player, DevelopmentCard devCard) {
                player.increasePlayerScore();
                devCard.log("Player " + player.getPlayerId() + " played a victory point development card");
            }
        };

        private final String displayName;

        DevelopmentCardType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }


        public abstract void play(Player player, DevelopmentCard devCard);
    }

    public void playingCard(Player player, DevelopmentCardType cardType){
        cardType.play(player,this);
    }

    public void playYearOfPlentyCard() {
        Player currentPlayer = gameplay.getCurrentPlayer();
        Map<String, Integer> selected;

        if (currentPlayer instanceof AIOpponent ai && ai.getStrategyLevel() == AIOpponent.StrategyLevel.HARD) {
            selected = ai.chooseResourcesForYearOfPlenty();
        } else {
            selected = drawOrDisplay.showYearOfPlentyDialog();
        }

        if (selected != null) {
            addResourcesToPlayer(currentPlayer, selected);

            String gained = selected.entrySet().stream()
                    .map(entry -> entry.getValue() + " " + entry.getKey()).collect(Collectors.joining(", "));

            catanBoardGameView.logToGameLog("Player " + currentPlayer.getPlayerId() + " used Year of Plenty and recieved " + gained + ".");

            catanBoardGameView.refreshSidebar();
            finishPlayingCard();
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
    public void startPlayingCard() {
        this.playingCard = true;
    }

    public void finishPlayingCard() {
        this.playingCard = false;
    }

    public boolean isPlayingCard() {
        return playingCard;
    }

    public void startPlacingFreeRoads(int count) {
        startPlayingCard();
        this.placingFreeRoads = true;
        this.freeRoadsLeft = count;
    }

    public boolean isPlacingFreeRoads() {
        return placingFreeRoads;
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
