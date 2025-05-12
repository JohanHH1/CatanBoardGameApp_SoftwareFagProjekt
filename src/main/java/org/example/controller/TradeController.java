package org.example.controller;

import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import org.example.controller.GameController;
import org.example.catanboardgameapp.DrawOrDisplay;
import org.example.catanboardgameapp.Gameplay;

import java.util.*;

public class TradeController {
    private final GameController gameController;
    private final DrawOrDisplay drawOrDisplay;

    public TradeController(GameController gameController) {
        this.gameController = gameController;
        this.drawOrDisplay = new DrawOrDisplay(gameController.getGameplay().getBoardRadius());
    }

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
}
