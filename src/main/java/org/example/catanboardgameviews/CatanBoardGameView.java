package org.example.catanboardgameviews;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import org.example.catanboardgameapp.*;
import org.example.controller.BuildController;
import org.example.controller.TradeController;
import org.example.controller.TurnController;

import java.io.InputStream;
import java.util.*;

import static org.example.catanboardgameapp.Robber.robberDeNiro;

public class CatanBoardGameView {
    public static Button nextTurnButton;
    public static Button rollDiceButton;
    public static int boardRadius;
    public static Circle currentRobberCircle = null;

    // __________________________KINDA CONSTRUCTOR - Creating the Game Scene_________________________
    public static Scene createGameScene(Stage primaryStage, int radius, Gameplay gameplay) {
        double sceneWidth = 800;
        double sceneHeight = 600;
        boardRadius = radius;

        // Initialize Board
        Board board = new Board(radius, sceneWidth, sceneHeight);
        gameplay.setBoard(board); // Link board to gameplay logic

        // Draw hex tiles
        Group boardGroup = Tile.createBoardTiles(board, radius);

        // Find and assign the desert tile (dice number 7)
        Tile desertTile = Board.getTiles().stream()
                .filter(t -> t.getTileDiceNumber() == 7)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No desert tile found"));

        BorderPane root = new BorderPane();
        VBox leftMenu = createLeftMenu(gameplay);
        root.setLeft(leftMenu);

        // draw edges and vertices
        BuildController buildController = new BuildController(gameplay, boardGroup);
        DrawOrDisplay.drawEdges(board, boardGroup, buildController, radius, root);
        DrawOrDisplay.displayVertices(board, boardGroup, buildController, radius, root);

        // Initialize the robber
        robberDeNiro = new Robber(desertTile, gameplay, boardGroup);

        Pane boardWrapper = new Pane(boardGroup);
        root.setCenter(boardWrapper);
        centerBoard(board, boardGroup, sceneWidth, sceneHeight);

        //________________________________BUTTONS_______________________________________________
        // Initializing top screen Buttons
        rollDiceButton = new Button("Roll Dice");
        nextTurnButton = new Button("Next Turn");
        Button centerButton = new Button("Centralize Board");
        Button zoomInButton = new Button("+");
        Button zoomOutButton = new Button("-");
        Button showCostsButton = new Button("Show Building Costs");
        Button tradeButton = new Button("Trade with Bank 4:1");
        ImageView diceImage1 = new ImageView();
        ImageView diceImage2 = new ImageView();
        HBox diceBox = new HBox(5, diceImage1, diceImage2);
        diceBox.setPadding(new Insets(5));
        Button exitButton = new Button("Exit");

        // Button Actions
        // Dice and next turn
        rollDiceButton.setOnAction(e ->
                rollDiceAndDistribute(gameplay, diceImage1, diceImage2, root, boardGroup, board)
        );
        TurnController turnController = new TurnController(gameplay, rollDiceButton, nextTurnButton);
        nextTurnButton.setOnAction(turnController::handleNextTurnButtonPressed);

        // Centralize and zoom buttons
        centerButton.setOnAction(e -> {centerBoard(board, boardGroup, sceneWidth, sceneHeight);});
        zoomInButton.setOnAction(e -> {
            double scale = boardGroup.getScaleX() * 1.1;
            boardGroup.setScaleX(scale);
            boardGroup.setScaleY(scale);
        });
        zoomOutButton.setOnAction(e -> {
            double scale = boardGroup.getScaleX() * 0.9;
            boardGroup.setScaleX(scale);
            boardGroup.setScaleY(scale);
        });

        // Trade, showCost and exit button actions
        TradeController.tradeButton(tradeButton, gameplay, root);
        showCostsButton.setOnAction(e -> showBuildingCostsPopup());
        exitButton.setOnAction(e -> {
            Alert alert = new Alert(AlertType.CONFIRMATION, "Are you sure you want to exit to the main menu?", ButtonType.YES, ButtonType.NO);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.YES) {
                returnToMainMenu(primaryStage);
            }
        });

        // Add all buttons to top bar
        HBox buttonBox = new HBox(10, rollDiceButton, nextTurnButton, centerButton, zoomInButton, zoomOutButton, tradeButton, showCostsButton, diceBox, exitButton);
        buttonBox.setStyle("-fx-padding: 10; -fx-alignment: top-left;");

        // Dont show roll dice or next turn button until initial placements and game starting
        rollDiceButton.setVisible(false);
        nextTurnButton.setVisible(false);
        root.setTop(buttonBox);

        // Scroll Zoom Handler
        boardWrapper.setOnScroll((ScrollEvent event) -> {
            double zoomFactor = event.getDeltaY() > 0 ? 1.05 : 0.95;
            double scale = boardGroup.getScaleX() * zoomFactor;
            scale = Math.max(0.5, Math.min(scale, 3.0));
            boardGroup.setScaleX(scale);
            boardGroup.setScaleY(scale);
            event.consume();
        });

        // Mouse Drag Board Movement
        final double[] anchorX = new double[1];
        final double[] anchorY = new double[1];
        final double[] initialTranslateX = new double[1];
        final double[] initialTranslateY = new double[1];

        boardWrapper.setOnMousePressed(event -> {
            anchorX[0] = event.getX();
            anchorY[0] = event.getY();
            initialTranslateX[0] = boardGroup.getTranslateX();
            initialTranslateY[0] = boardGroup.getTranslateY();
        });

        boardWrapper.setOnMouseDragged(event -> {
            double deltaX = event.getX() - anchorX[0];
            double deltaY = event.getY() - anchorY[0];
            boardGroup.setTranslateX(initialTranslateX[0] + deltaX);
            boardGroup.setTranslateY(initialTranslateY[0] + deltaY);
        });

