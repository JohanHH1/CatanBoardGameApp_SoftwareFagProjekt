package org.example.catanboardgameapp;

import javafx.application.Application;
import javafx.stage.Stage;
import org.example.catanboardgameviews.MenuView;
import org.example.controller.GameController;


public class CatanBoardGameApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        GameController gameController = new GameController(primaryStage);
        MenuView menuView = new MenuView(primaryStage, gameController);
        gameController.getGameplay(); // optional: access later if needed
        menuView.showMainMenu();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
