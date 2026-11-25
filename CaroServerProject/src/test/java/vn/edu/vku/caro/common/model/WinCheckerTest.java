package vn.edu.vku.caro.common.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WinCheckerTest {
    private static Board line(int r0, int c0, int dr, int dc, int n, Symbol s) {
        Board b = new Board();
        for (int i = 0; i < n; i++) b.place(r0 + i * dr, c0 + i * dc, s);
        return b;
    }

    @Test void horizontalFive() { assertEquals(5, WinChecker.findWinLine(line(3, 2, 0, 1, 5, Symbol.X), 3, 4).size()); }
    @Test void verticalFive() { assertEquals(5, WinChecker.findWinLine(line(2, 6, 1, 0, 5, Symbol.O), 2, 6).size()); }
    @Test void diagonalFive() { assertEquals(5, WinChecker.findWinLine(line(1, 1, 1, 1, 5, Symbol.X), 3, 3).size()); }
    @Test void antiDiagonalFive() { assertEquals(5, WinChecker.findWinLine(line(0, 8, 1, -1, 5, Symbol.X), 4, 4).size()); }
    @Test void fourIsNotAWin() { assertTrue(WinChecker.findWinLine(line(3, 2, 0, 1, 4, Symbol.X), 3, 2).isEmpty()); }
    @Test void sixInARowWinsAndReturnsAllSix() { assertEquals(6, WinChecker.findWinLine(line(5, 0, 0, 1, 6, Symbol.X), 5, 3).size()); }

    @Test void fiveAtBoardEdge() {
        assertEquals(5, WinChecker.findWinLine(line(0, 10, 0, 1, 5, Symbol.X), 0, 14).size());
        assertEquals(5, WinChecker.findWinLine(line(10, 14, 1, 0, 5, Symbol.O), 14, 14).size());
    }

    @Test void blockedBothEndsStillWins() {
        Board b = line(7, 3, 0, 1, 5, Symbol.X);
        b.place(7, 2, Symbol.O);
        b.place(7, 8, Symbol.O);
        assertEquals(5, WinChecker.findWinLine(b, 7, 5).size());
    }

    @Test void brokenLineIsNotAWin() {
        Board b = line(7, 3, 0, 1, 5, Symbol.X);
        b.place(7, 5, Symbol.O);
        assertTrue(WinChecker.findWinLine(b, 7, 3).isEmpty());
    }

    @Test void emptyCellHasNoLine() { assertTrue(WinChecker.findWinLine(new Board(), 7, 7).isEmpty()); }
}
