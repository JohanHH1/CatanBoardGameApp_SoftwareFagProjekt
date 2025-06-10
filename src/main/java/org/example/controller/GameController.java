package org.example.controller;

import javafx.stage.Stage;
import org.example.catanboardgameapp.AIOpponent;
import org.example.catanboardgameapp.Gameplay;
import org.example.catanboardgameapp.Player;
import org.example.catanboardgameviews.CatanBoardGameView;
import org.example.catanboardgameviews.MenuView;

public class GameController {

    private final Stage primaryStage;
    private CatanBoardGameView gameView;
    private TurnController turnController;
    private TradeController tradeController;
    private Gameplay gameplay;
    private BuildController buildController;
    private MenuView menuView;

    //___________________________CONSTRUCTOR_________________________________//
    public GameController(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    //___________________________FUNCTIONS__________________________________//

    public void startGame(int playerCount, int boardSize, int easyAI, int medAI, int hardAI) {
        gameplay = new Gameplay(primaryStage, boardSize - 1, this);
        gameplay.setMenuView(this.menuView);

        // Add players
        gameplay.initializeAllPlayers(playerCount, easyAI, medAI, hardAI);
        gameplay.resetCounters();

        // Initialize controllers FIRST
        turnController = new TurnController(this);
        this.setTurnController(turnController);

        tradeController = new TradeController(this, gameplay.getBoardRadius());
        this.setTradeController(tradeController);

        // THEN create the view
        gameView = new CatanBoardGameView(primaryStage, gameplay, this, boardSize - 1);
        gameplay.setCatanBoardGameView(gameView);

        // Build UI AFTER everything is set up
        gameView.buildGameUI();
        gameplay.initializeDevelopmentCards();

        primaryStage.setScene(gameView.getScene());
        primaryStage.show();

        Player currentPlayer = gameplay.getCurrentPlayer();
        if (gameplay.isInInitialPhase()) {
            if (currentPlayer instanceof AIOpponent ai) {
                ai.placeInitialSettlementAndRoad(gameplay, gameView.getBoardGroup());
            } else {
                gameView.prepareForHumanInitialPlacement(currentPlayer);
            }
        }
    }

    public void resetGame() {
        if (gameplay != null) {
            gameplay.stopAllAIThreads();  // Stop any active AI threads
            gameplay.resetCounters();
            gameplay = null;
        }

        if (gameView != null) {
            gameView = null;
        }

        // Reset other stuff, buildController, tradeController, etc. if needed
    }

    public void returnToMenu(MenuView menuView) {
        if (gameplay != null) {
            gameplay.pauseGame(); // ensures all threads and state are halted
        }
        if (menuView != null) {
            menuView.showMainMenu();
        }
    }

    public void resumeGame() {
        if (gameplay == null || gameView == null) return;
        gameplay.resumeGame(); // handles AI restart / treads
        gameplay.setCatanBoardGameView(gameView); // restore view reference
        primaryStage.setScene(gameView.getScene()); // bring game view back
    }

    public void setMenuView(MenuView menuView) {
        this.menuView = menuView;
        if (gameplay != null) {
            gameplay.setMenuView(menuView);
        }
    }

    public void setBuildController(BuildController buildController) {
        this.buildController = buildController;
    }
    public void setTradeController(TradeController tradeController) {
        this.tradeController = tradeController;
    }
    public void setTurnController(TurnController turnController) {
        this.turnController = turnController;
    }
    public Gameplay getGameplay() {
        return gameplay;
    }
    public TurnController getTurnController() {
        return turnController;
    }
    public TradeController getTradeController() {
        return tradeController;
    }

    public CatanBoardGameView getGameView() {
        return gameView;
    }

    public MenuView getMenuView() {
        return menuView;
    }

    public BuildController getBuildController() {
        return buildController;
    }

    //___________________________BOOLEAN__________________________________//
    public boolean hasSavedSession() {
        return gameplay != null;
    }


}
