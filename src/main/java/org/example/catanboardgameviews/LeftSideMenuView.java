package org.example.catanboardgameviews;

import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.example.catanboardgameapp.Gameplay;
import org.example.catanboardgameapp.Player;

import java.util.List;

public class LeftSideMenuView {

    public static VBox createLeftMenu(Gameplay gameplay) {
        return createAllPlayerMenu(gameplay.getPlayerList());
    }

    public static VBox createAllPlayerMenu(List<Player> players) {
        VBox leftMenu = new VBox(10);
        leftMenu.setStyle("-fx-padding: 10; -fx-background-color: #e0e0e0; -fx-min-width: 200;");

        Text title = new Text("Player Stats");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        leftMenu.getChildren().add(title);

        for (Player player : players) { //gameplay.getPlayerList()) {
            VBox playerBox = new VBox(5);

            Text playerName = new Text("Player " + player.getPlayerId());
            playerName.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            playerName.setFill(player.getColor());

            playerBox.getChildren().add(playerName);

            for (String resourceName : player.getResources().keySet()) {
                int count = player.getResources().get(resourceName);
                Text resourceText = new Text(resourceName + ": " + count);
                resourceText.setFont(Font.font("Arial", 12));
                playerBox.getChildren().add(resourceText);
            }

            leftMenu.getChildren().add(playerBox);
        }

        return leftMenu;
    }
}

