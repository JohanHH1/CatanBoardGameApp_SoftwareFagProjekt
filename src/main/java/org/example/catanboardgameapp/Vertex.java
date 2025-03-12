package org.example.catanboardgameapp;

import java.util.ArrayList;
import java.util.List;

public class Vertex {
    private double x;
    private double y;

    private final List<Tile> adjacentTiles = new ArrayList<>();


    public Vertex(double x, double y) {
        this.x = x;
        this.y = y;
    }

    //TJek om nødvendig
    public void addAdjacentTile(Tile tile) {
        // Avoid duplicates if desired
        if (!adjacentTiles.contains(tile)) {
            adjacentTiles.add(tile);
        }
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public List<Tile> getAdjacentTiles() {
        return adjacentTiles;
    }
}
