package org.example.catanboardgameapp;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.catanboardgameviews.CatanBoardGameView;

import java.util.*;

import static org.example.catanboardgameviews.CatanBoardGameView.*;

public class Robber {

    //____________________FIELDS__________________________

    public static Robber robberDeNiro;  //PUBLIC ROBBER FOR ALL OTHER CLASSES TO USE (only need ONE)
    private final Gameplay gameplay;
    private final Circle robberCircle;
    private Tile currentTile;
    private static boolean robberNeedsToMove = false;

    //___________________________CONSTRUCTOR___________________________
    public Robber(Tile startingTile, Gameplay gameplay, Group boardGroup) {
        this.currentTile = startingTile;
        this.gameplay = gameplay;

        // Initialize robber circle
        Point2D center = startingTile.getCenter();
        double radius = 50.0 / CatanBoardGameView.boardRadius;
        Circle circle = new Circle(center.getX(), center.getY(), radius, Color.TRANSPARENT);
        circle.setStroke(Color.BLACK);
        circle.setStrokeWidth(5);
        // add circle to starting board
        this.robberCircle = circle;
        boardGroup.getChildren().add(circle);
    }

    //___________________________FUNCTIONS___________________________

    // MANGLER AT BLIVE KALDT / BRUGT
    public void discardResourcesForPlayer(Player player, Map<String, Integer> discarded) {
        discarded.forEach((res, amt) -> {
            int current = player.getResources().getOrDefault(res, 0);
            player.getResources().put(res, Math.max(0, current - amt));
        });
    }
    // Move robber
    public void moveTo(Tile newTile) {
        this.currentTile = newTile;
    }

    // Highlights possible robber placement targets and handles stealing logic
    public static void showRobberTargets(Group boardGroup) {
        boardGroup.getChildren().remove(robberDeNiro.robberCircle); // remove initial robber
        List<Circle> robberTargetCircles = new ArrayList<>();
        for (Tile tile : Board.getTiles()) {
            // Remove the previous robber circle if present
            boardGroup.getChildren().remove(currentRobberCircle);
            // Skip current tile where robber is already located
            if (tile == robberDeNiro.currentTile) continue;

            Point2D center = tile.getCenter();
            double radius = 50.0 / boardRadius;

            Circle circle = new Circle(center.getX(), center.getY(), radius, Color.TRANSPARENT);
            circle.setStroke(Color.BLACK);
            circle.setStrokeWidth(5);

            circle.setOnMouseClicked(e -> {
                // Clear previous highlights
                for (Circle c : robberTargetCircles) boardGroup.getChildren().remove(c);
                robberTargetCircles.clear();

                // Mark the new robber position
                Circle newRobberMarker = new Circle(center.getX(), center.getY(), radius, Color.TRANSPARENT);
                newRobberMarker.setStroke(Color.BLACK);
                newRobberMarker.setStrokeWidth(5);
                boardGroup.getChildren().add(newRobberMarker);
                currentRobberCircle = newRobberMarker;

                // Move the robber
                robberDeNiro.moveTo(tile);
                robberHasMoved();
                nextTurnButton.setVisible(true);

                // Prompt for stealing from valid victims
                List<Player> victims = showPotentialVictims(tile, robberDeNiro.gameplay.getCurrentPlayer());

                if (victims.isEmpty()) {
                    CatanBoardGameView.logToGameLog("Bad Robber placement! No players to steal from");
                    return;
                }

                ChoiceDialog<Player> dialog = new ChoiceDialog<>(victims.get(0), victims);
                dialog.setTitle("Choose a player to steal from");
                dialog.setHeaderText("Select a player with a city/settlement on this tile:");
                dialog.setContentText("Player:");

                Optional<Player> result = dialog.showAndWait();
                result.ifPresent(victim -> {
                    boolean success = stealResourceFrom(victim);
                    if (!success) {
                        CatanBoardGameView.logToGameLog("Failed to steal a resource from " + victim);
                    }

                    // Refresh left player menu to show updated resources
                    VBox updatedMenu = createLeftMenu(robberDeNiro.gameplay);
                    ((BorderPane) boardGroup.getScene().getRoot()).setLeft(updatedMenu);
                });
            });

            boardGroup.getChildren().add(circle);
            robberTargetCircles.add(circle);
        }
    }

