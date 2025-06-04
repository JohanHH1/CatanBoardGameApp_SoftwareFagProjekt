package org.example.catanboardgameapp;

import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
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
    private Harbor harbor; // optional
    private boolean sea = false;          // default = land

    //___________________CONSTRUCTOR______________________//
    public Tile(int q, int r, Resource.ResourceType resourcetype,
                int tileDiceNumber, Point2D center, int boardRadius) {
        this.q = q;
        this.r = r;
        this.resourcetype = resourcetype;
        this.tileDiceNumber = tileDiceNumber;
        this.center = center;
    }

    //_____________________________SETTERS___________________________________//
    public void setVertices(List<Vertex> vertices) { this.vertices = vertices; }
    public void setEdges(List<Edge> edges)          { this.edges = edges;     }
    public void setSea(boolean sea)                 { this.sea = sea;         }

    //______________________________GETTERS__________________________________//
    public boolean isSea()            { return sea; }
    public List<Vertex> getVertices() { return vertices; }
    public List<Edge> getEdges()      { return edges; }
    public int getQ()                 { return q; }
    public int getR()                 { return r; }
    public int getTileDiceNumber()    { return tileDiceNumber; }
    public Resource.ResourceType getResourcetype() { return resourcetype; }
    public Point2D getCenter()        { return center; }

    // Returns a color depending on the tile's resource type
    public Color getTileColor(Resource.ResourceType type) {
        return switch (type) {
            case BRICK  -> Color.SADDLEBROWN;
            case WOOD   -> Color.DARKGREEN;
            case ORE    -> Color.DARKGRAY;
            case GRAIN  -> Color.GOLD;
            case WOOL   -> Color.YELLOWGREEN;
            case DESERT -> Color.BEIGE;
            case SEA   -> Color.CORNFLOWERBLUE;
        };
    }

    // Loads the correct resource icon for a tile
    public ImageView getResourceIcon(Resource.ResourceType type,
                                     double x, double y, double hexSize) {
        // Don't return icon for SEA tiles
        if (type == Resource.ResourceType.SEA) return null;

        Image image = imageCache.computeIfAbsent(type, t -> {
            String filename = switch (t) {
                case BRICK  -> "/Icons/brick.png";
                case WOOD   -> "/Icons/wood.png";
                case ORE    -> "/Icons/ore.png";
                case GRAIN  -> "/Icons/grain.png";
                case WOOL   -> "/Icons/wool.png";
                case DESERT -> "/Icons/desert.png";
                default     -> "/Icons/error.png"; // fallback for safety
            };
            InputStream stream = CatanBoardGameView.class.getResourceAsStream(filename);
            return stream == null ? new Image("/Icons/error.png") : new Image(stream);
        });

        ImageView iv = new ImageView(image);
        double w = Math.sqrt(3) * hexSize, h = 2 * hexSize;
        iv.setFitWidth(w);
        iv.setFitHeight(h);
        iv.setPreserveRatio(false);
        iv.setSmooth(true);
        iv.setX(Math.round(x - w / 2));
        iv.setY(Math.round(y - h / 2));
        return iv;
    }



    public void setHarbor(Harbor harbor) {
        this.harbor = harbor;
    }

    public Harbor getHarbor() {
        return harbor;
    }

}
