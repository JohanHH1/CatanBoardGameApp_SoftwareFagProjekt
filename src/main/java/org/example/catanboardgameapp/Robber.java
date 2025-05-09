package org.example.catanboardgameapp;

import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.layout.*;
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

    public static Robber robberDeNiro;  // Global robber reference
    private final Gameplay gameplay;
    private final Circle robberCircle;
    private Tile currentTile;
    private static boolean robberNeedsToMove = false;

    //____________________CONSTRUCTOR__________________________

    public Robber(Tile startingTile, Gameplay gameplay, Group boardGroup) {
        this.currentTile = startingTile;
        this.gameplay = gameplay;

        Point2D center = startingTile.getCenter();
        Circle circle = DrawOrDisplay.drawRobberCircle(center); // Use central method
        this.robberCircle = circle;
        boardGroup.getChildren().add(circle);
    }

    //____________________ROBBER PLACEMENT LOGIC__________________________

    public static void showRobberTargets(Group boardGroup) {
        boardGroup.getChildren().remove(robberDeNiro.robberCircle); // remove old robber

        List<Circle> highlightCircles = new ArrayList<>();

        for (Tile tile : Board.getTiles()) {
            if (tile == robberDeNiro.currentTile) continue;

            Point2D center = tile.getCenter();
            Circle highlight = DrawOrDisplay.drawRobberCircle(center);
            highlight.setOnMouseClicked(e -> {
                highlightCircles.forEach(boardGroup.getChildren()::remove);
                highlightCircles.clear();

                Circle newRobber = DrawOrDisplay.drawRobberCircle(center);
                boardGroup.getChildren().add(newRobber);
                currentRobberCircle = newRobber;

                robberDeNiro.moveTo(tile);
                robberHasMoved();
                nextTurnButton.setVisible(true);

                List<Player> victims = showPotentialVictims(tile, robberDeNiro.gameplay.getCurrentPlayer());
                if (victims.isEmpty()) {
                    logToGameLog("Bad Robber placement! No players to steal from");
                    return;
                }

                ChoiceDialog<Player> dialog = new ChoiceDialog<>(victims.get(0), victims);
                dialog.setTitle("Choose a player to steal from");
                dialog.setHeaderText("Select a player with a city/settlement on this tile:");
                dialog.setContentText("Player:");

                dialog.showAndWait().ifPresent(victim -> {
                    boolean success = stealResourceFrom(victim);
                    if (!success) {
                        logToGameLog("Failed to steal a resource from " + victim);
                    }
                    VBox updatedMenu = createLeftMenu(robberDeNiro.gameplay);
                    ((BorderPane) boardGroup.getScene().getRoot()).setLeft(updatedMenu);
                });
            });

            boardGroup.getChildren().add(highlight);
            highlightCircles.add(highlight);
        }
    }

    private static List<Player> showPotentialVictims(Tile tile, Player currentPlayer) {
        Set<Player> victims = new HashSet<>();
        for (Vertex v : tile.getVertices()) {
            Player owner = v.getOwner();
            if (owner != null && owner != currentPlayer) victims.add(owner);
        }
        return new ArrayList<>(victims);
    }

    //____________________CARD DISCARD HANDLER__________________________

    public void requireRobberMove() {
        robberNeedsToMove = true;
        for (Player p : gameplay.getPlayerList()) {
            int total = p.getResources().values().stream().mapToInt(Integer::intValue).sum();
            if (total > 7) {
                Map<String, Integer> discarded = showDiscardDialog(p, gameplay);
                if (discarded != null) discardResourcesForPlayer(p, discarded);
            }
        }
    }

    private static Map<String, Integer> showDiscardDialog(Player player, Gameplay gameplay) {
        Map<String, Integer> playerResources = new HashMap<>(player.getResources());
        int totalCards = playerResources.values().stream().mapToInt(Integer::intValue).sum();
        int toDiscard = totalCards / 2;

        if (toDiscard == 0) return null;

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
            result[0] = discardSelection;
            dialogStage.close();
        });

        VBox container = new VBox(15,
                new Text(player + " you must discard " + toDiscard + " resource cards."),
                grid,
                discardButton
        );
        container.setPadding(new Insets(15));
        container.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-width: 1;");

        dialogStage.setScene(new Scene(container));
        dialogStage.showAndWait();

        return result[0];
    }

    //____________________HELPERS__________________________

    public boolean isRobberMovementRequired() {
        return robberNeedsToMove;
    }

    public static void robberHasMoved() {
        robberNeedsToMove = false;
    }

    public void moveTo(Tile newTile) {
        this.currentTile = newTile;
    }

    public void discardResourcesForPlayer(Player player, Map<String, Integer> discarded) {
        discarded.forEach((res, amt) -> {
            int current = player.getResources().getOrDefault(res, 0);
            player.getResources().put(res, Math.max(0, current - amt));
        });
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

        Player thief = robberDeNiro.gameplay.getCurrentPlayer();
        thief.getResources().put(stolen, thief.getResources().getOrDefault(stolen, 0) + 1);

        logToGameLog("Player " + thief.getPlayerId() + " stole 1 " + stolen + " from Player " + victim.getPlayerId());
        return true;
    }
}
