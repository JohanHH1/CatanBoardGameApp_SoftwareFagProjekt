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
import org.example.catanboardgameapp.*;

import org.example.catanboardgameviews.CatanBoardGameView;

import static org.example.catanboardgameviews.CatanBoardGameView.createLeftMenu;
import static org.example.catanboardgameviews.CatanBoardGameView.nextTurnButton;

public class BuildController {
    private final Gameplay gameplay;
    private final Group boardGroup;

    public BuildController(Gameplay gameplay, Group boardGroup) {
        this.gameplay = gameplay;
        this.boardGroup = boardGroup;
    }

    public EventHandler<MouseEvent> createRoadClickHandler(Edge edge, Line visibleLine, BorderPane root) {
        return event -> {
            if (!gameplay.isInInitialPhase() && !gameplay.hasPlayerRolledThisTurn()) {
                DrawOrDisplay.showPopup("You must roll the dice before building!");
                return;
            }
            Player currentPlayer = gameplay.getCurrentPlayer();

            if (gameplay.buildRoad(edge)) {
                DrawOrDisplay.drawPlayerRoad(visibleLine, currentPlayer);

                boolean allInInitialPhase = gameplay.getPlayerList().stream()
                        .allMatch(p -> p.getRoads().size() <= 2);

                if (allInInitialPhase) {
                    gameplay.nextPlayerTurn();
                    CatanBoardGameView.handleInitialAITurn(gameplay, boardGroup);
                }

                CatanBoardGameView.logToGameLog("Road built by player " + currentPlayer.getPlayerId());
                root.setLeft(createLeftMenu(gameplay));
            } else {
                Point2D mid = new Point2D(
                        (edge.getVertex1().getX() + edge.getVertex2().getX()) / 2,
                        (edge.getVertex1().getY() + edge.getVertex2().getY()) / 2
                );
                DrawOrDisplay.showBuildErrorDot(boardGroup, mid);
            }
        };
    }


    public EventHandler<MouseEvent> createSettlementClickHandler(Circle visibleCircle, Vertex vertex, BorderPane root) {
        return MouseEvent -> {
            if (!gameplay.isInInitialPhase() && !gameplay.hasPlayerRolledThisTurn()) {
                DrawOrDisplay.showPopup("You must roll the dice before building!");
                return;
            }
            Player currentPlayer = gameplay.getCurrentPlayer();

            boolean success;
            if (gameplay.isInInitialPhase()) {
                success = gameplay.buildInitialSettlement(vertex);
            } else {
                success = gameplay.buildSettlement(vertex);
            }

            if (success) {
                vertex.setOwner(currentPlayer);
                DrawOrDisplay.drawPlayerSettlement(visibleCircle, vertex);
                CatanBoardGameView.logToGameLog("Settlement built by player " + currentPlayer.getPlayerId());
                root.setLeft(createLeftMenu(gameplay));
            }
            else if (gameplay.buildCity(vertex)) {
                vertex.setOwner(currentPlayer);
                boardGroup.getChildren().remove(visibleCircle);

                Rectangle citySquare = new Rectangle(vertex.getX() - 6, vertex.getY() - 6, 12, 12);
                citySquare.setFill(currentPlayer.getColor());
                citySquare.setStroke(Color.BLACK);
                boardGroup.getChildren().add(citySquare);
                CatanBoardGameView.logToGameLog(
                        "Player " + currentPlayer.getPlayerId() + " built a Settlement at (" +
                                vertex.getX() + ", " + vertex.getY() + ")"
                );

                root.setLeft(createLeftMenu(gameplay));
            }
            else {
                DrawOrDisplay.showPlacementError(boardGroup, vertex.getX(), vertex.getY());
            }
        };
    }

}