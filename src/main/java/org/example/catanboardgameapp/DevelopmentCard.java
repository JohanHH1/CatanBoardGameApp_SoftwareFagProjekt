package org.example.catanboardgameapp;

import javafx.scene.shape.Line;
import org.example.catanboardgameviews.CatanBoardGameView;
import org.example.controller.TradeController;

import java.util.*;
import java.util.stream.Collectors;

//__________________________________________________________
// DEVELOPMENT CARD ENGINE - Handles usage logic for all
// types of development cards in Catan, including player/AI logic.
//__________________________________________________________
public class DevelopmentCard {

    // Dependencies
    private final Gameplay gameplay;
    private final CatanBoardGameView view;
    private final DrawOrDisplay drawOrDisplay;
    private final CatanBoardGameView catanBoardGameView;

    // Game State Tracking Flags
    private final List<Player> playerList;
    private boolean placingFreeRoads = false;
    private boolean playingCard = false;
    private int freeRoadsLeft = 0;

    //_______________________________CONSTRUCTOR_________________________________//
    public DevelopmentCard(Gameplay gameplay, List<Player> playerList, CatanBoardGameView view, TradeController tradeController) {
        this.gameplay = gameplay;
        this.playerList = playerList;
        this.view = view;
        this.drawOrDisplay = gameplay.getDrawOrDisplay();
        this.catanBoardGameView = gameplay.getCatanBoardGameView();
    }

    //______________________BEHAVIORAL ENUM: CARD TYPE LOGIC_____________________//
    public enum DevelopmentCardType {

        // Take monopoly on a chosen resource by stealing every single resource of that type from everyone
        MONOPOLY("Monopoly") {
            @Override public void play(Player player, DevelopmentCard devCard) {
                devCard.playMonopolyCardAsPlayer(player);
            }
            @Override public void playAsAI(AIOpponent ai, DevelopmentCard devCard, Gameplay gameplay) {
                devCard.playMonopolyCardAsAI(ai);
            }
        },
        // Move the Robber to chosen Tile and steal from a player who owns the Tile.
        KNIGHT("Knight") {
            @Override public void play(Player player, DevelopmentCard devCard) {
                devCard.playKnightCardAsPlayer(player);
            }
            @Override public void playAsAI(AIOpponent ai, DevelopmentCard devCard, Gameplay gameplay) {
                devCard.playKnightCardAsAI(ai, gameplay);
            }
        },
        // Build 2 free roads immediately
        ROADBUILDING("Road Building") {
            @Override public void play(Player player, DevelopmentCard devCard) {
                devCard.playRoadBuildingCardAsPlayer(player);
            }
            @Override public void playAsAI(AIOpponent ai, DevelopmentCard devCard, Gameplay gameplay) {
                devCard.playRoadBuildingCardAsAI(ai, gameplay);
            }
        },
        // Receive two handpicked resources from the bank
        YEAROFPLENTY("Year Of Plenty") {
            @Override public void play(Player player, DevelopmentCard devCard) {
                devCard.playYearOfPlentyCardAsPlayer(player);
            }
            @Override public void playAsAI(AIOpponent ai, DevelopmentCard devCard, Gameplay gameplay) {
                devCard.playYearOfPlentyCardAsAI(ai, gameplay);
            }
        },
        // Get a free Victory Point
        VICTORYPOINT("Victory Point") {
            @Override public void play(Player player, DevelopmentCard devCard) {
                devCard.playVictoryPointCardAsPlayer(player);
            }
            @Override public void playAsAI(AIOpponent ai, DevelopmentCard devCard, Gameplay gameplay) {
                devCard.playVictoryPointCardAsAI(ai, gameplay);
            }
        };

        // Human Player play method
        public abstract void play(Player player, DevelopmentCard devCard);

        // AI Player play method
        public void playAsAI(AIOpponent ai, DevelopmentCard devCard, Gameplay gameplay) {
            throw new UnsupportedOperationException("AI play not implemented for " + this.name());
        }

        // Enum helpers
        private final String displayName;
        DevelopmentCardType(String displayName) {
            this.displayName = displayName;
        }
        public String getDisplayName() {
            return displayName;
        }
    }
    //______________________PLAYER & AI EXECUTION METHODS______________________//

