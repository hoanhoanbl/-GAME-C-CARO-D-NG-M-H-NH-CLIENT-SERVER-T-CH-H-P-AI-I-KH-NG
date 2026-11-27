package caro.server.model.bot;

import caro.common.model.Board;
import caro.common.model.Symbol;
import caro.common.model.WinChecker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Bot cầm O, người cầm X. Độ sâu 2 = mức 2 trong docs/05-thiet-ke-ai.md. */
class AIEngineTest {
    private final AIEngine engine = new AIEngine();

    static void row(Board b, int row, int col, int n, Symbol s) {
        for (int i = 0; i < n; i++) b.place(row, col + i, s);
    }

    /** Đặt thử nước bot chọn, trả về true nếu nước đó tạo 5 quân. */
    static boolean wins(Board b, int[] move, Symbol s) {
        Board copy = b.copy();
        copy.place(move[0], move[1], s);
        return !WinChecker.findWinLine(copy, move[0], move[1]).isEmpty();
    }

    @Test
    void emptyBoardPlaysCenter() {
        assertArrayEquals(new int[] { 7, 7 }, engine.bestMove(new Board(), Symbol.O, 2));
    }

    // T-13: bot thắng ngay khi có 4 quân
    @Test
    void botCompletesOpenFour() {
        Board b = new Board();
        row(b, 7, 4, 4, Symbol.O);           // O ở (7,4)..(7,7)
        b.place(8, 4, Symbol.X);
        b.place(8, 5, Symbol.X);
        b.place(8, 6, Symbol.X);
        int[] m = engine.bestMove(b, Symbol.O, 2);
        assertTrue(wins(b, m, Symbol.O), "phải đánh (7,3) hoặc (7,8), nhận " + m[0] + "," + m[1]);
    }

    @Test
    void botCompletesClosedFourVertical() {
        Board b = new Board();
        b.place(2, 10, Symbol.X);             // chặn đầu trên
        for (int r = 3; r < 7; r++) b.place(r, 10, Symbol.O);
        b.place(5, 5, Symbol.X);
        b.place(6, 6, Symbol.X);
        b.place(7, 7, Symbol.X);
        assertArrayEquals(new int[] { 7, 10 }, engine.bestMove(b, Symbol.O, 2));
    }

    @Test
    void botCompletesBrokenFourEvenAtDepthOne() {
        Board b = new Board();
        // O O _ O O theo chéo xuôi, ô trống ở (6,6)
        b.place(4, 4, Symbol.O);
        b.place(5, 5, Symbol.O);
        b.place(7, 7, Symbol.O);
        b.place(8, 8, Symbol.O);
        b.place(10, 2, Symbol.X);
        b.place(10, 3, Symbol.X);
        b.place(10, 4, Symbol.X);
        assertArrayEquals(new int[] { 6, 6 }, engine.bestMove(b, Symbol.O, 1));
    }

    @Test
    void botPrefersWinningOverBlocking() {
        Board b = new Board();
        b.place(3, 0, Symbol.O);              // chặn X một đầu
        row(b, 3, 1, 4, Symbol.X);            // X ở (3,1)..(3,4), thắng nếu đánh (3,5)
        b.place(10, 3, Symbol.X);             // chặn O một đầu
        row(b, 10, 4, 4, Symbol.O);           // O ở (10,4)..(10,7), thắng nếu đánh (10,8)
        assertArrayEquals(new int[] { 10, 8 }, engine.bestMove(b, Symbol.O, 2));
    }

    // T-14: người có 4 quân, bot phải chặn
    @Test
    void botBlocksClosedFour() {
        Board b = new Board();
        b.place(7, 3, Symbol.O);              // O đã chặn đầu trái
        row(b, 7, 4, 4, Symbol.X);            // X ở (7,4)..(7,7)
        b.place(9, 9, Symbol.O);
        b.place(10, 10, Symbol.O);
        b.place(11, 11, Symbol.O);            // O có ba mở, hấp dẫn nhưng phải chặn trước
        assertArrayEquals(new int[] { 7, 8 }, engine.bestMove(b, Symbol.O, 2));
    }

