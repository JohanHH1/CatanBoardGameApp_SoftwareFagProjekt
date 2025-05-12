package org.example.catanboardgameapp;

public class Resource {

    public enum ResourceType {
        BRICK("Brick"),
        WOOD("Wood"),
        ORE("Ore"),
        GRAIN("Grain"),
        WOOL("Wool"),
        DESERT("Desert");

        private final String name;

        //_______________CONSTRUCTOR________________//
        ResourceType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        // Method to convert a string to a ResourceType
        public ResourceType fromString(String name) {
            for (ResourceType type : ResourceType.values()) {
                if (type.name.equalsIgnoreCase(name)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid resource type: " + name);
        }
    }
}