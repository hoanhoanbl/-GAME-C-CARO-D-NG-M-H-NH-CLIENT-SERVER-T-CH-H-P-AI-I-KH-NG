package caro.server.model.bot;

import caro.common.model.Board;
import caro.common.model.Symbol;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MoveGeneratorTest {

    @Test
    void emptyBoardGivesCenterOnly() {
        List<int[]> moves = MoveGenerator.candidates(new Board());
        assertEquals(1, moves.size());
        assertArrayEquals(new int[] { 7, 7 }, moves.get(0));
    }

    @Test
    void singleStoneGivesSquareOfRadiusTwo() {
        Board b = new Board();
        b.place(7, 7, Symbol.X);
        List<int[]> moves = MoveGenerator.candidates(b);
        assertEquals(5 * 5 - 1, moves.size());
        for (int[] m : moves) {
            assertTrue(Math.abs(m[0] - 7) <= 2 && Math.abs(m[1] - 7) <= 2);
            assertTrue(b.isEmpty(m[0], m[1]));
        }
    }

    @Test
    void cornerStoneIsClippedToBoard() {
        Board b = new Board();
        b.place(0, 0, Symbol.O);
        assertEquals(3 * 3 - 1, MoveGenerator.candidates(b).size());
    }

    @Test
    void fullBoardGivesNoMoves() {
        Board b = new Board();
        for (int r = 0; r < Board.SIZE; r++)
            for (int c = 0; c < Board.SIZE; c++) b.place(r, c, (r + c) % 2 == 0 ? Symbol.X : Symbol.O);
        assertTrue(MoveGenerator.candidates(b).isEmpty());
    }
}
