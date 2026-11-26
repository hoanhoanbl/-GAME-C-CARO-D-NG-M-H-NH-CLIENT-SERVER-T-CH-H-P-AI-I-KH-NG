package caro.common.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WinCheckerTest {

    /** Đặt {@code n} quân {@code s} bắt đầu từ (row, col) theo hướng (dr, dc). */
    private static void line(Board b, Symbol s, int row, int col, int dr, int dc, int n) {
        for (int i = 0; i < n; i++) b.place(row + i * dr, col + i * dc, s);
    }

    private static void assertLine(List<int[]> actual, int[]... expected) {
        assertEquals(expected.length, actual.size(), "độ dài đường thắng");
        for (int i = 0; i < expected.length; i++) {
            assertArrayEquals(expected[i], actual.get(i), "ô thứ " + i);
        }
    }

    @Test
    void horizontal() {
        Board b = new Board();
        line(b, Symbol.X, 7, 3, 0, 1, 5);
        assertLine(WinChecker.findWinLine(b, 7, 5),
                new int[] {7, 3}, new int[] {7, 4}, new int[] {7, 5}, new int[] {7, 6}, new int[] {7, 7});
    }

    @Test
    void vertical() {
        Board b = new Board();
        line(b, Symbol.O, 2, 9, 1, 0, 5);
        assertLine(WinChecker.findWinLine(b, 6, 9),
                new int[] {2, 9}, new int[] {3, 9}, new int[] {4, 9}, new int[] {5, 9}, new int[] {6, 9});
    }

    @Test
    void diagonalDown() {
        Board b = new Board();
        line(b, Symbol.X, 4, 4, 1, 1, 5);
        assertLine(WinChecker.findWinLine(b, 6, 6),
                new int[] {4, 4}, new int[] {5, 5}, new int[] {6, 6}, new int[] {7, 7}, new int[] {8, 8});
    }

    @Test
    void diagonalUp() {
        Board b = new Board();
        line(b, Symbol.O, 4, 10, 1, -1, 5);
        assertLine(WinChecker.findWinLine(b, 8, 6),
                new int[] {4, 10}, new int[] {5, 9}, new int[] {6, 8}, new int[] {7, 7}, new int[] {8, 6});
    }

    @Test
    void lastStoneInTheMiddleOfLine() {
        Board b = new Board();
        line(b, Symbol.X, 7, 3, 0, 1, 2);
        line(b, Symbol.X, 7, 6, 0, 1, 2);
        assertTrue(WinChecker.findWinLine(b, 7, 3).isEmpty());
        b.place(7, 5, Symbol.X);
        assertEquals(5, WinChecker.findWinLine(b, 7, 5).size());
    }

    @Test
    void fourIsNotAWin() {
        Board b = new Board();
        line(b, Symbol.X, 7, 3, 0, 1, 4);
        for (int c = 3; c <= 6; c++) assertTrue(WinChecker.findWinLine(b, 7, c).isEmpty());
    }

    @Test
    void sixInARowWinsWithWholeLine() {
        Board b = new Board();
        line(b, Symbol.X, 7, 2, 0, 1, 6);
        List<int[]> win = WinChecker.findWinLine(b, 7, 4);
        assertEquals(6, win.size());
        assertArrayEquals(new int[] {7, 2}, win.get(0));
        assertArrayEquals(new int[] {7, 7}, win.get(5));
    }

    @Test
    void blockedBothEndsStillWins() {
        // D5: không áp dụng luật chặn hai đầu
        Board b = new Board();
        b.place(7, 2, Symbol.O);
        line(b, Symbol.X, 7, 3, 0, 1, 5);
        b.place(7, 8, Symbol.O);
        assertEquals(5, WinChecker.findWinLine(b, 7, 5).size());
    }

    @Test
    void opponentStoneBreaksLine() {
        Board b = new Board();
        line(b, Symbol.X, 7, 3, 0, 1, 3);
        b.place(7, 6, Symbol.O);
        line(b, Symbol.X, 7, 7, 0, 1, 3);
        assertTrue(WinChecker.findWinLine(b, 7, 5).isEmpty());
        assertTrue(WinChecker.findWinLine(b, 7, 7).isEmpty());
    }

    @Test
    void winOnEdges() {
        Board top = new Board();
        line(top, Symbol.X, 0, 10, 0, 1, 5);          // hàng 0, chạm cột 14
        assertEquals(5, WinChecker.findWinLine(top, 0, 14).size());

        Board left = new Board();
        line(left, Symbol.O, 10, 0, 1, 0, 5);         // cột 0, chạm hàng 14
        assertEquals(5, WinChecker.findWinLine(left, 14, 0).size());

        Board corner = new Board();
        line(corner, Symbol.X, 10, 10, 1, 1, 5);      // chéo tới góc (14,14)
        assertEquals(5, WinChecker.findWinLine(corner, 14, 14).size());

        Board anti = new Board();
        line(anti, Symbol.O, 0, 14, 1, -1, 5);        // chéo ngược từ góc (0,14)
        assertEquals(5, WinChecker.findWinLine(anti, 0, 14).size());
    }

    @Test
    void lineDoesNotWrapAroundEdge() {
        Board b = new Board();
        line(b, Symbol.X, 3, 12, 0, 1, 3);   // (3,12)..(3,14)
        line(b, Symbol.X, 4, 0, 0, 1, 2);    // (4,0),(4,1)
        assertTrue(WinChecker.findWinLine(b, 3, 14).isEmpty());
        assertTrue(WinChecker.findWinLine(b, 4, 0).isEmpty());
    }

    @Test
    void emptyCellIsNotAWin() {
        assertTrue(WinChecker.findWinLine(new Board(), 7, 7).isEmpty());
    }

    @Test
    void fullBoardWithoutFiveIsDraw() {
        Board b = new Board();
        for (int r = 0; r < Board.SIZE; r++)
            for (int c = 0; c < Board.SIZE; c++)
                b.place(r, c, drawPattern(r, c));
        assertTrue(b.isFull());
        for (int r = 0; r < Board.SIZE; r++)
            for (int c = 0; c < Board.SIZE; c++)
                assertTrue(WinChecker.findWinLine(b, r, c).isEmpty(), "không được thắng tại " + r + "," + c);
    }

    /**
     * Mỗi hàng là chuỗi XXOO lặp, hàng sau lệch 2 cột so với hàng trước. Theo dọc / chéo xuôi / chéo ngược
     * chuỗi XXOO bị dịch 2 / 3 / 1 bước mỗi ô, nên không hướng nào có quá 2 quân liên tiếp.
     */
    private static Symbol drawPattern(int r, int c) {
        return ((c + 2 * r) / 2) % 2 == 0 ? Symbol.X : Symbol.O;
    }

    @Test
    void gameResultFactories() {
        GameResult draw = GameResult.draw();
        assertTrue(draw.isDraw());
        assertEquals(EndReason.FULL, draw.reason());
        assertTrue(draw.winLine().isEmpty());

        GameResult left = GameResult.win(Symbol.O, EndReason.LEFT);
        assertFalse(left.isDraw());
        assertEquals(Symbol.O, left.winner());

        Board b = new Board();
        line(b, Symbol.X, 0, 0, 0, 1, 5);
        GameResult five = GameResult.five(Symbol.X, WinChecker.findWinLine(b, 0, 0));
        assertEquals(EndReason.FIVE, five.reason());
        assertEquals(5, five.winLine().size());

        assertThrows(IllegalArgumentException.class, () -> new GameResult(Symbol.EMPTY, EndReason.FIVE, null));
        assertThrows(NullPointerException.class, () -> new GameResult(Symbol.X, null, null));
    }
}