    // ______________MONOPOLY______________//
    public void playMonopolyCardAsPlayer(Player player) {
        startPlayingCard();
        Player currentPlayer = gameplay.getCurrentPlayer();
        String chosenResource = drawOrDisplay.showMonopolyDialog();
        if (chosenResource == null) return;
        int taken = monopolizeResource(chosenResource, currentPlayer);
        catanBoardGameView.logToGameLog("Player " + currentPlayer.getPlayerId() + " played a Monopoly card and took " + taken + " " + chosenResource + " from other players.");
        catanBoardGameView.refreshSidebar();
        finishPlayingCard();
        log("Player " + player.getPlayerId() + " played a monopoly development card");
    }
    private void playMonopolyCardAsAI(AIOpponent ai) {
        String resource = ai.chooseSmartResourceToMonopoly(gameplay);
        int total = monopolizeResource(resource, ai);
        log("AI played Monopoly and took " + total + " " + resource);
    }
    // ______________KNIGHT______________//
    private void playKnightCardAsPlayer(Player player) {
        view.hideDiceButton();
        view.hideTurnButton();
        startPlayingCard();
        view.getRobber().showRobberTargets(view.getBoardGroup());
        gameplay.setRobberMoveRequired(true);
        player.increasePlayedKnights();
        gameplay.getBiggestArmy().calculateAndUpdateBiggestArmy(player);
        log("Player " + player.getPlayerId() + " played a knight development card");
    }
    private void playKnightCardAsAI(AIOpponent ai, Gameplay gameplay) {
        view.runOnFX(() -> {
            ai.increasePlayedKnights();
            gameplay.getBiggestArmy().calculateAndUpdateBiggestArmy(ai);
            view.getRobber().showRobberTargets(view.getBoardGroup());
        });
    }

    // ______________ROADBUILDING______________//
    private void playRoadBuildingCardAsPlayer(Player player) {
        startPlayingCard();
        this.placingFreeRoads = true;
        this.freeRoadsLeft = 2;
        log("Player " + player.getPlayerId() + " played a road building development card");
    }

    private void playRoadBuildingCardAsAI(AIOpponent ai, Gameplay gameplay) {
        int placed = 0;
        for (Edge edge : gameplay.getBoard().getEdges()) {
            if (placed == 2) break;
            if (gameplay.isValidRoadPlacement(edge)) {
                if (gameplay.placeFreeRoad(ai, edge) == BuildResult.SUCCESS) {
                    placed++;
                    log("AI " + gameplay.getCurrentPlayer() + " placed a free road.");
                }
            }
        }
    }

    // ______________YEAR OF PLENTY______________//
    private void playYearOfPlentyCardAsPlayer(Player player) {
        startPlayingCard();
        Player currentPlayer = gameplay.getCurrentPlayer();
        Map<String, Integer> selected = (currentPlayer instanceof AIOpponent ai && ai.getStrategyLevel() == AIOpponent.StrategyLevel.HARD)
                ? ai.chooseResourcesForYearOfPlenty()
                : drawOrDisplay.showYearOfPlentyDialog(currentPlayer.getResources());

        if (selected != null) {
            addResourcesToPlayer(currentPlayer, selected);
            String gained = selected.entrySet().stream()
                    .map(entry -> entry.getValue() + " " + entry.getKey())
                    .collect(Collectors.joining(", "));
            catanBoardGameView.logToGameLog("Player " + currentPlayer.getPlayerId() + " used Year of Plenty and received " + gained + ".");
            catanBoardGameView.refreshSidebar();
            finishPlayingCard();
        }
        log("Player " + player.getPlayerId() + " played a year of plenty development card");
    }

    private void playYearOfPlentyCardAsAI(AIOpponent ai, Gameplay gameplay) {
        Map<String, Integer> selected = ai.chooseResourcesForYearOfPlenty();
        selected.forEach((res, amt) -> ai.getResources().merge(res, amt, Integer::sum));
        String gained = selected.entrySet().stream()
                .map(e -> "+ " + e.getValue() + " " + e.getKey())
                .collect(Collectors.joining(", "));
        log("AI played Year of Plenty: " + gained);
    }

    // ______________VICTORY POINT______________//
    private void playVictoryPointCardAsPlayer(Player player) {
        gameplay.increasePlayerScore(player);
        log("Player " + player.getPlayerId() + " played a victory point development card");
    }

    private void playVictoryPointCardAsAI(AIOpponent ai, Gameplay gameplay) {
        gameplay.increasePlayerScore(ai);
        gameplay.getCatanBoardGameView().runOnFX(() ->
                gameplay.getCatanBoardGameView().logToGameLog("AI played Victory Point and gained 1 point.")
        );
    }

    //__________________________STATE + LOGIC UTILITIES__________________________//

    public void log(String message) {
        if (view != null) view.logToGameLog(message);
        else System.err.println("LOG FAIL (view=null): " + message);
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

    public boolean isPlacingFreeRoads() {
        return placingFreeRoads;
    }

    public void decrementFreeRoads() {
        if (freeRoadsLeft > 0) freeRoadsLeft--;
        if (freeRoadsLeft == 0) placingFreeRoads = false;
    }

    public void addResourcesToPlayer(Player player, Map<String, Integer> added) {
        added.forEach((res, amt) ->
                player.getResources().merge(res, amt, Integer::sum)
        );
    }

    // Helper for Player and AI to play Monopoly Card
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