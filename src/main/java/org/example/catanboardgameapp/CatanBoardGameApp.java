package org.example.catanboardgameapp;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javafx.application.Application.launch;

public class CatanBoardGameApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        Board board = new Board(3,50,400,300);

        Group root = new Group();

        //Draw tiles
        for (Tile tile : board.getTiles()) {
            Polygon polygon = createTilePolygon(tile);
            //***********
            // ADD COLORS BASED ON RESOURCE TYPE
            //************
            polygon.setFill(Color.WHITE);
            polygon.setStroke(Color.BLACK);
            root.getChildren().add(polygon);
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

        Scene scene = new Scene(root, 800, 600, Color.LIGHTGRAY);
        primaryStage.setTitle("Catan Board Game");
        primaryStage.setScene(scene);
        primaryStage.show();




    }







    // Creates polygons as tiles based on the vertices in each tile.
    private Polygon createTilePolygon(Tile tile) {
        Polygon polygon = new Polygon();
        for (Vertex vertex : tile.getVertices()) {
            polygon.getPoints().addAll(vertex.getX(), vertex.getY());
        }
        return polygon;
    }

    public static void main(String[] args) {
        launch(args);

    }
}
