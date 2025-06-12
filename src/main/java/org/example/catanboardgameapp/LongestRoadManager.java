package org.example.catanboardgameapp;

import java.util.*;

public class LongestRoadManager {

    private Player currentHolder;
    private final int MIN_LENGTH_FOR_LONGEST = 5;
    private final Gameplay gameplay;

    // ---------------- Constructor ---------------- //
    public LongestRoadManager(Gameplay gameplay) {
        this.gameplay = gameplay;
    }

    // ---------------- Public API ---------------- //
    public void calculateAndUpdateLongestRoad(Player player, List<Player> allPlayers) {
        int roadLength = calculateLongestRoad(player, allPlayers);
        player.setLongestRoad(roadLength);

        if (roadLength >= MIN_LENGTH_FOR_LONGEST) {
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
        Set<Edge> visited = new HashSet<>();
        int longest = 0;

        for (Edge road : player.getRoads()) {
            Vertex v1 = road.getVertex1();
            Vertex v2 = road.getVertex2();

            visited.add(road);
            longest = Math.max(longest, 1 + dfs(v1, visited, player, allPlayers));
            longest = Math.max(longest, 1 + dfs(v2, visited, player, allPlayers));
            visited.remove(road);
        }
        return longest;
    }

    // ---------------- Internal Logic ---------------- //
    private int dfs(Vertex current, Set<Edge> visited, Player player, List<Player> allPlayers) {
        int maxLength = 0;

        for (Edge edge : player.getRoads()) {
            if (!visited.contains(edge) && edge.isConnectedTo(current)) {
                Vertex next = edge.getVertex1().equals(current) ? edge.getVertex2() : edge.getVertex1();

                if (isBlocked(next, player, allPlayers)) continue;

                visited.add(edge);
                int pathLength = 1 + dfs(next, visited, player, allPlayers);
                visited.remove(edge); // backtrack

                maxLength = Math.max(maxLength, pathLength);
            }
        }
        return maxLength;
    }

    private boolean isBlocked(Vertex vertex, Player player, List<Player> allPlayers) {
        for (Player p : allPlayers) {
            if (!p.equals(player) && p.getSettlements().contains(vertex)) {
                return true;
            }
        }
        return false;
    }
}
