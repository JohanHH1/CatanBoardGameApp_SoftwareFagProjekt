package org.example.catanboardgameapp;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.example.catanboardgameviews.CatanBoardGameView;
import org.example.catanboardgameviews.MenuView;
import org.example.controller.GameController;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import static org.mockito.Mockito.lenient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class AIOpponentTest {

    @Mock private Gameplay mockGameplay;
    @Mock private DrawOrDisplay mockDrawOrDisplay;
    @Mock private MenuView mockMenuView;
    @Mock private Board mockBoard;
    @Mock private CatanBoardGameView mockBoardView;

    private AIOpponent ai;

    @BeforeAll
    static void initToolkit() {
        try {
            // initialize javafx
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // already initialized — ignore
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        when(mockGameplay.getDrawOrDisplay()).thenReturn(mockDrawOrDisplay);
        when(mockGameplay.getMenuView()).thenReturn(mockMenuView);
        lenient().when(mockGameplay.getBoard()).thenReturn(mockBoard);
        lenient().when(mockBoard.getHarbors()).thenReturn(Collections.emptyList());
        // Create an EASY-level AI
        ai = new AIOpponent(1, Color.RED, AIOpponent.StrategyLevel.EASY, mockGameplay);
    }

    //Helper to flip private fields
    private static void setField(Object target, String fieldName, Object newValue) {
        try {
            Field f = AIOpponent.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, newValue);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testConstructorInitializesFields() {
        assertEquals(1, ai.getPlayerId());
        assertEquals(AIOpponent.StrategyLevel.EASY, ai.getStrategyLevel());
        // strategyUsageMap starts at 0 for every enum
        ai.getStrategyUsageMap().values().forEach(count -> assertEquals(0, count));
    }

    @Test
    void testGetMaxStrategyAttempts() {
        assertEquals(20, ai.getMaxStrategyAttempts());
    }

    @Test
    void testChooseSmartResourceToReceive() {
        // give the AI some resources
        ai.getResources().put("Brick", 2);
        ai.getResources().put("Wood",  3);
        ai.getResources().put("Ore",   1);
        ai.getResources().put("Grain", 1);
        ai.getResources().put("Wool",  5);

        // both Ore and Grain are tied for lowest (1), priority is ore,grain...
        String pick = ai.chooseSmartResourceToReceive();
        assertEquals("Ore", pick);
    }

    @Test
    void testGetNeededResourcesForStrategyCityUpgrader() {
        // set up AI that's missing both ore and grain
        ai.getResources().put("Ore", 2);
        ai.getResources().put("Grain", 1);
        var needed = ai.getNeededResourcesForStrategy(AIOpponent.Strategy.CITYUPGRADER);
        assertTrue(needed.contains("Ore"));
        assertTrue(needed.contains("Grain"));
    }

    @Test
    void testGetNeededResourcesForStrategySettlementPlacer() {
        // Ai with only wood
        ai.getResources().put("Wood", 1);
        var needed = ai.getNeededResourcesForStrategy(AIOpponent.Strategy.SETTLEMENTPLACER);
        assertEquals(Set.of("Brick", "Wool", "Grain"), needed);
    }

    @Test
    void testChooseSmartResourceToGiveForBankTrade_NoHarbors() {
        // no harbors
        when(mockBoard.getHarbors()).thenReturn(Collections.emptyList());
        // ai with lots of brick but no wood
        ai.getResources().put("Brick",5);
        ai.getResources().put("Wood",1);
        String give = ai.chooseSmartResourceToGive();
        assertEquals("Brick", give);
    }

    @Test
    void testChooseBestRobberTargetForHardAI() {
        //create 2 victims
        Player victim1 = new Player(2, Color.BLUE, mockGameplay);
        Player victim2 = new Player(3, Color.GREEN, mockGameplay);
        // victim1 has 3 brick, victim2 has 2 wood
        victim1.getResources().put("Brick", 3);
        victim2.getResources().put("Wood", 2);
        List<Player> victims = List.of(victim1, victim2);

        // ai has no resources, so determines strategy = NONE and needs all resource types
        Player chosen = ai.chooseBestRobberTargetForHardAI(ai, victims);
        // victim1 has 3 resoucres and victim2 on 2, so victim1 should be chosen
        assertEquals(victim1, chosen);
    }
}