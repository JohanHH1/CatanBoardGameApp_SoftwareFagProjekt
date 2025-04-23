package org.example.catanboardgameapp;

import java.util.ArrayList;
import java.util.List;

public class Vertex {
    private Player owner = null;
    private String typeOf = null;
    private double x;
    private double y;

    private final List<Tile> adjacentTiles = new ArrayList<>();
    private final List<Vertex> neighbors = new ArrayList<>(); // Added to store neighboring vertices

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
    public void makeSettlement(){
        this.typeOf = "Settlement";
    }
    public void makeCity(){
        this.typeOf = "City";
    }

    // Get all neighboring vertices
    public List<Vertex> getNeighbors() {
        return neighbors;
    }

    public void addAdjacentTile(Tile tile) {
        if (!adjacentTiles.contains(tile)) {
            adjacentTiles.add(tile);
        }
    }
    public boolean hasSettlement() {
        return owner != null;
    }
    public boolean hasCity() {

        return owner != null;
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
    public Player getOwner() {return owner;}
    public String getTypeOf() {
        return typeOf;
    }

    public void setOwner(Player owner) {
        this.owner = owner;}
}
