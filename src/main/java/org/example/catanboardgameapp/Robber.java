package org.example.catanboardgameapp;
import java.util.*;
public class Robber {
    private Tile currentTile;

    public Robber (Tile startingTile) {
        this.currentTile = startingTile;
    }

    public Tile getCurrentTile() {
        return currentTile;
    }
    public void moveTo(Tile newTile) {
        this.currentTile = newTile;
    }
    public List<Player> getPotentialVictims(Tile tile, Player currentPlayer) {
        Set<Player> potentialVictims = new HashSet<>();

        for (Vertex vertex : tile.getVertices()) {
            Player owner = vertex.getOwner();
            if (owner != null && owner != currentPlayer) {
                potentialVictims.add(owner);
            }
        }
        return new ArrayList<>(potentialVictims);
    }

}
