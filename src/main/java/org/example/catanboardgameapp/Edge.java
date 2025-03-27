package org.example.catanboardgameapp;

import java.util.ArrayList;
import java.util.List;

public class Edge {
    public Vertex vertex1;
    public Vertex vertex2;
    private final List<Tile> adjacentTiles = new ArrayList<>();

    //___________________CONSTRUCTOR______________________

    public Edge(Vertex vertex1, Vertex vertex2) {
        this.vertex1 = vertex1;
        this.vertex2 = vertex2;

        // Automatically mark the vertices as neighbors
        vertex1.addNeighbor(vertex2);
        vertex2.addNeighbor(vertex1);
    }

    //___________________FUNCTIONS_________________________

    // Check if edge is connected to a given vertex
    public boolean isConnectedTo(Vertex vertex) {
        return vertex.equals(vertex1) || vertex.equals(vertex2);
    }

    public void addAdjacentTile(Tile tile) {
        if (!adjacentTiles.contains(tile)) {
            adjacentTiles.add(tile);
        }
    }

    public boolean sharesVertexWith(Edge other) {
        return getVertex1().equals(other.getVertex1()) || getVertex1().equals(other.getVertex2()) ||
                getVertex2().equals(other.getVertex1()) || getVertex2().equals(other.getVertex2());
    }

    //___________________GETTERS_________________________
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