package vn.edu.vku.caro.client.model;

import org.junit.jupiter.api.Test;
import vn.edu.vku.caro.common.model.Symbol;

import static org.junit.jupiter.api.Assertions.*;

public class GameModelTest {
    @Test
    void startsWithX() {
        assertEquals(Symbol.X, new GameModel().currentTurn.get());
    }

    @Test
    void applyMovedPlacesPieceSwitchesTurnAndBumpsRevision() {
        GameModel m = new GameModel();
        int before = m.revision.get();
        m.applyMoved(Symbol.X, 7, 7, Symbol.O);
        assertEquals(Symbol.X, m.board.get(7, 7));
        assertEquals(Symbol.O, m.currentTurn.get());
        assertArrayEquals(new int[] { 7, 7 }, m.lastMove());
        assertTrue(m.revision.get() > before);
    }

    @Test
    void isMyTurnFollowsSymbolAndFinished() {
        GameModel m = new GameModel();
        m.mySymbol.set(Symbol.O);
        assertFalse(m.isMyTurn());
        m.applyMoved(Symbol.X, 0, 0, Symbol.O);
        assertTrue(m.isMyTurn());
        m.applyGameOver("");
        assertFalse(m.isMyTurn());
    }

    @Test
    void gameOverParsesWinLine() {
        GameModel m = new GameModel();
        m.applyGameOver("0,0;0,1;0,2");
        assertTrue(m.finished.get());
        assertEquals(3, m.winLine().size());
        assertArrayEquals(new int[] { 0, 2 }, m.winLine().get(2));
    }

    @Test
    void parseCellsHandlesEmpty() {
        assertTrue(GameModel.parseCells("").isEmpty());
        assertTrue(GameModel.parseCells(null).isEmpty());
    }
}
