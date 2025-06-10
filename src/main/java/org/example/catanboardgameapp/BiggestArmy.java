package org.example.catanboardgameapp;
import org.example.catanboardgameviews.CatanBoardGameView;

public class BiggestArmy {
    private Player biggestArmy;
    private CatanBoardGameView catanBoardGameView;

    public void calculateAndUpdateBiggestArmy(Player currentPlayer) {
        int currentKnights = currentPlayer.getPlayedKnights();

        if (currentKnights >= 3) {
            if (biggestArmy == null) {
                // First player to receive BiggestArmy
                biggestArmy = currentPlayer;
                biggestArmy.increasePlayerScore();
                biggestArmy.increasePlayerScore();
            } else {
                int previousKnights = biggestArmy.getPlayedKnights();

                if (currentKnights > previousKnights && currentPlayer != biggestArmy) {
                    biggestArmy.decreasePlayerScore();
                    biggestArmy.decreasePlayerScore();
                    biggestArmy = currentPlayer;
                    biggestArmy.increasePlayerScore();
                    biggestArmy.increasePlayerScore();

                }
            }
        }
    }
    public Player getCurrentHolder() {
        return biggestArmy;
    }
}
