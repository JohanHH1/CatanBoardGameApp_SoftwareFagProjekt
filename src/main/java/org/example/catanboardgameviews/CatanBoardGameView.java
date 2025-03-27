package org.example.catanboardgameviews;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import org.example.catanboardgameapp.*;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

import org.example.catanboardgameapp.Board;
import org.example.catanboardgameapp.Resource;
import org.example.catanboardgameapp.Tile;
import org.example.catanboardgameapp.Vertex;


import java.io.InputStream;

import static javafx.application.Application.launch;

public class CatanBoardGameView {
    public static Scene createGameScene(Stage primaryStage, int radius, Gameplay gameplay) {
        Board board = new Board(radius,50,400,300);
        Group boardGroup = new Group();

        //Group root = new Group();

        //Draw tiles
        for (Tile tile : board.getTiles()) {
            Polygon polygon = createTilePolygon(tile);
            //***********
            // ADD COLORS BASED ON RESOURCE TYPE
            //************
            polygon.setFill(getTileColor(tile.getResourcetype()));
            polygon.setStroke(Color.BLACK);


            Point2D center = tile.getCenter();
            double centerX = center.getX();
            double centerY = center.getY();
            ImageView icon = getResourceIcon(tile.getResourcetype(), centerX, centerY);
            boardGroup.getChildren().add(polygon); // background hex tile
            boardGroup.getChildren().add(getResourceIcon(tile.getResourcetype(), centerX, centerY)); // icon on top
            // No coloring on 7 (desert)
            if (tile.getTileDiceNumber() != 7) {
                Text number = new Text(centerX, centerY, String.valueOf(tile.getTileDiceNumber()));
                number.setFont(Font.font("Arial", FontWeight.BOLD, 18));
                number.setTextAlignment(TextAlignment.CENTER);

                // Color 6 and 8 red
                switch (tile.getTileDiceNumber()) {
                    case 6, 8:
                        number.setFill(Color.RED);
                        break;
                    default:
                        number.setFill(Color.DARKGREEN);
                        break;
                }
                // Measures largest number so rectangle can be based of this
                Text sampleNumber = new Text("12");
                sampleNumber.setFont(Font.font("Arial", FontWeight.BOLD, 18));
                Rectangle background = getRectangle(sampleNumber, centerX, centerY);

                // Center the numbers (represented as numbers)
                number.setX(centerX - number.getLayoutBounds().getWidth() / 2);
                number.setY(centerY + number.getLayoutBounds().getHeight() / 4);
                boardGroup.getChildren().addAll(background, number);
                }



        }
        /*
        //Draw edges
        for (Edge edge : board.getEdges()) {
            Line line = new Line(edge.getVertex1().getX(), edge.getVertex1().getY(),
                    edge.getVertex2().getX(), edge.getVertex2().getY());
            line.setStroke(Color.BLACK);
            root.getChildren().add(line);
        }

         */

        // Draw vertices
        for (Vertex vertex : board.getVertices()) {

            Circle visibleCircle = new Circle(vertex.getX(), vertex.getY(), 4); // vertex circle
            visibleCircle.setFill(Color.BLACK);
            visibleCircle.setStroke(Color.BLACK);
            boardGroup.getChildren().add(visibleCircle);

            Circle clickableCircle = new Circle(vertex.getX(), vertex.getY(), 4);
            clickableCircle.setFill(Color.TRANSPARENT);
            clickableCircle.setStroke(Color.TRANSPARENT);

            clickableCircle.setOnMouseClicked(event -> {
                if (gameplay.buildSettlement(vertex)) { // if conditions for building are true
                    vertex.setOwner(gameplay.getCurrentPlayer()); // take vertex and set owner to currentPlayer
                    updateVertexAppearance(visibleCircle, vertex); // update appearance
                    System.out.println("Settlement built by player " + gameplay.getCurrentPlayer().getPlayerId());
                } else {
                    showPlacementError(boardGroup, vertex.getX(), vertex.getY()); // if conditions for building are false
                }
            });

            boardGroup.getChildren().addAll(clickableCircle); // show chosen vertex
        }

        Button rollDiceButton = new Button("Roll Dice");
        Button nextTurnButton = new Button("Next Turn");
        Text diceResult = new Text("");

        rollDiceButton.setOnAction(e -> {
            int result = gameplay.rollDice();
            diceResult.setText("Dice:" + result);
        });
        nextTurnButton.setOnAction(e -> {
            gameplay.nextPlayerTurn();
            diceResult.setText("Turn: Player " + gameplay.getCurrentPlayer().getPlayerId());
        });
        HBox buttonBox = new HBox(10, rollDiceButton, nextTurnButton, diceResult);
        buttonBox.setStyle("-fx-padding: 10; -fx-alignment: top-left;");

        BorderPane root = new BorderPane();
        root.setCenter(boardGroup);
        root.setTop(buttonBox);


//        VBox leftMenu = new VBox();
//        leftMenu.setStyle("-fx-padding: 10; -fx-background-color: #e0e0e0; -fx-min-width: 150;");
//        Text title = new Text("Player Stats");
//        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
//        leftMenu.getChildren().add(title);
//        for (Player player : gameplay.getPlayerList()) {
//            Text playerName = new Text("Player " + player.getPlayerId());
//            playerName.setFont(Font.font("Arial", 14));
//            leftMenu.getChildren().add(playerName);
//        }
//
//        root.setLeft(leftMenu);
        VBox leftMenu = new VBox(10); // spacing between player sections
        leftMenu.setStyle("-fx-padding: 10; -fx-background-color: #e0e0e0; -fx-min-width: 200;");

        Text title = new Text("Player Stats");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        leftMenu.getChildren().add(title);

// Use getPlayerList() instead of getPlayers()
        for (Player player : gameplay.getPlayerList()) {
            VBox playerBox = new VBox(5); // spacing between name and resources

            Text playerName = new Text("Player " + player.getPlayerId());
            playerName.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            playerName.setFill(player.getColor());

            playerBox.getChildren().add(playerName);

            // Show all resources
            for (String resourceName : player.getResources().keySet()) {
                int count = player.getResources().get(resourceName);
                Text resourceText = new Text(resourceName + ": " + count);
                resourceText.setFont(Font.font("Arial", 12));
                playerBox.getChildren().add(resourceText);
            }

            leftMenu.getChildren().add(playerBox);
        }

        root.setLeft(leftMenu);


        boardGroup.setOnScroll(event -> {
            double zoomFactor = 1.05;
            if (event.getDeltaY() < 0) {
                zoomFactor = 0.95;
            }

            double newScaleX = boardGroup.getScaleX() * zoomFactor;
            double newScaleY = boardGroup.getScaleY() * zoomFactor;

            // Clamp zoom between 0.5x and 3x
            if (newScaleX >= 0.5 && newScaleX <= 3) {
                boardGroup.setScaleX(newScaleX);
                boardGroup.setScaleY(newScaleY);
            }

            event.consume();
        });
        return new Scene(root, 800, 600, Color.LIGHTGRAY);


    }

    private static void showPlacementError(Group boardGroup, double x, double y) {
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

    private static void updateVertexAppearance(Circle circle, Vertex vertex) {
        if (vertex.getOwner() != null) { // If vertex.getOwner() is not null then set color to the owner
            circle.setFill(vertex.getOwner().getColor());
            circle.setRadius(8);
        } else {
            circle.setFill(Color.TRANSPARENT);
            circle.setRadius(4); // Vertices without settlement are smaller
        }
    }

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
    private static ImageView getResourceIcon(Resource.ResourceType type, double x, double y) {
        //System.out.println("⏳ Attempting to load icon for: " + type);

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

        // Reduce tile size slightly to fit better inside the hex
        double hexRadius = 50;  // Adjust based on your Board class
        double tileSize = hexRadius * 2;  // Scale slightly smaller

        imageView.setFitWidth(tileSize);
        imageView.setFitHeight(tileSize);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        // Center the image over the tile with small vertical correction
        imageView.setX((x - tileSize / 2)+ 7);
        imageView.setY((y - tileSize / 2)); // Slight vertical adjustment

        return imageView;
    }

}