    // show list of players that can be stolen from
    private static List<Player> showPotentialVictims(Tile tile, Player currentPlayer) {
        Set<Player> potentialVictims = new HashSet<>();

        for (Vertex vertex : tile.getVertices()) {
            Player owner = vertex.getOwner();
            if (owner != null && owner != currentPlayer) {
                potentialVictims.add(owner);
            }
        }
        return new ArrayList<>(potentialVictims);
    }

    // Prompts a player to discard half their resources when a 7 is rolled
    private static Map<String, Integer> showDiscardDialog(Player player, Gameplay gameplay) {
        Map<String, Integer> playerResources = new HashMap<>(player.getResources());
        int totalCards = playerResources.values().stream().mapToInt(Integer::intValue).sum();
        int cardsToDiscard = totalCards / 2;

        if (cardsToDiscard == 0) return null;

        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.UNDECORATED);
        dialogStage.setTitle("Discard Resources");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(100);
        grid.getColumnConstraints().addAll(labelCol, new ColumnConstraints(), new ColumnConstraints(), new ColumnConstraints());

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
                        gameplay.getTotalSelectedCards(discardSelection) < cardsToDiscard) {
                    discardSelection.put(resource, discardSelection.get(resource) + 1);
                    counter.setText(String.valueOf(discardSelection.get(resource)));
                    discardButton.setDisable(gameplay.getTotalSelectedCards(discardSelection) != cardsToDiscard);
                }
            });

            minus.setOnAction(e -> {
                if (discardSelection.get(resource) > 0) {
                    discardSelection.put(resource, discardSelection.get(resource) - 1);
                    counter.setText(String.valueOf(discardSelection.get(resource)));
                    discardButton.setDisable(gameplay.getTotalSelectedCards(discardSelection) != cardsToDiscard);
                }
            });

            grid.add(label, 0, row);
            grid.add(minus, 1, row);
            grid.add(counter, 2, row);
            grid.add(plus, 3, row);
            row++;
        }

        Map<String, Integer>[] result = new Map[]{null}; // container for return value
        discardButton.setOnAction(e -> {
            result[0] = discardSelection;
            dialogStage.close();
        });

        VBox container = new VBox(15,
                new Text(player + " you must discard " + cardsToDiscard + " resource cards."),
                grid,
                discardButton
        );
        container.setPadding(new Insets(15));
        container.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-width: 1;");

        Scene scene = new Scene(container);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();

        return result[0];
    }

    public boolean isRobberMovementRequired() {
        return robberNeedsToMove;
    }

    public void requireRobberMove() {
        robberNeedsToMove = true;
        for (Player player : gameplay.getPlayerList()) {
            int totalCards = player.getResources().values().stream().mapToInt(Integer::intValue).sum();
            if (totalCards > 7) {
                Map<String, Integer> discarded = showDiscardDialog(player, gameplay);
                if (discarded != null) {
                    discardResourcesForPlayer(player, discarded);
                }
            }
        }
    }

    public static void robberHasMoved() {
        robberNeedsToMove = false;
    }

    public static boolean stealResourceFrom(Player victim) {
        List<String> pool = new ArrayList<>();
        victim.getResources().forEach((res, count) -> {
            for (int i = 0; i < count; i++) pool.add(res);
        });
        if (pool.isEmpty()) return false;
        Collections.shuffle(pool);
        String stolen = pool.get(0);
        victim.getResources().put(stolen, victim.getResources().get(stolen) - 1);
        robberDeNiro.gameplay.getCurrentPlayer().getResources().put(stolen, robberDeNiro.gameplay.getCurrentPlayer().getResources().getOrDefault(stolen, 0) + 1);
        CatanBoardGameView.logToGameLog("Player " + robberDeNiro.gameplay.getCurrentPlayer().getPlayerId() + " stole 1 " + stolen + " from Player " + victim.getPlayerId());
        return true;
    }
}