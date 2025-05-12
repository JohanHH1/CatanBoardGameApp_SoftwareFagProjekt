package org.example.catanboardgameapp;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Vertex {
    private Player owner = null;
    private String typeOf = null;
    private final double x;
    private final double y;

    private final List<Tile> adjacentTiles = new ArrayList<>();
    private final List<Vertex> neighbors = new ArrayList<>(); // Stores neighboring vertices

    //___________________CONSTRUCTOR_________________________

    public Vertex(double x, double y) {
        this.x = x;
        this.y = y;
    }

    //___________________FUNCTIONS_________________________

    // Add a neighboring vertex
    public void addNeighbor(Vertex neighbor) {
        if (!neighbors.contains(neighbor)) {
            neighbors.add(neighbor);
        }
    }

    // Set this vertex as a settlement
    public void makeSettlement() {
        this.typeOf = "Settlement";
    }

    // Upgrade this vertex to a city
    public void makeCity() {
        this.typeOf = "City";
    }

    // Check if a player owns this vertex (settlement or city)
    public boolean hasSettlement() {
        return owner != null;
    }

    public boolean hasCity() {
        return owner != null; // Same logic as hasSettlement; refined logic may be added if needed
    }

    // Add a tile adjacent to this vertex
    public void addAdjacentTile(Tile tile) {
        if (!adjacentTiles.contains(tile)) {
            adjacentTiles.add(tile);
        }
    }

    //___________________GETTERS_________________________

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public List<Tile> getAdjacentTiles() {
        return adjacentTiles;
    }

    public Player getOwner() {
        return owner;
    }

    public String getTypeOf() {
        return typeOf;
    }

    public List<Vertex> getNeighbors() {
        return neighbors;
    }

    public void setOwner(Player owner) {
        this.owner = owner;
    }

    //_______________________OVERRIDE____________________
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Vertex other = (Vertex) obj;
        return Double.compare(x, other.x) == 0 && Double.compare(y, other.y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

}