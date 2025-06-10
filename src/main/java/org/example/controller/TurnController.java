package org.example.controller;

import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import org.example.catanboardgameapp.DrawOrDisplay;
import javafx.util.Duration;
import org.example.catanboardgameapp.AIOpponent;
import org.example.catanboardgameapp.Player;

public class TurnController {

    private final GameController gameController;
    private final DrawOrDisplay drawOrDisplay;

    //___________________________CONTROLLER__________________________________//
    public TurnController(GameController gameController, Button rollDiceButton, Button nextTurnButton) {
        this.gameController = gameController;
        this.drawOrDisplay = gameController.getGameplay().getDrawOrDisplay();
    }

    //___________________________FUNCTIONS__________________________________//
    public void handleNextTurnButtonPressed(ActionEvent event) {
        if (gameController.getGameplay().isActionBlockedByDevelopmentCard()) {
            drawOrDisplay.showMustPlaceRobberPopup();
            return;
        }
        var gameplay = gameController.getGameplay();
        gameplay.nextPlayerTurn();
    }
    public GameController getGameController() {
        return gameController;
    }
}