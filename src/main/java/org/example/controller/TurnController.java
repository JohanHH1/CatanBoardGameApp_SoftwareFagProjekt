package org.example.controller;
import javafx.animation.PauseTransition;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.example.catanboardgameapp.AIOpponent;
import org.example.catanboardgameapp.AIOpponent;
import org.example.catanboardgameapp.Gameplay;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import org.example.catanboardgameapp.Player;
import org.example.catanboardgameviews.CatanBoardGameView;

public class TurnController {
    private final Gameplay gameplay;
    private final Button rollDiceButton;
    private final Button nextTurnButton;
    private final BorderPane root;

    public TurnController(Gameplay gameplay, Button rollDiceButton, Button nextTurnButton, BorderPane root) {
        this.gameplay = gameplay;
        this.rollDiceButton = rollDiceButton;
        this.nextTurnButton = nextTurnButton;
        this.root = root;
    }


    public void handleNextTurnButtonPressed(ActionEvent event) {

        gameplay.nextPlayerTurn();
        root.setLeft(CatanBoardGameView.createLeftMenu(gameplay));
        Player currentPlayer = gameplay.getCurrentPlayer();

        int totalRoads = 0;
        for (Player player : gameplay.getPlayerList()) {
            totalRoads += player.getRoads().size();
        }

        boolean allHavePlacedInitial = totalRoads >= (gameplay.getPlayerList().size() * 2 - 1);
        rollDiceButton.setVisible(allHavePlacedInitial);
        nextTurnButton.setVisible(!allHavePlacedInitial);

        if (currentPlayer instanceof AIOpponent && allHavePlacedInitial) {
            rollDiceButton.setDisable(true);
            nextTurnButton.setDisable(true);

            PauseTransition pause = new PauseTransition(Duration.seconds(1));
            pause.setOnFinished(e -> {
                // AI rolls dice and collects resources here
                int result = gameplay.rollDiceAndDistributeResources();

                if (result != 7) {
                    ((AIOpponent) currentPlayer).makeMoveAI(gameplay);
                    handleNextTurnButtonPressed(null); // go to next player
                } else {
                    // Implement AI robber logic here?
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