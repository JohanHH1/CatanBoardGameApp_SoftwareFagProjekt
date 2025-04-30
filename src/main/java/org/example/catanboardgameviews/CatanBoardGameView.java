package org.example.catanboardgameviews;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import org.example.catanboardgameapp.*;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.util.List;
import java.util.ArrayList;

import org.example.controller.BuildController;
import org.example.controller.TradeController;
import org.example.controller.TurnController;
import java.util.*;

import org.w3c.dom.css.Rect;

import java.io.InputStream;

public class CatanBoardGameView {
    public static Button nextTurnButton;
    public static Button rollDiceButton;
    public static int boardRadius;

    public static void returnToMainMenu(Stage primaryStage) {
        MenuView.showMainMenu(primaryStage);
    }

    public static Scene createGameScene(Stage primaryStage, int radius, Gameplay gameplay) {
        double sceneWidth = 800;
        double sceneHeight = 600;

        // Group root = new Group();
        Board board = new Board(radius, sceneWidth, sceneHeight);

        //Draw tiles
        Group boardGroup = createBoardTiles(board, radius);

        // Desert tile
        Tile desertTile = board.getTiles().stream()
                .filter(t -> t.getTileDiceNumber() == 7)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No desert tile found"));

        // Initialize robber
        gameplay.setRobber(new Robber(desertTile));

        BorderPane root = new BorderPane();



        // Create initial left menu
        VBox leftMenu = createLeftMenu(gameplay);
        root.setLeft(leftMenu);
        boardRadius = radius;

        // Edges with click handlers
        BuildController controller = new BuildController(gameplay, boardGroup);
        drawEdges(board,boardGroup,controller,radius, root);
        displayVertices(board, boardGroup, controller, radius, root);

        // Draw vertices

        Pane boardWrapper = new Pane(boardGroup);
        root.setCenter(boardWrapper);

        rollDiceButton = new Button("Roll Dice");
        nextTurnButton = new Button("Next Turn");
        Button centerButton = new Button("Center Board");
        Button zoomInButton = new Button("+");
        Button zoomOutButton = new Button("-");
        Button exitButton = new Button("Exit");
        Button tradeButton = new Button("Trade with bank 4:1");
        Text diceResult = new Text("");
        Text currentPlayersOnTurn = new Text("");

        rollDiceButton.setOnAction(e -> {

            if (gameplay.isRobberMovementRequired()) {
                System.out.println("move robber pls");
                return;
            }
            int result = gameplay.rollDice();
            diceResult.setText("Dice: " + result);
            if (result == 7) {
                gameplay.requireRobberMove();
                nextTurnButton.setVisible(false);
                showRobberTargets(boardGroup, board, gameplay);
            } else {
                gameplay.distributeResource(result);
                nextTurnButton.setVisible(true);
            }
            root.setLeft(createLeftMenu(gameplay));
            rollDiceButton.setVisible(false);
            //nextTurnButton.setVisible(true);
        });

        TurnController turnController = new TurnController(gameplay, rollDiceButton, nextTurnButton, currentPlayersOnTurn);
        nextTurnButton.setOnAction(turnController::handleNextTurnButtonPressed);

        centerButton.setOnAction(e -> {
            boardGroup.setTranslateX(0);
            boardGroup.setTranslateY(0);
            boardGroup.setScaleX(1.0);
            boardGroup.setScaleY(1.0);
        });

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

        exitButton.setOnAction(e -> {
            Alert alert = new Alert(AlertType.CONFIRMATION, "Are you sure you want to exit to main menu?", ButtonType.YES, ButtonType.NO);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.YES) {
                returnToMainMenu(primaryStage);
            }
        });

        HBox buttonBox = new HBox(10, rollDiceButton, nextTurnButton, centerButton, zoomInButton, zoomOutButton,exitButton, tradeButton, currentPlayersOnTurn, diceResult);
        buttonBox.setStyle("-fx-padding: 10; -fx-alignment: top-left;");
        rollDiceButton.setVisible(false);

        root.setTop(buttonBox);

        boardWrapper.setOnScroll((ScrollEvent event) -> {
            double zoomFactor = event.getDeltaY() > 0 ? 1.05 : 0.95;
            double scale = boardGroup.getScaleX() * zoomFactor;
            scale = Math.max(0.5, Math.min(scale, 3.0));
            boardGroup.setScaleX(scale);
            boardGroup.setScaleY(scale);
            event.consume();
        });

        final double[] mouseAnchorX = new double[1];
        final double[] mouseAnchorY = new double[1];
        final double[] initialTranslateX = new double[1];
        final double[] initialTranslateY = new double[1];

