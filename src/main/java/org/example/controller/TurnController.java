package org.example.controller;

import javafx.event.ActionEvent;
import org.example.catanboardgameapp.DrawOrDisplay;

public class TurnController {

    private final GameController gameController;
    private final DrawOrDisplay drawOrDisplay;

    //___________________________CONTROLLER__________________________________//
    public TurnController(GameController gameController) {
        this.gameController = gameController;
        this.drawOrDisplay = gameController.getGameplay().getDrawOrDisplay();
    }

    //___________________________FUNCTIONS__________________________________//
    public void handleNextTurnButtonPressed(ActionEvent event) {
        if (gameController.getGameplay().isActionBlockedByDevelopmentCard()) {
            drawOrDisplay.showFinishDevelopmentCardActionPopup();
            return;
        }
        var gameplay = gameController.getGameplay();
        gameplay.nextPlayerTurn();
    }
    public GameController getGameController() {
        return gameController;
    }
}