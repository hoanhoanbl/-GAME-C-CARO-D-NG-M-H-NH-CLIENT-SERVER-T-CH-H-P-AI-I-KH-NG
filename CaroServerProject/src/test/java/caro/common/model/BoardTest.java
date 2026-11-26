package caro.common.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BoardTest {

    @Test
    void newBoardIsEmpty() {
        Board b = new Board();
        for (int r = 0; r < Board.SIZE; r++)
            for (int c = 0; c < Board.SIZE; c++)
                assertTrue(b.isEmpty(r, c));
        assertFalse(b.isFull());
    }

    @Test
    void placeAndGet() {
        Board b = new Board();
        b.place(7, 7, Symbol.X);
        b.place(0, 14, Symbol.O);
        assertEquals(Symbol.X, b.get(7, 7));
        assertEquals(Symbol.O, b.get(0, 14));
        assertFalse(b.isEmpty(7, 7));
    }

    @Test
    void placeEmptyRemovesStone() {
        Board b = new Board();
        b.place(3, 3, Symbol.X);
        b.place(3, 3, Symbol.EMPTY);
        assertTrue(b.isEmpty(3, 3));
    }

    @Test
    void outOfBoundsIsRejected() {
        Board b = new Board();
        assertThrows(IllegalArgumentException.class, () -> b.place(-1, 0, Symbol.X));
        assertThrows(IllegalArgumentException.class, () -> b.place(0, Board.SIZE, Symbol.X));
        assertThrows(IllegalArgumentException.class, () -> b.place(Board.SIZE, 0, Symbol.X));
        assertThrows(IllegalArgumentException.class, () -> b.get(0, -1));
        assertThrows(IllegalArgumentException.class, () -> b.isEmpty(15, 15));
    }

    @Test
    void nullSymbolIsRejected() {
        assertThrows(NullPointerException.class, () -> new Board().place(0, 0, null));
    }

    @Test
    void isInside() {
        assertTrue(Board.isInside(0, 0));
        assertTrue(Board.isInside(14, 14));
        assertFalse(Board.isInside(-1, 0));
        assertFalse(Board.isInside(0, 15));
    }

    @Test
    void fullBoard() {
        Board b = new Board();
        for (int r = 0; r < Board.SIZE; r++)
            for (int c = 0; c < Board.SIZE; c++)
                b.place(r, c, (r + c) % 2 == 0 ? Symbol.X : Symbol.O);
        assertTrue(b.isFull());
        b.place(14, 14, Symbol.EMPTY);
        assertFalse(b.isFull());
    }

    @Test
    void copyIsIndependent() {
        Board b = new Board();
        b.place(1, 1, Symbol.X);
        Board c = b.copy();
        assertEquals(Symbol.X, c.get(1, 1));

        c.place(2, 2, Symbol.O);
        b.place(3, 3, Symbol.O);
        assertTrue(b.isEmpty(2, 2));
        assertTrue(c.isEmpty(3, 3));
    }
}