        boardWrapper.setOnMousePressed(event -> {
            mouseAnchorX[0] = event.getX();
            mouseAnchorY[0] = event.getY();
            initialTranslateX[0] = boardGroup.getTranslateX();
            initialTranslateY[0] = boardGroup.getTranslateY();
        });

        boardWrapper.setOnMouseDragged(event -> {
            double deltaX = event.getX() - mouseAnchorX[0];
            double deltaY = event.getY() - mouseAnchorY[0];
            boardGroup.setTranslateX(initialTranslateX[0] + deltaX);
            boardGroup.setTranslateY(initialTranslateY[0] + deltaY);
        });

        Scene scene = new Scene(root, sceneWidth, sceneHeight, Color.LIGHTGRAY);
        scene.setOnKeyPressed(event -> {
            double moveStep = 30;
            switch (event.getCode()) {
                case W -> boardGroup.setTranslateY(boardGroup.getTranslateY() - moveStep);
                case A -> boardGroup.setTranslateX(boardGroup.getTranslateX() - moveStep);
                case S -> boardGroup.setTranslateY(boardGroup.getTranslateY() + moveStep);
                case D -> boardGroup.setTranslateX(boardGroup.getTranslateX() + moveStep);
                case R -> {
                    boardGroup.setTranslateX(0);
                    boardGroup.setTranslateY(0);
                    boardGroup.setScaleX(1.0);
                    boardGroup.setScaleY(1.0);
                }
                case ESCAPE -> {
                    Alert alert = new Alert(AlertType.CONFIRMATION, "Are you sure you want to exit to main menu?", ButtonType.YES, ButtonType.NO);
                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.YES) {
                        returnToMainMenu(primaryStage);
                    }
                }
            }
        });

        return scene;
    }
    public static void showRobberTargets(Group boardGroup, Board board, Gameplay gameplay) {
        List<Circle> robberTargetCircles = new ArrayList<>();

        for (Tile tile : board.getTiles()) {
            if (tile.getTileDiceNumber() == 7) continue; // skip desert

            Point2D center = tile.getCenter();
            double circleX = center.getX();
            double circleY = center.getY();

            Circle circle = new Circle(circleX, circleY, 50 / boardRadius, Color.TRANSPARENT);
            circle.setStroke(Color.BLACK);
            circle.setStrokeWidth(5);

            circle.setOnMouseClicked(e -> {
                // Remove all target circles
                for (Circle c : robberTargetCircles) {
                    boardGroup.getChildren().remove(c);
                }

                // Mark the selected tile with a black circle
                Circle blackDot = new Circle(circleX, circleY, 50 / boardRadius, Color.TRANSPARENT);
                blackDot.setStroke(Color.BLACK);
                blackDot.setStrokeWidth(5);
                boardGroup.getChildren().add(blackDot);

                // Move robber
                Robber robber = gameplay.getRobber();
                robber.moveTo(tile);
                gameplay.robberHasMoved();
                nextTurnButton.setVisible(true);

                //Potential victims
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
                        System.out.println("Failed to steal resource from " + victim);
                    }
                    VBox newLeftMenu = createLeftMenu(gameplay);
                    ((BorderPane) boardGroup.getScene().getRoot()).setLeft(newLeftMenu);

                });
            });

            boardGroup.getChildren().add(circle);
            robberTargetCircles.add(circle);
        }
    }

    private static Group createBoardTiles(Board board, int radius) {
        Group boardGroup = new Group();

        for (Tile tile : board.getTiles()) {
            Polygon polygon = CatanBoardGameView.createTilePolygon(tile); // existerande metod
            polygon.setFill(CatanBoardGameView.getTileColor(tile.getResourcetype()));
            polygon.setStroke(Color.BLACK);

            Point2D center = tile.getCenter();
            double centerX = center.getX();
            double centerY = center.getY();

            boardGroup.getChildren().add(polygon); // bakgrundsplatta
            boardGroup.getChildren().add(CatanBoardGameView.getResourceIcon(tile.getResourcetype(), centerX, centerY, board.getHexSize())); // ikon

            if (tile.getTileDiceNumber() != 7) {
                Text number = new Text(centerX, centerY, String.valueOf(tile.getTileDiceNumber()));
                number.setFont(Font.font("Arial", FontWeight.BOLD, 40.0 / radius));
                number.setTextAlignment(TextAlignment.CENTER);

                switch (tile.getTileDiceNumber()) {
                    case 6, 8 -> number.setFill(Color.RED);
                    default -> number.setFill(Color.DARKGREEN);
                }

                Text sampleNumber = new Text("12");
                sampleNumber.setFont(Font.font("Arial", FontWeight.BOLD, 40.0 / radius));
                Rectangle background = CatanBoardGameView.getRectangle(sampleNumber, centerX, centerY);

                number.setX(centerX - number.getLayoutBounds().getWidth() / 2);
                number.setY(centerY + number.getLayoutBounds().getHeight() / 4);
                boardGroup.getChildren().addAll(background, number);
            }
        }

        return boardGroup;
    }

    private static void drawEdges(Board board, Group boardGroup, BuildController controller, int radius, BorderPane root) {
        for (Edge edge : board.getEdges()) {
            Line clickableLine = new Line(
                    edge.getVertex1().getX(), edge.getVertex1().getY(),
                    edge.getVertex2().getX(), edge.getVertex2().getY()
            );
            clickableLine.setStrokeWidth(10);
            clickableLine.setOpacity(0); // Osynlig

            Line visibleLine = new Line(
                    edge.getVertex1().getX(), edge.getVertex1().getY(),
                    edge.getVertex2().getX(), edge.getVertex2().getY()
            );
            visibleLine.setStroke(Color.WHITE);
            visibleLine.setStrokeWidth(2);

            clickableLine.setOnMouseClicked(controller.createRoadClickHandler(edge, visibleLine, root));

            boardGroup.getChildren().addAll(visibleLine, clickableLine);
        }
    }

    // Metod för att skapa och hantera användarklick på vägar och hörn
    // Metod för att skapa och hantera användarklick på vägar och hörn