        // Keyboard Shortcuts
        Scene scene = new Scene(root, sceneWidth, sceneHeight, Color.LIGHTGRAY);
        scene.setOnKeyPressed(event -> {
            double step = 30;
            switch (event.getCode()) {
                case W -> boardGroup.setTranslateY(boardGroup.getTranslateY() - step);
                case A -> boardGroup.setTranslateX(boardGroup.getTranslateX() - step);
                case S -> boardGroup.setTranslateY(boardGroup.getTranslateY() + step);
                case D -> boardGroup.setTranslateX(boardGroup.getTranslateX() + step);
                case R, C -> {centerBoard(board, boardGroup, sceneWidth, sceneHeight);
                }
                case ESCAPE -> {
                    Alert alert = new Alert(AlertType.CONFIRMATION, "Are you sure you want to exit to the main menu?", ButtonType.YES, ButtonType.NO);
                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.YES) {
                        returnToMainMenu(primaryStage);
                    }
                }
            }
        });

        return scene;
    }

    //__________________________ACTION FUNCTIONS_____________________________________

    // Navigate back to the main menu
    public static void returnToMainMenu(Stage primaryStage) {
        MenuView.showMainMenu(primaryStage);
    }

    // Builds the left player information menu
    public static VBox createLeftMenu(Gameplay gameplay) {
        VBox playerListVBox = new VBox(10);
        playerListVBox.setPadding(new Insets(10));

        int playerCount = gameplay.getPlayerList().size();
        double nameFontSize = 14;
        double infoFontSize = 12;

        Text title = new Text("Player Stats");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        playerListVBox.getChildren().add(title);

        for (Player player : gameplay.getPlayerList()) {
            VBox playerBox = new VBox(5);

            Text playerName = new Text("Player " + player.getPlayerId());
            playerName.setFont(Font.font("Arial", FontWeight.BOLD, nameFontSize));
            playerName.setFill(player.getColor());

            if (player == gameplay.getCurrentPlayer()) {
                playerBox.setStyle("-fx-background-color: lightyellow; -fx-border-color: black; -fx-border-width: 2px;");
                playerName.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, nameFontSize + 2));
            }

            playerBox.getChildren().add(playerName);

            for (String resourceName : player.getResources().keySet()) {
                int count = player.getResources().get(resourceName);
                Text resourceText = new Text(resourceName + ": " + count);
                resourceText.setFont(Font.font("Arial", infoFontSize));
                playerBox.getChildren().add(resourceText);
            }

            Text pointsText = new Text("Victory points: " + player.getPlayerScore());
            pointsText.setFont(Font.font("Arial", infoFontSize));
            playerBox.getChildren().add(pointsText);

            playerListVBox.getChildren().add(playerBox);
        }

        ScrollPane scrollPane = new ScrollPane(playerListVBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(500); // or adjust as needed
        scrollPane.setStyle("-fx-background-color: #e0e0e0;");

        VBox container = new VBox(scrollPane);
        container.setStyle("-fx-background-color: #e0e0e0; -fx-min-width: 200;");
        return container;
    }

    // Roll the dice and distribute resources
    public static void rollDiceAndDistribute(Gameplay gameplay, ImageView dice1, ImageView dice2, BorderPane root, Group boardGroup, Board board) {
        int result = gameplay.rollDiceAndDistributeResources();
        int die1 = gameplay.getLastRolledDie1(); // You’ll need to expose this from Gameplay
        int die2 = gameplay.getLastRolledDie2(); // Same here

        // Load and display images
        dice1.setImage(loadDiceImage(die1));
        dice2.setImage(loadDiceImage(die2));
        dice1.setFitWidth(40);
        dice2.setFitWidth(40);
        dice1.setFitHeight(40);
        dice2.setFitHeight(40);

        if (result == 7) {
            nextTurnButton.setVisible(false);
            Robber.showRobberTargets(boardGroup);
        } else {
            nextTurnButton.setVisible(true);
        }
        root.setLeft(createLeftMenu(gameplay));
        rollDiceButton.setVisible(false);
    }

    // Centralize the Board, called via centerButton or keyPressed R/C
    private static void centerBoard(Board board, Group boardGroup, double screenWidth, double screenHeight) {
        Tile centerTile = Board.getTiles().get((Board.getTiles().size() - 1) / 2);
        Point2D centerPoint = centerTile.getCenter();
        double centerX = (screenWidth - 200) / 2 - centerPoint.getX();
        double centerY = (screenHeight - 70) / 2 - centerPoint.getY();
        boardGroup.setTranslateX(centerX);
        boardGroup.setTranslateY(centerY);
        boardGroup.setScaleX(1.0);
        boardGroup.setScaleY(1.0);
    }

    //___________________________SHOW FUNCTIONS___________________________________
    private static Image loadDiceImage(int number) {
        String path = "/dice/dice" + number + ".png";
        InputStream stream = CatanBoardGameView.class.getResourceAsStream(path);
        if (stream == null) {
            System.err.println(" Could not load image: " + path);
            return new Image("/Icons/error.png");
        }
        return new Image(stream);
    }

    private static void showBuildingCostsPopup() {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Building Costs");

        InputStream imageStream = CatanBoardGameView.class.getResourceAsStream("/UI/catanBuildingCosts.png");
        if (imageStream == null) {
            System.err.println("Could not load building_costs.png");
            return;
        }
        ImageView imageView = new ImageView(new Image(imageStream));
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(400);

        VBox layout = new VBox(imageView);
        layout.setPadding(new Insets(10));

        Scene scene = new Scene(layout);
        popup.setScene(scene);
        popup.showAndWait();
    }

    // Make Dice button visible
    public static void showDiceButton() {
        rollDiceButton.setVisible(true);
    }
}