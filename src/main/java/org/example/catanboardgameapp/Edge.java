package org.example.catanboardgameapp;

import java.util.ArrayList;
import java.util.List;

public class Edge {
    private Vertex vertex1;
    private Vertex vertex2;
    private final List<Tile> adjacentTiles = new ArrayList<>();

    public Edge(Vertex vertex1, Vertex vertex2) {
        this.vertex1 = vertex1;
        this.vertex2 = vertex2;
    }

    //function to ensure no duplicated edges
    public void addAdjacentTile(Tile tile) {
        if (!adjacentTiles.contains(tile)) {
            adjacentTiles.add(tile);
        }
    }

    public Vertex getVertex1() {
        return vertex1;
    }

    public Vertex getVertex2() {
        return vertex2;
    }

    public List<Tile> getAdjacentTiles() {
        return adjacentTiles;
    }


}
