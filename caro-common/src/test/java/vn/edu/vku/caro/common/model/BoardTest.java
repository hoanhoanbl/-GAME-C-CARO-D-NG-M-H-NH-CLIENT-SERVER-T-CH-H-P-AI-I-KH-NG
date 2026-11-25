package vn.edu.vku.caro.common.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BoardTest {
    @Test
    void newBoardIsEmpty() {
        assertTrue(new Board().isEmpty(7, 7));
    }

    @Test
    void outOfBoundsIsNotEmptyAndPlaceThrows() {
        Board b = new Board();
        assertFalse(b.isEmpty(-1, 0));
        assertFalse(b.isEmpty(0, Board.SIZE));
        assertThrows(IllegalArgumentException.class, () -> b.place(15, 0, Symbol.X));
    }

    @Test
    void copyIsIndependent() {
        Board b = new Board();
        Board c = b.copy();
        c.place(1, 1, Symbol.X);
        assertTrue(b.isEmpty(1, 1));
    }

    @Test
    void isFullOnlyWhenEveryCellFilled() {
        Board b = new Board();
        assertFalse(b.isFull());
        for (int r = 0; r < Board.SIZE; r++) for (int c = 0; c < Board.SIZE; c++) b.place(r, c, Symbol.X);
        assertTrue(b.isFull());
    }
}
