package caro.client.model;

import caro.common.model.EndReason;
import caro.common.model.Symbol;
import org.junit.jupiter.api.Test;

import java.util.List;

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

    @Test
    void finishSetsOutcomeAndEndsTurns() {
        GameModel m = new GameModel();
        m.start(Symbol.X, "Bình", Symbol.X);
        m.errorMessage.set("lỗi cũ");
        List<GameModel.Cell> line = List.of(new GameModel.Cell(7, 3), new GameModel.Cell(7, 4),
                new GameModel.Cell(7, 5), new GameModel.Cell(7, 6), new GameModel.Cell(7, 7));

        m.finish(Symbol.X, EndReason.FIVE, line);

        assertTrue(m.isOver());
        assertFalse(m.isMyTurn());
        assertEquals(line, m.outcome.get().winLine());
        assertEquals(GameModel.Result.WIN, m.outcome.get().resultFor(Symbol.X));
        assertEquals(GameModel.Result.LOSE, m.outcome.get().resultFor(Symbol.O));
        assertEquals("", m.errorMessage.get());
    }

    @Test
    void drawOutcome() {
        GameModel m = new GameModel();
        m.finish(null, EndReason.FULL, List.of());
        assertTrue(m.outcome.get().isDraw());
        assertEquals(GameModel.Result.DRAW, m.outcome.get().resultFor(Symbol.O));
    }

    @Test
    void startClearsOutcome() {
        GameModel m = new GameModel();
        m.finish(Symbol.O, EndReason.SURRENDER, null);
        m.start(Symbol.O, "Bình", Symbol.X);
        assertFalse(m.isOver());
        assertNull(m.outcome.get());
    }

    @Test
    void finishRejectsBadInput() {
        GameModel m = new GameModel();
        assertThrows(IllegalArgumentException.class, () -> m.finish(Symbol.X, null, List.of()));
        assertThrows(IllegalArgumentException.class, () -> m.finish(Symbol.EMPTY, EndReason.FIVE, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> m.finish(Symbol.X, EndReason.FIVE, List.of(new GameModel.Cell(15, 0))));
        assertFalse(m.isOver());
    }
}