    @Test
    void botBlocksFourAgainstEdge() {
        Board b = new Board();
        for (int r = 0; r < 4; r++) b.place(r, 0, Symbol.X);   // sát biên trên, cột 0
        b.place(5, 5, Symbol.O);
        b.place(6, 6, Symbol.O);
        assertArrayEquals(new int[] { 4, 0 }, engine.bestMove(b, Symbol.O, 2));
    }

    @Test
    void botBlocksBrokenFour() {
        Board b = new Board();
        // X X _ X X theo chéo ngược, ô trống ở (7,7)
        b.place(5, 9, Symbol.X);
        b.place(6, 8, Symbol.X);
        b.place(8, 6, Symbol.X);
        b.place(9, 5, Symbol.X);
        b.place(2, 2, Symbol.O);
        b.place(2, 3, Symbol.O);
        assertArrayEquals(new int[] { 7, 7 }, engine.bestMove(b, Symbol.O, 2));
    }

    @Test
    void botPreventsDoubleOpenThree() {
        Board b = new Board();
        // X đánh (7,7) sẽ có ba mở ở hàng 7 và ba mở ở cột 7 cùng lúc
        b.place(7, 5, Symbol.X);
        b.place(7, 6, Symbol.X);
        b.place(5, 7, Symbol.X);
        b.place(6, 7, Symbol.X);
        b.place(2, 2, Symbol.O);
        b.place(2, 3, Symbol.O);
        assertArrayEquals(new int[] { 7, 7 }, engine.bestMove(b, Symbol.O, 2));
    }

    @Test
    void botDoesNotAllowBrokenThreePlusThreeFork() {
        Board b = new Board();
        // X đánh (7,7) sẽ có ba gãy mở X_XX ở hàng 7 (cột 4,6,7) và ba mở ở cột 7 (hàng 5..7)
        b.place(7, 4, Symbol.X);
        b.place(7, 6, Symbol.X);
        b.place(5, 7, Symbol.X);
        b.place(6, 7, Symbol.X);
        b.place(2, 2, Symbol.O);
        b.place(2, 3, Symbol.O);
        int[] m = engine.bestMove(b, Symbol.O, 2);
        Board after = b.copy();
        after.place(m[0], m[1], Symbol.O);
        for (int[] reply : MoveGenerator.candidates(after)) {
            after.place(reply[0], reply[1], Symbol.X);
            assertTrue(Evaluator.stats(after, Symbol.X).threats() < 2,
                    "bot đánh " + m[0] + "," + m[1] + " nhưng X đánh " + reply[0] + "," + reply[1] + " vẫn có thế đôi");
            after.place(reply[0], reply[1], Symbol.EMPTY);
        }
    }

    @Test
    void boardIsUnchangedAfterSearch() {
        Board b = new Board();
        row(b, 7, 5, 3, Symbol.X);
        b.place(8, 6, Symbol.O);
        Board before = b.copy();
        engine.bestMove(b, Symbol.O, 2);
        for (int r = 0; r < Board.SIZE; r++)
            for (int c = 0; c < Board.SIZE; c++) assertEquals(before.get(r, c), b.get(r, c));
    }

    @Test
    void invalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> engine.bestMove(new Board(), Symbol.O, 0));
        assertThrows(IllegalArgumentException.class, () -> engine.bestMove(new Board(), Symbol.EMPTY, 1));
        Board full = new Board();
        for (int r = 0; r < Board.SIZE; r++)
            for (int c = 0; c < Board.SIZE; c++) full.place(r, c, (r + c) % 2 == 0 ? Symbol.X : Symbol.O);
        assertThrows(IllegalStateException.class, () -> engine.bestMove(full, Symbol.O, 1));
    }
}
