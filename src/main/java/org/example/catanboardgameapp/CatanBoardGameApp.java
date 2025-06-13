package org.example.catanboardgameapp;

import javafx.application.Application;
import javafx.stage.Stage;
import org.example.catanboardgameviews.MenuView;
import org.example.controller.GameController;

//_______________________MAIN APPLICATION ENTRY POINT_________________________//
public class CatanBoardGameApp extends Application {

    // Application Start
    @Override
    public void start(Stage primaryStage) {
        // Initialize game controller (handles game logic and flow)
        GameController gameController = new GameController(primaryStage);

        // Initialize the menu view and link it to the controller
        MenuView menuView = new MenuView(primaryStage, gameController);
        gameController.setMenuView(menuView);

        // Optionally preload gameplay view (disabled for now)
        // gameController.getGameplay();

        // Display the main menu to the user
        menuView.showMainMenu();
    }

    // Launch Game Method
    public static void main(String[] args) {
        launch(args);
    }
}
