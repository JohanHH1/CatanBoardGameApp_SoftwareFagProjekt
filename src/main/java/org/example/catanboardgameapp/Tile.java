package org.example.catanboardgameapp;

import java.util.List;

public class Tile {
    private int q;
    private int r;
    private List<Vertex> vertices;
    private List<Edge> edges;

    public Tile(int q, int r) {
        this.q = q;
        this.r = r;
    }

    public List<Vertex> getVertices() {
        return vertices;
    }

    public void setVertices(List<Vertex> vertices) {
        this.vertices = vertices;
    }

    public void setEdges(List<Edge> edges) {
        this.edges = edges;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public int getQ() {
        return q;
    }

    public int getR() {
        return r;
    }
}
