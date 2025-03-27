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
            Circle circle = new Circle(vertex.getX(), vertex.getY(), 4);
            circle.setFill(Color.RED);
            boardGroup.getChildren().add(circle);
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

        Image image = new Image(stream);
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(30); // or 32 or 24, try what looks best
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setX(x - imageView.getFitWidth() / 2);
        if (type == Resource.ResourceType.BRICK) {
            imageView.setY(y - imageView.getFitHeight() / 2+18);
        } else if (type == Resource.ResourceType.WOOD) {
            imageView.setY(y - imageView.getFitHeight() / 2+20);
        } else if (type == Resource.ResourceType.ORE) {
            imageView.setY(y - imageView.getFitHeight() / 2+15);
        } else if (type == Resource.ResourceType.GRAIN) {
            imageView.setY(y - imageView.getFitHeight() / 2+10);
        } else if (type == Resource.ResourceType.WOOL) {
            imageView.setY(y - imageView.getFitHeight() / 2+15);
        } else {
            imageView.setY(y - imageView.getFitHeight() / 2-20);
        }
        //imageView.setY(y - imageView.getFitHeight() / 2+20);
        return imageView;
    }

}
