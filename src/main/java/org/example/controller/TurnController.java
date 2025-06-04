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

        if (currentPlayer instanceof AIOpponent) {
            rollDiceButton.setDisable(true);
            nextTurnButton.setDisable(true);

            PauseTransition pause = new PauseTransition(Duration.seconds(1));
            pause.setOnFinished(e -> {
                int result = gameplay.rollDice();

                if (result != 7) {
                    ((AIOpponent) currentPlayer).makeMoveAI(gameplay);
                    handleNextTurnButtonPressed(null); // move to next player
                } else {
                    handleNextTurnButtonPressed(null); // skip turn after robber
                }
            });
            pause.play();
        } else {
            // Human player's turn
            rollDiceButton.setDisable(false);
            rollDiceButton.setVisible(true);
            nextTurnButton.setVisible(false);

        }
    }
}