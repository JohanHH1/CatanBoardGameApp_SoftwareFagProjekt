package org.example.catanboardgameapp;

import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class AIiOpponent extends Player {

    public AIiOpponent(int playerId, Color color) {
        super(playerId, color);
        // You can add additional AI-specific setup here
    }

    // Add AI-specific methods and behavior here
    public void AiBuilds (){
    }

    public void makeMoveAI(Gameplay gameplay) {
    Random random = new Random();
    if(gameplay.canRemoveResource("Ore", 3) && gameplay.canRemoveResource("Grain", 2)){ // builds city if possible
        List<Vertex> settlements = gameplay.getCurrentPlayer().getSettlements();
        if (!settlements.isEmpty()) {
            int randomIndex = random.nextInt(settlements.size());
            gameplay.buildCity(settlements.get(randomIndex));
        }
    }
    List<Vertex> validSettlementSpots = new ArrayList<>();
    for (Edge road : gameplay.getCurrentPlayer().getRoads()) {
        if (gameplay.isValidSettlementPlacement(road.getVertex1()) && !validSettlementSpots.contains(road.getVertex1()) ){ // makes a list of all places to place a settelment
            validSettlementSpots.add(road.getVertex1());
        }
        if (gameplay.isValidSettlementPlacement(road.getVertex2()) && !validSettlementSpots.contains(road.getVertex2()) ) {
            validSettlementSpots.add(road.getVertex2());
        }
    }
    if(!validSettlementSpots.isEmpty() && gameplay.canRemoveResource("Wood", 1) && gameplay.canRemoveResource("Wool", 1) && gameplay.canRemoveResource("Brick", 1 ) && gameplay.canRemoveResource("Grain", 1)){
        Collections.shuffle(validSettlementSpots);
        Vertex randomValidSettlementSpot = validSettlementSpots.get(0);
        gameplay.buildSettlement(randomValidSettlementSpot);
    } else if (false ){ //  temp men skal tælle et par runder for at samle sammen til et hus
        //wait a coupel of turn
    }
    List<Edge> validPlacmentForRoad = new ArrayList<>();
    if (gameplay.canRemoveResource("Wood", 1) && gameplay.canRemoveResource("Brick",1)) {
        for (Edge road : Board.getEdges()) {
            if (gameplay.isValidRoadPlacement(road)) {
                validPlacmentForRoad.add(road);
            }
        }
        if (!validPlacmentForRoad.isEmpty()) {
            Collections.shuffle(validPlacmentForRoad);
            gameplay.buildRoad(validPlacmentForRoad.get(0));
        }
    }
}}