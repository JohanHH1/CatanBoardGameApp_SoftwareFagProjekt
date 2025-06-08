package org.example.catanboardgameapp;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Vertex {

    public enum StructureType {
        NONE, SETTLEMENT, CITY
    }

    private Player owner = null;
    private StructureType structure = StructureType.NONE;

    private final double x;
    private final double y;

    private final List<Tile> adjacentTiles = new ArrayList<>();
    private final List<Vertex> neighbors = new ArrayList<>();

    // -------------------- Constructor -------------------- //

    public Vertex(double x, double y) {
        this.x = x;
        this.y = y;
    }

    // -------------------- Placement State -------------------- //

    public void makeSettlement() {
        this.structure = StructureType.SETTLEMENT;
    }

    public void makeCity() {
        this.structure = StructureType.CITY;
    }

    public boolean isCity() {
        return structure == StructureType.CITY;
    }

    public boolean hasSettlement() {
        return owner != null; // All placed structures imply ownership
    }

    public boolean hasCity() {
        return owner != null && structure == StructureType.CITY;
    }

    public StructureType getStructureType() {
        return structure;
    }

    // -------------------- Relationship Logic -------------------- //

    public void addNeighbor(Vertex neighbor) {
        if (!neighbors.contains(neighbor)) {
            neighbors.add(neighbor);
        }
    }

    public void addAdjacentTile(Tile tile) {
        if (!adjacentTiles.contains(tile)) {
            adjacentTiles.add(tile);
        }
    }

    public boolean isSeaOnly() {
        return adjacentTiles.stream().allMatch(Tile::isSea);
    }

    // -------------------- Getters -------------------- //

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public List<Tile> getAdjacentTiles() {
        return adjacentTiles;
    }

    public List<Vertex> getNeighbors() {
        return neighbors;
    }

    public Player getOwner() {
        return owner;
    }

    public void setOwner(Player owner) {
        this.owner = owner;
    }

    public String getIdOrCoords() {
        return String.format("(%.2f, %.2f)", x, y);
    }

    // -------------------- Equality -------------------- //

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Vertex other = (Vertex) obj;
        return Double.compare(x, other.x) == 0 &&
                Double.compare(y, other.y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
