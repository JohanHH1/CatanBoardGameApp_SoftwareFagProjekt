package org.example.catanboardgameapp;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.*;

public class Board {
    // Initialize and create lists of tile, vertex, and edge classes
    private final List<Tile> tiles = new ArrayList<>();
    private final List<Vertex> vertices = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private final Map<Point2D, Vertex> vertexMap = new HashMap<>(); // For unifying shared Vertex objects
    private final Map<String, Edge> edgeMap = new HashMap<>();      // For unifying shared Edge objects

    private  final String[] TERRAIN_TYPES = {
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
    private final double GAME_WIDTH;
    private final double GAME_HEIGHT;
    private final int boardSize;
    private final DrawOrDisplay drawOrDisplay;

    //___________________________CONSTRUCTOR___________________________//
    public Board(int radius, double GAME_WIDTH, double GAME_HEIGHT) {
        this.radius = radius;
        this.hexSize = calculateHexSize(radius, GAME_WIDTH, GAME_HEIGHT);
        this.GAME_WIDTH = GAME_WIDTH;
        this.GAME_HEIGHT = GAME_HEIGHT;
        this.boardSize = radius + 1;
        initializeBoard();
        this.drawOrDisplay = new DrawOrDisplay(radius);
    }

    //___________________________FUNCTIONS___________________________//

    private double calculateHexSize(int radius, double screenWidth, double screenHeight) {
        double maxCols = 2 * radius + 1.5;
        double maxRows = 2 * radius * 1.5 + 1;
        double maxHexWidth = screenWidth / maxCols;
        double maxHexHeight = screenHeight / maxRows;
        return Math.round(Math.min(maxHexWidth / Math.sqrt(3), maxHexHeight / 1.5));
    }

    public void clearBoard() {
        tiles.clear();
        edges.clear();
    }

    private void initializeBoard() {
        tiles.clear();
        List<Tile> allTiles = new ArrayList<>();

        int tileCountMultiplier = ((3 * boardSize * boardSize - (3 * boardSize) + 1)) / 18;
        String[] desertArray = (boardSize % 3 == 2) ?
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

        if (boardSize != 3) Collections.shuffle(numberTokens);

        for (int q = -radius; q <= radius; q++) {
            for (int r = -radius; r <= radius; r++) {
                if (Math.abs(q + r) <= radius) {
                    String type = shuffledTerrains.remove(0);
                    int num = type.equals("Desert") ? 7 : numberTokens.remove(0);
                    Resource.ResourceType resType = Resource.ResourceType.BRICK.fromString(type);
                    Point2D center = axialToPixel(q, r);
                    Tile tile = new Tile(q, r, resType, num, center, radius);
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

    // Draws all the hex tiles and overlays them with icons and dice numbers
    public Group createBoardTiles(Board board, int radius) {
        Group boardGroup = new Group();

        for (Tile tile : getTiles()) {
            Polygon hexShape = drawOrDisplay.createTilePolygon(tile);
            hexShape.setFill(tile.getTileColor(tile.getResourcetype()));
            hexShape.setStroke(Color.BLACK);

            Point2D center = tile.getCenter();
            double centerX = center.getX();
            double centerY = center.getY();

            // Tile base
            boardGroup.getChildren().add(hexShape);

            // Resource icon
            boardGroup.getChildren().add(tile.getResourceIcon(tile.getResourcetype(), centerX, centerY, board.getHexSize()));

            // Dice number
            if (tile.getTileDiceNumber() != 7) {
                Text numberText = new Text(centerX, centerY, String.valueOf(tile.getTileDiceNumber()));
                numberText.setFont(Font.font("Arial", FontWeight.BOLD, 40.0 / radius));
                numberText.setTextAlignment(TextAlignment.CENTER);
                numberText.setFill((tile.getTileDiceNumber() == 6 || tile.getTileDiceNumber() == 8) ? Color.RED : Color.DARKGREEN);

                Text sample = new Text("12");
                sample.setFont(Font.font("Arial", FontWeight.BOLD, 40.0 / radius));
                Rectangle background = drawOrDisplay.createBoxBehindDiceNumber(sample, centerX, centerY);

                // Center align text
                numberText.setX(centerX - numberText.getLayoutBounds().getWidth() / 2);
                numberText.setY(centerY + numberText.getLayoutBounds().getHeight() / 4);

                boardGroup.getChildren().addAll(background, numberText);
            }
        }
        return boardGroup;
    }

    private Point2D axialToPixel(int q, int r) {
        double offsetX = GAME_WIDTH / 2;
        double offsetY = GAME_HEIGHT / 2;
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

    public List<Tile> getTiles() {
        return tiles;
    }

    public List<Vertex> getVertices() {
        return vertices;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public double getHexSize() {
        return hexSize;
    }
}