package caro.server.model.bot;

import caro.common.model.Board;
import caro.common.model.Symbol;
import org.junit.jupiter.api.Test;

import static caro.server.model.bot.Evaluator.*;
import static org.junit.jupiter.api.Assertions.*;

class EvaluatorTest {

    /** Đặt quân theo chuỗi mẫu ở hàng {@code row} từ cột 0: 'X', 'O', ký tự khác là ô trống. */
    static Board rowOf(int row, String pattern) {
        Board b = new Board();
        putRow(b, row, pattern);
        return b;
    }

    static void putRow(Board b, int row, String pattern) {
        for (int c = 0; c < pattern.length(); c++) {
            char ch = pattern.charAt(c);
            if (ch == 'X') b.place(row, c, Symbol.X);
            else if (ch == 'O') b.place(row, c, Symbol.O);
        }
    }

    // ---- Bảng điểm chuỗi liền ----

    @Test
    void emptyBoardScoresZero() {
        assertEquals(0, evaluate(new Board(), Symbol.O, Symbol.X));
    }

    @Test
    void patternTable() {
        assertEquals(FIVE, patternScore(5, 0));
        assertEquals(FIVE, patternScore(6, 1));
        assertEquals(OPEN_FOUR, patternScore(4, 2));
        assertEquals(CLOSED_FOUR, patternScore(4, 1));
        assertEquals(OPEN_THREE, patternScore(3, 2));
        assertEquals(CLOSED_THREE, patternScore(3, 1));
        assertEquals(OPEN_TWO, patternScore(2, 2));
        assertEquals(CLOSED_TWO, patternScore(2, 1));
        assertEquals(0, patternScore(4, 0));
    }

    @Test
    void openFourHorizontal() {
        assertEquals(OPEN_FOUR, score(rowOf(7, "...XXXX...."), Symbol.X));
    }

    @Test
    void fourBlockedByEdgeIsClosed() {
        assertEquals(CLOSED_FOUR, score(rowOf(3, "XXXX......."), Symbol.X));
    }

    @Test
    void fourBlockedBothEndsIsWorthless() {
        assertEquals(0, score(rowOf(7, "....OXXXXO"), Symbol.X));
    }

    @Test
    void diagonalsAreScanned() {
        Board b = new Board();
        for (int i = 0; i < 3; i++) b.place(5 + i, 5 + i, Symbol.O);     // chéo xuôi
        for (int i = 0; i < 3; i++) b.place(2 + i, 12 - i, Symbol.X);    // chéo ngược
        assertEquals(OPEN_THREE, score(b, Symbol.O));
        assertEquals(OPEN_THREE, score(b, Symbol.X));
    }

    // ---- Đoạn chết ----

    @Test
    void twoStonesSqueezedBetweenOpponentsAreDead() {
        assertEquals(0, score(rowOf(7, "...O.XX.O"), Symbol.X));
    }

    @Test
    void threeAgainstEdgeWithoutRoomAreDead() {
        assertEquals(0, score(rowOf(0, "XXX.O"), Symbol.X));
    }

    @Test
    void segmentOfExactlyFiveStillCounts() {
        // O _ X X X _ O: đoạn đúng 5 ô, vẫn có thể thành 5
        assertEquals(OPEN_THREE, score(rowOf(7, "..O.XXX.O"), Symbol.X));
    }

    // ---- Thế gãy ----

    @Test
    void brokenFourInMiddle() {
        assertEquals(CLOSED_FOUR, score(rowOf(7, "...XX.XX..."), Symbol.X));
    }

    @Test
    void brokenFourOneAndThree() {
        assertEquals(CLOSED_FOUR, score(rowOf(7, "...X.XXX..."), Symbol.X));
    }

    @Test
    void brokenOverlineCountsAsFour() {
        // XX_XXX: lấp ô trống thành 6 quân, vẫn thắng theo luật D5
        Stats st = stats(rowOf(7, "..XX.XXX..."), Symbol.X);
        assertEquals(CLOSED_FOUR, st.score);
        assertEquals(1, st.fours);
    }

