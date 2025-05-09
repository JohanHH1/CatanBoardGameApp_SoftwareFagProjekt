package org.example.catanboardgameapp;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.catanboardgameviews.CatanBoardGameView;
import org.example.controller.BuildController;

import java.io.InputStream;

import static org.example.catanboardgameviews.CatanBoardGameView.boardRadius;

public class DrawOrDisplay {

    //______________________________SETTLEMENTS & ROADS_________________________________

    // Draw a player's road with visual thickness tuned to avoid overpowering board
    public static void drawPlayerRoad(Line line, Player player) {
        line.setStroke(player.getColor());
        line.setStrokeWidth(1.5 * (10.0 / boardRadius)); // thinner than before
    }

    // Draw a player's settlement or city with tuned radius
    public static void drawPlayerSettlement(Circle circle, Vertex vertex) {
        if (vertex.getOwner() != null) {
            circle.setFill(vertex.getOwner().getColor());
            circle.setRadius(20.0 / boardRadius); // slightly larger
        } else {
            circle.setFill(Color.TRANSPARENT);
            circle.setRadius(10.0 / boardRadius);
        }
    }

    //______________________________EDGES & VERTICES_____________________________________

    // Draws board edges and clickable hitboxes — now with refined sizing
    public static void drawEdges(Board board, Group boardGroup, BuildController controller, int radius, BorderPane root) {
        for (Edge edge : Board.getEdges()) {
            Line visible = new Line(
                    edge.getVertex1().getX(), edge.getVertex1().getY(),
                    edge.getVertex2().getX(), edge.getVertex2().getY()
            );
            visible.setStroke(Color.WHITE);
            visible.setStrokeWidth(0.8 * (10.0 / boardRadius)); // thinner base line

            Line clickable = new Line(
                    edge.getVertex1().getX(), edge.getVertex1().getY(),
                    edge.getVertex2().getX(), edge.getVertex2().getY()
            );
            clickable.setStrokeWidth(4.0 * (10.0 / boardRadius)); // tighter hitbox
            clickable.setOpacity(0);
            clickable.setOnMouseClicked(controller.createRoadClickHandler(edge, visible, root));

            boardGroup.getChildren().addAll(visible, clickable);
        }
    }

    // Enlarged clickable vertex area for easier settlement placement
    public static void initVerticeClickHandlers(Board board, Group boardGroup, BuildController controller, int radius, BorderPane root) {
        for (Vertex vertex : board.getVertices()) {
            double visibleRadius = 10.0 / boardRadius;
            double clickableRadius = 10.0 / boardRadius; // increased for UX

            Circle visible = new Circle(vertex.getX(), vertex.getY(), visibleRadius);
            visible.setFill(Color.TRANSPARENT);
            boardGroup.getChildren().add(visible);

            Circle clickable = new Circle(vertex.getX(), vertex.getY(), clickableRadius);
            clickable.setFill(Color.TRANSPARENT);
            clickable.setStroke(Color.TRANSPARENT);
            clickable.setOnMouseClicked(controller.createSettlementClickHandler(visible, vertex, root));

            boardGroup.getChildren().add(clickable);
        }
    }

    //______________________________TILE RENDERING_______________________________________

    // Create a hexagon polygon based on a tile's vertices
    public static Polygon createTilePolygon(Tile tile) {
        Polygon polygon = new Polygon();
        for (Vertex v : tile.getVertices()) {
            polygon.getPoints().addAll(v.getX(), v.getY());
        }
        return polygon;
    }

    // Creates a background box behind a dice number, padding scaled by board size
    public static Rectangle createBoxBehindDiceNumber(Text sample, double centerX, double centerY) {
        double padding = 5.0 / boardRadius * 10;

        double boxW = sample.getLayoutBounds().getWidth() + padding;
        double boxH = sample.getLayoutBounds().getHeight() + padding;

        Rectangle background = new Rectangle(
                centerX - boxW / 2,
                centerY - boxH / 2,
                boxW,
                boxH
        );
        background.setFill(Color.BEIGE);
        background.setStroke(Color.BLACK);
        background.setArcWidth(5);
        background.setArcHeight(5);

        return background;
    }

    // Draws Robber Circle
    public static Circle drawRobberCircle(Point2D center) {
        double radius = 40.0 / boardRadius;
        double strokeWidth = 12.0 / boardRadius;
        Circle circle = new Circle(center.getX(), center.getY(), radius);
        circle.setFill(Color.TRANSPARENT);
        circle.setStroke(Color.BLACK);
        circle.setStrokeWidth(strokeWidth);
        return circle;
    }

    //______________________________LOAD IMAGES______________________________

    public static Image loadDiceImage(int number) {
        String path = "/dice/dice" + number + ".png";
        InputStream stream = CatanBoardGameView.class.getResourceAsStream(path);
        if (stream == null) {
            System.err.println("Could not load image: " + path);
            return new Image("/Icons/error.png");
        }
        return new Image(stream);
    }

    //______________________________ERROR POPUPS ETC______________________________

    // Display a red X error marker at a location, scaled by board radius
    public static void showErrorCross(Group boardGroup, double x, double y) {
        double size = 10.0 / boardRadius;

        Line line1 = new Line(x - size, y - size, x + size, y + size);
        Line line2 = new Line(x - size, y + size, x + size, y - size);

        line1.setStroke(Color.RED);
        line2.setStroke(Color.RED);
        line1.setStrokeWidth(2);
        line2.setStrokeWidth(2);

        Group error = new Group(line1, line2);
        boardGroup.getChildren().add(error);

        PauseTransition delay = new PauseTransition(Duration.seconds(1));
        delay.setOnFinished(e -> boardGroup.getChildren().remove(error));
        delay.play();
    }

    // Block action until dice is rolled
    public static void rollDiceBeforeActionPopup(String message) {
        Platform.runLater(() -> {
            Stage popup = new Stage();
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.setTitle("Action Blocked");

            VBox box = new VBox(10);
            box.setPadding(new Insets(20));
            box.setAlignment(Pos.CENTER);

            Label label = new Label(message);
            Button closeButton = new Button("OK");
            closeButton.setOnAction(e -> popup.close());

            box.getChildren().addAll(label, closeButton);
            Scene scene = new Scene(box);
            popup.setScene(scene);
            popup.showAndWait();
        });
    }

    // Trade fail popup with message
    public static void showTradeError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Trade Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
