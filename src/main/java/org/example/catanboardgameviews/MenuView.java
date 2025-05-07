package org.example.catanboardgameviews;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.example.catanboardgameapp.Gameplay;

import java.util.List;

public class MenuView {

    private static int playerCount = 3;
    private static int boardSize = 3;
    private static int amoutOfAiai = 0;

    public static void showMainMenu(Stage primaryStage) {
        VBox menuLayout = new VBox(25);
        menuLayout.setAlignment(Pos.CENTER);
        menuLayout.setStyle(
                "-fx-padding: 40;" +
                        "-fx-background-color: linear-gradient(to bottom, #1e1e2f, #3a3a5f);" +
                        "-fx-alignment: center;"
        );

        Label titleLabel = new Label("CATAN BOARD GAME");
        titleLabel.setFont(new Font("Arial Black", 36));
        titleLabel.setTextFill(Color.LIGHTGRAY);
        titleLabel.setEffect(new DropShadow());

        Button playButton = createMenuButton("Play", 220, 60);
        Button optionsButton = createMenuButton("Options", 220, 60);
        Button creditsButton = createMenuButton("Credits", 220, 60);
        Button quitButton = createMenuButton("Quit Game", 220, 60);

        playButton.setOnAction(e -> startGame(primaryStage));
        optionsButton.setOnAction(e -> showOptionsMenu(primaryStage));
        creditsButton.setOnAction(e -> showCreditsScreen(primaryStage));
        quitButton.setOnAction(e -> primaryStage.close());

        menuLayout.getChildren().addAll(titleLabel, playButton, optionsButton, creditsButton, quitButton);
        Scene menuScene = new Scene(menuLayout, 800, 600);
        primaryStage.setScene(menuScene);
        primaryStage.setTitle("Catan Board Game");
        primaryStage.show();
    }

    public static void showOptionsMenu(Stage primaryStage) {
        VBox optionsLayout = new VBox(20);
        optionsLayout.setAlignment(Pos.CENTER);
        optionsLayout.setStyle(
                "-fx-padding: 40;" +
                        "-fx-background-color: linear-gradient(to bottom, #2c2c3f, #505080);" +
                        "-fx-alignment: center;"
        );

        Label optionsTitle = new Label("Game Options");
        optionsTitle.setFont(new Font("Arial Black", 28));
        optionsTitle.setTextFill(Color.LIGHTGRAY);

        Label playerLabel = new Label("Number of Players (2-6):");
        playerLabel.setFont(new Font("Arial Black", 15));
        playerLabel.setTextFill(Color.WHITE);
        TextField playerInput = new TextField(String.valueOf(playerCount));
        playerInput.setMaxWidth(200);
        playerInput.setStyle("-fx-font-size: 16px;");

        Label boardLabel = new Label("Board Size (3-10):");
        boardLabel.setTextFill(Color.WHITE);
        boardLabel.setFont(new Font("Arial Black", 15));
        TextField boardInput = new TextField(String.valueOf(boardSize));
        boardInput.setMaxWidth(200);
        boardInput.setStyle("-fx-font-size: 16px;");

        Label aiLabel = new Label("Number of the players to be Ai (2-6):");
        aiLabel.setFont(new Font("Arial Black", 15));
        aiLabel.setTextFill(Color.WHITE);
        TextField aiInput = new TextField(String.valueOf(amoutOfAiai));
        aiInput.setMaxWidth(200);
        aiInput.setStyle("-fx-font-size: 16px;");

        Button backButton = createMenuButton("Accept Changes", 180, 50);
        backButton.setOnAction(e -> {
            try {
                int players = Integer.parseInt(playerInput.getText());
                int size = Integer.parseInt(boardInput.getText());
                int ais = Integer.parseInt(aiInput.getText());

                if (players < 2 || players > 6) {
                    System.out.println("Players must be between 2 and 6.");
                    return;
                }

                if (size < 3 || size > 10) {
                    System.out.println("Board size must be between 3 and 10.");
                    return;
                }
                if ((ais < 2 || ais > 6 )&&(ais > players)) {
                    System.out.println("AI must be between 2 and 6, and no more than the total player amount.");
                }

                playerCount = players;
                boardSize = size;
                amoutOfAiai = ais;

                showMainMenu(primaryStage);
            } catch (NumberFormatException ex) {
                System.out.println("Invalid input.");
            }
        });

        optionsLayout.getChildren().addAll(optionsTitle, playerLabel, playerInput, boardLabel, boardInput,aiLabel, aiInput, backButton);
        Scene optionsScene = new Scene(optionsLayout, 800, 600);
        primaryStage.setScene(optionsScene);
    }

    private static void startGame(Stage primaryStage) {
        if (boardSize < 3 || boardSize > 10 || playerCount < 2 || playerCount > 6) {
            System.out.println("Please configure game options first.");
            return;
        }

        Gameplay gameplay = new Gameplay();
        gameplay.reset();
        gameplay.initializePlayers(playerCount-amoutOfAiai);
        gameplay.initializeAis(amoutOfAiai);
        Scene gameScene = CatanBoardGameView.createGameScene(primaryStage, boardSize - 1, gameplay);
        primaryStage.setScene(gameScene);
    }

    private static Button createMenuButton(String text, int width, int height) {
        Button button = new Button(text);
        button.setPrefSize(width, height);
        button.setFont(new Font("Arial", 20));
        button.setStyle(
                "-fx-background-color: #4CAF50;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 10;" +
                        "-fx-cursor: hand;"
        );
        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: #45a049;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 10;"
        ));
        button.setOnMouseExited(e -> button.setStyle(
                "-fx-background-color: #4CAF50;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 10;"
        ));
        return button;
    }
    public static void showCreditsScreen(Stage primaryStage) {
        VBox creditsLayout = new VBox(15);
        creditsLayout.setAlignment(Pos.CENTER);
        creditsLayout.setStyle(
                "-fx-padding: 40;" +
                        "-fx-background-color: linear-gradient(to bottom, #0f2027, #203a43, #2c5364);" +
                        "-fx-alignment: center;"
        );

        Label title = new Label("CREDITS");
        title.setFont(new Font("Arial Black", 36));
        title.setTextFill(Color.WHITE);
        title.setEffect(new DropShadow());

        Label name1 = new Label("• Johan    - Game Developer");
        Label name2 = new Label("• Kajsa    - Game Developer");
        Label name3 = new Label("• Lizette  - Game Developer");
        Label name4 = new Label("• Patrick  - Game Developer");

        for (Label label : List.of(name1, name2, name3, name4)) {
            label.setFont(new Font("Arial", 18));
            label.setTextFill(Color.LIGHTGRAY);
        }

        Button backButton = createMenuButton("Back", 180, 50);
        backButton.setOnAction(e -> showMainMenu(primaryStage));

        creditsLayout.getChildren().addAll(title, name1, name2, name3, name4, backButton);
        Scene creditsScene = new Scene(creditsLayout, 800, 600);
        primaryStage.setScene(creditsScene);
    }

}
