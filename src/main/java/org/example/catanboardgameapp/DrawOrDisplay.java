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
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
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
    private final List<Circle> vertexClickHighlights = new ArrayList<>();
    private final List<Line> edgeClickHighlights = new ArrayList<>();
    private static int settlementCounter = 0;

    public DrawOrDisplay(int boardRadius) {
        this.boardRadius = boardRadius;
    }

    // ------------------------- Core Drawing ------------------------- //

    public void drawRoad(Line line, Player player, Group boardGroup) {
        line.setStroke(player.getColor());
        line.setStrokeWidth(1.5 * (10.0 / boardRadius));
        boardGroup.getChildren().add(line);
    }

    public void drawSettlement(Circle circle, Vertex vertex, Group boardGroup) {
        if (vertex.getOwner() != null) {
            circle.setFill(vertex.getOwner().getColor());
            circle.setRadius(20.0 / boardRadius);

            // Label with placement order
            settlementCounter++;
            Text label = new Text(String.valueOf(settlementCounter));
            label.setFill(Color.BLACK);
            label.setStyle("-fx-font-weight: bold;");
            label.setX(vertex.getX() - 4); // offset for centering
            label.setY(vertex.getY() + 4); // offset for vertical alignment

            boardGroup.getChildren().addAll(circle, label);
        } else {
            circle.setFill(Color.TRANSPARENT);
            circle.setRadius(10.0 / boardRadius);
            boardGroup.getChildren().add(circle);
        }
    }


    // -------------------- Initial Placement (Human) -------------------- //

    // -------------------- Click Initialization (Build Phase) -------------------- //

    public void initEdgesClickHandlers(Board board, Group boardGroup, BuildController controller, int radius, BorderPane root) {
        Group edgeBaseLayer = controller.getGameController().getGameView().getEdgeBaseLayer();
        Group edgeClickLayer = controller.getGameController().getGameView().getEdgeClickLayer();

        for (Edge edge : board.getEdges()) {
            if (edge.isSeaOnly()) continue;

            Line visible = new Line(edge.getVertex1().getX(), edge.getVertex1().getY(),
                    edge.getVertex2().getX(), edge.getVertex2().getY());
            visible.setStroke(Color.WHITE);
            visible.setStrokeWidth(0.8 * (10.0 / boardRadius));
            edgeBaseLayer.getChildren().add(visible);

            Line clickable = new Line(edge.getVertex1().getX(), edge.getVertex1().getY(),
                    edge.getVertex2().getX(), edge.getVertex2().getY());
            clickable.setStrokeWidth(1.2 * (10.0 / boardRadius));
            clickable.setOpacity(0);
            clickable.setPickOnBounds(false);
            clickable.setMouseTransparent(false);
            clickable.setOnMouseClicked(controller.createRoadClickHandler(edge, visible, root));
            edgeClickLayer.getChildren().add(clickable);
        }
    }

    public void initVerticeClickHandlers(Board board, Group boardGroup, BuildController controller, int radius, BorderPane root) {
        Group settlementLayer = controller.getGameController().getGameView().getSettlementLayer();
        Group edgeClickLayer = controller.getGameController().getGameView().getEdgeClickLayer();

        boolean DEBUG_VISUALIZE_CLICKS = true;

        for (Vertex vertex : board.getVertices()) {
            if (vertex.isSeaOnly()) continue;

            double visibleRadius = 10.0 / boardRadius;
            double clickableRadius = 20.0 / boardRadius;

            Circle visible = new Circle(vertex.getX(), vertex.getY(), visibleRadius);
            visible.setFill(Color.TRANSPARENT);
            visible.setStroke(Color.TRANSPARENT);
            settlementLayer.getChildren().add(visible);

            Circle clickable = new Circle(vertex.getX(), vertex.getY(), clickableRadius);
            clickable.setPickOnBounds(true);
            clickable.setOnMouseClicked(controller.createSettlementClickHandler(visible, vertex, root));
            clickable.setMouseTransparent(false);
            vertexClickHighlights.add(clickable);

            if (DEBUG_VISUALIZE_CLICKS) {
                clickable.setFill(Color.rgb(0, 255, 0, 0.2));
                clickable.setStroke(Color.BLACK);
                clickable.setStrokeWidth(0.3);
            } else {
                clickable.setFill(Color.TRANSPARENT);
                clickable.setStroke(Color.TRANSPARENT);
            }

            edgeClickLayer.getChildren().add(clickable);
        }
    }

    // ------------------------- Utility Drawing ------------------------- //

    public Polygon createTilePolygon(Tile tile) {
        Polygon polygon = new Polygon();
        for (Vertex v : tile.getVertices()) {
            polygon.getPoints().addAll(v.getX(), v.getY());
        }
        return polygon;
    }

    public Rectangle createBoxBehindDiceNumber(Text sample, double centerX, double centerY) {
        double padding = 5.0 / boardRadius * 10;
        double boxW = sample.getLayoutBounds().getWidth() + padding;
        double boxH = sample.getLayoutBounds().getHeight() + padding;
        Rectangle background = new Rectangle(centerX - boxW / 2, centerY - boxH / 2, boxW, boxH);
        background.setFill(Color.BEIGE);
        background.setStroke(Color.BLACK);
        background.setArcWidth(5);
        background.setArcHeight(5);
        return background;
    }

    public Circle drawRobberCircle(Point2D center, Group boardGroup) {
        double radius = 40.0 / boardRadius;
        double strokeWidth = 12.0 / boardRadius;
        Circle circle = new Circle(center.getX(), center.getY(), radius);
        circle.setFill(Color.TRANSPARENT);
        circle.setStroke(Color.BLACK);
        circle.setStrokeWidth(strokeWidth);
        boardGroup.getChildren().add(circle);
        return circle;
    }

    public void drawHarbors(List<Tile> tiles, Group boardGroup) {
        for (Tile tile : tiles) {
            Harbor harbor = tile.getHarbor();
            if (harbor == null) continue;

            Edge edge = harbor.getEdge();
            Vertex v1 = edge.getVertex1();
            Vertex v2 = edge.getVertex2();

            Point2D center = tile.getCenter();
            double centerX = center.getX();
            double centerY = center.getY();

            String text = (harbor.getType().specific == null)
                    ? "3:1"
                    : "2:1\n" + harbor.getType().specific.getName().toUpperCase();

            Text label = new Text(text);
            label.setFont(Font.font("Arial", FontWeight.BOLD, 30.0 / boardRadius));
            label.setTextAlignment(TextAlignment.CENTER);

            Text sample = new Text("2:1\nWOOL");
            sample.setFont(label.getFont());
            Rectangle box = createBoxBehindDiceNumber(sample, centerX, centerY);

            label.setX(centerX - label.getLayoutBounds().getWidth() / 2);
            label.setY(centerY + label.getLayoutBounds().getHeight() / 3);

            double dockWidth = 8.0 / boardRadius;
            Line dock1 = new Line(v1.getX(), v1.getY(), centerX, centerY);
            Line dock2 = new Line(v2.getX(), v2.getY(), centerX, centerY);
            dock1.setStroke(Color.BLACK);
            dock2.setStroke(Color.BLACK);
            dock1.setStrokeWidth(dockWidth);
            dock2.setStrokeWidth(dockWidth);

            boardGroup.getChildren().addAll(dock1, dock2, box, label);
        }
    }

    public Image loadDiceImage(int number) {
        String path = "/dice/dice" + number + ".png";
        InputStream stream = CatanBoardGameView.class.getResourceAsStream(path);
        if (stream == null) {
            System.err.println("Could not load image: " + path);
            return new Image("/Icons/error.png");
        }
        return new Image(stream);
    }

    // ------------------------- ERRORS AND Popups ------------------------- //
    public void showBuildingCostsPopup() {
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
        imageView.setFitWidth(400);

        VBox layout = new VBox(imageView);
        layout.setPadding(new Insets(10));

        Scene scene = new Scene(layout);
        popup.setScene(scene);
        popup.showAndWait();
    }

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

    public void showTradeError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Trade Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void showMaxRoadsReachedPopup() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Max Roads Reached");
            alert.setHeaderText("You already built 15 roads");
            alert.setContentText("You cannot build more than 15 roads in the game.");
            alert.showAndWait();
        });
    }

    public void showMaxSettlementsReachedPopup() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Max Settlements Reached");
            alert.setHeaderText("You already built 5 settlements");
            alert.setContentText("You cannot build more than 5 settlements in the game.");
            alert.showAndWait();
        });
    }

    public void showMaxCitiesReachedPopup() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Max Cities Reached");
            alert.setHeaderText("You already built 4 cities");
            alert.setContentText("You cannot build more than 4 roads in the game.");
            alert.showAndWait();
        });
    }

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
            alert.setTitle("No More Development Cards");
            alert.setHeaderText("There are no more development cards left in the game.");
            alert.setContentText("Buy something else.");
            alert.showAndWait();
        });
    }

    public void resetCounters() {
        settlementCounter = 0;
    }



    // ------------------------- Getter ------------------------- //

}
