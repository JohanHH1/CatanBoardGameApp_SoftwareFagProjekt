package org.example.catanboardgameapp;

public class DevelopmentCard {

    public enum DevelopmentCardType {
        MONOPOLY("Monopoly"),
        KNIGHT("Knight"),
        ROADBUILDING("Road Building"),
        YEAROFPLENTY("Year Of Plenty"),
        VICTORYPOINT("Victory Point");

        private final String name;

        //_______________CONSTRUCTOR________________//
        DevelopmentCardType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }


    }
}
