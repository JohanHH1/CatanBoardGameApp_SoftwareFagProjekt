package org.example.controller;
import javafx.animation.PauseTransition;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.example.catanboardgameapp.AIiOpponent;
import org.example.catanboardgameapp.Gameplay;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import org.example.catanboardgameapp.Player;
import org.example.catanboardgameviews.CatanBoardGameView;

public class TurnController {
    private final Gameplay gameplay;
    private final Button rollDiceButton;
    private final Button nextTurnButton;
    private final Text currentPlayersOnTurn;

    public TurnController(Gameplay gameplay, Button rollDiceButton, Button nextTurnButton, Text currentPlayersOnTurn) {
        this.gameplay = gameplay;
        this.rollDiceButton = rollDiceButton;
        this.nextTurnButton = nextTurnButton;
        this.currentPlayersOnTurn = currentPlayersOnTurn;
    }

    public void handleNextTurnButtonPressed(ActionEvent event) {

        gameplay.nextPlayerTurn();
        Player currentPlayer = gameplay.getCurrentPlayer();
        currentPlayersOnTurn.setText("Turn: Player " + gameplay.getCurrentPlayer().getPlayerId());

        int totalRoads = 0;
        for (Player player : gameplay.getPlayerList()) {
            totalRoads += player.getRoads().size();
        }

        boolean allHavePlacedInitial = totalRoads >= (gameplay.getPlayerList().size() * 2 - 1);
        rollDiceButton.setVisible(allHavePlacedInitial);
        nextTurnButton.setVisible(!allHavePlacedInitial);

        if (currentPlayer instanceof AIiOpponent && allHavePlacedInitial) {
            rollDiceButton.setDisable(true);
            nextTurnButton.setDisable(true);

            PauseTransition pause = new PauseTransition(Duration.seconds(1));
            pause.setOnFinished(e -> {
                // ✅ AI rolls dice and collects resources here
                int result = gameplay.rollDiceAndDistributeResources();

                if (result != 7) {
                    ((AIiOpponent) currentPlayer).makeMoveAI(gameplay);
                    handleNextTurnButtonPressed(null); // go to next player
                } else {
                    // TODO: Implement AI robber logic if needed
                    // For now, just skip their turn so game continues
                    handleNextTurnButtonPressed(null);
                }
            });
            pause.play();
        } else {
            // Human player's turn
            rollDiceButton.setDisable(false);
            nextTurnButton.setDisable(false);
        }
    }
}