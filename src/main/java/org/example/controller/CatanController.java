package org.example.controller;

import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import org.example.catanboardgameapp.Edge;
import org.example.catanboardgameapp.Gameplay;

import org.example.catanboardgameapp.Vertex;
import org.example.catanboardgameviews.CatanBoardGameView;

public class CatanController {
    private final Gameplay gameplay;
    private final Group boardGroup;

    public CatanController(Gameplay gameplay, Group boardGroup) {
        this.gameplay = gameplay;
        this.boardGroup = boardGroup;
    }

    public EventHandler<MouseEvent> createRoadClickHandler(Edge edge, Line visibleLine) {
        return event -> {
            if (gameplay.buildRoad(edge)) {
                CatanBoardGameView.updateRoadAppearance(visibleLine, gameplay.getCurrentPlayer());
                System.out.println("GUI Road built by player " + gameplay.getCurrentPlayer().getPlayerId());
            } else {
                Point2D mid = new Point2D(
                        (edge.getVertex1().getX() + edge.getVertex2().getX()) / 2,
                        (edge.getVertex1().getY() + edge.getVertex2().getY()) / 2
                );
                CatanBoardGameView.showBuildErrorDot(boardGroup, mid);
            }
        };
    }

    public EventHandler<MouseEvent> createSettlementClickHandler(Circle visibleCircle, Vertex vertex) {
        return MouseEvent -> {
            if (gameplay.buildSettlement(vertex)) {
                CatanBoardGameView.updateVertexAppearance(visibleCircle, vertex); // update appearance
                System.out.println("GUI Settlement built by player " + gameplay.getCurrentPlayer().getPlayerId());
            } else {
                CatanBoardGameView.showPlacementError(boardGroup, vertex.getX(), vertex.getY());
            }
        };
    }

    }


