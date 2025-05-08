package org.example.catanboardgameapp;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.catanboardgameviews.CatanBoardGameView;
import org.example.controller.BuildController;

import java.io.InputStream;

import static org.example.catanboardgameviews.CatanBoardGameView.boardRadius;

public class DrawOrDisplay {

    //_______________________________CatanBoardGameView draw/display functions______________________________________

    // Creates a background rectangle behind each dice number
    public static Rectangle createBoxBehindDiceNumber(Text sample, double centerX, double centerY) {
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
    public static void drawEdges(Board board, Group boardGroup, BuildController controller, int radius, BorderPane root) {
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
    public static void displayVertices(Board board, Group boardGroup, BuildController controller, int radius, BorderPane root) {
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
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Trade Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Creates a hex tile polygon from the tile's vertices
    public static Polygon createTilePolygon(Tile tile) {
        Polygon polygon = new Polygon();
        for (Vertex vertex : tile.getVertices()) {
            polygon.getPoints().addAll(vertex.getX(), vertex.getY());
        }
        return polygon;
    }

    // Draw a player's road line
    public static void drawPlayerRoad(Line visibleLine, Player currentPlayer) {
        visibleLine.setStroke(currentPlayer.getColor());
        visibleLine.setStrokeWidth(4);
    }

    // Used to show build errors (similar to showTemporaryDot but red)
    public static void showBuildErrorDot(Group boardGroup, Point2D mid) {
        showTemporaryDot(boardGroup, mid.getX(), mid.getY(), Color.RED);
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
}