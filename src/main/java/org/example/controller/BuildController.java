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

public class BuildController {
    private final Gameplay gameplay;
    private final Group boardGroup;

    public BuildController(Gameplay gameplay, Group boardGroup) {
        this.gameplay = gameplay;
        this.boardGroup = boardGroup;
    }

    //___________________________ ROAD PLACEMENT HANDLER ___________________________

    // Handles mouse click on road (edge) during gameplay or initial setup
    public EventHandler<MouseEvent> createRoadClickHandler(Edge edge, Line visibleLine, BorderPane root) {
        return event -> {
            // Enforce dice roll in main phase
            if (!gameplay.isInInitialPhase() && !gameplay.hasRolledDice()) {
                DrawOrDisplay.rollDiceBeforeActionPopup("You must roll the dice before building!");
                return;
            }

            Player currentPlayer = gameplay.getCurrentPlayer();

            if (gameplay.buildRoad(edge)) {
                // Visually draw the road
                DrawOrDisplay.drawPlayerRoad(visibleLine, currentPlayer);

                // If still in initial phase, advance to next player only if all players still have ≤ 2 roads
                boolean allStillInInitialPhase = gameplay.getPlayerList().stream()
                        .allMatch(p -> p.getRoads().size() <= 2);

                if (allStillInInitialPhase && gameplay.isInInitialPhase() && !gameplay.isWaitingForInitialRoad()) {
                    gameplay.nextPlayerTurn();  // let AI or human take next initial turn
                }

                root.setLeft(createLeftMenu(gameplay));
            } else {
                // Road placement failed → show red X at midpoint of edge
                double midX = (edge.getVertex1().getX() + edge.getVertex2().getX()) / 2;
                double midY = (edge.getVertex1().getY() + edge.getVertex2().getY()) / 2;
                DrawOrDisplay.showErrorCross(boardGroup, midX, midY);
            }
        };
    }

    //___________________________ SETTLEMENT / CITY PLACEMENT HANDLER ___________________________

    // Handles mouse click on a vertex (for building settlement or upgrading to city)
    public EventHandler<MouseEvent> createSettlementClickHandler(Circle visibleCircle, Vertex vertex, BorderPane root) {
        return event -> {
            // Enforce dice roll in main phase
            if (!gameplay.isInInitialPhase() && !gameplay.hasRolledDice()) {
                DrawOrDisplay.rollDiceBeforeActionPopup("You must roll the dice before building!");
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

            // If it's not a settlement, try building a city
            else if (gameplay.buildCity(vertex)) {
                vertex.setOwner(currentPlayer);
                boardGroup.getChildren().remove(visibleCircle);  // replace circle with city

                Rectangle citySquare = new Rectangle(vertex.getX() - 6, vertex.getY() - 6, 12, 12);
                citySquare.setFill(currentPlayer.getColor());
                citySquare.setStroke(Color.BLACK);
                boardGroup.getChildren().add(citySquare);

                root.setLeft(createLeftMenu(gameplay));
            }

            // If neither succeeded, show red X
            else {
                DrawOrDisplay.showErrorCross(boardGroup, vertex.getX(), vertex.getY());
            }
        };
    }
}
