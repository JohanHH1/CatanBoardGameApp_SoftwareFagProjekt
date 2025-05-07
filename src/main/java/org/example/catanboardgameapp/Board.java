package org.example.catanboardgameapp;

import javafx.geometry.Point2D;
import java.util.*;

public class Board {
    // Initialize and create lists of tile, vertex, and edge classes
    private static final List<Tile> tiles = new ArrayList<>();
    private final List<Vertex> vertices = new ArrayList<>();
    private static final List<Edge> edges = new ArrayList<>();
    private final Map<Point2D, Vertex> vertexMap = new HashMap<>(); // For unifying shared Vertex objects
    private final Map<String, Edge> edgeMap = new HashMap<>();      // For unifying shared Edge objects

    private static final String[] TERRAIN_TYPES = {
            "Grain", "Grain", "Grain", "Grain", "Wood", "Wood", "Wood", "Wood",
            "Wool", "Wool", "Wool", "Wool", "Brick", "Brick", "Brick",
            "Ore", "Ore", "Ore"
    };

    private final List<List<Integer>> originalNumberTokensAll = List.of(
            new ArrayList<>(List.of(8, 4, 9, 11, 3, 5, 10, 6, 2, 12, 6, 3, 9, 10, 5, 4, 8, 11)),
            new ArrayList<>(List.of(6, 4, 5, 11, 3, 9, 10, 6, 2, 12, 8, 3, 5, 10, 9, 4, 8, 11)),
            new ArrayList<>(List.of(8, 4, 9, 3, 11, 5, 10, 8, 12, 2, 6, 11, 9, 10, 5, 4, 6, 3)),
            new ArrayList<>(List.of(6, 10, 9, 3, 11, 5, 4, 8, 2, 12, 8, 11, 9, 4, 5, 10, 6, 3)),
            new ArrayList<>(List.of(11, 8, 4, 5, 10, 9, 3, 6, 12, 2, 6, 10, 5, 3, 11, 9, 4, 8)),
            new ArrayList<>(List.of(11, 8, 4, 9, 10, 5, 3, 8, 12, 2, 6, 10, 9, 3, 11, 5, 4, 6)),
            new ArrayList<>(List.of(3, 6, 4, 5, 10, 9, 11, 6, 2, 12, 8, 10, 5, 11, 3, 9, 4, 8)),
            new ArrayList<>(List.of(3, 6, 10, 5, 4, 9, 11, 8, 12, 2, 8, 4, 5, 11, 3, 9, 10, 6))
    );

    // Board size parameters
    private final int radius;      // e.g., 2 for standard 19-hex
    private final double hexSize;  // distance from hex center to a corner
    private final double offsetX;  // horizontal offset for centering
    private final double offsetY;  // vertical offset for centering
    private final int boardsize;

    //___________________________CONSTRUCTOR___________________________
    public Board(int radius, double screenWidth, double screenHeight) {
        this.radius = radius;
        this.hexSize = calculateHexSize(radius, screenWidth, screenHeight);
        this.offsetX = screenWidth / 2;
        this.offsetY = screenHeight / 2;
        this.boardsize = radius + 1;
        initializeBoard();
    }

    //___________________________FUNCTIONS___________________________

    private double calculateHexSize(int radius, double screenWidth, double screenHeight) {
        double maxCols = 2 * radius + 1.5;
        double maxRows = 2 * radius * 1.5 + 1;
        double maxHexWidth = screenWidth / maxCols;
        double maxHexHeight = screenHeight / maxRows;
        return Math.round(Math.min(maxHexWidth / Math.sqrt(3), maxHexHeight / 1.5));
    }