    @Test
    void brokenOpenThree() {
        assertEquals(OPEN_THREE, score(rowOf(7, "....X.XX...."), Symbol.X));
    }

    @Test
    void brokenClosedThree() {
        assertEquals(CLOSED_THREE, score(rowOf(7, "OX.XX......"), Symbol.X));
    }

    @Test
    void greedyMergeOfAlternatingStones() {
        // X_X_X: gộp tham lam cặp trái (X_X = hai mở), quân phải đứng riêng (một quân = 0).
        // Ghi lại hành vi hiện tại để sau này đổi có chủ đích.
        assertEquals(OPEN_TWO, score(rowOf(7, "....X.X.X...."), Symbol.X));
    }

    // ---- Thế đôi ----

    @Test
    void fourPlusOpenThreeGetsDoubleThreatBonus() {
        Board b = rowOf(3, "XXXX.......");                 // bốn một đầu ở hàng 3
        for (int r = 8; r < 11; r++) b.place(r, 10, Symbol.X); // ba mở ở cột 10
        Stats st = stats(b, Symbol.X);
        assertEquals(2, st.threats());
        assertEquals(CLOSED_FOUR + OPEN_THREE + OPEN_FOUR, st.score);
    }

    @Test
    void singleOpenThreeGetsNoBonus() {
        assertEquals(OPEN_THREE, score(rowOf(7, "....XXX...."), Symbol.X));
    }

    // ---- Lượt đi ----

    @Test
    void sideToMoveWithFourWins() {
        Board b = rowOf(7, "XOOOO......");                  // O có bốn một đầu
        b.place(12, 12, Symbol.X);
        assertEquals(FIVE, evaluate(b, Symbol.O, Symbol.O));
        assertEquals(-FIVE, evaluate(b, Symbol.X, Symbol.O));
    }

    @Test
    void singleFourOfSideThatJustMovedCanBeBlocked() {
        Board b = rowOf(7, "OXXXX......");                  // X vừa có bốn một đầu, O tới lượt
        long eval = evaluate(b, Symbol.O, Symbol.O);
        assertTrue(eval > -FIVE, "O còn chặn được, nhận " + eval);
    }

    @Test
    void opponentOpenFourIsLost() {
        Board b = rowOf(7, "...XXXX....");
        b.place(12, 3, Symbol.O);
        assertEquals(-FIVE, evaluate(b, Symbol.O, Symbol.O));
    }

    @Test
    void opponentTwoFoursIsLost() {
        Board b = rowOf(3, "XXXX.......");
        for (int r = 7; r < 11; r++) b.place(r, 14, Symbol.X);  // bốn dọc sát biên phải, hai thế khác ô thắng
        b.place(6, 14, Symbol.O);
        assertEquals(-FIVE, evaluate(b, Symbol.O, Symbol.O));
    }

    @Test
    void sideToMoveWithOpenThreeGetsTempoBonus() {
        Board b = rowOf(7, "....OOO....");
        assertEquals(OPEN_THREE + OPEN_FOUR, evaluate(b, Symbol.O, Symbol.O));
        assertEquals(OPEN_THREE, evaluate(b, Symbol.O, Symbol.X));
    }

    @Test
    void staticPositionUsesDefenseWeight() {
        Board b = rowOf(3, "....XX.....");
        putRow(b, 10, "....OO.....");
        assertEquals(OPEN_TWO - OPEN_TWO * 6 / 5, evaluate(b, Symbol.O, Symbol.X));
        assertEquals(-100, evaluate(b, Symbol.O, Symbol.X));
    }

    @Test
    void opponent() {
        assertEquals(Symbol.O, Evaluator.opponent(Symbol.X));
        assertEquals(Symbol.X, Evaluator.opponent(Symbol.O));
        assertThrows(IllegalArgumentException.class, () -> Evaluator.opponent(Symbol.EMPTY));
    }
}
