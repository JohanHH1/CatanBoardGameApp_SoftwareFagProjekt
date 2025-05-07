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

public class CatanBoardGameView {
    public static Button nextTurnButton;
    public static Button rollDiceButton;
    public static int boardRadius;
    private static Circle currentRobberCircle = null;


    // __________________________KINDA CONSTRUCTOR - Creating the Game Scene_________________________
    public static Scene createGameScene(Stage primaryStage, int radius, Gameplay gameplay) {
        double sceneWidth = 800;
        double sceneHeight = 600;

        Board board = new Board(radius, sceneWidth, sceneHeight);
        gameplay.setBoard(board); // Link board to gameplay logic

        Group boardGroup = createBoardTiles(board, radius); // Draw hex tiles

        // Find and assign the desert tile (dice number 7)
        Tile desertTile = Board.getTiles().stream()
                .filter(t -> t.getTileDiceNumber() == 7)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No desert tile found"));
        gameplay.setRobber(new Robber(desertTile)); // Initialize the robber

        BorderPane root = new BorderPane();
        VBox leftMenu = createLeftMenu(gameplay);
        root.setLeft(leftMenu);
        boardRadius = radius;

        BuildController buildController = new BuildController(gameplay, boardGroup);
        drawEdges(board, boardGroup, buildController, radius, root);
        displayVertices(board, boardGroup, buildController, radius, root);

        Pane boardWrapper = new Pane(boardGroup);
        root.setCenter(boardWrapper);
        centerBoard(board, boardGroup, sceneWidth, sceneHeight);

        // Top screen Buttons
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
        rollDiceButton.setOnAction(e ->
                rollDiceAndDistribute(gameplay, diceImage1, diceImage2, root, boardGroup, board)
        );

        TurnController turnController = new TurnController(gameplay, rollDiceButton, nextTurnButton);
        nextTurnButton.setOnAction(turnController::handleNextTurnButtonPressed);

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
    // Draws all the hex tiles and overlays them with icons and dice numbers
    private static Group createBoardTiles(Board board, int radius) {
        Group boardGroup = new Group();

        for (Tile tile : Board.getTiles()) {
            Polygon hexShape = createTilePolygon(tile);
            hexShape.setFill(tile.getTileColor(tile.getResourcetype()));
            hexShape.setStroke(Color.BLACK);

            Point2D center = tile.getCenter();
            double centerX = center.getX();
            double centerY = center.getY();

            // Tile base
            boardGroup.getChildren().add(hexShape);

            // Resource icon
            boardGroup.getChildren().add(tile.getResourceIcon(tile.getResourcetype(), centerX, centerY, board.getHexSize()));

            // Dice number
            if (tile.getTileDiceNumber() != 7) {
                Text numberText = new Text(centerX, centerY, String.valueOf(tile.getTileDiceNumber()));
                numberText.setFont(Font.font("Arial", FontWeight.BOLD, 40.0 / radius));
                numberText.setTextAlignment(TextAlignment.CENTER);
                numberText.setFill((tile.getTileDiceNumber() == 6 || tile.getTileDiceNumber() == 8) ? Color.RED : Color.DARKGREEN);

                Text sample = new Text("12");
                sample.setFont(Font.font("Arial", FontWeight.BOLD, 40.0 / radius));
                Rectangle background = createBoxBehindDiceNumber(sample, centerX, centerY);

                // Center align text
                numberText.setX(centerX - numberText.getLayoutBounds().getWidth() / 2);
                numberText.setY(centerY + numberText.getLayoutBounds().getHeight() / 4);

                boardGroup.getChildren().addAll(background, numberText);
            }
        }

        return boardGroup;
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
            showRobberTargets(boardGroup, board, gameplay);
        } else {
            nextTurnButton.setVisible(true);
        }

        root.setLeft(createLeftMenu(gameplay));
        rollDiceButton.setVisible(false);
    }

    //__________________________HELPER FUNCTIONS_____________________________________

