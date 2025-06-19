package org.example.catanboardgameapp;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
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
import java.util.function.Supplier;

// Handles everything related to drawing on the board, settlements and cities, etc with updates
// and all displays and UI features seen in the game, popups, warnings etc.
public class DrawOrDisplay {

    // UI components
    private StackPane aiOverlayPane;
    private Label thinkingLabel;
    private ImageView thinkingImage;
    private RotateTransition rotateAnimation;

    // Click highlights
    private final List<Circle> vertexClickHighlights = new ArrayList<>();

    private final int boardRadius;

    //______________________________CONSTRUCTOR________________________________//
    public DrawOrDisplay(int boardRadius) {
        this.boardRadius = boardRadius;
    }

    //___________________________CLICK INITIALIZATION___________________________//
    public void initEdgesClickHandlers(Board board, BuildController controller) {
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
            clickable.setOnMouseClicked(controller.createRoadClickHandler(edge));
            edgeClickLayer.getChildren().add(clickable);
        }
    }

    public void initVerticeClickHandlers(Board board, BuildController controller, BorderPane root) {
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
    //_____________________________________FUNCTIONS_________________________________________//
    public Rectangle createBoxBehindDiceNumber(Text sample, double centerX, double centerY) {
        double padding = 5.0 / boardRadius * 10;
        double boxW = sample.getLayoutBounds().getWidth() + padding;
        double boxH = sample.getLayoutBounds().getHeight() + padding;
        Rectangle rectangle = new Rectangle(centerX - boxW / 2, centerY - boxH / 2, boxW, boxH);
        rectangle.setFill(Color.BEIGE);
        rectangle.setStroke(Color.BLACK);
        rectangle.setArcWidth(5);
        rectangle.setArcHeight(5);
        return rectangle;
    }
    public Circle createRobberHighlight(Tile tile, Group boardGroup, Runnable onClick) {
        Point2D center = tile.getCenter();
        Circle highlight = drawRobberCircle(center, boardGroup);
        highlight.setOnMouseClicked(e -> onClick.run());
        return highlight;
    }
    public Polygon createTilePolygon(Tile tile) {
        Polygon polygon = new Polygon();
        for (Vertex v : tile.getVertices()) {
            polygon.getPoints().addAll(v.getX(), v.getY());
        }
        return polygon;
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
    public StackPane buildAIOverlay() {
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

    //_____________________________________DRAWING_________________________________________//
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
    public void drawErrorCross(Group boardGroup, double x, double y) {
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

    //_____________________________________CREATE DIALOGS AND POPUPS_________________________________________//
    public void showEndGamePopup(
            Player winner,
            List<Player> playerList,
            int turnCounter,
            double gameWidth,
            double gameHeight,
            Runnable onClose
    ) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.UNDECORATED);
        popup.setTitle("Game Over");

        VBox content = new VBox(15);
        content.setPadding(new Insets(15));
        content.setAlignment(Pos.TOP_CENTER);
        content.setStyle("""
        -fx-background-color: linear-gradient(to bottom, #f9ecd1, #d2a86e);
        -fx-border-color: #8c5b1a;
        -fx-border-width: 2;
        -fx-border-radius: 10;
        -fx-background-radius: 10;
    """);

        Label header = new Label("Player " + winner.getPlayerId() + " has won the game! (It took them " + turnCounter + " turns)");
        header.setFont(Font.font("Georgia", FontWeight.BOLD, 18));
        header.setTextFill(Color.DARKGREEN);

        VBox playerStats = new VBox(12);
        playerStats.setPadding(new Insets(10));

        // Sort by score, descending
        List<Player> sortedPlayers = playerList.stream()
                .sorted((a, b) -> Integer.compare(b.getPlayerScore(), a.getPlayerScore()))
                .toList();

        for (Player player : sortedPlayers) {
            VBox box = new VBox(6);
            box.setPadding(new Insets(10));
            box.setStyle("""
            -fx-background-color: linear-gradient(to bottom, #f3e2c7, #e0b97d);
            -fx-border-color: #a86c1f;
            -fx-border-width: 1.5;
            -fx-background-radius: 8;
            -fx-border-radius: 8;
        """);

            String displayName = (player instanceof AIOpponent ai)
                    ? "AI Player " + ai.getPlayerId() + " (" + ai.getStrategyLevel().name() + ")"
                    : "Player " + player.getPlayerId();

            Text name = new Text(displayName + " : " + player.getPlayerScore() + " points");
            name.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
            name.setFill(player.getColor());
            box.getChildren().add(name);

            //Base stats
            int resources = player.getResources().values().stream().mapToInt(Integer::intValue).sum();
            int devCards = player.getDevelopmentCards().values().stream().mapToInt(Integer::intValue).sum();

            Text resText = new Text("Current Resources: " + resources);
            Text devText = new Text("Current Development Cards: " + devCards);
            Text lroadText = new Text("Longest Road: " + player.getLongestRoad());
            Text knightText = new Text("Biggest Army: " + player.getPlayedKnights());
            Text cityText = new Text("Cities: " + player.getCities().size());
            Text settlementText = new Text("Settlements: " + player.getSettlements().size());
            Text roadText = new Text("Roads: " + player.getRoads().size());

            List<Text> baseStats = List.of(
                    resText, devText, lroadText, knightText,
                    cityText, settlementText, roadText
            );
            baseStats.forEach(stat -> stat.setFont(Font.font("Georgia", 12)));
            box.getChildren().addAll(baseStats);

            if (player instanceof AIOpponent ai) {
                VBox strategyBox = new VBox(4);
                for (Map.Entry<AIOpponent.Strategy, Integer> entry : ai.getStrategyUsageMap().entrySet()) {
                    Text stat = new Text("* " + entry.getKey().name() + ": " + entry.getValue() + " times");
                    stat.setFont(Font.font("Georgia", 12));
                    strategyBox.getChildren().add(stat);
                }

                TitledPane togglePane = new TitledPane("Strategy Usage", strategyBox);
                togglePane.setExpanded(false);
                togglePane.setFont(Font.font("Georgia", FontWeight.NORMAL, 12));
                box.getChildren().add(togglePane);
            }
            playerStats.getChildren().add(box);
        }
        ScrollPane scrollPane = new ScrollPane(playerStats);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(500);
        scrollPane.setStyle("""
        -fx-background: transparent;
        -fx-border-color: #a86c1f;
        -fx-border-radius: 8;
    """);
        Button closeBtn = new Button("Back to Main Menu");
        closeBtn.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        closeBtn.setStyle("""
        -fx-background-color: linear-gradient(to bottom, #d8b173, #a86c1f);
        -fx-text-fill: black;
    """);
        closeBtn.setOnAction(e -> {
            popup.close();
            onClose.run();
        });
        content.getChildren().addAll(header, scrollPane, closeBtn);
        Scene scene = new Scene(content, gameWidth, gameHeight);
        popup.setScene(scene);
        popup.show();
    }

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

    public Optional<Player> showRobberVictimDialog(List<Player> victims) {
        if (victims == null || victims.isEmpty()) return Optional.empty();

        Dialog<Player> dialog = new Dialog<>();
        dialog.setTitle("Choose a player to steal from");
        dialog.setHeaderText("Select a player with a city or settlement on this tile:");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED); // Optional: remove window decorations

        // Disable default close behavior
        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.setOnCloseRequest(Event::consume);

        // UI Setup
        ComboBox<Player> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(victims);
        comboBox.getSelectionModel().selectFirst();

        dialog.getDialogPane().setContent(new VBox(10,
                new Label("Player:"), comboBox
        ));

        ButtonType confirmType = new ButtonType("Steal", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(confirmType);

        Node confirmButton = dialog.getDialogPane().lookupButton(confirmType);
        confirmButton.setDisable(false); // Allow clicking, but we can add validation if needed

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmType) {
                return comboBox.getValue();
            }
            return null; // Not possible unless dialog is forcibly closed
        });
        Optional<Player> result = dialog.showAndWait();
        return result;
    }

    public void notEnoughResourcesPopup(String message) {
        showCustomPopup("Not enough resources", message, true);
    }

    public void rollDiceBeforeActionPopup(String message) {
        showCustomPopup("Action Blocked", message, true);
    }

    public void showAITurnPopup() {
        showAlert(Alert.AlertType.INFORMATION,"AI Turn in Progress",null,"Wait for AI to finish turn before making moves",null);
    }

    public void showTradeError(String message) {
        showAlert(Alert.AlertType.ERROR, "Trade Error", null, message,null);
    }

    public void showMaxRoadsReachedPopup() {
        showAlert(Alert.AlertType.ERROR, "Max Roads Reached", "You already built 15 roads", "You cannot build more than 15 roads in the game.",null);
    }
    public void showMaxSettlementsReachedPopup() {
        showAlert(Alert.AlertType.ERROR, "Max Settlements Reached", "You already built 5 settlements", "You cannot build more than 5 settlements in the game.",null);
    }

    public void showMaxCitiesReachedPopup() {
        showAlert(Alert.AlertType.ERROR, "Max Cities Reached", "You already built 4 cities", "You cannot build more than 4 roads in the game.", null);
    }

    public void showFailToBuyDevelopmentCardPopup() {
        showAlert(Alert.AlertType.ERROR, "Insufficient Resources", "You do not have enough resources to buy a development card", "You need 1 grain, 1 wool, and 1 ore", null);
    }

    public void showNoMoreDevelopmentCardToBuyPopup() {
        showAlert(Alert.AlertType.ERROR, "No More Development Cards", "There are no more development cards left in the game.", "Buy something else.", null);
    }

    public void showFinishDevelopmentCardActionPopup() {
        showAlert(Alert.AlertType.WARNING, "Action Required: Development Card", "Complete your development card action first.", "You must finish using your current development card before performing any other actions.", null);
    }

    public Map<String, Integer> showDiscardDialog(Player player, int toDiscard, Map<String, Integer> playerResources, Gameplay gameplay) {
        List<String> resources = new ArrayList<>(playerResources.keySet());
        return showResourceSelectionDialog(
                "Discard Resources",
                player + ", you must discard " + toDiscard + " resource cards.",
                resources,
                toDiscard,
                true,       // allowAutoSelection
                player::chooseDiscardCards, // auto-selection function
                playerResources
        );
    }

    public String showMonopolyDialog() {
        List<String> resources = Arrays.asList("Ore", "Wood", "Brick", "Grain", "Wool");

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Monopoly");
        dialog.setHeaderText("Select a resource to monopolize:");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("""
            -fx-background-color: linear-gradient(to bottom, #f9ecd1, #d2a86e);
            -fx-border-color: #8c5b1a;
            -fx-border-width: 2;
            -fx-border-radius: 8;
            -fx-background-radius: 8;
            -fx-font-family: 'Georgia';
            -fx-font-size: 14px;
            -fx-text-fill: #3e2b1f;
        """);
        // Prevent closing with X button
        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.setOnCloseRequest(Event::consume);

        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(resources);
        comboBox.getSelectionModel().selectFirst();

        dialog.getDialogPane().setContent(new VBox(10,
                new Label("Resource:"), comboBox
        ));

        ButtonType confirmType = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(confirmType);

        Node confirmButton = dialog.getDialogPane().lookupButton(confirmType);
        confirmButton.setDisable(false); // Always enabled (1 option only)

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmType) {
                return comboBox.getValue();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    public Map<String, Integer> showYearOfPlentyDialog(Map<String, Integer> playerResources) {
        List<String> resources = Arrays.asList("Ore", "Wood", "Brick", "Grain", "Wool");
        return showResourceSelectionDialog(
                "Year of Plenty",
                "Select exactly 2 resources to gain from the bank:",
                resources,
                2,
                false,
                null,
                playerResources
        );
    }

    //___________________________________POPUP HELPER FUNCTIONS____________________________________//
    public void showAlert(Alert.AlertType type, String title, String header, String content, Runnable onClose) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.setResizable(true);
            alert.getDialogPane().setPrefWidth(400);
            DialogPane pane = alert.getDialogPane();
            pane.setStyle("""
                -fx-background-color: linear-gradient(to bottom, #f9ecd1, #d2a86e);
                -fx-border-color: #8c5b1a;
                -fx-border-width: 2;
                -fx-font-family: 'Georgia';
                -fx-font-size: 13px;
                -fx-text-fill: #3e2b1f;
            """);
            if (onClose != null) {
                alert.setOnHidden(e -> onClose.run());
            }
            alert.showAndWait();
        });
    }
    public void showCustomPopup(String title, String message, boolean runLater) {
        Runnable task = () -> {
            Stage popup = new Stage();
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.setTitle(title);

            VBox box = new VBox(10);
            box.setPadding(new Insets(20));
            box.setAlignment(Pos.CENTER);
            box.setStyle("""
                -fx-background-color: linear-gradient(to bottom, #f9ecd1, #d2a86e);
                -fx-border-color: #8c5b1a;
                -fx-border-width: 2;
                -fx-border-radius: 10;
                -fx-background-radius: 10;
            """);
            Label label = new Label(message);
            Button closeButton = new Button("OK");
            closeButton.setOnAction(e -> popup.close());
            closeButton.setFont(Font.font("Georgia", FontWeight.BOLD, 13));

            box.getChildren().addAll(label, closeButton);

            Scene scene = new Scene(box);
            popup.setScene(scene);
            popup.showAndWait();
        };

        if (runLater) {
            Platform.runLater(task);
        } else {
            task.run();
        }
    }
    // Helper function to create NON-closable Dialog boxes for multiple recourse selections
    private Map<String, Integer> showResourceSelectionDialog(
            String title,
            String message,
            List<String> resources,
            int maxSelection,
            boolean allowAutoSelection,
            Supplier<Map<String, Integer>> autoSelectionSupplier,
            Map<String, Integer> ownedResourceMap
    ) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.UNDECORATED);
        dialogStage.setTitle(title);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.getColumnConstraints().addAll(
                new ColumnConstraints(100), // Resource label
                new ColumnConstraints(),    // Minus
                new ColumnConstraints(),    // Counter
                new ColumnConstraints(),    // Plus
                new ColumnConstraints(50)   // Owned count
        );

        Map<String, Integer> selection = new HashMap<>();
        Map<String, Text> counterTexts = new HashMap<>();
        Button confirmButton = new Button("Confirm");
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

            int owned = ownedResourceMap != null ? ownedResourceMap.getOrDefault(resource, 0) : -1;
            Text ownedLabel = new Text(owned >= 0 ? "(" + owned + ")" : "");
            ownedLabel.setWrappingWidth(30);
            ownedLabel.setTextAlignment(TextAlignment.CENTER);

            plus.setOnAction(e -> {
                if (selection.get(resource) < maxSelection && getTotalSelected(selection) < maxSelection) {
                    selection.put(resource, selection.get(resource) + 1);
                    counter.setText(String.valueOf(selection.get(resource)));
                    confirmButton.setDisable(getTotalSelected(selection) != maxSelection);
                }
            });
            minus.setOnAction(e -> {
                if (selection.get(resource) > 0) {
                    selection.put(resource, selection.get(resource) - 1);
                    counter.setText(String.valueOf(selection.get(resource)));
                    confirmButton.setDisable(getTotalSelected(selection) != maxSelection);
                }
            });
            grid.addRow(row++, label, minus, counter, plus, ownedLabel);
        }
        final Map<String, Integer>[] result = new Map[]{null};
        confirmButton.setOnAction(e -> {
            result[0] = new HashMap<>(selection);
            dialogStage.close();
        });

        VBox container = new VBox(15, new Text(message), grid);
        HBox buttons = new HBox(10, confirmButton);
        container.setStyle("""
            -fx-background-color: linear-gradient(to bottom, #f9ecd1, #d2a86e);
            -fx-border-color: #8c5b1a;
            -fx-border-width: 2;
            -fx-border-radius: 10;
            -fx-background-radius: 10;
        """);
        if (allowAutoSelection && autoSelectionSupplier != null) {
            Button autoButton = new Button("Auto-Discard");
            autoButton.setOnAction(e -> {
                Map<String, Integer> autoResult = autoSelectionSupplier.get();
                if (autoResult != null && !autoResult.isEmpty()) {
                    result[0] = new HashMap<>(autoResult);
                    dialogStage.close();
                }
            });
            buttons.getChildren().add(autoButton);
        }
        container.getChildren().add(buttons);
        container.setPadding(new Insets(15));
        dialogStage.setScene(new Scene(container));
        dialogStage.showAndWait();
        return result[0];
    }

    //__________________________AI OVERLAY FUNCTIONS______________________________//
    public void setThinkingMessage(String text) {
        thinkingLabel.setText(text);
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

    //___________________________________GETTERS____________________________________//
    public StackPane getOverlayPane() {
        return aiOverlayPane;
    }
    private int getTotalSelected(Map<String, Integer> map) {
        return map.values().stream().mapToInt(Integer::intValue).sum();
    }
}
