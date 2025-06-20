package org.example.catanboardgameapp;

import java.util.ArrayList;
import java.util.List;

public class Edge {

    // Vertices and adjacent Tiles
    private final Vertex vertex1;
    private final Vertex vertex2;
    private final List<Tile> adjacentTiles = new ArrayList<>();

    private Harbor harbor;

    //________________________CONSTRUCTOR____________________________//
    public Edge(Vertex vertex1, Vertex vertex2) {
        this.vertex1 = vertex1;
        this.vertex2 = vertex2;
        // Automatically mark the vertices as neighbors
        vertex1.addNeighbor(vertex2);
        vertex2.addNeighbor(vertex1);
    }

    //_________________________FUNCTIONS____________________________//
    // Add a tile that this edge borders
    public void addAdjacentTile(Tile tile) {
        if (!adjacentTiles.contains(tile)) {
            adjacentTiles.add(tile);
        }
    }

    // Check if this edge is connected to a specific vertex
    public boolean isConnectedTo(Vertex vertex) {
        if (vertex == null) return false; // Prevent null pointer
        return vertex.equals(vertex1) || vertex.equals(vertex2);
    }

    // Given one end of the edge, return the other
    public Vertex getOppositeVertex(Vertex v) {
        if (v.equals(vertex1)) return vertex2;
        if (v.equals(vertex2)) return vertex1;
        throw new IllegalArgumentException("Vertex is not part of this edge.");
    }

    public boolean isSeaOnly() {
        return adjacentTiles.stream().allMatch(Tile::isSea);
    }

    //___________________GETTERS_________________________//
    public Harbor getHarbor() {
        return harbor;
    }
    public Vertex getVertex1() { return vertex1; }
    public Vertex getVertex2() { return vertex2; }
    public List<Tile> getAdjacentTiles() { return adjacentTiles; }

    public void setHarbor(Harbor harbor) {
        this.harbor = harbor;
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
