package org.example.catanboardgameviews;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
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
    public static Group boardGroup;
    private static Stage primaryStage;
    private static final TextArea gameLogArea = new TextArea();

    
    // __________________________KINDA CONSTRUCTOR - Creating the Game Scene_________________________
    public static Scene createGameScene(Stage primaryStage, int radius, Gameplay gameplay) {
        ImageView diceImage1 = new ImageView();
        ImageView diceImage2 = new ImageView();
        setPrimaryStage(primaryStage);
        double sceneWidth = 800;
        double sceneHeight = 600;
        boardRadius = radius;

        // Initialize Board (clear old one first...)
        Board.clearBoard(); //  Clear old static tile/edge data
        Board board = new Board(radius, sceneWidth, sceneHeight);
        gameplay.setBoard(board); // Link board to gameplay logic

        // Draw hex tiles
        boardGroup = Tile.createBoardTiles(board, radius);

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
        DrawOrDisplay.initVerticeClickHandlers(board, boardGroup, buildController, radius, root);

        // Initialize the robber
        robberDeNiro = new Robber(desertTile, gameplay, boardGroup);

        Pane boardWrapper = new Pane(boardGroup);
        root.setCenter(boardWrapper);
        centerBoard(boardGroup, sceneWidth, sceneHeight);

        //________________________________BUTTONS_______________________________________________
        // Initializing top screen Buttons
        rollDiceButton = new Button("Roll Dice");
        nextTurnButton = new Button("Next Turn");
        Button centerButton = new Button("Center");
        Button zoomInButton = new Button("+");
        Button zoomOutButton = new Button("-");
        Button showCostsButton = new Button("Show Recipes");
        Button tradeButton = new Button("Trade with Bank 4:1");
        Button exitButton = new Button("Exit");

        // Button Actions
        // Dice and next turn
        rollDiceButton.setOnAction(e -> {
            gameplay.rollDiceAndDistribute(gameplay, diceImage1, diceImage2, root, boardGroup, board);
            gameplay.setHasRolledThisTurn(true);
            rollDiceButton.setDisable(true);
        });

        TurnController turnController = new TurnController(gameplay, rollDiceButton, nextTurnButton, root);
        nextTurnButton.setOnAction(turnController::handleNextTurnButtonPressed);

        // Centralize and zoom buttons
        centerButton.setOnAction(e -> {centerBoard(boardGroup, sceneWidth, sceneHeight);});
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

                returnToMainMenu();
            }
        });

        // TOP BAR - Buttons etc
        HBox buttonBox = new HBox(12, rollDiceButton, nextTurnButton, centerButton, zoomInButton, zoomOutButton, tradeButton, showCostsButton, exitButton);
        buttonBox.setPadding(new Insets(10));
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.setStyle("-fx-background-color: linear-gradient(to bottom, #ececec, #d4d4d4); -fx-border-color: #aaa; -fx-border-width: 0 0 1 0;");
        String fancyButtonStyle = """
            -fx-background-color: linear-gradient(to bottom, #f9f9f9, #e0e0e0);
            -fx-background-radius: 8;
            -fx-border-radius: 8;
            -fx-border-color: #b0b0b0;
            -fx-font-weight: bold;
            -fx-cursor: hand;
            -fx-padding: 8 14 8 14;
        """;
        String hoverStyle = "-fx-background-color: linear-gradient(to bottom, #e6e6e6, #cccccc);";
        for (Button btn : List.of(rollDiceButton, nextTurnButton, centerButton, zoomInButton, zoomOutButton, tradeButton, showCostsButton, exitButton)) {
            btn.setStyle(fancyButtonStyle);
            btn.setOnMouseEntered(e -> btn.setStyle(fancyButtonStyle + hoverStyle));
            btn.setOnMouseExited(e -> btn.setStyle(fancyButtonStyle));
        }

        // Dont show roll dice or next turn button until initial placements and game starting
        rollDiceButton.setVisible(false);
        nextTurnButton.setVisible(false);
        root.setTop(buttonBox);

        // Dice title + images
        Label diceTitle = new Label("Dice Roll");
        diceTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        diceTitle.setPadding(new Insets(0, 0, 5, 5));

        diceImage1.setFitWidth(40);
        diceImage2.setFitWidth(40);
        diceImage1.setFitHeight(40);
        diceImage2.setFitHeight(40);
        HBox diceImages = new HBox(5, diceImage1, diceImage2);
        diceImages.setAlignment(Pos.CENTER_LEFT);
        diceImages.setPadding(new Insets(0, 0, 0, 5));

        VBox diceColumn = new VBox(diceTitle, diceImages);
        diceColumn.setAlignment(Pos.TOP_LEFT);
        diceColumn.setPadding(new Insets(5));
        diceColumn.setSpacing(5);

        // Game log title + scroll
        Label logTitle = new Label("Game Log");
        logTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        logTitle.setPadding(new Insets(0, 0, 5, 0));

        gameLogArea.setEditable(false);
        gameLogArea.setWrapText(true);
        gameLogArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12;");
        ScrollPane scrollPane = new ScrollPane(gameLogArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPrefHeight(100);

        VBox logColumn = new VBox(logTitle, scrollPane);
        logColumn.setSpacing(5);
        logColumn.setPadding(new Insets(5, 10, 5, 0));
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Final horizontal layout: Dice | Log
        HBox bottomContent = new HBox(diceColumn, logColumn);
        bottomContent.setSpacing(10);
        bottomContent.setPadding(new Insets(5));
        HBox.setHgrow(logColumn, Priority.ALWAYS);

        // Wrap in a vertical container for use in SplitPane
        VBox logBox = new VBox(bottomContent);
        VBox.setVgrow(bottomContent, Priority.ALWAYS);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        logBox.setMinHeight(80);

        // Combine board + bottom section with resizable divider
        VBox boardOnly = new VBox(boardWrapper);
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.getItems().addAll(boardOnly, logBox);
        splitPane.setDividerPositions(0.85); // adjust if needed
        root.setCenter(splitPane);

// ✅ Ensure log area grows with window resizing
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        VBox.setVgrow(logColumn, Priority.ALWAYS);
        VBox.setVgrow(bottomContent, Priority.ALWAYS);
        VBox.setVgrow(logBox, Priority.ALWAYS);
        SplitPane.setResizableWithParent(logBox, true);



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
                case R, C -> {centerBoard(boardGroup, sceneWidth, sceneHeight);
                }
                case ESCAPE -> {
                    Alert alert = new Alert(AlertType.CONFIRMATION, "Are you sure you want to exit to the main menu?", ButtonType.YES, ButtonType.NO);
                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.YES) {
                        returnToMainMenu();
                    }
                }
            }
        });

        return scene;
    }

    //__________________________ACTION FUNCTIONS_____________________________________

    // Clear STATIC variables/cache etc.
    public static void resetGameUIState() {
        if (boardGroup != null) {
            boardGroup.getChildren().clear();
            boardGroup = null;
        }
        gameLogArea.clear();
        currentRobberCircle = null;
        robberDeNiro = null;
        boardRadius = 0;
        Board.clearBoard();
    }

    // Navigate back to the main menu (AND RESET EVERYTHING)
    public static void returnToMainMenu() {
        resetGameUIState();
        // Clear persistent UI state
        if (boardGroup != null) {
            boardGroup.getChildren().clear();
        }
        gameLogArea.clear();
        currentRobberCircle = null;
        robberDeNiro = null;
        boardGroup = null;
        boardRadius = 0;
        // Now safely return to menu
        if (primaryStage != null) {
            MenuView.showMainMenu(primaryStage);
        } else {
            System.err.println("Error: primaryStage is null.");
        }
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

            // Add name correct for players AND AI players
            String displayName;
            if (player instanceof AIOpponent ai) {
                displayName = "AIPlayer " + player.getPlayerId() + " (" + ai.getStrategyLevel().name() + ")";
            } else {
                displayName = "Player " + player.getPlayerId();
            }
            Text playerName = new Text(displayName);

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

    // Centralize the Board, called via centerButton or keyPressed R/C
    private static void centerBoard(Group boardGroup, double screenWidth, double screenHeight) {
        Tile centerTile = Board.getTiles().get((Board.getTiles().size() - 1) / 2);
        Point2D centerPoint = centerTile.getCenter();
        double centerX = (screenWidth - 200) / 2 - centerPoint.getX();
        double centerY = (screenHeight - 130) / 2 - centerPoint.getY();
        boardGroup.setTranslateX(centerX);
        boardGroup.setTranslateY(centerY);
        boardGroup.setScaleX(1.0);
        boardGroup.setScaleY(1.0);
    }

    //___________________________SHOW FUNCTIONS___________________________________

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
    // Make Dice button visible
    public static void showTradeButton() {
        rollDiceButton.setVisible(true);
    }

    public static Group getBoardGroup() {
        return boardGroup;
    }
    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void logToGameLog(String message) {
        Platform.runLater(() -> {
            gameLogArea.appendText(message + "\n");
            gameLogArea.setScrollTop(Double.MAX_VALUE); // auto-scroll down
        });
    }
}