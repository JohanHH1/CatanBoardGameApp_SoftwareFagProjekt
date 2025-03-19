package org.example.catanboardgameapp;

import java.util.List;

public class Tile {
    private int q;
    private int r;
    private int tileDiceNumber;
    private Resource.ResourceType resourcetype;
    private List<Vertex> vertices;
    private List<Edge> edges;


    public Tile(int q, int r, Resource.ResourceType resourcetype, int  tileDiceNumber) {
        this.q = q;
        this.r = r;
        this.resourcetype = resourcetype;
        this .tileDiceNumber = tileDiceNumber;
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
