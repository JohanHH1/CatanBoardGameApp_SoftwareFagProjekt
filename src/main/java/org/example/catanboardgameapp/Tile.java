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
import java.util.List;

public class Tile {
    private final int q;
    private final int r;
    private final int tileDiceNumber;
    private final Point2D center;
    private final Resource.ResourceType resourcetype;
    private List<Vertex> vertices;
    private List<Edge> edges;

    //___________________CONSTRUCTOR______________________

    public Tile(int q, int r, Resource.ResourceType resourcetype, int tileDiceNumber, Point2D center) {
        this.q = q;
        this.r = r;
        this.resourcetype = resourcetype;
        this.tileDiceNumber = tileDiceNumber;
        this.center = center;
    }

    //__________________________ACTION FUNCTIONS______________________________

    // Draws all the hex tiles and overlays them with icons and dice numbers
    public static Group createBoardTiles(Board board, int radius) {
        Group boardGroup = new Group();

        for (Tile tile : Board.getTiles()) {
            Polygon hexShape = DrawOrDisplay.createTilePolygon(tile);
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
                Rectangle background = DrawOrDisplay.createBoxBehindDiceNumber(sample, centerX, centerY);

                // Center align text
                numberText.setX(centerX - numberText.getLayoutBounds().getWidth() / 2);
                numberText.setY(centerY + numberText.getLayoutBounds().getHeight() / 4);

                boardGroup.getChildren().addAll(background, numberText);
            }
        }

        return boardGroup;
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
        String filename = switch (type) {
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
            return new ImageView(); // fallback
        }

        Image image = new Image(stream);
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
