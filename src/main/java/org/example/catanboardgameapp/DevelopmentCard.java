package org.example.catanboardgameapp;

import org.example.catanboardgameviews.CatanBoardGameView;
import org.example.controller.TradeController;

import java.util.*;
import java.util.stream.Collectors;

public class DevelopmentCard {
    private final Gameplay gameplay;
    private final List<Player> playerList;
    private final CatanBoardGameView view;
    private final TradeController tradeController;
    private final DrawOrDisplay drawOrDisplay;
    private final CatanBoardGameView catanBoardGameView;
    private boolean placingFreeRoads = false;
    private boolean playingCard = false;
    private int freeRoadsLeft = 0;

    public DevelopmentCard(Gameplay gameplay, List<Player> playerList, CatanBoardGameView view, TradeController tradeController) {
        this.gameplay = gameplay;
        this.playerList = playerList;
        this.view = view;
        this.tradeController = tradeController;
        this.drawOrDisplay = gameplay.getDrawOrDisplay();
        this.catanBoardGameView = gameplay.getCatanBoardGameView();
    }

    public enum DevelopmentCardType {
        MONOPOLY("Monopoly") {
            @Override
            public void play(Player player, DevelopmentCard devCard) {

                devCard.startPlayingCard();
                devCard.playMonopolyCard();
                devCard.log("Player " + player.getPlayerId() + " played a monopoly development card");
            }

            @Override
            public void playAsAI(AIOpponent ai, DevelopmentCard devCard, Gameplay gameplay) {
                String resource = ai.chooseSmartResourceToMonopoly(gameplay);
                int total = devCard.monopolizeResource(resource, ai);
                devCard.log("AI played Monopoly and took " + total + " " + resource);
            }
        },

        KNIGHT("Knight") {
            @Override
            public void play(Player player, DevelopmentCard devCard) {
                devCard.view.hideDiceButton();
                devCard.view.hideTurnButton();
                devCard.startPlayingCard();
                devCard.view.getRobber().showRobberTargets(devCard.view.getBoardGroup());
                devCard.gameplay.setRobberMoveRequired(true);
                player.increasePlayedKnights();
                devCard.gameplay.getBiggestArmy().calculateAndUpdateBiggestArmy(player);
                devCard.log("Player " + player.getPlayerId() + " played a knight development card");
            }

            @Override
            public void playAsAI(AIOpponent ai, DevelopmentCard devCard, Gameplay gameplay) {
                ai.increasePlayedKnights();
                gameplay.getBiggestArmy().calculateAndUpdateBiggestArmy(ai);

                Player victim = ai.chooseBestRobberyTargetForHardAI(ai, gameplay.getPlayerList());
                if (victim != null) {
                    List<String> pool = new ArrayList<>();
                    victim.getResources().forEach((res, count) -> {
                        for (int i = 0; i < count; i++) pool.add(res);
                    });

                    if (!pool.isEmpty()) {
                        Collections.shuffle(pool);
                        String stolen = pool.get(0);

                        victim.getResources().put(stolen, Math.max(0, victim.getResources().get(stolen) - 1));
                        ai.getResources().merge(stolen, 1, Integer::sum);

                        devCard.log("AI played Knight and stole 1 " + stolen + " from Player " + victim.getPlayerId());
                    } else {
                        devCard.log("AI played Knight but " + victim + " had no resources.");
                    }
                }
            }
        },

        ROADBUILDING("Road Building") {
            @Override
            public void play(Player player, DevelopmentCard devCard) {
                devCard.startPlacingFreeRoads(2);
                devCard.log("Player " + player.getPlayerId() + " played a road building development card");
            }

            @Override
            public void playAsAI(AIOpponent ai, DevelopmentCard devCard, Gameplay gameplay) {
                int placed = 0;
                for (Edge edge : gameplay.getBoard().getEdges()) {
                    if (placed == 2) break;
                    if (gameplay.isValidRoadPlacement(edge)) {
                        if (gameplay.placeFreeRoad(ai, edge) == BuildResult.SUCCESS) {
                            placed++;
                            devCard.log("AI " + devCard.gameplay.getCurrentPlayer() + " placed a free road.");
                        }
                    }
                }
            }
        },

        YEAROFPLENTY("Year Of Plenty") {
            @Override
            public void play(Player player, DevelopmentCard devCard) {
                devCard.startPlayingCard();
                devCard.playYearOfPlentyCard();
                devCard.log("Player " + player.getPlayerId() + " played a year of plenty development card");
            }

            @Override
            public void playAsAI(AIOpponent ai, DevelopmentCard devCard, Gameplay gameplay) {
                Map<String, Integer> selected = ai.chooseResourcesForYearOfPlenty();
                selected.forEach((res, amt) ->
                        ai.getResources().merge(res, amt, Integer::sum)
                );
                String gained = selected.entrySet().stream()
                        .map(e -> "+ " + e.getValue() + " " + e.getKey())
                        .collect(Collectors.joining(", "));

                devCard.log("AI played Year of Plenty: " + gained);
            }
        },

        VICTORYPOINT("Victory Point") {
            @Override
            public void play(Player player, DevelopmentCard devCard) {
                devCard.gameplay.increasePlayerScore();
                devCard.log("Player " + player.getPlayerId() + " played a victory point development card");
            }

            @Override
            public void playAsAI(AIOpponent ai, DevelopmentCard devCard, Gameplay gameplay) {
                gameplay.increasePlayerScore();
                gameplay.getCatanBoardGameView().runOnFX(() ->
                        gameplay.getCatanBoardGameView().logToGameLog("AI played Victory Point and gained 1 point.")
                );
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

        public void playAsAI(AIOpponent ai, DevelopmentCard devCard, Gameplay gameplay) {
            throw new UnsupportedOperationException("AI play not implemented for " + this.name());
        }
    }

    public void playYearOfPlentyCard() {
        Player currentPlayer = gameplay.getCurrentPlayer();
        Map<String, Integer> selected;

        if (currentPlayer instanceof AIOpponent ai && ai.getStrategyLevel() == AIOpponent.StrategyLevel.HARD) {
            selected = ai.chooseResourcesForYearOfPlenty();
        } else {
            Map<String, Integer> playerResources = new HashMap<>(currentPlayer.getResources());
            selected = drawOrDisplay.showYearOfPlentyDialog(playerResources);
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

    public void playMonopolyCard() {
        Player currentPlayer = this.gameplay.getCurrentPlayer();
        String chosenResource = drawOrDisplay.showMonopolyDialog();
        if (chosenResource == null) return;
        int taken = monopolizeResource(chosenResource, currentPlayer);
        catanBoardGameView.logToGameLog("Player " + currentPlayer.getPlayerId() + " played a Monopoly card and took " + taken + " " + chosenResource + " from other players." );
        catanBoardGameView.refreshSidebar();
        finishPlayingCard();
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
