package org.example.catanboardgameapp;

import javafx.geometry.Point2D;
import javafx.scene.effect.Light;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Board {
    //Initialize and create lists of tile, vertex and edge classes
    private final List<Tile> tiles = new ArrayList<>();
    private final List<Vertex> vertices = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private final Map<Point2D, Vertex> vertexMap = new HashMap<>(); // For unifying shared Vertex objects
    private final Map<String, Edge> edgeMap = new HashMap<>();

    //Board size parameters
    private final int radius;         // e.g., 2 for standard 19-hex
    private final double hexSize;     // distance from hex center to a corner
    private final double offsetX;     // shift everything horizontally
    private final double offsetY;

    //Constructor
    public Board(int radius, double hexSize, double offsetX, double offsetY) {
        this.radius = radius;
        this.hexSize = hexSize;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        initializeBoard();
    }

    private void initializeBoard() {
        //Initialize empty list of all the tiles.
        List<Tile> allTiles = new ArrayList<>();

        // Generates all the tiles in axial coordiantes (q,r). Goes from -r to r (radius) and then
        // check if abs(q+r) <= radius. If it is more than the radius,
        // the tile would be outside our board.
        // For axial coordinate explanation: https://www.redblobgames.com/grids/hexagons/
        for (int q = -radius; q <= radius; q++) {
            for (int r = -radius; r <= radius; r++) {
                if (Math.abs(q + r) <= radius) {
                    // This (q,r) is inside the hex region
                    Tile tile = new Tile(q, r);
                    allTiles.add(tile);
                }
            }
        }
        for (Tile tile : allTiles) {
            //Compute tile centers in pixels
            Point2D center = axialToPixel(tile.getQ(), tile.getR()); //*****

            //Array with 6 vertex objects for this tile
            Vertex[] corners = new Vertex[6];

            for (int cornerNumber = 0; cornerNumber < 6; cornerNumber++) {
                // Corner angle = 60*cornerNumber - 30 for a pointy top hex (compared to flat top hex)
                double angleDeg = 60. * cornerNumber - 30.;
                double angleRad = Math.toRadians(angleDeg);

                //Calculate corner point locations in pixel coordiantes.
                //Corner point calculation from "pointy_hex_corner": https://www.redblobgames.com/grids/hexagons/
                double centerX = center.getX() + hexSize * Math.cos(angleRad);
                double centerY = center.getY() + hexSize * Math.sin(angleRad);

                // Rounding the corner coordinates to avoid floating errors when multiples tiles share the same corner.
                centerX = Math.round(centerX * 1000.0) / 1000.0;
                centerY = Math.round(centerY * 1000.0) / 1000.0;
                Point2D cornerPoint = new Point2D(centerX, centerY);

                //Check if there is already a vertex for this cornerpoint
                Vertex vertex = vertexMap.get(cornerPoint);
                if (vertex == null) {
                    //Create the new vertex
                    vertex = new Vertex(cornerPoint.getX(), cornerPoint.getY());
                    vertexMap.put(cornerPoint, vertex);
                }

                vertex.addAdjacentTile(tile);

                corners[cornerNumber] = vertex;

            }

            // Unifying Edges that are between corners[cornerNumber] and corners[cornerNumber+1 mod 6]
            List<Edge> tileEdges = new ArrayList<>();
            for (int cornerNumber = 0; cornerNumber < 6; cornerNumber++) {
                Vertex vertex1 = corners[cornerNumber];
                Vertex vertex2 = corners[(cornerNumber + 1) % 6];

                //Ensure correct order of the vertices for the edge
                String correctOrder = sortEdgeVertices(vertex1, vertex2);
                Edge edge = edgeMap.get(correctOrder);
                if (edge == null) {
                    //Create the new edge
                    edge = new Edge(vertex1, vertex2);
                    edgeMap.put(correctOrder, edge);
                }
                //Connect tile and edge
                edge.addAdjacentTile(tile);
                tileEdges.add(edge);
            }

            //Add for each tile
            tile.setVertices(List.of(corners));
            tile.setEdges(tileEdges);
        }
        //Add everything
        this.tiles.addAll(allTiles);
        this.vertices.addAll(vertexMap.values());
        this.edges.addAll(edgeMap.values());


    }
    //Point2D is built in class in JavaFX which stores a coordinate
    //Formula used is "pointy_hex_to_pixel(hex)" from https://www.redblobgames.com/grids/hexagons/
    private Point2D axialToPixel(int q, int r){
        double x = hexSize * (Math.sqrt(3) * q + Math.sqrt(3)/2 * r) + offsetX; //offset for correct placement in app window
        double y = hexSize * (3./2.) * r + offsetY; //offset for correct placement in app window
        return new Point2D(x,y);
    }

    public String sortEdgeVertices(Vertex vertex1, Vertex vertex2) {
        //First sort by x, then y
        if (vertex1.getX() > vertex2.getX() ||
                (vertex1.getX() == vertex2.getX() && vertex1.getY() > vertex2.getY())){

            //set vertex 2 first in the string since its lowest
            Vertex tempVertex = vertex1;
            vertex1 = vertex2;
            vertex2 = tempVertex;
        }
        return String.format("(%.3f,%.3f)-(%.3f,%.3f)",
                vertex1.getX(), vertex1.getY(),
                vertex2.getX(), vertex2.getY());
    }

    //Getters

    //Setters
}
