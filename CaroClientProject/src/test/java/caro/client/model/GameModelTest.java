package caro.client.model;

import caro.common.model.Symbol;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GameModelTest {
    @Test
    void startsWithX() {
        assertEquals(Symbol.X, new GameModel().currentTurn.get());
    }

    @Test
    void startSetsSymbolsAndOpponent() {
        GameModel m = new GameModel();
        m.start(Symbol.O, "Bình", Symbol.X);
        assertEquals(Symbol.O, m.mySymbol.get());
        assertEquals("Bình", m.opponentName.get());
        assertEquals(Symbol.X, m.currentTurn.get());
        assertFalse(m.isMyTurn());
    }

    @Test
    void applyMovePlacesPieceAndSwitchesTurn() {
        GameModel m = new GameModel();
        m.start(Symbol.X, "Bình", Symbol.X);
        int before = m.boardVersionProperty().get();

        m.applyMove(Symbol.X, 7, 7, Symbol.O);

        assertEquals(Symbol.X, m.board.get(7, 7));
        assertEquals(Symbol.O, m.currentTurn.get());
        assertEquals(new GameModel.Cell(7, 7), m.lastMove.get());
        assertEquals(before + 1, m.boardVersionProperty().get());
        assertFalse(m.isMyTurn());
    }

    @Test
    void applyMoveClearsError() {
        GameModel m = new GameModel();
        m.errorMessage.set("Chưa tới lượt");
        m.applyMove(Symbol.X, 0, 0, Symbol.O);
        assertEquals("", m.errorMessage.get());
    }

    @Test
    void startClearsPreviousGame() {
        GameModel m = new GameModel();
        m.applyMove(Symbol.X, 3, 4, Symbol.O);
        m.errorMessage.set("lỗi");

        m.start(Symbol.X, "BOT", Symbol.X);

        assertEquals(Symbol.EMPTY, m.board.get(3, 4));
        assertNull(m.lastMove.get());
        assertEquals("", m.errorMessage.get());
        assertTrue(m.isMyTurn());
    }

    @Test
    void applyMoveRejectsOutOfBoardOrEmptySymbol() {
        GameModel m = new GameModel();
        assertThrows(IllegalArgumentException.class, () -> m.applyMove(Symbol.X, 15, 0, Symbol.O));
        assertThrows(IllegalArgumentException.class, () -> m.applyMove(Symbol.EMPTY, 0, 0, Symbol.O));
        assertNull(m.lastMove.get());
    }
}