    private static void showDiceImages(int result, BorderPane root) {
        HBox diceBox = new HBox(5);
        diceBox.setPadding(new Insets(0, 10, 0, 10));

        Random rand = new Random();
        int die1 = rand.nextInt(6) + 1;
        int die2 = result - die1;

        // Clamp die2 to range 1–6, fallback if out of bounds
        if (die2 < 1 || die2 > 6) {
            die1 = result / 2;
            die2 = result - die1;
        }

        ImageView die1Image = getDiceImage(die1);
        ImageView die2Image = getDiceImage(die2);

        diceBox.getChildren().addAll(die1Image, die2Image);
        root.setTop(diceBox);  // Set new top section only with dice
    }

    private static ImageView getDiceImage(int value) {
        String path = "/dice/dice" + value + ".png";

        InputStream stream = CatanBoardGameView.class.getResourceAsStream(path);
        if (stream == null) {
            System.err.println("Missing image: " + path);
            return new ImageView();
        }

        Image image = new Image(stream);
        ImageView view = new ImageView(image);
        view.setFitWidth(32);
        view.setFitHeight(32);
        view.setPreserveRatio(true);
        return view;
    }

    private static Image loadDiceImage(int number) {
        String path = "/dice/dice" + number + ".png";
        InputStream stream = CatanBoardGameView.class.getResourceAsStream(path);
        if (stream == null) {
            System.err.println("⚠️ Could not load image: " + path);
            return new Image("/Icons/error.png"); // fallback image (add one if needed)
        }
        return new Image(stream);
    }


