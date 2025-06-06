package org.example.catanboardgameviews;

import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.catanboardgameapp.*;
import org.example.controller.BuildController;
import org.example.controller.GameController;
import org.example.controller.TradeController;
import org.example.controller.TurnController;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CatanBoardGameView {

    //---------------------------- Dimensions ----------------------------//
    private final double GAME_WIDTH = 1050;
    private final double GAME_HEIGHT = 700;
    private final int boardRadius;

    //---------------------------- Game Components ----------------------------//
    private final DrawOrDisplay drawOrDisplay;
    private final Board board;
    private final Robber robber;
    private final Stage primaryStage;
    private final Gameplay gameplay;
    private final GameController gameController;

    //---------------------------- JavaFX Root Scene ----------------------------//
    private final Scene scene;
    private final BorderPane root;

    //---------------------------- Render Layers ----------------------------//
    private final Group edgeBaseLayer;
    private final Group roadLayer;
    private final Group settlementLayer;
    private final Group edgeClickLayer;
    private final Group boardGroup;

    //---------------------------- UI Controls ----------------------------//
    private final Button rollDiceButton;
    private final Button nextTurnButton;
    private final TextArea gameLogArea;

    //---------------------------- Dice Display ----------------------------//
    private final ImageView diceImage1;
    private final ImageView diceImage2;

    //---------------------------- Layout Containers ----------------------------//
    private ScrollPane scrollPane;
    private SplitPane splitPane;
    private VBox playerListVBox;

    //________________________________CONSTRUCTOR____________________________________//
    public CatanBoardGameView(Stage primaryStage, Gameplay gameplay, GameController gameController, int boardRadius) {
        this.primaryStage = primaryStage;
        this.gameplay = gameplay;
        this.gameController = gameController;
        this.boardRadius = boardRadius;

        // Layout root
        this.root = new BorderPane();

        // Initialize individual render layers
        this.edgeBaseLayer = new Group();
        this.roadLayer = new Group();
        this.settlementLayer = new Group();
        this.edgeClickLayer = new Group();

        // Combine layers into one render-able board group
        this.boardGroup = new Group(edgeBaseLayer, roadLayer, settlementLayer, edgeClickLayer);

        // Main game control buttons
        this.rollDiceButton = new Button("Roll Dice");
        this.nextTurnButton = new Button("Next Turn");


        // Game log text area setup
        this.gameLogArea = new TextArea();
        gameLogArea.setEditable(false);
        gameLogArea.setWrapText(true);
        gameLogArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12;");

        // Drawing utility for rendering tiles, dice, etc.
        this.drawOrDisplay = new DrawOrDisplay(boardRadius);

        // Dice visuals (default to 1)
        this.diceImage1 = new ImageView(drawOrDisplay.loadDiceImage(1));
        this.diceImage2 = new ImageView(drawOrDisplay.loadDiceImage(1));

        // Create and assign game board
        this.board = new Board(boardRadius, GAME_WIDTH, GAME_HEIGHT);
        this.gameplay.setBoard(this.board);

        // Build main scene
        this.scene = new Scene(root, GAME_WIDTH, GAME_HEIGHT);

        // Find desert tile and place Robber on it
        Tile desertTile = board.getTiles().stream()
                .filter(t -> t.getTileDiceNumber() == 7)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No desert tile found"));
        this.robber = new Robber(desertTile, gameplay, this, boardGroup);
    }


    //__________________________UI SETUP METHODS_____________________________//

    public void buildGameUI() {
        Group tiles = board.createBoardTiles(board, boardRadius);
        boardGroup.getChildren().add(0, tiles);

        BuildController buildController = new BuildController(gameController);
        gameController.setBuildController(buildController);
        drawOrDisplay.initEdgesClickHandlers(board, boardGroup, buildController, boardRadius, root);
        drawOrDisplay.initVerticeClickHandlers(board, boardGroup, buildController, boardRadius, root);

        VBox gameLogPanel = createGameLogPanel();
        Pane boardWrapper = new Pane(boardGroup);
        VBox boardOnly = new VBox(boardWrapper);
        VBox.setVgrow(boardWrapper, Priority.ALWAYS);
        centerBoard(boardGroup, GAME_WIDTH, GAME_HEIGHT);
        splitPane = new SplitPane(boardOnly, gameLogPanel);
        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.setDividerPositions(0.85);
        root.setCenter(splitPane);
        root.setTop(createTopButtonBar());
        root.setLeft(createLeftMenu(false));

        // Setup KEY buttons clickable ESC, SPACE, ASDW, R for reset board etc
        setupInputHandlers(boardWrapper);
        // Ensure focus so key events work
        root.setFocusTraversable(true);
        root.requestFocus();
    }

    private VBox createGameLogPanel() {
        diceImage1.setFitWidth(40);
        diceImage1.setFitHeight(40);
        diceImage2.setFitWidth(40);
        diceImage2.setFitHeight(40);

        HBox diceImages = new HBox(5, diceImage1, diceImage2);
        diceImages.setAlignment(Pos.CENTER_LEFT);
        diceImages.setPadding(new Insets(0, 0, 0, 5));

        VBox diceColumn = new VBox(new Label("Dice Roll"), diceImages);
        diceColumn.setAlignment(Pos.TOP_LEFT);
        diceColumn.setPadding(new Insets(5));

        Label logLabel = new Label("Game Log");

        scrollPane = null;
        VBox logColumn = new VBox(logLabel, gameLogArea);
        logColumn.setPadding(new Insets(5, 10, 5, 0));
        VBox.setVgrow(gameLogArea, Priority.ALWAYS);

        HBox bottomContent = new HBox(diceColumn, logColumn);
        HBox.setHgrow(logColumn, Priority.ALWAYS);

        VBox logBox = new VBox(bottomContent);
        VBox.setVgrow(logBox, Priority.ALWAYS);
        return logBox;
    }

    private HBox createTopButtonBar() {
        Button centerButton = new Button("Center");
        Button zoomInButton = new Button("+");
        Button zoomOutButton = new Button("-");
        Button showCostsButton = new Button("Show Recipes");
        Button developmentCardButton = new Button("Buy Development Card");
        Button tradeButton = new Button("Trade with Bank");
        Button exitButton = new Button("Exit");
        ToggleButton toggleConfirmBtn = new ToggleButton("Confirm: OFF");
        toggleConfirmBtn.setSelected(false);
        toggleConfirmBtn.setOnAction(e -> {
            boolean enabled = toggleConfirmBtn.isSelected();
            toggleConfirmBtn.setText(enabled ? "Confirm: ON" : "Confirm: OFF");
            gameController.getBuildController().toggleConfirmBeforeBuild();
        });
        rollDiceButton.setOnAction(e -> {
            gameplay.rollDice();
        });
        developmentCardButton.setOnAction(e-> {
            if (!gameplay.hasRolledDice()) {
                drawOrDisplay.rollDiceBeforeActionPopup("You must roll the dice before buying Development Cards!");
                return;
            }
            gameplay.buyDevelopmentCard();
        });

        TurnController turnController = new TurnController(gameController, rollDiceButton, nextTurnButton);
        nextTurnButton.setOnAction(turnController::handleNextTurnButtonPressed);

        centerButton.setOnAction(e -> centerBoard(boardGroup, GAME_WIDTH, GAME_HEIGHT));
        zoomInButton.setOnAction(e -> zoom(boardGroup, 1.1));
        zoomOutButton.setOnAction(e -> zoom(boardGroup, 0.9));

        new TradeController(gameController, boardRadius).setupTradeButton(tradeButton);
        showCostsButton.setOnAction(e -> drawOrDisplay.showBuildingCostsPopup());
        exitButton.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to exit to the main menu?", ButtonType.YES, ButtonType.NO);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.YES) {
                gameController.returnToMenu(gameplay.getMenuView());
            }
        });

        List<ButtonBase> allButtons = List.of(
                rollDiceButton, nextTurnButton, centerButton, zoomInButton, zoomOutButton,
                tradeButton,developmentCardButton, showCostsButton, toggleConfirmBtn, exitButton
        );

        String style = "-fx-background-color: linear-gradient(to bottom, #f9f9f9, #e0e0e0); -fx-background-radius: 8;" +
                "-fx-border-radius: 8; -fx-border-color: #b0b0b0; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 14 8 14;";
        String hover = "-fx-background-color: linear-gradient(to bottom, #e6e6e6, #cccccc);";

        allButtons.forEach(btn -> {
            btn.setStyle(style);
            btn.setOnMouseEntered(e -> btn.setStyle(style + hover));
            btn.setOnMouseExited(e -> btn.setStyle(style));
        });

        rollDiceButton.setVisible(false);
        nextTurnButton.setVisible(false);

        HBox buttonBox = new HBox(12);
        buttonBox.getChildren().addAll(allButtons);
        buttonBox.setPadding(new Insets(10));
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.setStyle("-fx-background-color: linear-gradient(to bottom, #ececec, #d4d4d4); -fx-border-color: #aaa; -fx-border-width: 0 0 1 0;");
        return buttonBox;
    }

    private VBox createPlayerBox(Player player, double nameFontSize, double infoFontSize) {
        VBox playerBox = new VBox(5);

        String displayName = (player instanceof AIOpponent ai)
                ? "AIPlayer " + player.getPlayerId() + " (" + ai.getStrategyLevel().name() + ")"
                : "Player " + player.getPlayerId();

        Text playerName = new Text(displayName);
        playerName.setFont(Font.font("Arial", FontWeight.BOLD, nameFontSize));
        playerName.setFill(player.getColor());

        if (player == gameplay.getCurrentPlayer()) {
            playerBox.setStyle("-fx-background-color: lightyellow; -fx-border-color: black; -fx-border-width: 2px;");
            playerName.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, nameFontSize + 2));
        }

        playerBox.getChildren().add(playerName);

        if (player == gameplay.getCurrentPlayer()) {
            // Expandable resource section
            int totalResources = player.getResources().values().stream().mapToInt(Integer::intValue).sum();
            Button resourceButton = new Button("Resources: " + totalResources);
            resourceButton.setFont(Font.font("Arial", infoFontSize));

            VBox resourceDetailsBox = new VBox(3);
            resourceDetailsBox.setPadding(new Insets(5, 0, 0, 10));
            resourceDetailsBox.setVisible(false);
            resourceDetailsBox.setManaged(false);

            for (Map.Entry<String, Integer> entry : player.getResources().entrySet()) {
                Text resourceText = new Text(entry.getKey() + ": " + entry.getValue());
                resourceText.setFont(Font.font("Arial", infoFontSize));
                resourceDetailsBox.getChildren().add(resourceText);
            }

            resourceButton.setOnAction(e -> {
                boolean showing = resourceDetailsBox.isVisible();
                resourceDetailsBox.setVisible(!showing);
                resourceDetailsBox.setManaged(!showing);
            });

            playerBox.getChildren().addAll(resourceButton, resourceDetailsBox);

            // Expandable dev cards
            int totalDevCards = player.getDevelopmentCards().values().stream().mapToInt(Integer::intValue).sum();
            Button devCardButton = new Button("Development Cards: " + totalDevCards);
            devCardButton.setFont(Font.font("Arial", infoFontSize));

            VBox devCardDetailsBox = new VBox(3);
            devCardDetailsBox.setPadding(new Insets(5, 0, 0, 10));
            devCardDetailsBox.setVisible(false);
            devCardDetailsBox.setManaged(false);

            for (Map.Entry<String, Integer> entry : player.getDevelopmentCards().entrySet()) {
                if (entry.getValue() > 0) {
                    Button cardButton = new Button(entry.getKey() + " (" + entry.getValue() + ")");
                    cardButton.setFont(Font.font("Arial", infoFontSize));
                    cardButton.setOnAction(e -> gameplay.playDevelopmentCard(player, entry.getKey()));
                    devCardDetailsBox.getChildren().add(cardButton);
                }
            }

            devCardButton.setOnAction(e -> {
                boolean showing = devCardDetailsBox.isVisible();
                devCardDetailsBox.setVisible(!showing);
                devCardDetailsBox.setManaged(!showing);
            });

            playerBox.getChildren().addAll(devCardButton, devCardDetailsBox);
        } else {
            int totalResources = player.getResources().values().stream().mapToInt(Integer::intValue).sum();
            Text resourceTotal = new Text("Resources: " + totalResources);
            resourceTotal.setFont(Font.font("Arial", infoFontSize));
            playerBox.getChildren().add(resourceTotal);

            int totalDevCards = player.getDevelopmentCards().values().stream().mapToInt(Integer::intValue).sum();
            Text devCardTotal = new Text("Development Cards: " + totalDevCards);
            devCardTotal.setFont(Font.font("Arial", infoFontSize));
            playerBox.getChildren().add(devCardTotal);
        }

        Text pointsText = new Text("Victory points: " + player.getPlayerScore());
        pointsText.setFont(Font.font("Arial", infoFontSize));
        playerBox.getChildren().add(pointsText);

        return playerBox;
    }
    public VBox createLeftMenu(Boolean hasBeenInitialized) {
        if (playerListVBox == null) {
            playerListVBox = new VBox(10);
            playerListVBox.setPadding(new Insets(10));
        } else {
            playerListVBox.getChildren().clear();
        }

        double nameFontSize = 14;
        double infoFontSize = 12;

        Text title = new Text("Player Stats");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        playerListVBox.getChildren().add(title);

        for (Player player : gameplay.getPlayerList()) {
            VBox playerBox = createPlayerBox(player, nameFontSize, infoFontSize);
            playerListVBox.getChildren().add(playerBox);
        }

        if (!hasBeenInitialized) {
            ScrollPane scrollPane = new ScrollPane(playerListVBox);
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefHeight(500);
            scrollPane.setStyle("-fx-background-color: #e0e0e0;");

            VBox container = new VBox(scrollPane);
            container.setStyle("-fx-background-color: #e0e0e0; -fx-min-width: 200;");
            return container;
        }

        return null;
    }


    //__________________________INPUT HANDLING_____________________________//

    private void setupInputHandlers(Pane boardWrapper) {
        scene.setOnKeyPressed(event -> {
            double step = 30;
            switch (event.getCode()) {
                case W -> boardGroup.setTranslateY(boardGroup.getTranslateY() - step);
                case A -> boardGroup.setTranslateX(boardGroup.getTranslateX() - step);
                case S -> boardGroup.setTranslateY(boardGroup.getTranslateY() + step);
                case D -> boardGroup.setTranslateX(boardGroup.getTranslateX() + step);
                case R, C -> centerBoard(boardGroup, GAME_WIDTH, GAME_HEIGHT);
                case SPACE -> {
                    if (!gameplay.isGamePaused()) {
                        gameplay.pauseGame();
                        Alert pauseAlert = new Alert(Alert.AlertType.INFORMATION, "Game is paused. Press OK to resume.", ButtonType.OK);
                        pauseAlert.setTitle("Game Paused");
                        pauseAlert.setHeaderText(null);

                        pauseAlert.showAndWait(); // Wait for user to press OK
                        gameController.resumeGame(); // Always resume after OK
                    }
                }
                case ESCAPE -> {
                    gameplay.pauseGame();
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Exit to main menu?", ButtonType.YES, ButtonType.NO);
                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.YES) {
                        gameController.returnToMenu(gameplay.getMenuView());
                    } else if (result.isPresent() && result.get() == ButtonType.NO) {
                        gameController.resumeGame();  // Only resume if they cancel the exit
                    }
                }
            }
        });

        boardWrapper.setOnScroll(event -> {
            double zoomFactor = (event.getDeltaY() > 0) ? 1.05 : 0.95;
            zoom(boardGroup, zoomFactor);
            event.consume();
        });

        final double[] dragAnchorX = new double[1];
        final double[] dragAnchorY = new double[1];
        final double[] initialTranslateX = new double[1];
        final double[] initialTranslateY = new double[1];

        boardWrapper.setOnMousePressed(event -> {
            dragAnchorX[0] = event.getSceneX();
            dragAnchorY[0] = event.getSceneY();
            initialTranslateX[0] = boardGroup.getTranslateX();
            initialTranslateY[0] = boardGroup.getTranslateY();
        });

        boardWrapper.setOnMouseDragged(event -> {
            double deltaX = event.getSceneX() - dragAnchorX[0];
            double deltaY = event.getSceneY() - dragAnchorY[0];
            boardGroup.setTranslateX(initialTranslateX[0] + deltaX);
            boardGroup.setTranslateY(initialTranslateY[0] + deltaY);
        });
    }

    //__________________________UTILITY METHODS_____________________________//

    private void zoom(Group group, double factor) {
        double newScale = group.getScaleX() * factor;
        newScale = Math.max(0.5, Math.min(newScale, 3.0));
        group.setScaleX(newScale);
        group.setScaleY(newScale);
    }

    public void centerBoard(Group boardGroup, double screenWidth, double screenHeight) {
        // Compute bounds of all tile centers
        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;

        for (Tile tile : board.getTiles()) {
            Point2D c = tile.getCenter();
            minX = Math.min(minX, c.getX());
            maxX = Math.max(maxX, c.getX());
            minY = Math.min(minY, c.getY());
            maxY = Math.max(maxY, c.getY());
        }

        double boardCenterX = (minX + maxX) / 2;
        double boardCenterY = (minY + maxY) / 2;

        // Adjusted screen center excluding VBox (200px) and some bottom area (150px)
        double usableWidth = screenWidth - 200;
        double usableHeight = screenHeight - 150;
        double screenCenterX = usableWidth / 2;
        double screenCenterY = usableHeight / 2;

        double targetTranslateX = screenCenterX - boardCenterX;
        double targetTranslateY = screenCenterY - boardCenterY;

        // Animate translation
        TranslateTransition move = new TranslateTransition(Duration.millis(500), boardGroup);
        move.setToX(targetTranslateX);
        move.setToY(targetTranslateY);
        // Animate scaling
        ScaleTransition zoom = new ScaleTransition(Duration.millis(500), boardGroup);
        zoom.setToX(0.75);
        zoom.setToY(0.75);
        SequentialTransition sequence = new SequentialTransition(zoom, move);
        sequence.play();
        // Optional UI adjustments
        if (splitPane != null) splitPane.setDividerPositions(0.85);
    }



    //__________________________VIEW UPDATES_____________________________//
    public void updateDiceImages(int die1, int die2) {
        diceImage1.setImage(drawOrDisplay.loadDiceImage(die1));
        diceImage2.setImage(drawOrDisplay.loadDiceImage(die2));
        diceImage1.setFitWidth(40);
        diceImage1.setFitHeight(40);
        diceImage2.setFitWidth(40);
        diceImage2.setFitHeight(40);
    }

    public void prepareForHumanInitialPlacement(Player currentPlayer) {
        logToGameLog("Player " + currentPlayer.getPlayerId() + ", place your initial settlement.");
        System.out.println("HIDING BUTTONS FOR HUMAN IN INITIAL PHASE");
        hideTurnButton();
        hideDiceButton();
    }

    public void refreshSidebar() {
        if (playerListVBox != null) {
            createLeftMenu(true); // this clears and rebuilds the VBox contents
            playerListVBox.requestLayout(); // ensure JavaFX re-renders it
        }
    }


    public void logToGameLog(String message) {
        System.out.println(message);
        Platform.runLater(() -> {
            gameLogArea.appendText(message + "\n");

            // Force scroll to bottom after layout using timeline delay
            javafx.animation.Timeline scrollTimeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(
                            javafx.util.Duration.millis(50),
                            ae -> {
                                gameLogArea.setScrollTop(Double.MAX_VALUE);
                                gameLogArea.positionCaret(gameLogArea.getLength());
                            }
                    )
            );
            scrollTimeline.play();
        });
    }


    //________________________SHOW/HIDE BUTTONS____________________________________//

    public void showDiceButton() {
        rollDiceButton.setVisible(true);
    }
    public void hideDiceButton() {
        rollDiceButton.setVisible(false);
    }
    public void showTurnButton() {
        nextTurnButton.setVisible(true);
    }
    public void hideTurnButton() {
        nextTurnButton.setVisible(false);
    }

    //__________________________GETTERS_____________________________//
    public ImageView getDiceImage1() {
        return diceImage1;
    }
    public ImageView getDiceImage2() {
        return diceImage2;
    }
    public Scene getScene() {
        return scene;
    }
    public Robber getRobber() {
        return robber;
    }
    public Group getBoardGroup() {
        return boardGroup;
    }
    public Button getRollDiceButton() {
        return rollDiceButton;
    }
    public Button getNextTurnButton() {
        return nextTurnButton;
    }
    public Group getEdgeBaseLayer() {
        return edgeBaseLayer;
    }
    public Group getRoadLayer() {
        return roadLayer;
    }
    public Group getSettlementLayer() {
        return settlementLayer;
    }
    public Group getEdgeClickLayer() {
        return edgeClickLayer;
    }
    public Stage getPrimaryStage() {
        return primaryStage;
    }
    public double getGAME_WIDTH() {
        return GAME_WIDTH;
    }
    public double getGAME_HEIGHT() {
        return GAME_HEIGHT;
    }

}