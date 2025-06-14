package org.example.catanboardgameapp;

public class Resource {
    
    // Enum containing all resources (SEA AND DESERT not collectable but has Tile values)
    public enum ResourceType {
        BRICK("Brick"),
        WOOD("Wood"),
        ORE("Ore"),
        GRAIN("Grain"),
        WOOL("Wool"),
        DESERT("Desert"),
        SEA("Sea");

        private final String name;

        // CONSTRUCTOR
        ResourceType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        // Static method for safe access
        public static ResourceType fromString(String name) {
            for (ResourceType type : ResourceType.values()) {
                if (type.name.equalsIgnoreCase(name)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid resource type: " + name);
        }
    }
}