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
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.example.catanboardgameapp.*;
import org.example.controller.BuildController;
import org.example.controller.GameController;
import org.example.controller.TradeController;
import org.example.controller.TurnController;
import org.example.catanboardgameapp.DevelopmentCard.DevelopmentCardType;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class CatanBoardGameView {

    //---------------------------- Dimensions ----------------------------//
    private final int boardRadius;

    //---------------------------- Game Components ----------------------------//
    private final DrawOrDisplay drawOrDisplay;
    private final Board board;
    private final Robber robber;
    private final Gameplay gameplay;
    private final GameController gameController;
    private final TurnController turnController;
    private final TradeController tradeController;

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
    private SplitPane splitPane;
    private VBox playerListVBox;

    //________________________________CONSTRUCTOR____________________________________//
    public CatanBoardGameView(Gameplay gameplay, GameController gameController, int boardRadius) {
        this.gameplay = gameplay;
        this.gameController = gameController;
        this.boardRadius = boardRadius;
        this.turnController = gameController.getTurnController();
        this.tradeController = gameController.getTradeController();

        // Layout root
        this.root = new BorderPane();

        // Initialize individual render layers
        this.edgeBaseLayer = new Group();
        this.roadLayer = new Group();
        this.settlementLayer = new Group();
        this.edgeClickLayer = new Group();

        // Combine layers into one render-able board group
        this.boardGroup = new Group(edgeBaseLayer, roadLayer, settlementLayer, edgeClickLayer);

        // Buttons
        this.rollDiceButton = new Button("Roll Dice");
        this.nextTurnButton = new Button("Next Turn");

        // Game log
        this.gameLogArea = new TextArea();
        gameLogArea.setEditable(false);
        gameLogArea.setWrapText(true);

        gameLogArea.setFocusTraversable(false);
        gameLogArea.setPrefRowCount(8);
        gameLogArea.setStyle(BUTTON_STYLE);

        // Drawing/rendering utility
        this.drawOrDisplay = gameplay.getDrawOrDisplay();

        // Dice visuals
        this.diceImage1 = new ImageView(drawOrDisplay.loadDiceImage(1));
        this.diceImage2 = new ImageView(drawOrDisplay.loadDiceImage(1));

        // Create board and register it with gameplay
        this.board = new Board(gameplay, boardRadius, gameController.getMenuView().getGAME_WIDTH(), gameController.getMenuView().getGAME_HEIGHT());
        gameplay.setBoard(this.board);

        // Scene root container — initially just root
        this.scene = new Scene(root, gameController.getMenuView().getGAME_WIDTH(), gameController.getMenuView().getGAME_HEIGHT());

        // Place Robber
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
        drawOrDisplay.initEdgesClickHandlers(board, boardGroup, buildController, boardRadius);
        drawOrDisplay.initVerticeClickHandlers(board, boardGroup, buildController, boardRadius, root);

        VBox gameLogPanel = createGameLogPanel();

        Pane boardWrapper = createBoardWrapperWithBackground(boardGroup);
        VBox boardOnly = new VBox(boardWrapper);
        VBox.setVgrow(boardWrapper, Priority.ALWAYS);
        centerBoard(boardGroup, gameController.getMenuView().getGAME_WIDTH(), gameController.getMenuView().getGAME_HEIGHT());
        splitPane = new SplitPane(boardOnly, gameLogPanel);
        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.setDividerPositions(0.85);
        root.setCenter(splitPane);
        root.setTop(createTopButtonBar());
        root.setLeft(createLeftMenu(false));
        root.setStyle("-fx-background-color: #f9f0d2;");


        // Overlay for when AI is making a move (thinking)
        drawOrDisplay.buildAIOverlay();
        StackPane aiTurnOverlay = drawOrDisplay.buildAIOverlay();
        StackPane layeredRoot = new StackPane(root, aiTurnOverlay);
        scene.setRoot(layeredRoot);

        // Setup KEY buttons clickable ESC, SPACE, ASDW, R,C for reset board etc
        setupInputHandlers(boardWrapper);
        // Ensure focus so key events work
        root.setFocusTraversable(true);
        root.requestFocus();
    }

    private VBox createGameLogPanel() {
        // Set fixed size for dice images
        diceImage1.setFitWidth(40);
        diceImage1.setFitHeight(40);
        diceImage2.setFitWidth(40);
        diceImage2.setFitHeight(40);

        // Group both dice images in a horizontal box
        HBox diceImages = new HBox(5, diceImage1, diceImage2);
        diceImages.setAlignment(Pos.CENTER_LEFT);
        diceImages.setPadding(new Insets(0, 0, 0, 5));

        // Label for the dice section
        Label diceLabel = new Label("Dice Roll");
        diceLabel.setFont(Font.font("Georgia", FontWeight.BOLD, 14));

        // Vertical container for dice label and images
        VBox diceColumn = new VBox(diceLabel, diceImages);
        diceColumn.setAlignment(Pos.TOP_LEFT);
        diceColumn.setPadding(new Insets(5));

        // Label for the game log
        Label logLabel = new Label("Game Log");
        logLabel.setFont(Font.font("Georgia", FontWeight.BOLD, 14));

        // Vertical container for the game log label and text area
        VBox logColumn = new VBox(logLabel, gameLogArea);
        logColumn.setPadding(new Insets(5, 10, 5, 0));
        VBox.setVgrow(gameLogArea, Priority.ALWAYS);
        VBox.setVgrow(logColumn, Priority.ALWAYS);

        // Combine dice and log columns side by side
        HBox bottomContent = new HBox(diceColumn, logColumn);
        HBox.setHgrow(logColumn, Priority.ALWAYS);

        // Wrap everything in a parent VBox
        VBox logBox = new VBox(bottomContent);
        VBox.setVgrow(bottomContent, Priority.ALWAYS);
        VBox.setVgrow(logBox, Priority.ALWAYS);

        logBox.setStyle("""
            -fx-background-color: linear-gradient(to bottom, #f9f0d2, #e2c18f);
            -fx-border-color: #a86c1f;
            -fx-border-width: 2;
        """);

        return logBox;
    }

    // Creates a horizontal toolbar containing key game interaction buttons
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
            toggleConfirmBtn.getScene().getRoot().requestFocus();
        });

        // Dice rolling button action
        rollDiceButton.setOnAction(e -> {
            if (gameplay.isActionBlockedByDevelopmentCard()){
                drawOrDisplay.showFinishDevelopmentCardActionPopup();
                return;
            }
            gameplay.rollDice();
        });

        // Development card purchase
        developmentCardButton.setOnAction(e -> {
            if (gameplay.getDevelopmentCard().isPlayingCard()) {
                drawOrDisplay.showFinishDevelopmentCardActionPopup();
                return;
            }
            // Prevent buying a new development card while another one is being played
            if (gameplay.isBlockedByAITurn()) return;
            if (!gameplay.hasRolledDice()) {
                drawOrDisplay.rollDiceBeforeActionPopup("You must roll the dice before buying Development Cards!");
                return;
            }
            gameplay.buyDevelopmentCard();
            developmentCardButton.getScene().getRoot().requestFocus();
        });

        // End turn
        nextTurnButton.setOnAction(e -> {
            turnController.handleNextTurnButtonPressed();
            nextTurnButton.getScene().getRoot().requestFocus();
        });

        // Center the game board view
        centerButton.setOnAction(e -> {
            // Optional UI adjustments
            if (splitPane != null) splitPane.setDividerPositions(0.85);
            centerBoard(boardGroup, gameController.getMenuView().getGAME_WIDTH(), gameController.getMenuView().getGAME_HEIGHT());
            centerButton.getScene().getRoot().requestFocus();
        });

        // Zoom in/out actions
        zoomInButton.setOnAction(e -> {
            zoom(boardGroup, 1.1);
            zoomInButton.getScene().getRoot().requestFocus();
        });

        zoomOutButton.setOnAction(e -> {
            zoom(boardGroup, 0.9);
            zoomOutButton.getScene().getRoot().requestFocus();
        });

        tradeController.setupTradeButton(tradeButton);

        // Show resource cost popup
        showCostsButton.setOnAction(e -> {
            drawOrDisplay.showBuildingCostsPopup();
            showCostsButton.getScene().getRoot().requestFocus();
        });

        // Handle exit confirmation and return to main menu
        exitButton.setOnAction(e -> {
            gameplay.pauseGame();
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to exit to the main menu?", ButtonType.YES, ButtonType.NO);
            DialogPane pane = alert.getDialogPane();
            pane.setStyle(BUTTON_STYLE);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.YES) {
                gameController.returnToMenu(gameplay.getMenuView());
            } else {
                gameController.resumeGame();
            }
            exitButton.getScene().getRoot().requestFocus();
        });

        // Group all buttons, unified styling
        List<ButtonBase> allButtons = List.of(
                rollDiceButton, nextTurnButton, centerButton, zoomInButton, zoomOutButton,
                tradeButton, developmentCardButton, showCostsButton, toggleConfirmBtn, exitButton
        );

        String baseStyle = """
        -fx-background-color: linear-gradient(to bottom, #d8b173, #a86c1f);
        -fx-text-fill: #2b1d0e;
        -fx-background-radius: 12;
        -fx-border-radius: 12;
        -fx-border-color: #a86c1f;
        -fx-border-width: 2;
        -fx-padding: 6 14 6 14;
        -fx-font-family: 'Georgia', 'Serif';
        -fx-cursor: hand;
    """;

        String hoverStyle = "-fx-background-color: linear-gradient(to bottom, #f0c785, #c5852f);";

        allButtons.forEach(btn -> {
            btn.setStyle(baseStyle);
            btn.setOnMouseEntered(e -> btn.setStyle(baseStyle + hoverStyle));
            btn.setOnMouseExited(e -> btn.setStyle(baseStyle));
        });

        // Hide buttons initially
        hideTurnButton();
        hideDiceButton();

        // Assemble all buttons into the button bar
        HBox buttonBox = new HBox(12);
        buttonBox.getChildren().addAll(allButtons);
        buttonBox.setPadding(new Insets(10));
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        buttonBox.setStyle("""
            -fx-background-color: linear-gradient(to bottom, #f3e2c7, #d2a86e);
            -fx-border-color: #a86c1f;
            -fx-border-width: 0 0 3 0;
        """);

        return buttonBox;
    }

    private VBox createPlayerBox(Player player, double nameFontSize, double infoFontSize) {
        VBox playerBox = new VBox(5);
        playerBox.setPadding(new Insets(8));
        playerBox.setSpacing(4);

        // Base and conditional styling for player panels
        String baseStyle = """
            -fx-background-radius: 10;
            -fx-border-radius: 10;
        """;

        String playerStyle;
        String displayName = (player instanceof AIOpponent ai)
                ? "AIPlayer " + player.getPlayerId() + " (" + ai.getStrategyLevel().name() + ")"
                : "Player " + player.getPlayerId();

        Text playerName = new Text(displayName);
        playerName.setFont(Font.font("Georgia", FontWeight.BOLD, nameFontSize));
        playerName.setFill(player.getColor());


        if (player == gameplay.getCurrentPlayer()) {
            // Expandable: Resources
            playerStyle = """
            -fx-background-color: linear-gradient(to bottom, #fff6cc, #eedc9a);
            -fx-border-color: #d4a627, #000000;
            -fx-border-insets: 0, 2;
            -fx-border-width: 4, 2;
        """;
            playerName.setFont(Font.font("Georgia", FontWeight.EXTRA_BOLD, nameFontSize + 6));
            playerName.setText(displayName);
        } else {
            playerStyle = """
            -fx-background-color: linear-gradient(to bottom, #f3e2c7, #e0b97d);
            -fx-border-color: #a86c1f;
            -fx-border-width: 1.5;
        """;
        }

        playerBox.setStyle(baseStyle + playerStyle);
        playerBox.getChildren().add(playerName);

        if (player == gameplay.getCurrentPlayer()) {
            // Expandable: Development Cards
            int totalResources = player.getResources().values().stream().mapToInt(Integer::intValue).sum();
            Button resourceButton = new Button("Resources: " + totalResources);
            resourceButton.setFont(Font.font("Georgia", infoFontSize));
            resourceButton.setStyle("""
                                    -fx-background-color: linear-gradient(to bottom, #d8b173, #a86c1f);
                                    -fx-text-fill: #2b1d0e; """
            );

            VBox resourceDetailsBox = new VBox(3);
            resourceDetailsBox.setPadding(new Insets(5, 0, 0, 10));
            resourceDetailsBox.setVisible(false);
            resourceDetailsBox.setManaged(false);

            for (Map.Entry<String, Integer> entry : player.getResources().entrySet()) {
                Text resourceText = new Text(entry.getKey() + ": " + entry.getValue());
                resourceText.setFont(Font.font("Georgia", infoFontSize));
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
            devCardButton.setFont(Font.font("Georgia", infoFontSize));
            devCardButton.setStyle("""
                                    -fx-background-color: linear-gradient(to bottom, #d8b173, #a86c1f);
                                    -fx-text-fill: #2b1d0e;"""
                                    );


            VBox devCardDetailsBox = new VBox(3);
            devCardDetailsBox.setPadding(new Insets(5, 0, 0, 10));
            devCardDetailsBox.setVisible(false);
            devCardDetailsBox.setManaged(false);

            for (Map.Entry<DevelopmentCardType, Integer> entry : player.getDevelopmentCards().entrySet()) {
                if (entry.getValue() > 0) {
                    Button cardButton = styleButton(player, entry);
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
            // Compact view for other players
            int totalResources = player.getResources().values().stream().mapToInt(Integer::intValue).sum();
            Text resourceTotal = new Text("Resources: " + totalResources);
            resourceTotal.setFont(Font.font("Georgia", infoFontSize));
            playerBox.getChildren().add(resourceTotal);

            int totalDevCards = player.getDevelopmentCards().values().stream().mapToInt(Integer::intValue).sum();
            Text devCardTotal = new Text("Development Cards: " + totalDevCards);
            devCardTotal.setFont(Font.font("Georgia", infoFontSize));
            playerBox.getChildren().add(devCardTotal);
        }

        // Longest road indicator
        Text roadText = new Text("Longest road: " + player.getLongestRoad());
        roadText.setFont(Font.font("Georgia", infoFontSize));
        if (player == gameplay.getLongestRoadManager().getCurrentHolder()) {
            roadText.setText("🏅 LONGEST ROAD: " + player.getLongestRoad());
            roadText.setFont(Font.font("Georgia", FontWeight.BOLD, infoFontSize));
        }
        playerBox.getChildren().add(roadText);

        // Largest army indicator
        Text armyText = new Text("Knights Played: " + player.getPlayedKnights());
        armyText.setFont(Font.font("Georgia", infoFontSize));
        BiggestArmyManager biggestArmy = gameplay.getBiggestArmy();
        if (player == biggestArmy.getCurrentHolder()) {
            armyText.setText("🏅 BIGGEST ARMY: " + player.getPlayedKnights());
            armyText.setFont(Font.font("Georgia", FontWeight.BOLD, infoFontSize));
        }
        playerBox.getChildren().add(armyText);

        // Victory points
        Text pointsText = new Text("Victory points: " + player.getPlayerScore());
        pointsText.setFont(Font.font("Georgia", infoFontSize));
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

        title.setFont(Font.font("Georgia", FontWeight.BOLD, 16));
        title.setFill(Color.web("#5e3c10"));
        playerListVBox.getChildren().add(title);

       for (Player player : gameplay.getPlayerList()) {
            VBox playerBox = createPlayerBox(player, nameFontSize, infoFontSize);
            playerListVBox.getChildren().add(playerBox);
        }

        if (!hasBeenInitialized) {
            return scrollVBox();
        }

        return null;
    }

    private VBox scrollVBox() {
        ScrollPane scrollPane = new ScrollPane(playerListVBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(500);
        scrollPane.setStyle("""
        -fx-background: transparent;
        -fx-background-color: transparent;
        -fx-border-color: #a86c1f;
        -fx-border-radius: 10;
    """);

        VBox container = new VBox(scrollPane);
        container.setPadding(new Insets(8));
        container.setStyle("""
        -fx-background-color: linear-gradient(to bottom, #f9ecd1, #d2a86e);
        -fx-border-color: #8c5b1a;
        -fx-border-width: 2;
        -fx-border-radius: 0;
        -fx-background-radius: 0;
        -fx-min-width: 220;
    """);
        return container;
    }

    private static final String BUTTON_STYLE = """
        -fx-font-family: 'Georgia';
        -fx-background-color: #d8b173;
        -fx-background-radius: 6;
        -fx-border-radius: 6;
        -fx-border-color: #8c5b1a;
        -fx-border-width: 2;
        -fx-cursor: hand;
    """;

    private Button styleButton(Player player, Map.Entry<DevelopmentCardType, Integer> entry) {
        DevelopmentCardType type = entry.getKey();

        Button cardButton = new Button(type.getDisplayName() + " (" + entry.getValue() + ")");
        cardButton.setStyle(BUTTON_STYLE);


        cardButton.setOnAction(e -> {
            try {
                gameplay.playDevelopmentCard(player, type);
            } catch (IllegalArgumentException ex) {
                logToGameLog("Invalid card: " + type);
            }
        });
        return cardButton;
    }

    public void styleDialog(Dialog<?> dialog) {
        DialogPane pane = dialog.getDialogPane();
        pane.setStyle(BUTTON_STYLE);

        Platform.runLater(() -> {
            for (ButtonType bt : pane.getButtonTypes()) {
                Button btn = (Button) pane.lookupButton(bt);
                if (btn != null) {
                    btn.setStyle(BUTTON_STYLE);
                }
            }
        });
    }


    //__________________________INPUT HANDLING_____________________________//

    //boardWrapper The pane that wraps the board and listens for input events
    private void setupInputHandlers(Pane boardWrapper) {
        scene.setOnKeyPressed(event -> {
            double step = 30;
            switch (event.getCode()) {
                case W -> boardGroup.setTranslateY(boardGroup.getTranslateY() - step);
                case A -> boardGroup.setTranslateX(boardGroup.getTranslateX() - step);
                case S -> boardGroup.setTranslateY(boardGroup.getTranslateY() + step);
                case D -> boardGroup.setTranslateX(boardGroup.getTranslateX() + step);
                case R, C -> {
                    if (splitPane != null) splitPane.setDividerPositions(0.85); // snap gameLog back in place
                    centerBoard(boardGroup, gameController.getMenuView().getGAME_WIDTH(), gameController.getMenuView().getGAME_HEIGHT());
                }
                // Pause the game
                case SPACE -> {
                    gameplay.pauseGame();
                    Alert pauseAlert = new Alert(Alert.AlertType.INFORMATION, "Game is paused. Press OK to resume.", ButtonType.OK);
                    pauseAlert.setTitle("Game Paused");
                    pauseAlert.setHeaderText(null);
                    // wait for input
                    pauseAlert.showAndWait();
                    // only resume after dialog is confirmed
                    gameController.resumeGame();

                    // Reset focus to scene root
                    if (scene.getRoot() != null) {
                        scene.getRoot().requestFocus();
                    }
                }

                // Ask to confirm exit
                case ESCAPE -> {
                    gameplay.pauseGame();
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Exit to main menu?", ButtonType.YES, ButtonType.NO);
                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.YES) {
                        gameController.returnToMenu(gameplay.getMenuView());
                    } else {
                        // resume only if they cancel the exit
                        gameController.resumeGame();

                        // Reset focus to scene root
                        if (scene.getRoot() != null) {
                            scene.getRoot().requestFocus();
                        }
                    }
                }
            }
        });

        // Mouse scroll to zoom in/out
        boardWrapper.setOnScroll(event -> {
            double zoomFactor = (event.getDeltaY() > 0) ? 1.05 : 0.95;
            zoom(boardGroup, zoomFactor);
            event.consume();
        });

        // Variables to store initial mouse press and translation state
        final double[] dragAnchorX = new double[1];
        final double[] dragAnchorY = new double[1];
        final double[] initialTranslateX = new double[1];
        final double[] initialTranslateY = new double[1];

        // Mouse press: record starting point for drag
        boardWrapper.setOnMousePressed(event -> {
            dragAnchorX[0] = event.getSceneX();
            dragAnchorY[0] = event.getSceneY();
            initialTranslateX[0] = boardGroup.getTranslateX();
            initialTranslateY[0] = boardGroup.getTranslateY();
        });

        // Mouse drag: calculate delta and move board accordingly
        boardWrapper.setOnMouseDragged(event -> {
            double deltaX = event.getSceneX() - dragAnchorX[0];
            double deltaY = event.getSceneY() - dragAnchorY[0];
            boardGroup.setTranslateX(initialTranslateX[0] + deltaX);
            boardGroup.setTranslateY(initialTranslateY[0] + deltaY);
        });
    }

    // Create the wrapper Pane and set its preferred size based on the game dimensions
    private Pane createBoardWrapperWithBackground(Group boardGroup) {
        Pane boardWrapper = new Pane(boardGroup);
        boardWrapper.setPrefSize(
                gameController.getMenuView().getGAME_WIDTH(),
                gameController.getMenuView().getGAME_HEIGHT()
        );

        Image backgroundImage = new Image(
                Objects.requireNonNull(getClass().getResource("/backgrounds/boardBackground.png")).toExternalForm()
        );

        BackgroundSize backgroundSize = new BackgroundSize(
                100, 100, true, true, false, true
        );

        BackgroundImage bgImage = new BackgroundImage(
                backgroundImage,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                backgroundSize
        );

        boardWrapper.setBackground(new Background(bgImage));
        return boardWrapper;
    }


    //__________________________UTILITY METHODS_____________________________//

    private void zoom(Group group, double factor) {
        // Calculate the new scale, then clamp it to stay between 0.5 and 3.0
        double newScale = group.getScaleX() * factor;
        newScale = Math.max(0.5, Math.min(newScale, 3.0));

        // Apply scale uniformly on both axes
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
    }

    //__________________________VIEW UPDATES_____________________________//

    public void updateDiceImages(int die1, int die2) {
        diceImage1.setImage(drawOrDisplay.loadDiceImage(die1));
        diceImage2.setImage(drawOrDisplay.loadDiceImage(die2));
        // Ensure consistent display size
        diceImage1.setFitWidth(40);
        diceImage1.setFitHeight(40);
        diceImage2.setFitWidth(40);
        diceImage2.setFitHeight(40);
    }

    public void prepareForHumanInitialPlacement(Player currentPlayer) {
        logToGameLog("Player " + currentPlayer.getPlayerId() + ", place your initial settlement.");
        refreshSidebar();
        hideTurnButton();
        hideDiceButton();
    }

    // Refreshes the left sidebar UI that displays player information.
    public void refreshSidebar() {
        if (playerListVBox != null) {
            createLeftMenu(true);
            playerListVBox.requestLayout();
        }
    }

    // Logs a message to the console and game log area, ensuring it runs on the FX thread.
    public void logToGameLog(String message) {
        if (!gameplay.isGameOver()) {
            System.out.println(message);
        }
        if (Platform.isFxApplicationThread()) {
            if (!gameplay.isGameOver()) {
                appendToGameLog(message);
            }
        } else {
            if (!gameplay.isGameOver()) {
                Platform.runLater(() -> appendToGameLog(message));
            }
        }
    }

    // Appends a message to the game log text area and scrolls to bottom
    private void appendToGameLog(String message) {
        gameLogArea.appendText(message + "\n");

        // Delay to ensure scroll happens after rendering
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
    }

    // Displays a visual overlay and message while an AI opponent is taking its turn.
    public void showAITurnOverlay(Player aiPlayer) {
        Platform.runLater(() -> {
            if (aiPlayer instanceof AIOpponent ai) {
                drawOrDisplay.setThinkingMessage("AI Player " + ai.getPlayerId() + " (" + ai.getStrategyLevel().name() + ") is thinking...");
            } else {
                drawOrDisplay.setThinkingMessage("Opponent is thinking...");
            }
            drawOrDisplay.getOverlayPane().setVisible(true);
            drawOrDisplay.getOverlayPane().setMouseTransparent(false);
            drawOrDisplay.startThinkingAnimation();
        });
    }

    // Hides the AI turn overlay and stops any active animation.
    public void hideAITurnOverlay() {
        Platform.runLater(() -> {
            drawOrDisplay.getOverlayPane().setVisible(false);
            drawOrDisplay.getOverlayPane().setMouseTransparent(true);
            drawOrDisplay.stopThinkingAnimation();
        });
    }

    // Runs a given task on the JavaFX Application Thread.
    public void runOnFX(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    //________________________SHOW/HIDE BUTTONS____________________________________//

    // Enables the roll dice button.
    public void showDiceButton() {
    rollDiceButton.setDisable(false);}

    // Disables the roll dice button.
    public void hideDiceButton() {
        rollDiceButton.setDisable(true);
    }

    // Enables the next turn button.
    public void showTurnButton() {
        nextTurnButton.setDisable(false);
    }

    // Disables the next turn button.
    public void hideTurnButton() {
        nextTurnButton.setDisable(true);
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
}