    // Creates a hex tile polygon from the tile's vertices
    private static Polygon createTilePolygon(Tile tile) {
        Polygon polygon = new Polygon();
        for (Vertex vertex : tile.getVertices()) {
            polygon.getPoints().addAll(vertex.getX(), vertex.getY());
        }
        return polygon;
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

    // Show whether a vertex belongs to a player or is empty
    public static void updateVertexAppearance(Circle circle, Vertex vertex) {
        if (vertex.getOwner() != null) {
            circle.setFill(vertex.getOwner().getColor());
            circle.setRadius(16.0 / boardRadius);
        } else {
            circle.setFill(Color.TRANSPARENT);
            circle.setRadius(8.0 / boardRadius);
        }
    }

    //_______________________________DRAW/DISPLAY CLASS?______________________________________________

    // Creates a background rectangle behind each dice number
    private static Rectangle createBoxBehindDiceNumber(Text sample, double centerX, double centerY) {
        double boxPadding = 5;
        double boxWidth = sample.getLayoutBounds().getWidth() + boxPadding;
        double boxHeight = sample.getLayoutBounds().getHeight() + boxPadding;

        Rectangle background = new Rectangle(
                centerX - boxWidth / 2,
                centerY - boxHeight / 2,
                boxWidth,
                boxHeight
        );

        background.setFill(Color.BEIGE);
        background.setStroke(Color.BLACK);
        background.setArcWidth(5);
        background.setArcHeight(5);

        return background;
    }

    // Draws both visible and clickable lines for edges
    private static void drawEdges(Board board, Group boardGroup, BuildController controller, int radius, BorderPane root) {
        for (Edge edge : Board.getEdges()) {
            Line visibleLine = new Line(
                    edge.getVertex1().getX(), edge.getVertex1().getY(),
                    edge.getVertex2().getX(), edge.getVertex2().getY()
            );
            visibleLine.setStroke(Color.WHITE);
            visibleLine.setStrokeWidth(2);

            Line clickableLine = new Line(
                    edge.getVertex1().getX(), edge.getVertex1().getY(),
                    edge.getVertex2().getX(), edge.getVertex2().getY()
            );
            clickableLine.setStrokeWidth(10);
            clickableLine.setOpacity(0); // invisible but interactive

            clickableLine.setOnMouseClicked(controller.createRoadClickHandler(edge, visibleLine, root));

            boardGroup.getChildren().addAll(visibleLine, clickableLine);
        }
    }

    // Displays a temporary red dot at a location (used for feedback like invalid placement)
    public static void showTemporaryDot(Group boardGroup, double midX, double midY, Color color) {
        Circle dot = new Circle(midX, midY, 5, color);
        boardGroup.getChildren().add(dot);

        PauseTransition delay = new PauseTransition(Duration.seconds(1));
        delay.setOnFinished(e -> boardGroup.getChildren().remove(dot));
        delay.play();
    }

    // Displays an 'X' at an invalid placement spot
    public static void showPlacementError(Group boardGroup, double x, double y) {
        Line line1 = new Line(x - 5, y - 5, x + 5, y + 5);
        Line line2 = new Line(x - 5, y + 5, x + 5, y - 5);
        line1.setStroke(Color.RED);
        line2.setStroke(Color.RED);
        line1.setStrokeWidth(2);
        line2.setStrokeWidth(2);

        Group errorGroup = new Group(line1, line2);
        boardGroup.getChildren().add(errorGroup);
        // Error goes away after 1s
        System.out.println("Placement is invalid");
        PauseTransition delay = new PauseTransition(Duration.seconds(1));
        delay.setOnFinished(e -> boardGroup.getChildren().remove(errorGroup));
        delay.play();
    }

    // Displays all vertex points on the board and registers click events
    private static void displayVertices(Board board, Group boardGroup, BuildController controller, int radius, BorderPane root) {
        for (Vertex vertex : board.getVertices()) {
            // Visual vertex
            Circle visible = new Circle(vertex.getX(), vertex.getY(), 10.0 / radius);
            visible.setFill(Color.TRANSPARENT);
            boardGroup.getChildren().add(visible);

            // Invisible click handler
            Circle clickable = new Circle(vertex.getX(), vertex.getY(), 4);
            clickable.setFill(Color.TRANSPARENT);
            clickable.setStroke(Color.TRANSPARENT);
            clickable.setOnMouseClicked(controller.createSettlementClickHandler(visible, vertex, root));

            boardGroup.getChildren().add(clickable);
        }
    }

    // Displays a generic error popup for invalid trade attempts
    public static void showTradeError(String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Trade Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    public static void showBuildingCostsPopup() {
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
        imageView.setFitWidth(400); // Adjust to your preferred size

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

    // Colors a player's road line
    public static void updateRoadAppearance(Line visibleLine, Player currentPlayer) {
        visibleLine.setStroke(currentPlayer.getColor());
        visibleLine.setStrokeWidth(4);
    }

    // Used to show build errors (similar to showTemporaryDot but red)
    public static void showBuildErrorDot(Group boardGroup, Point2D mid) {
        showTemporaryDot(boardGroup, mid.getX(), mid.getY(), Color.RED);
    }


    //__________________________ROBBER FUNCTIONS_____________________________________

    // Highlights possible robber placement targets and handles stealing logic
    public static void showRobberTargets(Group boardGroup, Board board, Gameplay gameplay) {
        List<Circle> robberTargetCircles = new ArrayList<>();

        for (Tile tile : Board.getTiles()) {
            if (tile.getTileDiceNumber() == 7) continue; // Skip desert tile

            Point2D center = tile.getCenter();
            double radius = 50.0 / boardRadius;

            Circle circle = new Circle(center.getX(), center.getY(), radius, Color.TRANSPARENT);
            circle.setStroke(Color.BLACK);
            circle.setStrokeWidth(5);

            circle.setOnMouseClicked(e -> {
                // Clear previous highlights
                for (Circle c : robberTargetCircles) boardGroup.getChildren().remove(c);
                robberTargetCircles.clear();

                // Remove the previous robber circle if present
                if (currentRobberCircle != null) boardGroup.getChildren().remove(currentRobberCircle);

                // Mark the new robber position
                Circle newRobberMarker = new Circle(center.getX(), center.getY(), radius, Color.TRANSPARENT);
                newRobberMarker.setStroke(Color.BLACK);
                newRobberMarker.setStrokeWidth(5);
                boardGroup.getChildren().add(newRobberMarker);
                currentRobberCircle = newRobberMarker;

                // Move the robber
                Robber robber = gameplay.getRobber();
                robber.moveTo(tile);
                gameplay.robberHasMoved();
                nextTurnButton.setVisible(true);

                // Prompt for stealing from valid victims
                List<Player> victims = robber.getPotentialVictims(tile, gameplay.getCurrentPlayer());

                if (victims.isEmpty()) {
                    System.out.println("No players to steal from");
                    return;
                }

                ChoiceDialog<Player> dialog = new ChoiceDialog<>(victims.get(0), victims);
                dialog.setTitle("Choose a player to steal from");
                dialog.setHeaderText("Select a player with a city/settlement on this tile:");
                dialog.setContentText("Player:");

                Optional<Player> result = dialog.showAndWait();
                result.ifPresent(victim -> {
                    boolean success = gameplay.stealResourceFrom(victim);
                    if (!success) {
                        System.out.println("Failed to steal a resource from " + victim);
                    }

                    // Refresh left player menu to show updated resources
                    VBox updatedMenu = createLeftMenu(gameplay);
                    ((BorderPane) boardGroup.getScene().getRoot()).setLeft(updatedMenu);
                });
            });

            boardGroup.getChildren().add(circle);
            robberTargetCircles.add(circle);
        }
    }

    // Prompts a player to discard half their resources when a 7 is rolled
    public static Map<String, Integer> showDiscardDialog(Player player, Gameplay gameplay) {
        Map<String, Integer> playerResources = new HashMap<>(player.getResources());
        int totalCards = playerResources.values().stream().mapToInt(Integer::intValue).sum();
        int cardsToDiscard = totalCards / 2;

        if (cardsToDiscard == 0) return null;

        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.UNDECORATED);
        dialogStage.setTitle("Discard Resources");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(100);
        grid.getColumnConstraints().addAll(labelCol, new ColumnConstraints(), new ColumnConstraints(), new ColumnConstraints());

        Map<String, Integer> discardSelection = new HashMap<>();
        Map<String, Text> counterTexts = new HashMap<>();
        Button discardButton = new Button("Discard");
        discardButton.setDisable(true);

        int row = 0;
        for (Map.Entry<String, Integer> entry : playerResources.entrySet()) {
            String resource = entry.getKey();
            int count = entry.getValue();

            Text label = new Text(resource + " (" + count + ")");
            Button minus = new Button("-");
            Button plus = new Button("+");
            Text counter = new Text("0");
            counter.setWrappingWidth(30);
            counter.setTextAlignment(TextAlignment.CENTER);

            discardSelection.put(resource, 0);
            counterTexts.put(resource, counter);

            plus.setOnAction(e -> {
                if (discardSelection.get(resource) < count &&
                        gameplay.getTotalSelectedCards(discardSelection) < cardsToDiscard) {
                    discardSelection.put(resource, discardSelection.get(resource) + 1);
                    counter.setText(String.valueOf(discardSelection.get(resource)));
                    discardButton.setDisable(gameplay.getTotalSelectedCards(discardSelection) != cardsToDiscard);
                }
            });

            minus.setOnAction(e -> {
                if (discardSelection.get(resource) > 0) {
                    discardSelection.put(resource, discardSelection.get(resource) - 1);
                    counter.setText(String.valueOf(discardSelection.get(resource)));
                    discardButton.setDisable(gameplay.getTotalSelectedCards(discardSelection) != cardsToDiscard);
                }
            });

            grid.add(label, 0, row);
            grid.add(minus, 1, row);
            grid.add(counter, 2, row);
            grid.add(plus, 3, row);
            row++;
        }

        Map<String, Integer>[] result = new Map[]{null}; // container for return value
        discardButton.setOnAction(e -> {
            result[0] = discardSelection;
            dialogStage.close();
        });

        VBox container = new VBox(15,
                new Text(player + " you must discard " + cardsToDiscard + " resource cards."),
                grid,
                discardButton
        );
        container.setPadding(new Insets(15));
        container.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-width: 1;");

        Scene scene = new Scene(container);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();

        return result[0];
    }
}