package org.example.catanboardgameviews;

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
import org.example.catanboardgameapp.*;
import org.example.controller.BuildController;
import org.example.controller.GameController;
import org.example.controller.TradeController;
import org.example.controller.TurnController;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public class CatanBoardGameView {

    //---------------------------- Dimensions ----------------------------//
    private final double GAME_WIDTH = 850;
    private final double GAME_HEIGHT = 600;
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

        setupInputHandlers(boardWrapper);
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

        scrollPane = new ScrollPane(gameLogArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox logColumn = new VBox(logLabel, scrollPane);
        logColumn.setPadding(new Insets(5, 10, 5, 0));
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

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
        Button tradeButton = new Button("Trade with Bank 4:1");
        Button exitButton = new Button("Exit");
        ToggleButton toggleConfirmBtn = new ToggleButton("Confirm ON");
        toggleConfirmBtn.setSelected(true);

        toggleConfirmBtn.setOnAction(e -> {
            boolean enabled = toggleConfirmBtn.isSelected();
            toggleConfirmBtn.setText(enabled ? "Confirm ON" : "Confirm OFF");
            gameController.getBuildController().toggleConfirmBeforeBuild();
        });

        rollDiceButton.setOnAction(e -> {
            gameplay.rollDiceAndDistribute(gameplay, diceImage1, diceImage2, root, boardGroup, board);
            gameplay.setHasRolledThisTurn(true);
            rollDiceButton.setDisable(true);
            nextTurnButton.setVisible(true);
        });

        TurnController turnController = new TurnController(gameController, rollDiceButton, nextTurnButton);
        nextTurnButton.setOnAction(turnController::handleNextTurnButtonPressed);

        centerButton.setOnAction(e -> centerBoard(boardGroup, GAME_WIDTH, GAME_HEIGHT));
        zoomInButton.setOnAction(e -> zoom(boardGroup, 1.1));
        zoomOutButton.setOnAction(e -> zoom(boardGroup, 0.9));

        new TradeController(gameController).setupTradeButton(tradeButton);
        showCostsButton.setOnAction(e -> showBuildingCostsPopup());
        exitButton.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to exit to the main menu?", ButtonType.YES, ButtonType.NO);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.YES) {
                gameController.returnToMenu(gameplay.getMenuView());
            }
        });

        List<ButtonBase> allButtons = List.of(
                rollDiceButton, nextTurnButton, centerButton, zoomInButton, zoomOutButton,
                tradeButton, showCostsButton, toggleConfirmBtn, exitButton
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

    public VBox createLeftMenu(Boolean hasBeenInitialized) {
        if (hasBeenInitialized && playerListVBox != null) {
            playerListVBox.getChildren().clear();

            Text title = new Text("Player Stats");
            title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            playerListVBox.getChildren().add(title);

            double nameFontSize = 14;
            double infoFontSize = 12;

            for (Player player : gameplay.getPlayerList()) {
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

                int totalResources = 0;
                for (String resourceName : player.getResources().keySet()) {
                    int count = player.getResources().get(resourceName);
                    totalResources += count;
                    Text resourceText = new Text(resourceName + ": " + count);
                    resourceText.setFont(Font.font("Arial", infoFontSize));
                    playerBox.getChildren().add(resourceText);
                }

                Text totalText = new Text("Total resources: " + totalResources);
                totalText.setFont(Font.font("Arial", infoFontSize));
                playerBox.getChildren().add(totalText);

                Text pointsText = new Text("Victory points: " + player.getPlayerScore());
                pointsText.setFont(Font.font("Arial", infoFontSize));
                playerBox.getChildren().add(pointsText);

                playerListVBox.getChildren().add(playerBox);
            }

            return null;
        }

        // First-time creation path
        playerListVBox = new VBox(10);
        playerListVBox.setPadding(new Insets(10));

        double nameFontSize = 14;
        double infoFontSize = 12;

        Text title = new Text("Player Stats");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        playerListVBox.getChildren().add(title);

        for (Player player : gameplay.getPlayerList()) {
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

            int totalResources = 0;
            for (String resourceName : player.getResources().keySet()) {
                int count = player.getResources().get(resourceName);
                totalResources += count;
                Text resourceText = new Text(resourceName + ": " + count);
                resourceText.setFont(Font.font("Arial", infoFontSize));
                playerBox.getChildren().add(resourceText);
            }

            Text totalText = new Text("Total resources: " + totalResources);
            totalText.setFont(Font.font("Arial", infoFontSize));
            playerBox.getChildren().add(totalText);

            Text pointsText = new Text("Victory points: " + player.getPlayerScore());
            pointsText.setFont(Font.font("Arial", infoFontSize));
            playerBox.getChildren().add(pointsText);

            playerListVBox.getChildren().add(playerBox);
        }

        ScrollPane scrollPane = new ScrollPane(playerListVBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(500);
        scrollPane.setStyle("-fx-background-color: #e0e0e0;");

        VBox container = new VBox(scrollPane);
        container.setStyle("-fx-background-color: #e0e0e0; -fx-min-width: 200;");
        return container;
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
                case ESCAPE -> {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Exit to main menu?", ButtonType.YES, ButtonType.NO);
                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.YES) {
                        gameController.returnToMenu(gameplay.getMenuView());
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

    private void centerBoard(Group boardGroup, double screenWidth, double screenHeight) {
        Tile centerTile = board.getTiles().get((board.getTiles().size() - 1) / 2);
        Point2D centerPoint = centerTile.getCenter();
        double centerX = (screenWidth - 200) / 2 - centerPoint.getX();
        double centerY = (screenHeight - 130) / 2 - centerPoint.getY();
        boardGroup.setTranslateX(centerX);
        boardGroup.setTranslateY(centerY);
        boardGroup.setScaleX(1.0);
        boardGroup.setScaleY(1.0);
        scrollPane.setPrefHeight(120);
        if (splitPane != null) splitPane.setDividerPositions(0.85);
    }

    private void showBuildingCostsPopup() {
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

    //__________________________VIEW UPDATES_____________________________//
    public void resetGameUIState() {
        boardGroup.getChildren().clear();
        gameLogArea.clear();
        board.clearBoard();
    }

    public void refreshSidebar() {
        createLeftMenu(true);
    }

    public void logToGameLog(String message) {
        Platform.runLater(() -> {
            gameLogArea.appendText(message + "\n");
            gameLogArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    public void showDiceButton() {
        rollDiceButton.setVisible(true);
    }

    // hides the starting circles for settlement placements.
    public void hideAllVertexClickCircles() {
        for (Circle circle : drawOrDisplay.getRegisteredVertexClickable()) {
            circle.setOpacity(0); // makes it invisible
            //circle.setOpacity(1); // makes it vivible again if needed

        }
    }

    //__________________________GETTERS_____________________________//
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