private static void centerBoard(Board board, Group boardGroup, double screenWidth, double screenHeight) {

        Tile centerTile = board.getTiles().get((board.getTiles().size() - 1) / 2);
        Point2D centerPoint = centerTile.getCenter();
        double centerX = (screenWidth - 200) / 2 - centerPoint.getX();
        double centerY = screenHeight / 2 - centerPoint.getY();
        boardGroup.setTranslateX(centerX);
        boardGroup.setTranslateY(centerY);
        boardGroup.setScaleX(1.0);
        boardGroup.setScaleY(1.0);
    }

    private static void zoom(Group group, double zoomFactor) {
        double scale = group.getScaleX() * zoomFactor;
        group.setScaleX(Math.max(0.5, Math.min(scale, 3.0)));
        group.setScaleY(Math.max(0.5, Math.min(scale, 3.0)));
    }

    public static VBox createLeftMenu(Gameplay gameplay) {
        VBox leftMenu = new VBox(10);
        leftMenu.setStyle("-fx-padding: 10; -fx-background-color: #e0e0e0; -fx-min-width: 200;");

        Text title = new Text("Player Stats");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        leftMenu.getChildren().add(title);

        for (Player player : gameplay.getPlayerList()) {
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
            Text pointsText = new Text("Victory points: " + player.getplayerScore());
            pointsText.setFont(Font.font("Arial", 12));
            playerBox.getChildren().add(pointsText);

            leftMenu.getChildren().add(playerBox);
        }

        return leftMenu;
    }

    public static void showTemporaryDot(Group boardGroup, double midX, double midY, Color red) {
        // create red dot
        Circle dot = new Circle(midX, midY, 5, red);

        // add to scene
        boardGroup.getChildren().add(dot);

        // remove after 1s
        PauseTransition delay = new PauseTransition(Duration.seconds(1));
        delay.setOnFinished(e -> boardGroup.getChildren().remove(dot));
        delay.play();
    }

    public static void showPlacementError(Group boardGroup, double x, double y) {
        Line line1 = new Line(x-5, y-5, x+5, y+5);
        Line line2 = new Line(x-5, y+5, x+5, y-5);
        line1.setStroke(Color.RED);
        line2.setStroke(Color.RED);
        line1.setStrokeWidth(2);
        line2.setStrokeWidth(2);

        Group errorGroup = new Group(line1, line2);
        boardGroup.getChildren().add(errorGroup);

        // Goes away after 1s
        System.out.println("Placement is invalid");
        PauseTransition delay = new PauseTransition(Duration.seconds(1));
        delay.setOnFinished(e -> {
            boardGroup.getChildren().remove(errorGroup);
        });
        delay.play();
    }

    public static void updateVertexAppearance(Circle circle, Vertex vertex) {
        if (vertex.getOwner() != null) { // If vertex.getOwner() is not null then set color to the owner
            circle.setFill(vertex.getOwner().getColor());
            circle.setRadius(16./boardRadius);
        } else {
            circle.setFill(Color.TRANSPARENT);
            circle.setRadius(8./boardRadius); // Vertices without settlement are smaller
        }
    }

   /* private static void updateVertexApperanceCity(Rectangle rectangle, Vertex vertex) {
        if (vertex.getOwner() != null) { // If vertex.getOwner() is not null then set color to the owner
            rectangle.setFill(vertex.getOwner().getColor());
            rectangle.setRadius(8);
        } else {
            updateVertexAppearance(Circle circle, Vertex vertex);
        }
    }*/


    private static Rectangle getRectangle(Text sampleNumber, double centerX, double centerY) {
        double maxNumberWidth = sampleNumber.getLayoutBounds().getWidth();
        double maxNumberHeight = sampleNumber.getLayoutBounds().getHeight();

        // Defining the box
        double boxPadding = 5;
        double boxWidth = maxNumberWidth + boxPadding;
        double boxHeight = maxNumberHeight + boxPadding;

        // Rectangle background
        Rectangle background = new Rectangle(
                centerX - boxWidth / 2,
                centerY - boxHeight / 2,
                boxWidth,
                boxHeight);

        background.setFill(Color.BEIGE);
        background.setStroke(Color.BLACK);
        // Round corners
        background.setArcWidth(5);
        background.setArcHeight(5);
        return background;
    }

    public static void showTradeError(String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Trade Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Creates polygons as tiles based on the vertices in each tile.
    private static Polygon createTilePolygon(Tile tile) {
        Polygon polygon = new Polygon();
        for (Vertex vertex : tile.getVertices()) {
            polygon.getPoints().addAll(vertex.getX(), vertex.getY());
        }
        return polygon;
    }

    private static Color getTileColor(Resource.ResourceType type) {
        return switch (type) {
            case BRICK -> Color.SADDLEBROWN;
            case WOOD -> Color.DARKGREEN;
            case ORE -> Color.DARKGRAY;
            case GRAIN -> Color.GOLD;
            case WOOL -> Color.YELLOWGREEN;
            case DESERT -> Color.BEIGE;
        };
    }

    private static ImageView getResourceIcon(Resource.ResourceType type, double x, double y, double hexSize) {
        String filename = switch (type) {
            case BRICK -> "/Icons/brick.png";
            case WOOD -> "/Icons/wood.png";
            case ORE -> "/Icons/ore.png";
            case GRAIN -> "/Icons/grain.png";
            case WOOL -> "/Icons/wool.png";
            case DESERT -> "/Icons/desert.png";
        };

        InputStream stream = CatanBoardGameView.class.getResourceAsStream(filename);
        if (stream == null) {
            System.err.println("⚠️ Image not found: " + filename);
            return new ImageView(); // fallback
        }
        // Increase tile coverage size
        Image image = new Image(stream);
        ImageView imageView = new ImageView(image);

        double imageWidth = Math.sqrt(3) * hexSize;
        double imageHeight = 2 * hexSize;

        imageView.setFitWidth(imageWidth);
        imageView.setFitHeight(imageHeight);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);

        imageView.setX(Math.round(x - imageWidth / 2));
        imageView.setY(Math.round(y - imageHeight / 2));

        return imageView;
    }

    public static void showBuildErrorDot(Group boardGroup, Point2D mid) {
        showTemporaryDot(boardGroup, mid.getX(), mid.getY(), Color.RED);
    }

    public static void updateRoadAppearance(Line visibleLine, Player currentPlayer) {
        visibleLine.setStroke(currentPlayer.getColor());
        visibleLine.setStrokeWidth(4);
    }

    private static void displayVertices(Board board, Group boardGroup, BuildController controller, int radius, BorderPane root) {

        for (Vertex vertex : board.getVertices()) {
            Circle visibleCircle = new Circle(vertex.getX(), vertex.getY(), 10/radius); // vertex circle
            visibleCircle.setFill(Color.TRANSPARENT);
            //visibleCircle.setStroke(Color.BLACK);
            //visibleCircle.setStroke(Color.TRANSPARENT);
            boardGroup.getChildren().add(visibleCircle);

            Circle clickableCircle = new Circle(vertex.getX(), vertex.getY(), 4);
            clickableCircle.setFill(Color.TRANSPARENT);
            clickableCircle.setStroke(Color.TRANSPARENT);

            clickableCircle.setOnMouseClicked(controller.createSettlementClickHandler(visibleCircle, vertex, root));

            boardGroup.getChildren().addAll(clickableCircle);
    }
    }
}



