package org.example.catanboardgameviews;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import org.example.catanboardgameapp.Board;
import org.example.catanboardgameapp.Resource;
import org.example.catanboardgameapp.Tile;
import org.example.catanboardgameapp.Vertex;

import static javafx.application.Application.launch;

public class CatanBoardGameView {
    public static Scene createGameScene(Stage primaryStage, int radius) {
        Board board = new Board(radius,50,400,300);

        Group root = new Group();

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
            root.getChildren().add(polygon);

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
                root.getChildren().addAll(background, number);
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
            root.getChildren().add(circle);
        }

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
            case WOOD -> Color.FORESTGREEN;
            case ORE -> Color.DARKGRAY;
            case GRAIN -> Color.GOLD;
            case WOOL -> Color.LIGHTGREEN;
            case DESERT -> Color.BEIGE;
        };
    }
}
