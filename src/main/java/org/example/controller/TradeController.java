package org.example.controller;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.example.catanboardgameapp.DrawOrDisplay;
import org.example.catanboardgameviews.CatanBoardGameView;

import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import org.example.catanboardgameapp.Gameplay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class TradeController {
    public static void tradeButton(Button tradeButton, Gameplay gameplay, BorderPane root) {
        tradeButton.setOnAction(e -> {
            List<String> resourceOptions = new ArrayList<>();
            gameplay.getCurrentPlayer().getResources().forEach((resource, amount) -> {
                if (amount >= 4) {
                    resourceOptions.add(resource);
                }
            });

            if (resourceOptions.isEmpty()) {
                DrawOrDisplay.showTradeError("You don't have 4 of any resources to trade.");
                return;
            }

            ChoiceDialog<String> giveDialog = new ChoiceDialog<>("Choose a resource...", resourceOptions);
            giveDialog.setTitle("Trade with Bank");
            giveDialog.setHeaderText("Select resource you want to give 4x of:");
            giveDialog.setContentText("Give:");
            Optional<String> giveResult = giveDialog.showAndWait();
            if (giveResult.isEmpty()) return;

            String giveResource = giveResult.get();
            ArrayList<String> receiveOptions = new ArrayList<>(Arrays.asList("Ore", "Wood", "Brick", "Grain", "Wool"));
            receiveOptions.remove(giveResource);

            ChoiceDialog<String> receiveDialog = new ChoiceDialog<>(receiveOptions.get(0), receiveOptions);
            receiveDialog.setTitle("Trade with Bank");
            receiveDialog.setHeaderText("Select the resource you want to receive 1x:");
            receiveDialog.setContentText("Receive:");
            Optional<String> receiveResult = receiveDialog.showAndWait();
            if (receiveResult.isEmpty()) return;

            String receiveResource = receiveResult.get();
            if (!gameplay.tradeWithBank(giveResource, receiveResource)) {
                DrawOrDisplay.showTradeError("You don't have enough " + giveResource + " to trade.");
            } else {
                root.setLeft(CatanBoardGameView.createLeftMenu(gameplay));
            }
        });
    }
}
