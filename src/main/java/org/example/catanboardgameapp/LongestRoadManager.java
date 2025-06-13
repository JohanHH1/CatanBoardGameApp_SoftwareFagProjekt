package org.example.catanboardgameapp;

import java.util.*;

public class LongestRoadManager {

    private Player currentHolder;
    private final Gameplay gameplay;

    // ---------------- Constructor ---------------- //
    public LongestRoadManager(Gameplay gameplay) {
        this.gameplay = gameplay;
    }

    // ---------------- Public API ---------------- //
    public void calculateAndUpdateLongestRoad(Player player, List<Player> allPlayers) {
        int roadLength = calculateLongestRoad(player, allPlayers);
        player.setLongestRoad(roadLength);

        if (roadLength >= 5) {
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

    public int calculateLongestRoad(Player player, List<Player> allPlayers) {
        Set<Edge> allRoads = new HashSet<>(player.getRoads());
        Set<Vertex> allVertices = new HashSet<>();

        for (Edge e : allRoads) {
            allVertices.add(e.getVertex1());
            allVertices.add(e.getVertex2());
        }

        int longest = 0;
        for (Vertex start : allVertices) {
            Set<Edge> visited = new HashSet<>();
            longest = Math.max(longest, dfs(start, visited, player));
        }

        return longest;
    }

    // ---------------- Internal Logic ---------------- //
    // Depth-first search to find the longest path from a starting vertex.
    private int dfs(Vertex current, Set<Edge> visited, Player player) {
        int maxLength = 0;

        // Check all edges belonging to the player
        for (Edge edge : player.getRoads()) {
            if (!visited.contains(edge) && edge.isConnectedTo(current)) {
                Vertex next = edge.getVertex1().equals(current) ? edge.getVertex2() : edge.getVertex1();

                // Skip if the path is blocked (e.g., by another player's settlement)
                if (player.isBlocked(current, gameplay)) continue;

                visited.add(edge);
                int pathLength = 1 + dfs(next, visited, player);
                visited.remove(edge);

                maxLength = Math.max(maxLength, pathLength);
            }
        }
        return maxLength;
    }


}
