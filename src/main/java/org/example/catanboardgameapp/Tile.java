package org.example.catanboardgameapp;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import org.example.catanboardgameviews.CatanBoardGameView;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Tile {
    private final int q;
    private final int r;
    private final int tileDiceNumber;
    private final Point2D center;
    private final Resource.ResourceType resourcetype;
    private List<Vertex> vertices;
    private List<Edge> edges;
    private static final Map<Resource.ResourceType, Image> imageCache = new HashMap<>();

    //___________________CONSTRUCTOR______________________

    public Tile(int q, int r, Resource.ResourceType resourcetype, int tileDiceNumber, Point2D center, int boardRadius) {
        this.q = q;
        this.r = r;
        this.resourcetype = resourcetype;
        this.tileDiceNumber = tileDiceNumber;
        this.center = center;
        DrawOrDisplay drawOrDisplay = new DrawOrDisplay(boardRadius);
    }

    //_____________________________SETTERS___________________________________

    public void setVertices(List<Vertex> vertices) {
        this.vertices = vertices;
    }

    public void setEdges(List<Edge> edges) {
        this.edges = edges;
    }

    //______________________________GETTERS__________________________________

    // Returns a color depending on the tile's resource type
    public Color getTileColor(Resource.ResourceType type) {
        return switch (type) {
            case BRICK -> Color.SADDLEBROWN;
            case WOOD -> Color.DARKGREEN;
            case ORE -> Color.DARKGRAY;
            case GRAIN -> Color.GOLD;
            case WOOL -> Color.YELLOWGREEN;
            case DESERT -> Color.BEIGE;
        };
    }

    // Loads the correct resource icon for a tile
    public ImageView getResourceIcon(Resource.ResourceType type, double x, double y, double hexSize) {
        Image image = imageCache.computeIfAbsent(type, t -> {
            String filename = switch (t) {
                case BRICK -> "/Icons/brick.png";
                case WOOD -> "/Icons/wood.png";
                case ORE -> "/Icons/ore.png";
                case GRAIN -> "/Icons/grain.png";
                case WOOL -> "/Icons/wool.png";
                case DESERT -> "/Icons/desert.png";
            };
            InputStream stream = CatanBoardGameView.class.getResourceAsStream(filename);
            if (stream == null) {
                System.err.println("Image not found: " + filename);
                return new Image("/Icons/error.png"); // fallback
            }
            return new Image(stream);
        });

        ImageView imageView = new ImageView(image);
        double imageWidth = Math.sqrt(3) * hexSize;
        double imageHeight = 2 * hexSize;

        imageView.setFitWidth(imageWidth);
        imageView.setFitHeight(imageHeight);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        imageView.setX(Math.round(x - imageWidth / 2));
        imageView.setY(Math.round(y - imageHeight / 2));

        return imageView;
    }

    public List<Vertex> getVertices() {
        return vertices;
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

    public int getTileDiceNumber() {
        return tileDiceNumber;
    }

    public Resource.ResourceType getResourcetype() {
        return resourcetype;
    }

    public Point2D getCenter() {
        return center;
    }
}