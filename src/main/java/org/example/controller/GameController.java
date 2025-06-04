package org.example.controller;

import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.catanboardgameapp.Gameplay;
import org.example.catanboardgameviews.CatanBoardGameView;
import org.example.catanboardgameviews.MenuView;

public class GameController {

    private final Stage primaryStage;
    private CatanBoardGameView gameView;
    private Gameplay gameplay;
    private BuildController buildController;

    //___________________________CONSTRUCTOR_________________________________//
    public GameController(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    //___________________________FUNCTIONS__________________________________//

    public void startGame(int playerCount, int boardSize, int easyAI, int medAI, int hardAI) {
        // Initialize core game logic and data
        gameplay = new Gameplay(primaryStage, boardSize - 1);

        // Set up the menu reference (for returnToMenu use)
        MenuView menuView = new MenuView(primaryStage, this);
        gameplay.setMenuView(menuView);

        // Add all players (And AI players if chosen)
        gameplay.initializeAllPlayers(playerCount, easyAI, medAI, hardAI);

        // Create the game UI and logic
        gameView = new CatanBoardGameView(primaryStage, gameplay, this, boardSize - 1); // scene is initialized inside constructor

        // Register game view back into gameplay (after it's fully constructed)
        gameplay.setCatanBoardGameView(gameView);

        // Build the full game User Interface
        gameView.buildGameUI();
        gameplay.initializeDevelopmentCards();

        // Show the game
        primaryStage.setScene(gameView.getScene());
        primaryStage.show();
    }

    public void returnToMenu(MenuView menuView) {
        if (gameView != null) {
            //gameView.resetGameUIState();
        }
        menuView.showMainMenu();
    }

    public void resumeGame() {
        if (gameplay == null || gameView == null) return;

        // Reconnect view and logic
        gameplay.setCatanBoardGameView(gameView);

        // Restore scene
        primaryStage.setScene(gameView.getScene());
    }




    public void setBuildController(BuildController buildController) {
        this.buildController = buildController;
    }

    public Gameplay getGameplay() {
        return gameplay;
    }

    public CatanBoardGameView getGameView() {
        return gameView;
    }

    public BuildController getBuildController() {
        return buildController;
    }

    //___________________________BOOLEAN__________________________________//
    public boolean hasSavedSession() {
        return gameplay != null;
    }

}
