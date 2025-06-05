package org.example.catanboardgameviews;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import org.example.catanboardgameapp.Gameplay;
import org.example.controller.GameController;

import java.util.List;

public class MenuView {

    private int playerCount = 3;
    private int boardSize = 3;
    private int AIOpponentsCountEASY = 0;
    private int AIOpponentsCountMEDIUM = 0;
    private int AIOpponentsCountHARD = 0;

    private final GameController gameController;
    private Gameplay gameplay;
    private final Stage primaryStage;

    public MenuView(Stage primaryStage, GameController gameController) {
        this.primaryStage = primaryStage;
        this.gameController = gameController;
    }

    public void showMainMenu() {
        VBox menuLayout = createMenuLayout();
        Scene menuScene = new Scene(menuLayout, 1050, 700);
        primaryStage.setScene(menuScene);
        primaryStage.setTitle("Catan Board Game");
        primaryStage.show();
    }

    private VBox createMenuLayout() {
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

        Button playButton = createMenuButton("Start New Game", 220, 60);
        Button aiTestButton = createMenuButton("Start AI vs AI Test Match", 220, 60); // <-- New button
        Button optionsButton = createMenuButton("Options", 220, 60);
        Button creditsButton = createMenuButton("Credits", 220, 60);
        Button quitButton = createMenuButton("Quit Game", 220, 60);
        Button resumeButton = createMenuButton("Resume current Game", 220, 60);

        playButton.setOnAction(e -> startGame());
        aiTestButton.setOnAction(e -> startAITestMatch()); // <-- New action
        optionsButton.setOnAction(e -> showOptionsMenu(primaryStage));
        creditsButton.setOnAction(e -> showCreditsScreen(primaryStage));
        quitButton.setOnAction(e -> primaryStage.close());

        resumeButton.setDisable(!gameController.hasSavedSession());
        resumeButton.setOnAction(e -> gameController.resumeGame());

        menuLayout.getChildren().addAll(
                titleLabel, playButton, aiTestButton, optionsButton, creditsButton, quitButton, resumeButton
        );
        return menuLayout;
    }


    private void startGame() {
        System.out.println("NEW GAME IS STARTING");
        gameController.startGame(playerCount, boardSize, AIOpponentsCountEASY, AIOpponentsCountMEDIUM, AIOpponentsCountHARD);
    }
    private void startAITestMatch() {
        System.out.println("AI VS AI TEST MATCH IS STARTING");
        playerCount = 0;              // No human players
        boardSize = 3;                // Small board for quick testing
        AIOpponentsCountEASY = 1;
        AIOpponentsCountMEDIUM = 1;
        AIOpponentsCountHARD = 1;
        gameController.startGame(playerCount, boardSize, AIOpponentsCountEASY, AIOpponentsCountMEDIUM, AIOpponentsCountHARD);
    }


