package org.example.catanboardgameapp;
import org.example.catanboardgameviews.CatanBoardGameView;
import org.example.controller.TradeController;
import java.util.*;
import java.util.stream.Collectors;

// Handles usage logic for all types of development cards in Catan, including player/AI logic.
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
        String resource = chooseSmartResourceToMonopoly(gameplay, ai);
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

    // Second Monopoly helper function
    public String chooseSmartResourceToMonopoly(Gameplay gameplay, AIOpponent ai){
        //getting all opponents
        List<Player> opponents = gameplay.getPlayerList().stream()
                .filter(p -> p != ai)
                .toList();
        // finding there resources
        Map<String, Integer> totalOpponentResources = new HashMap<>();

        for (Player p : opponents) {
            for (Map.Entry<String, Integer> entry : p.getResources().entrySet()) {
                totalOpponentResources.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }
        // Calculate how much the player has of each resource
        int oreHave = ai.getResources().getOrDefault("Ore", 0);
        int grainHave = ai.getResources().getOrDefault("Grain", 0);
        int woolHave = ai.getResources().getOrDefault("Wool", 0);
        int woodHave = ai.getResources().getOrDefault("Wood", 0);
        int brickHave = ai.getResources().getOrDefault("Brick", 0);

        // Calculate how much you can steal in total of each resource
        int oreFromOpponents = totalOpponentResources.getOrDefault("Ore", 0);
        int grainFromOpponents = totalOpponentResources.getOrDefault("Grain", 0);
        int woolFromOpponents = totalOpponentResources.getOrDefault("Wool", 0);
        int woodFromOpponents = totalOpponentResources.getOrDefault("Wood", 0);
        int brickFromOpponents = totalOpponentResources.getOrDefault("Brick", 0);

        // Check if we can steal enough to build a full city
        int oreNeed = Math.max(0, 3 - oreHave);
        int grainNeed = Math.max(0, 2 - grainHave);
        if ((oreFromOpponents >= oreNeed) && grainNeed==0 ){
            return "Ore";
        } else if (grainFromOpponents >= grainNeed && oreNeed==0) {
            return"Grain";
        }
        // Check if we can steal enough to build a settlement
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
        // If not, pick the one with the largest trading potential
        List<Harbor.HarborType> harborTypes = new ArrayList<>();
        for (Harbor harbor : gameplay.getBoard().getHarbors()) {
            if (harbor.usableBy(ai)) {
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
}