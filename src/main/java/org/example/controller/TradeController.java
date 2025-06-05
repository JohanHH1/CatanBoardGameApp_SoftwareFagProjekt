package org.example.controller;

import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import org.example.catanboardgameapp.DrawOrDisplay;
import org.example.catanboardgameapp.Gameplay;
import org.example.catanboardgameapp.Player;

import java.util.*;
import java.util.stream.Collectors;

public class TradeController {

    private final GameController gameController;
    private final DrawOrDisplay drawOrDisplay;

    //___________________________CONTROLLER__________________________________//
    public TradeController(GameController gameController) {
        this.gameController = gameController;
        this.drawOrDisplay = new DrawOrDisplay(gameController.getGameplay().getBoardRadius());
    }

    //___________________________FUNCTIONS__________________________________//
    public void setupTradeButton(Button tradeButton) {
        tradeButton.setOnAction(e -> {
            Gameplay gameplay = gameController.getGameplay();

            if (!gameplay.isInInitialPhase() && gameplay.hasRolledDice()) {
                drawOrDisplay.rollDiceBeforeActionPopup("You must roll the dice before Trading!");
                return;
            }

            List<String> resourceOptions = new ArrayList<>();
            gameplay.getCurrentPlayer().getResources().forEach((resource, amount) -> {
                if (amount >= 4) {
                    resourceOptions.add(resource);
                }
            });

            if (resourceOptions.isEmpty()) {
                drawOrDisplay.showTradeError("You don't have 4 of any resources to trade.");
                return;
            }

            ChoiceDialog<String> giveDialog = new ChoiceDialog<>(resourceOptions.get(0), resourceOptions);
            giveDialog.setTitle("Trade with Bank");
            giveDialog.setHeaderText("Select resource you want to give 4x of:");
            giveDialog.setContentText("Give:");
            Optional<String> giveResult = giveDialog.showAndWait();
            if (giveResult.isEmpty()) return;

            String giveResource = giveResult.get();
            List<String> receiveOptions = new ArrayList<>(Arrays.asList("Ore", "Wood", "Brick", "Grain", "Wool"));
            receiveOptions.remove(giveResource);

            ChoiceDialog<String> receiveDialog = new ChoiceDialog<>(receiveOptions.get(0), receiveOptions);
            receiveDialog.setTitle("Trade with Bank");
            receiveDialog.setHeaderText("Select the resource you want to receive 1x:");
            receiveDialog.setContentText("Receive:");
            Optional<String> receiveResult = receiveDialog.showAndWait();
            if (receiveResult.isEmpty()) return;

            String receiveResource = receiveResult.get();
            if (!gameplay.tradeWithBank(giveResource, receiveResource)) {
                drawOrDisplay.showTradeError("You don't have enough " + giveResource + " to trade.");
            } else {
                gameController.getGameView().refreshSidebar();
            }
        });
    }
    public void playMonopolyCardFromButton() {
        Gameplay gameplay = gameController.getGameplay();
        Player currentPlayer = gameplay.getCurrentPlayer();

        List<String> resourceOptions = Arrays.asList("Ore", "Wood", "Brick", "Grain", "Wool");

        ChoiceDialog<String> dialog = new ChoiceDialog<>(resourceOptions.get(0), resourceOptions);
        dialog.setTitle("Monopoly Development Card");
        dialog.setHeaderText("Select the resource to monopolize:");
        dialog.setContentText("Resource:");
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return;

        String chosenResource = result.get();
        int taken = gameplay.monopolizeResource(chosenResource, currentPlayer);

        gameplay.getCatanBoardGameView().logToGameLog("Player " + currentPlayer.getPlayerId() +
                " played a Monopoly card and took " + taken + " " + chosenResource + " from other players.");
        gameplay.getCatanBoardGameView().refreshSidebar();
    }
    public void playYearOfPlentyCardFromButton() {
        Gameplay gameplay = gameController.getGameplay();
        Player currentPlayer = gameplay.getCurrentPlayer();

        Map<String, Integer> selected = new DrawOrDisplay(gameplay.getBoardRadius()).showYearOfPlentyDialog();
        if (selected != null) {
            gameplay.addResourcesToPlayer(selected);
            gameplay.getCatanBoardGameView().logToGameLog("Player " + currentPlayer.getPlayerId() +
                    " used Year of Plenty and received: " +
                    selected.entrySet().stream()
                            .map(e -> e.getValue() + " " + e.getKey())
                            .collect(Collectors.joining(", ")) + ".");
            gameplay.getCatanBoardGameView().refreshSidebar();
        }
    }
}
