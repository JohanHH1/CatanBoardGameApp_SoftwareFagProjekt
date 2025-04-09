package org.example.controller;

import java.util.*;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import org.example.catanboardgameapp.Edge;
import org.example.catanboardgameapp.Gameplay;

import org.example.catanboardgameviews.CatanBoardGameView;

public class GUI {
    private final Gameplay gameplay;
    private final Group boardGroup;

    public GUI(Gameplay gameplay, Group boardGroup) {
        this.gameplay = gameplay;
        this.boardGroup = boardGroup;
    }

    public EventHandler<MouseEvent> createRoadClickHandler(Edge edge, Line visibleLine) {
        return event -> {
            if (gameplay.buildRoad(edge)) {
                visibleLine.setStroke(gameplay.getCurrentPlayer().getColor());
                visibleLine.setStrokeWidth(4);
                System.out.println("Road built by player " + gameplay.getCurrentPlayer().getPlayerId());
            } else {
                double midX = (edge.getVertex1().getX() + edge.getVertex2().getX()) / 2 + boardGroup.getTranslateX();;
                double midY = (edge.getVertex2().getY() + edge.getVertex2().getY()) / 2 + boardGroup.getTranslateY();;
                CatanBoardGameView.showTemporaryDot(boardGroup, midX, midY, Color.RED);
                System.out.println("Cannot build road here");
            }
        };

    }

}
