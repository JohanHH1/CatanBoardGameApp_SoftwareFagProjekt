package org.example.controller;

import javafx.event.ActionEvent;
import org.example.catanboardgameapp.DrawOrDisplay;

public class TurnController {

    private final GameController gameController;
    private final DrawOrDisplay drawOrDisplay;

    //___________________________CONTROLLER__________________________________//

    // Initialize with reference to game logic and UI
    public TurnController(GameController gameController) {
        this.gameController = gameController;
        this.drawOrDisplay = gameController.getGameplay().getDrawOrDisplay();
    }

    //___________________________FUNCTIONS__________________________________//

    // Handle "Next Turn" button press
    public void handleNextTurnButtonPressed(ActionEvent event) {
        // Prevent turn change if a development card action is still pending
        if (gameController.getGameplay().isActionBlockedByDevelopmentCard()) {
            drawOrDisplay.showFinishDevelopmentCardActionPopup();
            return;
        }
        // Advance to next player's turn
        gameController.getGameplay().nextPlayerTurn();
    }
}