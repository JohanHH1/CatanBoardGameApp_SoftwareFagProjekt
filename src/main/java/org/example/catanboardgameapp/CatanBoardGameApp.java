package org.example.catanboardgameapp;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import static javafx.application.Application.launch;

public class CatanBoardGameApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        MenuView.showSetupScreen(primaryStage);
    }



    public static void main(String[] args) {
        launch(args);

    }
}