    private void initializeBoard() {
        tiles.clear();
        List<Tile> allTiles = new ArrayList<>();

        int tileCountMultiplier = ((3 * boardsize * boardsize - (3 * boardsize) + 1)) / 18;
        String[] desertArray = (boardsize % 3 == 2) ?
                new String[]{"Desert", "Desert", "Desert", "Desert", "Desert", "Desert", "Desert"} :
                new String[]{"Desert"};

        List<Integer> numberTokens = new ArrayList<>();
        int fullSize = TERRAIN_TYPES.length * tileCountMultiplier + desertArray.length;
        String[] terrainPool = new String[fullSize];

        for (int i = 0; i < tileCountMultiplier; i++) {
            System.arraycopy(TERRAIN_TYPES, 0, terrainPool, i * TERRAIN_TYPES.length, TERRAIN_TYPES.length);
            numberTokens.addAll(originalNumberTokensAll.get((int) (Math.random() * originalNumberTokensAll.size())));
        }
        System.arraycopy(desertArray, 0, terrainPool, TERRAIN_TYPES.length * tileCountMultiplier, desertArray.length);

        List<String> shuffledTerrains = new ArrayList<>(Arrays.asList(terrainPool));
        Collections.shuffle(shuffledTerrains);

        if (boardsize != 3) Collections.shuffle(numberTokens);

        for (int q = -radius; q <= radius; q++) {
            for (int r = -radius; r <= radius; r++) {
                if (Math.abs(q + r) <= radius) {
                    String type = shuffledTerrains.remove(0);
                    int num = type.equals("Desert") ? 7 : numberTokens.remove(0);
                    Resource.ResourceType resType = Resource.ResourceType.fromString(type);
                    Point2D center = axialToPixel(q, r);
                    Tile tile = new Tile(q, r, resType, num, center);
                    allTiles.add(tile);
                }
            }
        }

        for (Tile tile : allTiles) {
            Point2D center = axialToPixel(tile.getQ(), tile.getR());
            Vertex[] corners = new Vertex[6];

            for (int i = 0; i < 6; i++) {
                double angleRad = Math.toRadians(60. * i - 30.);
                double x = center.getX() + hexSize * Math.cos(angleRad);
                double y = center.getY() + hexSize * Math.sin(angleRad);
                x = Math.round(x * 1000.0) / 1000.0;
                y = Math.round(y * 1000.0) / 1000.0;
                Point2D point = new Point2D(x, y);

                Vertex vertex = vertexMap.computeIfAbsent(point, p -> new Vertex(p.getX(), p.getY()));
                vertex.addAdjacentTile(tile);
                corners[i] = vertex;
            }

            List<Edge> tileEdges = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                Vertex v1 = corners[i];
                Vertex v2 = corners[(i + 1) % 6];
                String key = sortEdgeVertices(v1, v2);
                Edge edge = edgeMap.computeIfAbsent(key, k -> new Edge(v1, v2));
                edge.addAdjacentTile(tile);
                tileEdges.add(edge);
            }

            tile.setVertices(List.of(corners));
            tile.setEdges(tileEdges);
        }

        tiles.addAll(allTiles);
        vertices.addAll(vertexMap.values());
        edges.addAll(edgeMap.values());
    }

    private Point2D axialToPixel(int q, int r) {
        double x = hexSize * (Math.sqrt(3) * q + Math.sqrt(3) / 2 * r) + offsetX;
        double y = hexSize * (3.0 / 2.0) * r + offsetY;
        return new Point2D(x, y);
    }

    private String sortEdgeVertices(Vertex v1, Vertex v2) {
        if (v1.getX() > v2.getX() || (v1.getX() == v2.getX() && v1.getY() > v2.getY())) {
            Vertex temp = v1;
            v1 = v2;
            v2 = temp;
        }
        return String.format("(%.3f,%.3f)-(%.3f,%.3f)", v1.getX(), v1.getY(), v2.getX(), v2.getY());
    }

    //___________________________GETTERS___________________________

    public static List<Tile> getTiles() {
        return tiles;
    }

    public List<Vertex> getVertices() {
        return vertices;
    }

    public static List<Edge> getEdges() {
        return edges;
    }

    public double getHexSize() {
        return hexSize;
    }

    public int getRadius() {
        return radius;
    }
}
