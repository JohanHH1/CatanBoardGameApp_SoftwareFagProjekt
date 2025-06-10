package org.example.catanboardgameapp;

import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.catanboardgameviews.CatanBoardGameView;

import java.util.*;

public class Robber {

    //____________________FIELDS__________________________

    private final Gameplay gameplay;
    private Tile currentTile;
    private boolean robberNeedsToMove = false;
    private final DrawOrDisplay drawOrDisplay;
    private final CatanBoardGameView catanBoardGameView;
    private final Board board;
    private Circle robberCircle;
    private final List<Circle> activeRobberHighlights = new ArrayList<>();

    //____________________CONSTRUCTOR__________________________
    public Robber(Tile startingTile, Gameplay gameplay, CatanBoardGameView catanBoardGameView, Group boardGroup) {
        this.currentTile = startingTile;
        this.gameplay = gameplay;
        this.drawOrDisplay = gameplay.getDrawOrDisplay();
        this.catanBoardGameView = catanBoardGameView;
        this.board = gameplay.getBoard();
        Point2D center = startingTile.getCenter();
        this.robberCircle = drawOrDisplay.drawRobberCircle(center, boardGroup);
    }

    //____________________ROBBER PLACEMENT LOGIC__________________________

    public void showRobberTargets(Group boardGroup) {
        if (gameplay.getCurrentPlayer() instanceof AIOpponent ai) {
            placeRobberAutomatically(ai, boardGroup);
            return;
        }
        boardGroup.getChildren().remove(this.robberCircle); // remove old robber

        List<Circle> highlightCircles = new ArrayList<>();
        activeRobberHighlights.clear();

        for (Tile tile : board.getTiles()) {
            if (tile == this.currentTile) continue;
            if (tile.isSea()) continue;

            Point2D center = tile.getCenter();
            Circle highlight = drawOrDisplay.drawRobberCircle(center, boardGroup); // already added inside
            highlight.setOnMouseClicked(e -> {
                activeRobberHighlights.forEach(boardGroup.getChildren()::remove);
                activeRobberHighlights.clear();
                highlightCircles.forEach(boardGroup.getChildren()::remove);

                System.out.println("is here");
                gameplay.setRobberMoveRequired(false);

                // ✅ Remove the old robber circle
                boardGroup.getChildren().remove(this.robberCircle);

                // ✅ Draw the new robber circle
                this.robberCircle = drawOrDisplay.drawRobberCircle(center, boardGroup);
                this.moveTo(tile);
                /*
            highlight.setOnMouseClicked(e -> {
                activeRobberHighlights.forEach(boardGroup.getChildren()::remove);
                activeRobberHighlights.clear();
                highlightCircles.forEach(boardGroup.getChildren()::remove);
                System.out.println("is here");
                gameplay.setRobberMoveRequired(false);
                this.robberCircle = drawOrDisplay.drawRobberCircle(center, boardGroup);
                this.moveTo(tile);
                robberHasMoved();
                gameplay.getDevelopmentCard().finishMovingKnight();
*/
                // Proceed with potential steal
                List<Player> victims = showPotentialVictims(tile, this.gameplay.getCurrentPlayer());
                if (victims.isEmpty()) {
                    catanBoardGameView.logToGameLog("Bad Robber placement! No players to steal from.");
                    catanBoardGameView.getNextTurnButton().setDisable(false);
                    return;
                }

                // Ask user to steal from a victim
                ChoiceDialog<Player> dialog = new ChoiceDialog<>(victims.get(0), victims);
                dialog.setTitle("Choose a player to steal from");
                dialog.setHeaderText("Select a player with a city/settlement on this tile:");
                dialog.setContentText("Player:");

                dialog.showAndWait().ifPresent(victim -> {
                    boolean success = stealResourceFrom(victim);
                    if (!success) {
                        catanBoardGameView.logToGameLog("Failed to steal a resource from " + victim);
                    }
                    catanBoardGameView.refreshSidebar();
                    catanBoardGameView.getNextTurnButton().setDisable(false);
                });
                activeRobberHighlights.clear();

            });

            highlightCircles.add(highlight); // now safe to track
        }
    }

