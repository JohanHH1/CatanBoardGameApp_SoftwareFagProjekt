package org.example.controller;

import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import org.example.catanboardgameapp.*;

import java.util.Optional;

public class BuildController {

    private final Group boardGroup;
    private final DrawOrDisplay drawOrDisplay;
    private final GameController gameController;
    private boolean confirmBeforeBuild = true;

    //___________________________CONTROLLER__________________________________//
    public BuildController(GameController gameController) {
        this.gameController = gameController;
        this.drawOrDisplay = new DrawOrDisplay(gameController.getGameplay().getBoardRadius());
        this.boardGroup = gameController.getGameView().getBoardGroup(); // or pass in if needed
    }

    //___________________________ ROAD PLACEMENT HANDLER ___________________________

    // Handles mouse click on road (edge) during gameplay or initial setup
    public EventHandler<MouseEvent> createRoadClickHandler(Edge edge, Line visibleLine, BorderPane root) {
        return event -> {
            // Enforce dice roll in main phase
            if (!gameController.getGameplay().isInInitialPhase() && gameController.getGameplay().hasRolledDice()) {
                drawOrDisplay.rollDiceBeforeActionPopup("You must roll the dice before building!");
                return;
            }

            Player currentPlayer = gameController.getGameplay().getCurrentPlayer();

            // Confirm action
            if (isConfirmBeforeBuildEnabled()) {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Confirm Build");
                confirmAlert.setHeaderText("Build Road");
                confirmAlert.setContentText("Are you sure you want to place a road here?");
                Optional<ButtonType> result = confirmAlert.showAndWait();

                if (result.isEmpty() || result.get() != ButtonType.OK) {
                    return; // Player cancelled
                }
            }

            BuildResult result = gameController.getGameplay().buildRoad(edge);
            switch (result) {
                case SUCCESS -> {
                    buildRoad(edge, currentPlayer);

                    boolean allStillInInitialPhase = gameController.getGameplay().getPlayerList().stream()
                            .allMatch(p -> p.getRoads().size() <= 2);

                    if (allStillInInitialPhase && gameController.getGameplay().isInInitialPhase()
                            && gameController.getGameplay().isWaitingForInitialRoad()) {
                        gameController.getGameplay().nextPlayerTurn();
                    }

                    gameController.getGameplay().getCatanBoardGameView().refreshSidebar();
                }

                case TOO_MANY_ROADS -> drawOrDisplay.showMaxRoadsReachedPopup();

                case NOT_CONNECTED, INVALID_EDGE, INSUFFICIENT_RESOURCES -> {
                    double midX = (edge.getVertex1().getX() + edge.getVertex2().getX()) / 2;
                    double midY = (edge.getVertex1().getY() + edge.getVertex2().getY()) / 2;
                    drawOrDisplay.showErrorCross(boardGroup, midX, midY);
            }
            }
        };
    }

    //___________________________ SETTLEMENT / CITY PLACEMENT HANDLER ___________________________

    // Handles mouse click on a vertex (for building settlement or upgrading to city)
    public EventHandler<MouseEvent> createSettlementClickHandler(Circle visibleCircle, Vertex vertex, BorderPane root) {
        return event -> {
            // Enforce dice roll in main phase
            if (!gameController.getGameplay().isInInitialPhase() && gameController.getGameplay().hasRolledDice()) {
                drawOrDisplay.rollDiceBeforeActionPopup("You must roll the dice before building!");
                return;
            }
            Player currentPlayer = gameController.getGameplay().getCurrentPlayer();

            // Confirm action first
            if (isConfirmBeforeBuildEnabled()) {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Confirm Build");
                confirmAlert.setHeaderText("Build Structure");
                confirmAlert.setContentText("Are you sure you want to place a settlement or upgrade to a city?");
                Optional<ButtonType> result = confirmAlert.showAndWait();

                if (result.isEmpty() || result.get() != ButtonType.OK) {
                    return; // Player cancelled
                }
            }


            BuildResult result;

            if (gameController.getGameplay().isInInitialPhase()) {
                result = gameController.getGameplay().buildInitialSettlement(vertex);
            } else {
                result = gameController.getGameplay().buildSettlement(vertex);
            }
            switch (result) {
                case SUCCESS -> {
                    vertex.setOwner(currentPlayer);
                    drawOrDisplay.drawPlayerSettlement(visibleCircle, vertex, boardGroup);
                    gameController.getGameView().logToGameLog("Settlement built by player " + currentPlayer.getPlayerId());
                    gameController.getGameplay().getCatanBoardGameView().refreshSidebar();
                }
                case INSUFFICIENT_RESOURCES, INVALID_VERTEX -> {
                    // Try to build city if settlement failed
                    BuildResult cityResult = gameController.getGameplay().buildCity(vertex);
                    if (cityResult == BuildResult.TOO_MANY_CITIES) {
                            drawOrDisplay.showMaxCitiesReachedPopup();
                            drawOrDisplay.showErrorCross(boardGroup, vertex.getX(), vertex.getY());

                    }
                    if (cityResult == BuildResult.UPGRADED_TO_CITY) {
                        vertex.setOwner(currentPlayer);
                        gameController.getGameView().getSettlementLayer().getChildren().remove(visibleCircle);
                        Rectangle citySquare = new Rectangle(vertex.getX() - 6, vertex.getY() - 6, 12, 12);
                        citySquare.setFill(currentPlayer.getColor());
                        citySquare.setStroke(Color.BLACK);
                        gameController.getGameView().getSettlementLayer().getChildren().add(citySquare);
                        gameController.getGameplay().getCatanBoardGameView().refreshSidebar();
                        gameController.getGameView().logToGameLog("City built by player " + currentPlayer.getPlayerId());

                    } else {
                        // Neither worked
                        drawOrDisplay.showErrorCross(boardGroup, vertex.getX(), vertex.getY());

                    }
                }
                case TOO_MANY_SETTLEMENTS -> {
                    drawOrDisplay.showMaxSettlementsReachedPopup();
                }
                default -> drawOrDisplay.showErrorCross(boardGroup, vertex.getX(), vertex.getY());
            }
        };
    }

    private void buildRoad(Edge edge, Player currentPlayer) {
        Line playerRoadLine = new Line(
                edge.getVertex1().getX(), edge.getVertex1().getY(),
                edge.getVertex2().getX(), edge.getVertex2().getY()
        );
        drawOrDisplay.drawPlayerRoad(playerRoadLine, currentPlayer, boardGroup);
        gameController.getGameView().getRoadLayer().getChildren().add(playerRoadLine);

    }
    public void toggleConfirmBeforeBuild() {
        confirmBeforeBuild = !confirmBeforeBuild;
    }

    public boolean isConfirmBeforeBuildEnabled() {
        return confirmBeforeBuild;
    }
    public GameController getGameController() {
        return gameController;
    }

}