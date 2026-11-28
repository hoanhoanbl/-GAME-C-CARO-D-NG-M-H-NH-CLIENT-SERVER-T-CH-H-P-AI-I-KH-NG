package caro.client.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GameInfoTest {

    @Test
    void testGameInfoProperties() {
        GameInfo info = new GameInfo("R1", "X", "Bob", "X", false);

        assertEquals("R1", info.roomId());
        assertEquals("X", info.mySymbol());
        assertEquals("Bob", info.opponentName());
        assertEquals("X", info.firstSymbol());
        assertFalse(info.vsBot());
    }

    @Test
    void testGameInfoBotMatch() {
        GameInfo info = new GameInfo("BOT_1", "X", "BOT", "X", true);

        assertEquals("BOT_1", info.roomId());
        assertEquals("BOT", info.opponentName());
        assertTrue(info.vsBot());
    }
}
