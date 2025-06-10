package org.example.catanboardgameapp;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.example.catanboardgameviews.CatanBoardGameView;
import org.example.controller.BuildController;

import java.io.InputStream;
import java.util.*;

public class DrawOrDisplay {

    private final int boardRadius;
    private final List<Circle> vertexClickHighlights = new ArrayList<>();
    private final List<Line> edgeClickHighlights = new ArrayList<>();
    private static int settlementCounter = 0;
    private static final boolean SHOW_SETTLEMENT_ORDER = true;
    private StackPane aiOverlayPane;
    private Label thinkingLabel;
    private ImageView thinkingImage;
    private RotateTransition rotateAnimation;

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
        } else {
            System.out.println("SOMETHING WRONG DRAWING WITHOUT ANY PLAYER AS OWNER???");
            circle.setFill(Color.TRANSPARENT);
            circle.setRadius(10.0 / boardRadius);
        }

        // 1. Add circle
        boardGroup.getChildren().add(circle);

        // 2. Add label *after* road is drawn, and ensure it’s last

        if (SHOW_SETTLEMENT_ORDER && vertex.getOwner() != null) {
            settlementCounter++;
            Text label = new Text(String.valueOf(settlementCounter));
            label.setFill(Color.BLACK);
            label.setStyle("-fx-font-weight: bold;");
            label.applyCss();
            label.setX(vertex.getX() - label.getLayoutBounds().getWidth() / 2);
            label.setY(vertex.getY() + label.getLayoutBounds().getHeight() / 4);

            // Add *after* all game objects
            Platform.runLater(() -> boardGroup.getChildren().add(label));
        }

    }
    public void drawCity(Vertex vertex, Group boardGroup) {
        double radius = 24.0 / boardRadius;
        Polygon cityShape = new Polygon();
        double x = vertex.getX();
        double y = vertex.getY();

        // Example shape: a hexagon to represent a city
        double height = radius * Math.sqrt(3) / 2;

        double yOffset = 0.0; // pixels downward

        cityShape.getPoints().addAll(
                x,             y - radius + yOffset,
                x + height,    y - radius / 2 + yOffset,
                x + height,    y + radius / 2 + yOffset,
                x,             y + radius + yOffset,
                x - height,    y + radius / 2 + yOffset,
                x - height,    y - radius / 2 + yOffset
        );

        if (vertex.getOwner() != null) {
            cityShape.setFill(vertex.getOwner().getColor());
        } else {
            cityShape.setFill(Color.GRAY);
        }
        cityShape.setStroke(Color.BLACK);
        cityShape.setStrokeWidth(1.0);
        boardGroup.getChildren().add(cityShape);
    }


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

    public Circle createRobberHighlight(Tile tile, Group boardGroup, Runnable onClick) {
        Point2D center = tile.getCenter();
        Circle highlight = drawRobberCircle(center, boardGroup);
        highlight.setOnMouseClicked(e -> onClick.run());
        return highlight;
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

    public void notEnoughResources(String message) {
        Platform.runLater(() -> {
            Stage popup = new Stage();
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.setTitle("Not enough resources");

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

    public void showAITurnPopup() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("AI Turn in Progress");
        alert.setHeaderText(null);
        alert.setContentText("Wait for AI to finish turn before making moves");
        alert.showAndWait();
    }

    public StackPane buildFancyAIOverlay() {
        thinkingLabel = new Label("Waiting for AI...");
        thinkingLabel.setStyle("-fx-font-size: 26px; -fx-text-fill: white; -fx-font-weight: bold;");
        thinkingLabel.setOpacity(0.85);

        Image img = new Image(getClass().getResource("/icons/robot_think.png").toExternalForm());
        thinkingImage = new ImageView(img);
        thinkingImage.setFitWidth(120);
        thinkingImage.setFitHeight(120);

        // Rotate animation
        rotateAnimation = new RotateTransition(Duration.seconds(4), thinkingImage);
        rotateAnimation.setByAngle(360);
        rotateAnimation.setCycleCount(Animation.INDEFINITE);
        rotateAnimation.setInterpolator(Interpolator.LINEAR);

        VBox content = new VBox(20, thinkingImage, thinkingLabel);
        content.setAlignment(Pos.CENTER);

        aiOverlayPane = new StackPane(content);
        aiOverlayPane.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6);");
        aiOverlayPane.setVisible(false);
        aiOverlayPane.setMouseTransparent(true);

        return aiOverlayPane;
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
    public void showFinishDevelopmentCardActionPopup() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Action Required: Development Card");
            alert.setHeaderText("Complete your development card action first.");
            alert.setContentText("You must finish using your current development card before performing any other actions.");
            alert.showAndWait();
        });
    }

    public Optional<Player> showRobberVictimDialog(List<Player> victims) {
        if (victims == null || victims.isEmpty()) return Optional.empty();

        ChoiceDialog<Player> dialog = new ChoiceDialog<>(victims.get(0), victims);
        dialog.setTitle("Choose a player to steal from");
        dialog.setHeaderText("Select a player with a city or settlement on this tile:");
        dialog.setContentText("Player:");

        return dialog.showAndWait();
    }

    public Map<String, Integer> showDiscardDialog(Player player, int toDiscard, Map<String, Integer> playerResources, Gameplay gameplay) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.UNDECORATED);
        dialogStage.setTitle("Discard Resources");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.getColumnConstraints().addAll(new ColumnConstraints(100), new ColumnConstraints(), new ColumnConstraints(), new ColumnConstraints());

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
                        gameplay.getTotalSelectedCards(discardSelection) < toDiscard) {
                    discardSelection.put(resource, discardSelection.get(resource) + 1);
                    counter.setText(discardSelection.get(resource).toString());
                    discardButton.setDisable(gameplay.getTotalSelectedCards(discardSelection) != toDiscard);
                }
            });
            minus.setOnAction(e -> {
                if (discardSelection.get(resource) > 0) {
                    discardSelection.put(resource, discardSelection.get(resource) - 1);
                    counter.setText(discardSelection.get(resource).toString());
                    discardButton.setDisable(gameplay.getTotalSelectedCards(discardSelection) != toDiscard);
                }
            });

            grid.addRow(row++, label, minus, counter, plus);
        }
        final Map<String, Integer>[] result = new Map[]{null};

        discardButton.setOnAction(e -> {
            result[0] = new HashMap<>(discardSelection);
            dialogStage.close();
        });

        Button randomDiscardButton = new Button("Discard cards randomly");
        randomDiscardButton.setOnAction(e -> {
            result[0] = player.chooseDiscardCards();
            dialogStage.close();
        });

        VBox container = new VBox(15,
                new Text(player + " you must discard " + toDiscard + " resource cards."),
                grid,
                new HBox(10, discardButton, randomDiscardButton)
        );
        container.setPadding(new Insets(15));
        container.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-width: 1;");

        dialogStage.setScene(new Scene(container));
        dialogStage.showAndWait();
        return result[0];

    }


    public Map<String, Integer> showYearOfPlentyDialog() {
        List<String> resources = Arrays.asList("Ore", "Wood", "Brick", "Grain", "Wool");

        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.UNDECORATED);
        dialogStage.setTitle("Year of Plenty");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.getColumnConstraints().addAll(
                new ColumnConstraints(100), new ColumnConstraints(), new ColumnConstraints(), new ColumnConstraints()
        );

        Map<String, Integer> selection = new HashMap<>();
        Map<String, Text> counterTexts = new HashMap<>();
        Button confirmButton = new Button("Take Resources");
        confirmButton.setDisable(true);

        int row = 0;
        for (String resource : resources) {
            selection.put(resource, 0);

            Text label = new Text(resource);
            Button minus = new Button("-");
            Button plus = new Button("+");
            Text counter = new Text("0");

            counter.setWrappingWidth(30);
            counter.setTextAlignment(TextAlignment.CENTER);
            counterTexts.put(resource, counter);

            plus.setOnAction(e -> {
                if (selection.get(resource) < 2 && getTotalSelected(selection) < 2) {
                    selection.put(resource, selection.get(resource) + 1);
                    counter.setText(selection.get(resource).toString());
                    confirmButton.setDisable(getTotalSelected(selection) != 2);
                }
            });

            minus.setOnAction(e -> {
                if (selection.get(resource) > 0) {
                    selection.put(resource, selection.get(resource) - 1);
                    counter.setText(selection.get(resource).toString());
                    confirmButton.setDisable(getTotalSelected(selection) != 2);
                }
            });

            grid.addRow(row++, label, minus, counter, plus);
        }

        VBox container = new VBox(15,
                new Text("Select exactly 2 resources to gain from the bank:"),
                grid,
                confirmButton
        );
        container.setPadding(new Insets(15));
        container.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-width: 1;");

        final Map<String, Integer>[] selected = new Map[]{null};
        confirmButton.setOnAction(e -> {
            selected[0] = new HashMap<>(selection);
            dialogStage.close();
        });

        dialogStage.setScene(new Scene(container));
        dialogStage.showAndWait();
        return selected[0];
    }

    private int getTotalSelected(Map<String, Integer> map) {
        return map.values().stream().mapToInt(Integer::intValue).sum();
    }

    public void resetCounters() {
        settlementCounter = 0;
    }

    public Label getThinkingLabel() {
        return thinkingLabel;
    }

    // AI THINKING OVERLAY FUNCTIONS:
    public void setThinkingMessage(String text) {
        thinkingLabel.setText(text);
    }

    public StackPane getOverlayPane() {
        return aiOverlayPane;
    }

    public void startThinkingAnimation() {
        rotateAnimation.playFromStart();
    }

    public void stopThinkingAnimation() {
        rotateAnimation.stop();
    }

    public void pauseThinkingAnimation(DrawOrDisplay draw) {
        if (rotateAnimation != null) {
            draw.rotateAnimation.pause();
        }
    }

    public void resumeThinkingAnimation(DrawOrDisplay draw) {
        if (rotateAnimation != null) {
            draw.rotateAnimation.play();
        }
    }

    // ------------------------- Getter ------------------------- //

}
