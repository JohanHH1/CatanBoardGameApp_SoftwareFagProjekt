package org.example.catanboardgameapp;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.example.catanboardgameapp.Robber.robberDeNiro;
import static org.junit.jupiter.api.Assertions.*;

public class GameplayTest {

    private Gameplay gameplay;

    @BeforeEach
    public void setup() {
        gameplay = new Gameplay();
        gameplay.initializePlayers(2); // assumes this sets 2 players
    }

    @Test
    public void testInitialPlayerId() {
        Player player = gameplay.getCurrentPlayer();
        assertEquals(1, player.getPlayerId());
    }

    @Test
    public void testAddAndRemoveResources() {
        gameplay.addResource("Brick", 2);
        assertTrue(gameplay.canRemoveResource("Brick", 2));
        gameplay.removeResource("Brick", 2);
        assertFalse(gameplay.canRemoveResource("Brick", 1));
    }

    @Test
    public void testDiceRollInValidRange() {
        for (int i = 0; i < 100; i++) {
            int roll = gameplay.rollDice();
            assertTrue(roll >= 2 && roll <= 12);
        }
    }

    @Test
    public void testRobbingWorksIfVictimHasResources() {
        Player victim = gameplay.getPlayerList().get(1);
        victim.getResources().put("Ore", 2);

        boolean success = Robber.stealResourceFrom(victim);
        assertTrue(success);
    }

    @Test
    public void testRobbingFailsIfVictimHasNoResources() {
        Player victim = gameplay.getPlayerList().get(1);
        victim.getResources().clear();

        boolean success = Robber.stealResourceFrom(victim);
        assertFalse(success);
    }
}