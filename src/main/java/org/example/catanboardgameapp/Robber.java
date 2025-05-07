package org.example.catanboardgameapp;
import java.util.*;
public class Robber {
    private Tile currentTile;

    //___________________________CONSTRUCTOR___________________________
    public Robber (Tile startingTile) {
        this.currentTile = startingTile;
    }

    //___________________________FUNCTIONS___________________________

    // Move Mr Robber De Niro
    public void moveTo(Tile newTile) {
        this.currentTile = newTile;
    }

    //___________________________GETTERS___________________________

    public Tile getCurrentTile() {
        return currentTile;
    }

    // get list of players that can be stolen from
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
