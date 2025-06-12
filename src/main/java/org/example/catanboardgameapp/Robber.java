package org.example.catanboardgameapp;

import javafx.application.Platform;
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

import java.awt.*;
import java.util.*;
import java.util.List;

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
        catanBoardGameView.runOnFX(() -> {
            catanBoardGameView.logToGameLog(gameplay.getCurrentPlayer() + ", place the Robber on a highlighted Tile");

            if (gameplay.getCurrentPlayer() instanceof AIOpponent ai) {
                placeRobberAutomatically(ai, boardGroup);
                return; // AI will handle everything automatically
            }

            // Human Player:
            boardGroup.getChildren().remove(this.robberCircle);
            catanBoardGameView.hideTurnButton();
            catanBoardGameView.hideDiceButton();
            activeRobberHighlights.clear();

            for (Tile tile : board.getTiles()) {
                if (tile == this.currentTile || tile.isSea()) continue;

                Runnable onClick = () -> {
                    catanBoardGameView.runOnFX(() -> {
                        activeRobberHighlights.forEach(boardGroup.getChildren()::remove);
                        activeRobberHighlights.clear();

                        if (gameplay.isActionBlockedByDevelopmentCard()) {
                            gameplay.getDevelopmentCard().finishPlayingCard();
                        }

                        gameplay.setRobberMoveRequired(false);
                        boardGroup.getChildren().remove(this.robberCircle);
                        this.robberCircle = drawOrDisplay.drawRobberCircle(tile.getCenter(), boardGroup);
                        this.moveTo(tile);

                        if (gameplay.hasRolledDice()) {
                            catanBoardGameView.showTurnButton();
                        } else {
                            catanBoardGameView.showDiceButton();
                        }

                        List<Player> victims = showPotentialVictims(tile, gameplay.getCurrentPlayer());
                        if (victims.isEmpty()) {
                            catanBoardGameView.logToGameLog("Bad Robber placement! No players to steal from.");
                            return;
                        }

                        drawOrDisplay.showRobberVictimDialog(victims).ifPresent(victim -> {
                            boolean success = stealResourceFrom(victim);
                            if (!success) {
                                catanBoardGameView.logToGameLog("Failed to steal a resource from " + victim);
                            }
                            catanBoardGameView.refreshSidebar();
                        });

                        activeRobberHighlights.clear();
                    });
                };

                Circle highlight = drawOrDisplay.createRobberHighlight(tile, boardGroup, onClick);
                activeRobberHighlights.add(highlight);
            }
        });
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
            catanBoardGameView.logToGameLog(gameplay.getCurrentPlayer() + " (EASY) placed robber randomly.");
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
            catanBoardGameView.logToGameLog(gameplay.getCurrentPlayer() + " (" + level + ") placed robber on best possible tile with score: " + bestScore);
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
        catanBoardGameView.showTurnButton();
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
                Platform.runLater(() -> {
                Map<String, Integer> discarded;
                if (p instanceof AIOpponent ai) {
                    discarded = ai.chooseDiscardCards();
                    catanBoardGameView.refreshSidebar();
                } else {
                    discarded = discardCards(p, gameplay);
                    catanBoardGameView.refreshSidebar();
                }
                if (discarded != null) {
                    discardResourcesForPlayer(p, discarded);
                    catanBoardGameView.refreshSidebar();
                }});
                }
            }
        }

    private Map<String, Integer> discardCards(Player player, Gameplay gameplay) {
        Map<String, Integer> playerResources = new HashMap<>(player.getResources());
        int totalCards = playerResources.values().stream().mapToInt(Integer::intValue).sum();
        int toDiscard = totalCards / 2;

        if (toDiscard == 0) return null;
        return drawOrDisplay.showDiscardDialog(player, toDiscard, playerResources, gameplay);
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