package org.example.catanboardgameapp;

import javafx.application.Application;
import javafx.stage.Stage;
import org.example.catanboardgameviews.MenuView;

import static javafx.application.Application.launch;

public class CatanBoardGameApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        MenuView.showMainMenu(primaryStage);
    }



    public static void main(String[] args) {
        launch(args);
    }
}