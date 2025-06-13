package org.example.catanboardgameapp;

import java.util.*;

public class LongestRoadManager {

    //Current player with longest road
    private Player currentHolder;
    private final Gameplay gameplay;

    // ---------------- Constructor ---------------- //
    public LongestRoadManager(Gameplay gameplay) {
        this.gameplay = gameplay;
    }

    // Calculates the player's longest road and updates victory points accordingly
    // If the road is at least 5 and longer than the current holder's they take
    // the longest Road and gains +2 VP, and the previous holder loses 2 VP.
    public void calculateAndUpdateLongestRoad(Player player, List<Player> allPlayers) {
        int roadLength = calculateLongestRoad(player, allPlayers);
        player.setLongestRoad(roadLength);

        if (roadLength >= 5) {
            // If no holder or the player has a longer road than current holder
            if (currentHolder == null || roadLength > calculateLongestRoad(currentHolder, allPlayers)) {
                if (currentHolder != null && currentHolder != player) {
                    // Old holder loses Longest Road (-2 VP)
                    gameplay.decreasePlayerScoreByTwo(currentHolder);
                }
                currentHolder = player;

                // New holder gains Longest Road (+2 VP)
                gameplay.increasePlayerScoreByTwo(currentHolder);
            }
        }
    }

    public Player getCurrentHolder() {
        return currentHolder;
    }

    // Calculates longest continous road for a given player
    // Uses DFS (depth first search) to explore all possible paths on the player's roads.
    public int calculateLongestRoad(Player player, List<Player> allPlayers) {
        Set<Edge> allRoads = new HashSet<>(player.getRoads()); //Roads owned by the player
        Set<Vertex> allVertices = new HashSet<>(); // Endpoints of roads

        // Collects all unique vertices connect by the player's roads
        for (Edge e : allRoads) {
            allVertices.add(e.getVertex1());
            allVertices.add(e.getVertex2());
        }

        int longest = 0;

        // Try DFS starting from each vertex
        for (Vertex start : allVertices) {
            Set<Edge> visited = new HashSet<>();
            longest = Math.max(longest, dfs(start, visited, player));
        }

        return longest;
    }

    // Depth first search to find longest road from current vertex.
    // Avoids revisiting edges (roads) and backtracks after exploring each path
    // Skips paths blocked by other players' settlement/cities.
    private int dfs(Vertex current, Set<Edge> visited, Player player) {
        int maxLength = 0;

        // Check all edges belonging to the player
        for (Edge edge : player.getRoads()) {
            // Explores only unvisited edges
            if (!visited.contains(edge) && edge.isConnectedTo(current)) {
                Vertex next = edge.getVertex1().equals(current) ? edge.getVertex2() : edge.getVertex1();

                // Skip blocked paths
                if (player.isBlocked(current, gameplay)) continue;

                visited.add(edge);
                int pathLength = 1 + dfs(next, visited, player);
                visited.remove(edge); // backtrack

                maxLength = Math.max(maxLength, pathLength);
            }
        }
        return maxLength;
    }


}