    public void showOptionsMenu(Stage primaryStage) {
        VBox optionsLayout = new VBox(20);
        optionsLayout.setAlignment(Pos.CENTER);
        optionsLayout.setStyle(
                "-fx-padding: 40;" +
                        "-fx-background-color: linear-gradient(to bottom, #2c2c3f, #505080);"
        );

        Label optionsTitle = new Label("Game Options");
        optionsTitle.setFont(new Font("Arial Black", 28));
        optionsTitle.setTextFill(Color.LIGHTGRAY);

        Label totalNote = new Label("Choose 2–6 Total Players");
        totalNote.setFont(Font.font("Arial", 16));
        totalNote.setTextFill(Color.LIGHTGRAY);

        int[] humanPlayers = {3}, boardSizeVal = {3}, easyAI = {0}, mediumAI = {0}, hardAI = {0};

        Font labelFont = Font.font("Arial", FontWeight.BOLD, 14);
        Color fontColor = Color.WHITE;

        Label[] labels = {
                new Label("Number of Human Players:"),
                new Label("Number of EASY AI:"),
                new Label("Number of MEDIUM AI:"),
                new Label("Number of HARD AI:"),
                new Label("Board Size (3-10):")
        };
        Label[] values = {
                new Label(String.valueOf(humanPlayers[0])),
                new Label(String.valueOf(easyAI[0])),
                new Label(String.valueOf(mediumAI[0])),
                new Label(String.valueOf(hardAI[0])),
                new Label(String.valueOf(boardSizeVal[0]))
        };
        for (int i = 0; i < labels.length; i++) {
            labels[i].setFont(labelFont);
            labels[i].setTextFill(fontColor);
            values[i].setFont(labelFont);
            values[i].setTextFill(fontColor);
        }

        Runnable updateCounts = () -> {
            values[0].setText(String.valueOf(humanPlayers[0]));
            values[1].setText(String.valueOf(easyAI[0]));
            values[2].setText(String.valueOf(mediumAI[0]));
            values[3].setText(String.valueOf(hardAI[0]));
            values[4].setText(String.valueOf(boardSizeVal[0]));
        };

        Button[][] controls = new Button[5][2];
        for (int i = 0; i < 5; i++) {
            controls[i][0] = new Button("-");
            controls[i][1] = new Button("+");
            controls[i][0].setMinWidth(35);
            controls[i][1].setMinWidth(35);
        }

        controls[0][1].setOnAction(e -> {
            if (humanPlayers[0] + easyAI[0] + mediumAI[0] + hardAI[0] < 6) humanPlayers[0]++;
            updateCounts.run();
        });
        controls[0][0].setOnAction(e -> {
            if (humanPlayers[0] > 0) humanPlayers[0]--;
            updateCounts.run();
        });

        controls[1][1].setOnAction(e -> {
            if (humanPlayers[0] + easyAI[0] + mediumAI[0] + hardAI[0] < 6) easyAI[0]++;
            updateCounts.run();
        });
        controls[1][0].setOnAction(e -> {
            if (easyAI[0] > 0) easyAI[0]--;
            updateCounts.run();
        });

        controls[2][1].setOnAction(e -> {
            if (humanPlayers[0] + easyAI[0] + mediumAI[0] + hardAI[0] < 6) mediumAI[0]++;
            updateCounts.run();
        });
        controls[2][0].setOnAction(e -> {
            if (mediumAI[0] > 0) mediumAI[0]--;
            updateCounts.run();
        });

        controls[3][1].setOnAction(e -> {
            if (humanPlayers[0] + easyAI[0] + mediumAI[0] + hardAI[0] < 6) hardAI[0]++;
            updateCounts.run();
        });
        controls[3][0].setOnAction(e -> {
            if (hardAI[0] > 0) hardAI[0]--;
            updateCounts.run();
        });

        controls[4][1].setOnAction(e -> {
            if (boardSizeVal[0] < 10) boardSizeVal[0]++;
            updateCounts.run();
        });
        controls[4][0].setOnAction(e -> {
            if (boardSizeVal[0] > 3) boardSizeVal[0]--;
            updateCounts.run();
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setAlignment(Pos.CENTER);
        for (int i = 0; i < labels.length; i++) {
            grid.add(labels[i], 0, i);
            grid.add(controls[i][0], 1, i);
            grid.add(values[i], 2, i);
            grid.add(controls[i][1], 3, i);
        }

        Button accept = new Button("Accept Changes");
        accept.setStyle("-fx-font-size: 18px; -fx-background-color: #28a745; -fx-text-fill: white; -fx-padding: 10 20 10 20;");
        accept.setOnAction(e -> {
            int total = humanPlayers[0] + easyAI[0] + mediumAI[0] + hardAI[0];
            if (total < 2 || total > 6) {
                System.out.println("Total players must be between 2 and 6.");
                return;
            }
            playerCount = humanPlayers[0];
            boardSize = boardSizeVal[0];
            AIOpponentsCountEASY = easyAI[0];
            AIOpponentsCountMEDIUM = mediumAI[0];
            AIOpponentsCountHARD = hardAI[0];
            showMainMenu();
        });

        optionsLayout.getChildren().addAll(optionsTitle, totalNote, grid, accept);
        Scene scene = new Scene(optionsLayout, 850, 600);
        primaryStage.setScene(scene);
    }

    public void showCreditsScreen(Stage primaryStage) {
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
        backButton.setOnAction(e -> showMainMenu());

        creditsLayout.getChildren().addAll(title, name1, name2, name3, name4, backButton);
        Scene creditsScene = new Scene(creditsLayout, 850, 600);
        primaryStage.setScene(creditsScene);
    }

    private Button createMenuButton(String text, int width, int height) {
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
}
