package org.example.catanboardgameapp;

public class BiggestArmyManager {

    private Player biggestArmy;
    private final Gameplay gameplay;

    // ---------------- Constructor ---------------- //
    public BiggestArmyManager(Gameplay gameplay) {
        this.gameplay = gameplay;
    }

    // ---------------- Public API ---------------- //
    public void calculateAndUpdateBiggestArmy(Player currentPlayer) {
        int currentKnights = currentPlayer.getPlayedKnights();

        if (currentKnights >= 3) {
            if (biggestArmy == null) {
                // First player to receive Biggest Army
                biggestArmy = currentPlayer;
                gameplay.increasePlayerScoreByTwo();
            } else {
                int previousKnights = biggestArmy.getPlayedKnights();

                if (currentKnights > previousKnights && currentPlayer != biggestArmy) {
                    // New player takes over the title
                    gameplay.decreasePlayerScoreByTwo(biggestArmy);
                    biggestArmy = currentPlayer;
                    gameplay.increasePlayerScoreByTwo();
                }
            }
        }
    }

    public Player getCurrentHolder() {
        return biggestArmy;
    }
}
