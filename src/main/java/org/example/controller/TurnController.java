package org.example.controller;
import javafx.scene.text.Text;
import org.example.catanboardgameapp.Gameplay;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import org.example.catanboardgameapp.Player;

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
        currentPlayersOnTurn.setText("Turn: Player " + gameplay.getCurrentPlayer().getPlayerId());

        int totalRoads = 0;
        for (Player player : gameplay.getPlayerList()) {
            totalRoads += player.getRoads().size();
        }

        boolean allHavePlacedInitial = totalRoads >= (gameplay.getPlayerList().size() * 2 - 1);
        rollDiceButton.setVisible(allHavePlacedInitial);
        nextTurnButton.setVisible(!allHavePlacedInitial);
    }
}