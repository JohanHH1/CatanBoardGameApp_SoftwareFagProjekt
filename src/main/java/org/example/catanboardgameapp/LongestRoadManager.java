package org.example.catanboardgameapp;

import java.util.*;

public class LongestRoadManager {
    private Player currentHolder;
    private final Gameplay gameplay;

    //_________________________________CONSTRUCTOR___________________________________//
    public LongestRoadManager(Gameplay gameplay) {
        this.gameplay = gameplay;
    }

    //_________________________________LONGEST ROAD FUNCTIONS___________________________________//
    // Called after each turn to update who holds the Longest Road
    public void calculateAndUpdateLongestRoad(Player player) {
        int roadLength = calculateLongestRoad(player);
        player.setLongestRoad(roadLength);
        if (roadLength >= 5) {
            if (currentHolder == null || roadLength > calculateLongestRoad(currentHolder)) {
                if (currentHolder != null && currentHolder != player) {
                    gameplay.decreasePlayerScoreByTwo(currentHolder);
                }
                currentHolder = player;
                gameplay.increasePlayerScoreByTwo();
            }
        }
    }

    // Calculates longest continuous road for a player using DFS
    public int calculateLongestRoad(Player player) {
        Set<Edge> allRoads = new HashSet<>(player.getRoads());
        Set<Vertex> allVertices = new HashSet<>();
        for (Edge e : allRoads) {
            allVertices.add(e.getVertex1());
            allVertices.add(e.getVertex2());
        }
        int longest = 0;
        for (Vertex start : allVertices) {
            Set<Edge> visited = new HashSet<>();
            longest = Math.max(longest, dfsPlayer(start, visited, player));
        }
        return longest;
    }

    // Used for AI simulations – calculate longest road from a list of edges (not tied to a player)
    public int calculateLongestRoadFromEdges(List<Edge> edges) {
        Set<Edge> edgeSet = new HashSet<>(edges);
        Set<Vertex> allVertices = new HashSet<>();
        for (Edge e : edgeSet) {
            allVertices.add(e.getVertex1());
            allVertices.add(e.getVertex2());
        }
        int longest = 0;
        for (Vertex start : allVertices) {
            Set<Edge> visited = new HashSet<>();
            longest = Math.max(longest, dfsPlayer(start, visited, edgeSet));
        }
        return longest;
    }

    //_________________________________DFS METHODS___________________________________//
    // DFS based on a Player object (used in gameplay)
    private int dfsPlayer(Vertex current, Set<Edge> visited, Player player) {
        int maxLength = 0;
        for (Edge edge : player.getRoads()) {
            if (!visited.contains(edge) && edge.isConnectedTo(current)) {
                Vertex next = edge.getOppositeVertex(current);
                // Skip blocked paths (e.g., opponent settlements)
                if (player.isBlocked(current, gameplay)) continue;
                visited.add(edge);
                int pathLength = 1 + dfsPlayer(next, visited, player);
                visited.remove(edge);

                maxLength = Math.max(maxLength, pathLength);
            }
        }
        return maxLength;
    }

    // DFS based on a raw edge list (used for AI simulations)
    private int dfsPlayer(Vertex current, Set<Edge> visited, Set<Edge> allEdges) {
        int maxLength = 0;
        for (Edge edge : allEdges) {
            if (!visited.contains(edge) && edge.isConnectedTo(current)) {
                Vertex next = edge.getOppositeVertex(current);
                visited.add(edge);
                int pathLength = 1 + dfsPlayer(next, visited, allEdges);
                visited.remove(edge);
                maxLength = Math.max(maxLength, pathLength);
            }
        }
        return maxLength;
    }

    //_________________________________GETTERS___________________________________//
    public Player getCurrentHolder() {
        return currentHolder;
    }
}