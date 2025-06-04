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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.catanboardgameviews.CatanBoardGameView;
import org.example.controller.BuildController;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class DrawOrDisplay {
    private final int boardRadius;
    private final List<Circle> vertexClickables = new ArrayList<>();


    //___________________________CONSTRUCTOR___________________________//
    public DrawOrDisplay(int boardRadius) {
        this.boardRadius = boardRadius;
    }


    //______________________________SETTLEMENTS & ROADS_________________//

    // Draw a player's road with visual thickness tuned to avoid overpowering board
    public void drawPlayerRoad(Line line, Player player) {
        line.setStroke(player.getColor());
        line.setStrokeWidth(1.5 * (10.0 / boardRadius)); // thinner than before
    }

    // Draw a player's settlement or city with tuned radius
    public void drawPlayerSettlement(Circle circle, Vertex vertex) {
        if (vertex.getOwner() != null) {
            circle.setFill(vertex.getOwner().getColor());
            circle.setRadius(20.0 / boardRadius); // slightly larger
        } else {
            circle.setFill(Color.TRANSPARENT);
            circle.setRadius(10.0 / boardRadius);
        }
    }

    //______________________________EDGES & VERTICES_____________________________________

    // Draws board edges and clickable hitboxes
    public void initEdgesClickHandlers(Board board, Group boardGroup, BuildController controller, int radius, BorderPane root) {
        Group edgeBaseLayer = controller.getGameController().getGameView().getEdgeBaseLayer();
        Group edgeClickLayer = controller.getGameController().getGameView().getEdgeClickLayer();

        for (Edge edge : board.getEdges()) {
            if (edge.isSeaOnly()) continue; // Skip sea-only edges
            Line visible = new Line(
                    edge.getVertex1().getX(), edge.getVertex1().getY(),
                    edge.getVertex2().getX(), edge.getVertex2().getY()
            );
            visible.setStroke(Color.WHITE);
            visible.setStrokeWidth(0.8 * (10.0 / boardRadius));

            Line clickable = new Line(
                    edge.getVertex1().getX(), edge.getVertex1().getY(),
                    edge.getVertex2().getX(), edge.getVertex2().getY()
            );
            clickable.setStrokeWidth(1.2 * (10.0 / boardRadius));

            clickable.setOpacity(0);
            clickable.setPickOnBounds(false);
            clickable.setMouseTransparent(false);
            clickable.setOnMouseClicked(controller.createRoadClickHandler(edge, visible, root));

            edgeBaseLayer.getChildren().add(visible);
            edgeClickLayer.getChildren().add(clickable);
        }
    }

    // Enlarged clickable vertex area for easier settlement placement
    public void initVerticeClickHandlers(Board board, Group boardGroup, BuildController controller, int radius, BorderPane root) {
        Group settlementLayer = controller.getGameController().getGameView().getSettlementLayer();
        Group edgeClickLayer = controller.getGameController().getGameView().getEdgeClickLayer();

        boolean DEBUG_VISUALIZE_CLICKS = true; // Turn off when fixed

        for (Vertex vertex : board.getVertices()) {
            if (vertex.isSeaOnly()) continue; // Skip sea-only vertices
            double visibleRadius = 10.0 / boardRadius;
            double clickableRadius = 20.0 / boardRadius;

            // Visible circle — shown only when needed
            Circle visible = new Circle(vertex.getX(), vertex.getY(), visibleRadius);
            visible.setFill(Color.TRANSPARENT);
            visible.setStroke(Color.TRANSPARENT);
            settlementLayer.getChildren().add(visible);

            // Clickable hitbox
            Circle clickable = new Circle(vertex.getX(), vertex.getY(), clickableRadius);
            clickable.setPickOnBounds(true);
            clickable.setOnMouseClicked(controller.createSettlementClickHandler(visible, vertex, root));
            clickable.setMouseTransparent(false);
            vertexClickables.add(clickable);

            if (DEBUG_VISUALIZE_CLICKS) {
                clickable.setFill(Color.rgb(0, 255, 0, 0.2));  // Green transparent for debug
                clickable.setStroke(Color.BLACK);
                clickable.setStrokeWidth(0.3);
            } else {
                clickable.setFill(Color.TRANSPARENT);
                clickable.setStroke(Color.TRANSPARENT);
            }
            edgeClickLayer.getChildren().add(clickable);
        }
    }

    //______________________________TILE RENDERING_______________________________________

    // Create a hexagon polygon based on a tile's vertices
    public Polygon createTilePolygon(Tile tile) {
        Polygon polygon = new Polygon();
        for (Vertex v : tile.getVertices()) {
            polygon.getPoints().addAll(v.getX(), v.getY());
        }
        return polygon;
    }

    // Creates a background box behind a dice number, padding scaled by board size
    public Rectangle createBoxBehindDiceNumber(Text sample, double centerX, double centerY) {
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
    public Circle drawRobberCircle(Point2D center) {
        double radius = 40.0 / boardRadius;
        double strokeWidth = 12.0 / boardRadius;
        Circle circle = new Circle(center.getX(), center.getY(), radius);
        circle.setFill(Color.TRANSPARENT);
        circle.setStroke(Color.BLACK);
        circle.setStrokeWidth(strokeWidth);
        return circle;
    }

    public void drawHarbors(Board board, Group boardGroup) {
        for (Edge edge : board.getEdges()) {
            Harbor harbor = edge.getHarbor();
            if (harbor != null) {
                Point2D v1 = new Point2D(edge.getVertex1().getX(), edge.getVertex1().getY());
                Point2D v2 = new Point2D(edge.getVertex2().getX(), edge.getVertex2().getY());
                Point2D mid = v1.midpoint(v2);

                // Visual marker
                Circle circle = new Circle(mid.getX(), mid.getY(), 12.0 / boardRadius);
                circle.setFill(Color.PURPLE);
                circle.setStroke(Color.BLACK);

                // Optional label (abbreviated harbor type)
                String label = harbor.getType().specific != null
                        ? harbor.getType().specific.getName().substring(0, 1)
                        : "3:1";

                Text text = new Text(mid.getX(), mid.getY(), label);
                text.setFont(Font.font("Arial", FontWeight.BOLD, 10));
                text.setFill(Color.WHITE);
                text.setStroke(Color.BLACK);
                text.setX(text.getX() - text.getLayoutBounds().getWidth() / 2);
                text.setY(text.getY() + text.getLayoutBounds().getHeight() / 2);

                boardGroup.getChildren().addAll(circle, text);
            }
        }
    }


    //______________________________LOAD IMAGES______________________________

    public Image loadDiceImage(int number) {
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
    public void showErrorCross(Group boardGroup, double x, double y) {
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
    public void rollDiceBeforeActionPopup(String message) {
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
    public void showTradeError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Trade Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Build road fail too many roads
    public void showMaxRoadsReachedPopup() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Max Roads Reached");
            alert.setHeaderText("You already built 15 roads");
            alert.setContentText("You cannot build more than 15 roads in the game.");
            alert.showAndWait();
        });
    }

    // Build settlement fail too many settlements
    public void showMaxSettlementsReachedPopup() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Max Settlements Reached");
            alert.setHeaderText("You already built 5 settlements");
            alert.setContentText("You cannot build more than 5 settlements in the game.");
            alert.showAndWait();
        });
    }

    // Upgrade city fail too many cities
    public void showMaxCitiesReachedPopup() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Max Cities Reached");
            alert.setHeaderText("You already built 4 cities");
            alert.setContentText("You cannot build more than 4 roads in the game.");
            alert.showAndWait();
        });
    }

    // Fail to buy development card
    public void showFailToBuyDevelopmentCardPopup() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Insufficient Resources");
            alert.setHeaderText("You do not have enough resources to buy a development card");
            alert.setContentText("You need 1 grain, 1 wool, and 1 ore");
            alert.showAndWait();
        });
    }
    public void showNoMoreDevelopmentCardToBuyPopup() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("No More Development Dards");
            alert.setHeaderText("There are no more development cards left in the game.");
            alert.setContentText("Buy something else");
            alert.showAndWait();
        });
    }




    //_____________________________GETTERS__________________________//

    public List<Circle> getRegisteredVertexClickable() {
        return vertexClickables;
    }

}