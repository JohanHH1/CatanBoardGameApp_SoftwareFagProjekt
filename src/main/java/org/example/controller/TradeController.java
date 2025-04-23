package org.example.controller;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
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
            List<String> resourceOptions = Arrays.asList("Brick", "Wood", "Grain", "Wool", "Ore");

            ChoiceDialog<String> giveDialog = new ChoiceDialog<>("Brick", resourceOptions);
            giveDialog.setTitle("Trade with Bank");
            giveDialog.setHeaderText("Select resource you want to give 4x of:");
            giveDialog.setContentText("Give:");
            Optional<String> giveResult = giveDialog.showAndWait();
            if (giveResult.isEmpty()) return;

            String giveResource = giveResult.get();
            List<String> receiveOptions = new ArrayList<>(resourceOptions);
            receiveOptions.remove(giveResource);

            ChoiceDialog<String> receiveDialog = new ChoiceDialog<>(receiveOptions.get(0), receiveOptions);
            receiveDialog.setTitle("Trade with Bank");
            receiveDialog.setHeaderText("Select the resource you want to receive 1x:");
            receiveDialog.setContentText("Receive:");
            Optional<String> receiveResult = receiveDialog.showAndWait();
            if (receiveResult.isEmpty()) return;

            String receiveResource = receiveResult.get();
            if (!gameplay.tradeWithBank(giveResource, receiveResource)) {
                CatanBoardGameView.showTradeError("You don't have enough " + giveResource + " to trade.");
            } else {
                root.setLeft(CatanBoardGameView.createLeftMenu(gameplay));
            }
        });
    }
}
