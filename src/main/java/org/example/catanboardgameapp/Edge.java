package org.example.catanboardgameapp;

import java.util.ArrayList;
import java.util.List;

public class Edge {
    private final Vertex vertex1;
    private final Vertex vertex2;
    private final List<Tile> adjacentTiles = new ArrayList<>();

    //___________________CONSTRUCTOR______________________//
    public Edge(Vertex vertex1, Vertex vertex2) {
        this.vertex1 = vertex1;
        this.vertex2 = vertex2;
        // Automatically mark the vertices as neighbors
        vertex1.addNeighbor(vertex2);
        vertex2.addNeighbor(vertex1);
    }

    //___________________FUNCTIONS_________________________

    // Add a tile that this edge borders
    public void addAdjacentTile(Tile tile) {
        if (!adjacentTiles.contains(tile)) {
            adjacentTiles.add(tile);
        }
    }

    // Check if two edges share any common vertex
    public boolean sharesVertexWith(Edge other) {
        return vertex1.equals(other.vertex1) || vertex1.equals(other.vertex2) ||
                vertex2.equals(other.vertex1) || vertex2.equals(other.vertex2);
    }

    // Check if this edge is connected to a specific vertex
    public boolean isConnectedTo(Vertex vertex) {
        if (vertex == null) return false; // Prevent null pointer
        return vertex.equals(vertex1) || vertex.equals(vertex2);
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

    //_______________________OVERRIDE____________________//
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Edge other = (Edge) obj;
        return (vertex1.equals(other.vertex1) && vertex2.equals(other.vertex2)) ||
                (vertex1.equals(other.vertex2) && vertex2.equals(other.vertex1));
    }

    @Override
    public int hashCode() {
        return vertex1.hashCode() + vertex2.hashCode();
    }

}