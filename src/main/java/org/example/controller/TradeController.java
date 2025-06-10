package org.example.controller;

import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import org.example.catanboardgameapp.*;

import java.util.*;
import java.util.stream.Collectors;

public class TradeController {

    private final GameController gameController;
    private final DrawOrDisplay drawOrDisplay;

    //___________________________CONTROLLER__________________________________//
    public TradeController(GameController gameController, int boardRadius) {
        this.gameController = gameController;
        this.drawOrDisplay = gameController.getGameplay().getDrawOrDisplay();
    }

    //___________________________FUNCTIONS__________________________________//
    public void setupTradeButton(Button tradeButton) {
        // Prevent trading while placing free roads (e.g., from Road Building card)
        tradeButton.setOnAction(e -> {
            if (gameController.getGameplay().isActionBlockedByDevelopmentCard()) {
                drawOrDisplay.showMustPlaceRobberPopup();
                return;
            }
            Gameplay gameplay = gameController.getGameplay();
            // Enforce Humans cant Trade while its AI turn
            if (gameplay.isBlockedByAITurn()) {System.out.println("AI TURN");}
            // Enforce dice roll before trading
            else if (!gameplay.isInInitialPhase() && !gameplay.hasRolledDice()) {
                drawOrDisplay.rollDiceBeforeActionPopup("You must roll the dice before Trading!");
            }
            else {
                Map<String, Integer> bestRatios = new HashMap<>();
                List<Harbor> harbors = gameplay.getBoard().getHarbors();

                // All resources start with default 4:1 bank trade ratio
                for (String res : Arrays.asList("Ore", "Wood", "Brick", "Grain", "Wool")) {
                    bestRatios.put(res, 4);
                }

                // Update trade ratios based on harbors the player has access to
                for (Harbor harbor : harbors) {
                    if (harbor.usableBy(gameplay.getCurrentPlayer())) {
                        Harbor.HarborType type = harbor.getType();
                        if (type == Harbor.HarborType.GENERIC) { // 3:1 harbor applies to all resources
                            for (String res : bestRatios.keySet()) {
                                bestRatios.put(res, Math.min(bestRatios.get(res), 3));
                            }
                        } else {
                            String specific = type.specific.getName(); // 2:1 harbor for a specific resource
                            bestRatios.put(specific, Math.min(bestRatios.get(specific), 2));
                        }
                    }
                }

                // Filter resources the player has enough of to trade
                List<String> tradeableResources = new ArrayList<>();
                for (Map.Entry<String, Integer> entry : gameplay.getCurrentPlayer().getResources().entrySet()) {
                    String resource = entry.getKey();
                    int amount = entry.getValue();
                    if (amount >= bestRatios.getOrDefault(resource, 4)) {
                        tradeableResources.add(resource);
                    }
                }
                if (tradeableResources.isEmpty()) {
                    drawOrDisplay.showTradeError("You don't have enough resources to trade based on your harbors.");
                    return;
                }
                // First dialog: pick what resource to give
                ChoiceDialog<String> giveDialog = new ChoiceDialog<>(tradeableResources.get(0), tradeableResources);
                giveDialog.setTitle("Harbor Trade");
                giveDialog.setHeaderText("Select the resource you want to give:");
                giveDialog.setContentText("Give:");
                Optional<String> giveResult = giveDialog.showAndWait();
                if (giveResult.isEmpty()) return;

                String giveResource = giveResult.get();
                int ratio = bestRatios.getOrDefault(giveResource, 4);

                List<String> receiveOptions = new ArrayList<>(Arrays.asList("Ore", "Wood", "Brick", "Grain", "Wool"));
                receiveOptions.remove(giveResource);

                // Second dialog: pick what resource to receive
                ChoiceDialog<String> receiveDialog = new ChoiceDialog<>(receiveOptions.get(0), receiveOptions);
                receiveDialog.setTitle("Harbor Trade");
                receiveDialog.setHeaderText("Select the resource you want to receive:");
                receiveDialog.setContentText("Receive:");
                Optional<String> receiveResult = receiveDialog.showAndWait();
                if (receiveResult.isEmpty()) return;

                String receiveResource = receiveResult.get();

                // Final validation: check if player has enough to complete the trade
                if (!gameplay.canRemoveResource(giveResource, ratio)) {
                    drawOrDisplay.showTradeError("You don't have enough " + giveResource + " to trade (requires " + ratio + ").");
                    return;
                }
                // Perform the trade
                gameplay.removeResource(giveResource, ratio);
                gameplay.addResource(receiveResource, 1);
                gameplay.getCatanBoardGameView().logToGameLog("Traded " + ratio + " " + giveResource + " for 1 " + receiveResource);
                gameplay.getCatanBoardGameView().refreshSidebar();
            }
        });
    }
// ___________________________DEVELOPMENT CARD: MONOPOLY___________________________ //
    public void playMonopolyCardFromButton() {
        Gameplay gameplay = gameController.getGameplay();
        DevelopmentCard devCard = gameplay.getDevelopmentCard();
        Player currentPlayer = gameplay.getCurrentPlayer();

        List<String> resourceOptions = Arrays.asList("Ore", "Wood", "Brick", "Grain", "Wool");
        // Let player pick a resource to monopolize
        ChoiceDialog<String> dialog = new ChoiceDialog<>(resourceOptions.get(0), resourceOptions);
        dialog.setTitle("Monopoly Development Card");
        dialog.setHeaderText("Select the resource to monopolize:");
        dialog.setContentText("Resource:");
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return;

        String chosenResource = result.get();

        // Take all resources of that type from other players
        int taken = devCard.monopolizeResource(chosenResource, currentPlayer);

        // Log the result
        gameplay.getCatanBoardGameView().logToGameLog("Player " + currentPlayer.getPlayerId() +
                " played a Monopoly card and took " + taken + " " + chosenResource + " from other players.");
        gameplay.getCatanBoardGameView().refreshSidebar();
        gameplay.getDevelopmentCard().finishPlayingCard();
    }
// ___________________________DEVELOPMENT CARD: YEAR OF PLENTY___________________________ //
    public void playYearOfPlentyCardFromButton() {
        Gameplay gameplay = gameController.getGameplay();
        DevelopmentCard devCard = gameplay.getDevelopmentCard();
        Player currentPlayer = gameplay.getCurrentPlayer();

        // Show selection popup to choose any 2 resources
        Map<String, Integer> selected = drawOrDisplay.showYearOfPlentyDialog();
        if (selected != null) {
            devCard.addResourcesToPlayer(currentPlayer, selected);
            gameplay.getCatanBoardGameView().logToGameLog("Player " + currentPlayer.getPlayerId() +
                    " used Year of Plenty and received: " +
                    selected.entrySet().stream()
                            .map(e -> e.getValue() + " " + e.getKey())
                            .collect(Collectors.joining(", ")) + ".");
            gameplay.getCatanBoardGameView().refreshSidebar();
            gameplay.getDevelopmentCard().finishPlayingCard();
        }
    }

}
