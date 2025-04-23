package org.example.catanboardgameviews;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.catanboardgameapp.Gameplay;

public class MenuView {

    public static void showSetupScreen(Stage primaryStage) {
        VBox layout = new VBox(10); // new scen base is called layout
        layout.setStyle("-fx-padding: 20; -fx-alignment: center;");

        //input from user
        Label playerLable = new Label("How many players");
        TextField playerInput = new TextField("2"); // initializing with 3 players // mellem 2 og 6

        Label boardSizeLable = new Label("Insert board size");
        TextField boardSizeInput = new TextField("3"); // må ikke være under 3

        Button startButton = getButton(primaryStage, boardSizeInput, playerInput);
        layout.getChildren().addAll(playerLable, playerInput, boardSizeLable,boardSizeInput, startButton);
        Scene setupScene = new Scene(layout, 800, 600); // creating new scene with the base layout

        primaryStage.setTitle("Catan Board Game");
        primaryStage.setScene(setupScene);
        primaryStage.show();
    }

    private static Button getButton(Stage primaryStage, TextField boardSizeInput, TextField playerInput) {
        Button startButton = new Button("Start game");
        startButton.setOnAction(event -> { // if Start game pushed we redirect to gameScene
            try {
                int players = Integer.parseInt(playerInput.getText()); // converts input string to int
                int radius = Integer.parseInt(boardSizeInput.getText());

                //check radius is not too small
                if (radius < 3) {
                    System.out.println("Board size must be at least 3, try again");
                    return;
                } else if (radius > 10){
                    System.out.println("Board size must be at most 10, try again");
                    return;
                }

                //Creates new gameveiw from the BoardGame-class and puts it in primaryStage
                Gameplay gameplay = new Gameplay();
                gameplay.reset();
                gameplay.initializePlayers(players);
                Scene gameScene = CatanBoardGameView.createGameScene(primaryStage, radius-1, gameplay); // creating the board as our gameScene based on input
                //Creates new gameview from the BoardGame-class and puts it in primaryStage
                //Scene gameScene = CatanBoardGameView.createGameScene(primaryStage, radius-1); // creating the board as our gameScene based on input
                primaryStage.setScene(gameScene); //setting current scene to gameScene
            } catch (NumberFormatException exception) {
                System.out.println("Not acceptable input, try again");
            }
        });
        return startButton;
    }
}
