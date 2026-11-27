package caro.server.model.bot;

import caro.common.model.Board;
import caro.common.model.Symbol;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EvaluatorTest {

    /** Đặt {@code n} quân {@code s} theo hàng {@code row} bắt đầu từ cột {@code col}. */
    static void row(Board b, int row, int col, int n, Symbol s) {
        for (int i = 0; i < n; i++) b.place(row, col + i, s);
    }

    @Test
    void emptyBoardScoresZero() {
        assertEquals(0, Evaluator.evaluate(new Board(), Symbol.O));
    }

    @Test
    void patternTable() {
        assertEquals(Evaluator.FIVE, Evaluator.patternScore(5, 0));
        assertEquals(Evaluator.FIVE, Evaluator.patternScore(6, 1));
        assertEquals(Evaluator.OPEN_FOUR, Evaluator.patternScore(4, 2));
        assertEquals(Evaluator.CLOSED_FOUR, Evaluator.patternScore(4, 1));
        assertEquals(Evaluator.OPEN_THREE, Evaluator.patternScore(3, 2));
        assertEquals(Evaluator.CLOSED_THREE, Evaluator.patternScore(3, 1));
        assertEquals(Evaluator.OPEN_TWO, Evaluator.patternScore(2, 2));
        assertEquals(Evaluator.CLOSED_TWO, Evaluator.patternScore(2, 1));
        assertEquals(0, Evaluator.patternScore(4, 0));
    }

    @Test
    void openFourHorizontal() {
        Board b = new Board();
        row(b, 7, 5, 4, Symbol.X);
        assertEquals(Evaluator.OPEN_FOUR, Evaluator.score(b, Symbol.X));
    }

    @Test
    void fourBlockedByEdgeIsClosed() {
        Board b = new Board();
        row(b, 3, 0, 4, Symbol.X);
        assertEquals(Evaluator.CLOSED_FOUR, Evaluator.score(b, Symbol.X));
    }

    @Test
    void fourBlockedBothEndsIsWorthless() {
        Board b = new Board();
        b.place(7, 4, Symbol.O);
        row(b, 7, 5, 4, Symbol.X);
        b.place(7, 9, Symbol.O);
        assertEquals(0, Evaluator.score(b, Symbol.X));
    }

    @Test
    void diagonalsAreScanned() {
        Board b = new Board();
        for (int i = 0; i < 3; i++) b.place(5 + i, 5 + i, Symbol.O);     // chéo xuôi
        for (int i = 0; i < 3; i++) b.place(2 + i, 12 - i, Symbol.X);    // chéo ngược
        assertEquals(Evaluator.OPEN_THREE, Evaluator.score(b, Symbol.O));
        assertEquals(Evaluator.OPEN_THREE, Evaluator.score(b, Symbol.X));
    }

    @Test
    void defenseWeightMakesEqualPositionNegative() {
        Board b = new Board();
        row(b, 3, 3, 3, Symbol.X);
        row(b, 10, 3, 3, Symbol.O);
        long eval = Evaluator.evaluate(b, Symbol.O);
        assertEquals(Evaluator.OPEN_THREE - Evaluator.OPEN_THREE * 6 / 5, eval);
        assertTrue(eval < 0);
    }

    @Test
    void opponent() {
        assertEquals(Symbol.O, Evaluator.opponent(Symbol.X));
        assertEquals(Symbol.X, Evaluator.opponent(Symbol.O));
        assertThrows(IllegalArgumentException.class, () -> Evaluator.opponent(Symbol.EMPTY));
    }
}
