package org.example.catanboardgameapp;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javafx.application.Application.launch;

public class CatanBoardGameApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        Board board = new Board(2,25,400,300);

    }





    public static void main(String[] args) {
        launch(args);

    }
}
