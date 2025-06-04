package org.example.catanboardgameapp;

public class DevelopmentCard {

    public enum DevelopmentCardType {
        MONOPOLY("Monopoly"),
        KNIGHT("Knight");

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
