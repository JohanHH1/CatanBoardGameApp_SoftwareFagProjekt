package org.example.controller;

import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.util.Duration;
import org.example.catanboardgameapp.AIOpponent;
import org.example.catanboardgameapp.Player;

public class TurnController {

    private final GameController gameController;
    private final Button rollDiceButton;
    private final Button nextTurnButton;

    //___________________________CONTROLLER__________________________________//
    public TurnController(GameController gameController, Button rollDiceButton, Button nextTurnButton) {
        this.gameController = gameController;
        this.rollDiceButton = rollDiceButton;
        this.nextTurnButton = nextTurnButton;
    }

    //___________________________FUNCTIONS__________________________________//
    public void handleNextTurnButtonPressed(ActionEvent event) {
        var gameplay = gameController.getGameplay();
        var gameView = gameController.getGameView();

        gameplay.nextPlayerTurn();
        gameView.refreshSidebar();
        Player currentPlayer = gameplay.getCurrentPlayer();

        int totalRoads = gameplay.getPlayerList().stream()
                .mapToInt(p -> p.getRoads().size())
                .sum();

        if (currentPlayer instanceof AIOpponent ai) {
            rollDiceButton.setDisable(true);
            nextTurnButton.setDisable(true);

            PauseTransition pause = new PauseTransition(Duration.seconds(1));
            pause.setOnFinished(e -> {
                int result = gameplay.rollDice();

                if (result != 7) {
                    ai.makeMoveAI(gameplay);
                }

                // Always continue to next player after AI finishes or skips due to 7
                gameplay.nextPlayerTurn();
                gameController.getGameView().refreshSidebar();

                // Recursively call to update UI for the next player (if human)
                handleNextTurnButtonPressed(null);
            });
            pause.play();
        }
        else {
            // Human player's turn
            rollDiceButton.setDisable(false);
            rollDiceButton.setVisible(true);
            nextTurnButton.setVisible(false);

        }
    }
}