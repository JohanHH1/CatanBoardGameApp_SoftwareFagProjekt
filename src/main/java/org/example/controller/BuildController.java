package org.example.controller;

import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import org.example.catanboardgameapp.Edge;
import org.example.catanboardgameapp.Gameplay;

import org.example.catanboardgameapp.Vertex;
import org.example.catanboardgameviews.CatanBoardGameView;

import static org.example.catanboardgameviews.CatanBoardGameView.createLeftMenu;

public class BuildController {
    private final Gameplay gameplay;
    private final Group boardGroup;

    public BuildController(Gameplay gameplay, Group boardGroup) {
        this.gameplay = gameplay;
        this.boardGroup = boardGroup;
    }

    public EventHandler<MouseEvent> createRoadClickHandler(Edge edge, Line visibleLine, BorderPane root) {
        return event -> {

            if (gameplay.buildRoad(edge)) {
                CatanBoardGameView.updateRoadAppearance(visibleLine, gameplay.getCurrentPlayer());
                System.out.println("Road built by player " + gameplay.getCurrentPlayer().getPlayerId());
                root.setLeft(createLeftMenu(gameplay));
            } else {
                Point2D mid = new Point2D(
                        (edge.getVertex1().getX() + edge.getVertex2().getX()) / 2,
                        (edge.getVertex1().getY() + edge.getVertex2().getY()) / 2
                );
                CatanBoardGameView.showBuildErrorDot(boardGroup, mid);
            }
        };

    }

    public EventHandler<MouseEvent> createSettlementClickHandler(Circle visibleCircle, Vertex vertex, BorderPane root) {
        return MouseEvent -> {
            if (gameplay.buildSettlement(vertex)) { // if conditions for building are true
                vertex.setOwner(gameplay.getCurrentPlayer()); // take vertex and set owner to currentPlayer
                CatanBoardGameView.updateVertexAppearance(visibleCircle, vertex); // update appearance
                System.out.println("Settlement built by player " + gameplay.getCurrentPlayer().getPlayerId());
                root.setLeft(createLeftMenu(gameplay));
            }
            else if (gameplay.buildCity(vertex)) {
                vertex.setOwner(gameplay.getCurrentPlayer());
                boardGroup.getChildren().remove(visibleCircle);

                Rectangle citySquare = new Rectangle(vertex.getX() - 6, vertex.getY() - 6, 12, 12);
                citySquare.setFill(gameplay.getCurrentPlayer().getColor());
                citySquare.setStroke(Color.BLACK);

                boardGroup.getChildren().add(citySquare);
                System.out.println("city built");
                root.setLeft(createLeftMenu(gameplay));
            }
            else {
                CatanBoardGameView.showPlacementError(boardGroup, vertex.getX(), vertex.getY()); // if conditions for building are false
            }
        };
    }
}
