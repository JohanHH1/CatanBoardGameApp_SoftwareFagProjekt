package org.example.catanboardgameapp;
import org.example.catanboardgameviews.CatanBoardGameView;
import org.example.catanboardgameviews.MenuView;
import org.example.controller.GameController;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.Mockito.lenient;
import java.lang.reflect.Field;
import org.mockito.*;
import javafx.stage.Stage;
import javafx.application.Platform;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class GameplayTest {

    @Mock private GameController mockGameController;
    @Mock private MenuView mockMenuView;
    @Mock private CatanBoardGameView mockBoardView;
    @Mock private Stage mockStage;

    private Gameplay gameplay;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        try {
            // initialize javafx
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // already initialized—ignore
        }
    }

    @BeforeEach
    void setUp() {
        gameplay = new Gameplay(/*boardRadius=*/2, mockGameController);
        gameplay.setMenuView(mockMenuView);
        gameplay.setCatanBoardGameView(mockBoardView);
        // Two human players, no AI, no shuffle
        gameplay.initializeAllPlayers(2, 0, 0, 0, false);
        assertEquals(2, gameplay.getPlayerList().size());
    }

    // ------------------------- Helper ------------------------- //
    // Helper to flip private boolean
    private static void setField(Object target, String fieldName, Object newValue) {
        try {
            Field f = Gameplay.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, newValue);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    void testEachPlayerBuildsTwoSettlementsAndTwoRoads() {
        // Classic 4-step opening: P1 → P2 → P2 → P1
        for (int turn = 0; turn < 4; turn++) {
            Player current = gameplay.getCurrentPlayer();

            // Build two vertices + connecting edge
            Vertex v1 = new Vertex(turn, turn);
            Vertex v2 = new Vertex(turn, turn + 1);
            Edge   e  = new Edge(v1, v2);

            // create 1 tile with a resource type
            Tile land = mock(Tile.class);
            when(land.isSea()).thenReturn(false);
            lenient().when(land.getResourcetype())
                    .thenReturn(Resource.ResourceType.BRICK);
            v1.addAdjacentTile(land);
            v2.addAdjacentTile(land);
            e.addAdjacentTile(land);

            // settlement
            BuildResult sr = gameplay.buildInitialSettlement(v1);
            assertEquals(BuildResult.SUCCESS, sr);

            // road
            BuildResult rr = gameplay.buildRoad(e);
            assertEquals(BuildResult.SUCCESS, rr);

            // Next placement
            gameplay.nextPlayerTurn();
        }

        // After 4 placements, initialPhase ends
        assertFalse(gameplay.isInInitialPhase());

        // Each player now has exactly 2 settlements & 2 roads
        for (Player p : gameplay.getPlayerList()) {
            assertEquals(2, p.getSettlements().size());
            assertEquals(2, p.getRoads().size());
        }
    }

    @Test
    void testUpgradeSettlementToCity() {
            // prepare the board so we're out of initial phase and
            // in main phase
        setField(gameplay, "initialPhase", false);
        when(mockMenuView.getMaxCities()).thenReturn(4);
        Player p = gameplay.getCurrentPlayer();

        // Make a vertex, mark it as a settlement, and give owner to player
        Vertex v = new Vertex(0, 0);
        v.makeSettlement();
        v.setOwner(p);
        p.getSettlements().add(v);

        // Give player 3 ore and 2 grain
        gameplay.addResource("Ore", 3);
        gameplay.addResource("Grain", 2);

        // Call buildCity on that same vertex
        BuildResult result = gameplay.buildCity(v);

        // Verify the outcome
        assertEquals(BuildResult.UPGRADED_TO_CITY, result);

        // settlement list no longer contains v
        assertFalse(p.getSettlements().contains(v));

        // cities list now contains v
        assertTrue(p.getCities().contains(v));

        // – resources were spent
        assertEquals(0, p.getResources().getOrDefault("Ore", 0));
        assertEquals(0, p.getResources().getOrDefault("Grain", 0));

    }

    @Test
    void testBuildRoadMainPhaseSuccess() {
        // go to main phase
        setField(gameplay, "initialPhase", false);
        // give plenty road capacity
        when(mockMenuView.getMaxRoads()).thenReturn(15);
        // prepare land tile
        Tile land = mock(Tile.class);
        when(land.isSea()).thenReturn(false);

        // create two neighbouring vertices
        Vertex v1 = new Vertex(0, 0);
        Vertex v2 = new Vertex(0, 1);
        v1.addAdjacentTile(land);
        v2.addAdjacentTile(land);
        //mark as neighbours
        v1.addNeighbor(v2);
        v2.addNeighbor(v1);
        // put settlement on v1 for current player
        Player p = gameplay.getCurrentPlayer();
        v1.makeSettlement();
        v1.setOwner(p);
        p.getSettlements().add(v1);

        // build edge between v1 and v2 and put it on same land tile
        Edge edge = new Edge(v1, v2);
        edge.addAdjacentTile(land);
        edge.addAdjacentTile(land);

        // give player resource for a road
        gameplay.addResource("Brick", 1);
        gameplay.addResource("Wood", 1);

        // do it
        BuildResult result = gameplay.buildRoad(edge);

        // check to see if success
        assertEquals(BuildResult.SUCCESS, result);
        // edge should be added to the players roads
        assertTrue(p.getRoads().contains(edge));
        // brick and wood should have been used.
        assertEquals(0, p.getResources().getOrDefault("Brick", 0));
        assertEquals(0, p.getResources().getOrDefault("Wood", 0));
        }

    @Test
    void testBuildRoadMainPhaseInsufficientResources() {
        // out of initial phase
        setField(gameplay, "initialPhase", false);
        when(mockMenuView.getMaxRoads()).thenReturn(15);

        Tile land = mock(Tile.class);
        when(land.isSea()).thenReturn(false);

        Vertex v1 = new Vertex(1, 1);
        Vertex v2 = new Vertex(1, 2);
        v1.addAdjacentTile(land);
        v2.addAdjacentTile(land);
        v1.addNeighbor(v2);
        v2.addNeighbor(v1);

        Player p = gameplay.getCurrentPlayer();
        v1.makeSettlement();
        v1.setOwner(p);
        p.getSettlements().add(v1);

        Edge edge = new Edge(v1, v2);
        edge.addAdjacentTile(land);
        edge.addAdjacentTile(land);

        // gave no resources to player, so should fail.
        BuildResult result = gameplay.buildRoad(edge);

        assertEquals(BuildResult.INSUFFICIENT_RESOURCES, result);
        assertFalse(p.getRoads().contains(edge));
    }
    @Test
    void testBuildRoadMainPhaseInvalidNotConnected() {
        // main phase
        setField(gameplay, "initialPhase", false);
        // prepare land tile
        Tile land = mock(Tile.class);
        when(land.isSea()).thenReturn(false);

        // get two vertices far from settlement/road
        Vertex v1 = new Vertex(5, 5);
        Vertex v2 = new Vertex(5, 6);
        v1.addAdjacentTile(land);
        v2.addAdjacentTile(land);

        // make the edge
        Edge edge = new Edge(v1, v2);
        edge.addAdjacentTile(land);

        // attempt to build (in invalied position)
        BuildResult result = gameplay.buildRoad(edge);

        // check it rejects as invalid edge
        assertEquals(BuildResult.INVALID_EDGE, result);
        assertFalse(gameplay.getCurrentPlayer().getRoads().contains(edge));
    }

    @Test
    void testBuildSettlementMainPhaseSuccess() {
        // past initial phase
        setField(gameplay, "initialPhase", false);
        // fix max settlements
        when(mockMenuView.getMaxSettlements()).thenReturn(5);
        Tile land = mock(Tile.class);
        when(land.isSea()).thenReturn(false);
        // creating three vertices in a line: start -> mid -> target
        Vertex start = new Vertex(0, 0);
        Vertex mid = new Vertex(0, 1);
        Vertex target = new Vertex(0, 2);
        for (Vertex v : List.of(start, mid, target)) {
            v.addAdjacentTile(land);
        }
        // give player settlement on start vertex
        Player p = gameplay.getCurrentPlayer();
        start.makeSettlement();
        start.setOwner(p);
        p.getSettlements().add(start);

        // build two connected roads from start -> mid and mid -> target
        Edge r1 = new Edge(start, mid);
        Edge r2 = new Edge(mid, target);
        for (Edge e : List.of(r1, r2)) {
            e.addAdjacentTile(land);
        }
        p.getRoads().add(r1);
        p.getRoads().add(r2);

        // Give 4 resources to build settlement (addresource gives to current player)
        gameplay.addResource("Brick", 1);
        gameplay.addResource("Wood", 1);
        gameplay.addResource("Grain", 1);
        gameplay.addResource("Wool", 1);

        // attempt to build
        BuildResult result = gameplay.buildSettlement(target);

        // assert
        assertEquals(BuildResult.SUCCESS, result);
        assertTrue(p.getSettlements().contains(target));
    }

    @Test
    void testBuildSettlementFailsAdjacentSettlement() {
        // past initial phase
        setField(gameplay, "initialPhase", false);
        // prepare land
        Tile land = mock(Tile.class);
        when(land.isSea()).thenReturn(false);

        // create two neighbouring vertices: center (target) and a "blocker" blocking the build
        Vertex center  = new Vertex(1, 1);
        Vertex blocker = new Vertex(1, 2);
        center.addAdjacentTile(land);
        blocker.addAdjacentTile(land);
        // link them so blocker is neighbour
        center.addNeighbor(blocker);
        blocker.addNeighbor(center);

        // put a “fake” settlement on blocker
        blocker.makeSettlement();
        // no need to setOwner or add to playerList since hasSettlement() == true is enough to block
        // give necessary resources to current player
        gameplay.addResource("Brick", 1);
        gameplay.addResource("Wood", 1);
        gameplay.addResource("Grain", 1);
        gameplay.addResource("Wool", 1);

        // try to build on center
        BuildResult result = gameplay.buildSettlement(center);

        // check it is blocked.
        assertEquals(BuildResult.INVALID_VERTEX, result);
        assertFalse(gameplay.getCurrentPlayer().getSettlements().contains(center));
    }

}