    //______________________________AI HELPERS__________________________________//
    private void placeRobberAutomatically(AIOpponent ai, Group boardGroup) {
        AIOpponent.StrategyLevel level = ai.getStrategyLevel();
        Tile chosenTile;

        // ----------------- EASY: Random placement -----------------
        if (level == AIOpponent.StrategyLevel.EASY) {
            List<Tile> candidates = board.getTiles().stream()
                    .filter(t -> !t.isSea() && t != currentTile)
                    .toList();
            chosenTile = candidates.get(new Random().nextInt(candidates.size()));
            catanBoardGameView.logToGameLog("AI " + ai.getPlayerId() + " (EASY) placed robber randomly.");
        }

        // ----------------- MEDIUM / HARD: Smart scoring -----------------
        else {
            List<Tile> validTargets = board.getTiles().stream()
                    .filter(t -> !t.isSea() && t != currentTile)
                    .toList();

            Tile bestTile = null;
            int bestScore = Integer.MIN_VALUE;

            for (Tile tile : validTargets) {
                int score = 0;
                boolean blocksSelf = tile.getVertices().stream()
                        .anyMatch(v -> v.getOwner() == ai);

                if (blocksSelf) {
                    continue; // MEDIUM/HARD: never block self
                }

                for (Vertex v : tile.getVertices()) {
                    Player owner = v.getOwner();
                    if (owner == null || owner == ai) continue;
                    int weight = v.isCity() ? 2 : 1;
                    int diceValue = ai.getSettlementDiceValue(v, gameplay);
                    score += diceValue * weight;

                    if (level == AIOpponent.StrategyLevel.HARD && ai.lateGame() && owner.getPlayerScore() >= 7) {
                        score += diceValue * weight * 2; // Threat multiplier
                    }
                }

                if (score > bestScore) {
                    bestScore = score;
                    bestTile = tile;
                }
            }

            chosenTile = bestTile != null ? bestTile : validTargets.get(0);
            catanBoardGameView.logToGameLog("AI " + ai.getPlayerId() + " (" + level + ") placed robber on tile with score: " + bestScore);
        }

        // ---------- Place Robber on Tile ----------
        boardGroup.getChildren().remove(this.robberCircle); //  Remove old circle before drawing new one
        moveTo(chosenTile);
        this.robberCircle = drawOrDisplay.drawRobberCircle(chosenTile.getCenter(), boardGroup);
        robberHasMoved();

        // ---------- Determine valid victims ----------
        List<Player> victims = showPotentialVictims(chosenTile, ai).stream()
                .filter(p -> p.getTotalResourceCount() > 0)
                .toList();

        if (!victims.isEmpty()) {
            Player target;

            if (level == AIOpponent.StrategyLevel.HARD) {
                target = ai.chooseBestRobberyTargetForHardAI(ai, victims);
            } else {
                target = victims.stream()
                        .max(Comparator.comparingInt(Player::getTotalResourceCount))
                        .orElse(victims.get(0));
            }

            if (target != null) {
                stealResourceFrom(target);
            }
        }
        catanBoardGameView.refreshSidebar();
        catanBoardGameView.getNextTurnButton().setDisable(false);
    }

    private List<Player> showPotentialVictims(Tile tile, Player currentPlayer) {
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
                Map<String, Integer> discarded;
                if (p instanceof AIOpponent ai) {
                    discarded = ai.chooseDiscardCards();
                } else {
                    discarded = showDiscardDialog(p, gameplay);
                }
                if (discarded != null) {
                    discardResourcesForPlayer(p, discarded);
                }
            }
        }
    }

    private Map<String, Integer> showDiscardDialog(Player player, Gameplay gameplay) {
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

        // Manual discard handler
        discardButton.setOnAction(e -> {
            result[0] = discardSelection;
            dialogStage.close();
        });

        // Random discard button
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

    //____________________HELPERS__________________________

    public boolean isRobberMovementRequired() {
        return robberNeedsToMove;
    }

    public void robberHasMoved() {
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

    public boolean stealResourceFrom(Player victim) {
        List<String> pool = new ArrayList<>();
        victim.getResources().forEach((res, count) -> {
            for (int i = 0; i < count; i++) pool.add(res);
        });
        if (pool.isEmpty()) return false;

        Collections.shuffle(pool);
        String stolen = pool.get(0);
        victim.getResources().put(stolen, victim.getResources().get(stolen) - 1);

        Player thief = this.gameplay.getCurrentPlayer();
        thief.getResources().put(stolen, thief.getResources().getOrDefault(stolen, 0) + 1);

        catanBoardGameView.logToGameLog("Player " + thief.getPlayerId() + " stole 1 " + stolen + " from Player " + victim.getPlayerId());
        return true;
    }
}