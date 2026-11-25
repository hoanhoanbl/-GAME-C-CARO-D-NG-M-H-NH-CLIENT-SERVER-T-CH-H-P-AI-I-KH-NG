package vn.edu.vku.caro.client.model;

import org.junit.jupiter.api.Test;
import vn.edu.vku.caro.common.model.EndReason;
import vn.edu.vku.caro.common.model.GameResult;
import vn.edu.vku.caro.common.model.Symbol;
import vn.edu.vku.caro.common.protocol.Message;

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

    @Test
    void movedMessageUpdatesBoardAndTurn() {
        GameModel m = new GameModel();
        m.applyMoved(Message.parse("MOVED|X|7|8|O"));
        assertEquals(Symbol.X, m.board.get(7, 8));
        assertEquals(Symbol.O, m.currentTurn.get());
    }

    @Test
    void movedMessageWithEmptyNextKeepsTurn() {
        GameModel m = new GameModel();
        m.applyMoved(Message.parse("MOVED|X|0|0|"));
        assertEquals(Symbol.X, m.board.get(0, 0));
        assertEquals(Symbol.X, m.currentTurn.get());
    }

    @Test
    void gameOverMessageFillsResultAndWinLine() {
        GameModel m = new GameModel();
        m.applyGameOver(Message.parse("GAME_OVER|O|FIVE|1,1;1,2;1,3;1,4;1,5"));
        GameResult r = m.result.get();
        assertEquals(Symbol.O, r.winner());
        assertEquals(EndReason.FIVE, r.reason());
        assertEquals(5, r.winLine().size());
        assertTrue(m.finished.get());
        assertEquals(5, m.winLine().size());
    }

    @Test
    void gameOverDrawHasNoWinner() {
        GameModel m = new GameModel();
        m.applyGameOver(Message.parse("GAME_OVER|DRAW|FULL|"));
        assertNull(m.result.get().winner());
        assertEquals(EndReason.FULL, m.result.get().reason());
    }

    @Test
    void errorMessageSetsStatusAndNextMoveClearsIt() {
        GameModel m = new GameModel();
        m.applyError(Message.parse("ERROR|NOT_YOUR_TURN|Chưa đến lượt bạn"));
        assertEquals("Chưa đến lượt bạn", m.statusMessage.get());
        m.applyMoved(Message.parse("MOVED|X|1|1|O"));
        assertEquals("", m.statusMessage.get());
    }
}
