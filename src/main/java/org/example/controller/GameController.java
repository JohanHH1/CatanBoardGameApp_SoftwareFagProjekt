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
    // Set by options menu; default true — do not make final
    private boolean shufflePlayers = true;

    //___________________________CONSTRUCTOR_________________________________//
    // Initialize with primary application stage
    public GameController(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    //___________________________FUNCTIONS__________________________________//
    // Starts a new game with specified settings
    public void startGame(int playerCount, int boardSize, int easyAI, int medAI, int hardAI) {
        gameplay = new Gameplay(primaryStage, boardSize - 1, this);
        gameplay.setMenuView(this.menuView);

        // Add players
        gameplay.initializeAllPlayers(playerCount, easyAI, medAI, hardAI, shufflePlayers);
        gameplay.resetCounters();

        // Initialize controllers before creating view
        turnController = new TurnController(this);
        this.setTurnController(turnController);

        tradeController = new TradeController(this);
        this.setTradeController(tradeController);

        // Create view after controllers
        gameView = new CatanBoardGameView(primaryStage, gameplay, this, boardSize - 1);
        gameplay.setCatanBoardGameView(gameView);

        // Build UI
        gameView.buildGameUI();
        gameplay.initializeDevelopmentCards();

        primaryStage.setScene(gameView.getScene());
        primaryStage.show();

        // Handle first player's initial placement
        Player currentPlayer = gameplay.getCurrentPlayer();
        if (gameplay.isInInitialPhase()) {
            if (currentPlayer instanceof AIOpponent ai) {
                ai.placeInitialSettlementAndRoad(gameplay, gameView.getBoardGroup());
            } else {
                gameView.prepareForHumanInitialPlacement(currentPlayer);
            }
        }
    }
    // Resets the current game state
    public void resetGame() {
        if (gameplay != null) {
            gameplay.stopAllAIThreads();  // Stop any active AI threads
            gameplay.resetCounters();
            gameplay = null;
        }

        if (gameView != null) {
            gameView = null;
        }

    }
    // Returns to the main menu
    public void returnToMenu(MenuView menuView) {
        if (gameplay != null) {
            gameplay.pauseGame(); // ensures all threads and state are halted
        }
        if (menuView != null) {
            menuView.showMainMenu();
        }
    }
    // Resumes an existing game session
    public void resumeGame() {
        if (gameplay == null || gameView == null) return;
        gameplay.resumeGame(); // handles AI restart / treads
        gameplay.setCatanBoardGameView(gameView); // restore view reference
        primaryStage.setScene(gameView.getScene()); // bring game view back
    }


    //___________________________SETTERS__________________________________//
    public void setMenuView(MenuView menuView) {
        this.menuView = menuView;
        if (gameplay != null) {
            gameplay.setMenuView(menuView);}}

    public void setBuildController(BuildController buildController) {
        this.buildController = buildController;
    }
    public void setTradeController(TradeController tradeController) {
        this.tradeController = tradeController;
    }
    public void setTurnController(TurnController turnController) {
        this.turnController = turnController;
    }

    //___________________________GETTERS__________________________________//
    public Gameplay getGameplay() {
        return gameplay;
    }
    public TurnController getTurnController() {
        return turnController;
    }
    public TradeController getTradeController() {
        return tradeController;
    }
    public BuildController getBuildController() {
        return buildController;
    }
    public CatanBoardGameView getGameView() {
        return gameView;
    }

    public MenuView getMenuView() {
        return menuView;
    }


    //___________________________BOOLEAN__________________________________//
    // Returns true if a game session is currently active
    public boolean hasSavedSession() {
        return gameplay != null;
    }
}
