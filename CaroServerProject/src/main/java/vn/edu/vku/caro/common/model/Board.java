package vn.edu.vku.caro.common.model;

public class Board {
    public static final int SIZE = 15;
    private final Symbol[][] cells = new Symbol[SIZE][SIZE];

    public Board() {
        for (Symbol[] row : cells) java.util.Arrays.fill(row, Symbol.EMPTY);
    }

    public static boolean inBounds(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }

    /** Trả về EMPTY nếu ô nằm ngoài bàn. */
    public Symbol get(int row, int col) { return inBounds(row, col) ? cells[row][col] : Symbol.EMPTY; }
    public boolean isEmpty(int row, int col) { return inBounds(row, col) && cells[row][col] == Symbol.EMPTY; }

    public void place(int row, int col, Symbol s) {
        if (!inBounds(row, col)) throw new IllegalArgumentException("Ô ngoài bàn: " + row + "," + col);
        cells[row][col] = s;
    }

    public boolean isFull() {
        for (Symbol[] row : cells) for (Symbol s : row) if (s == Symbol.EMPTY) return false;
        return true;
    }

    public Board copy() {
        Board b = new Board();
        for (int r = 0; r < SIZE; r++) b.cells[r] = cells[r].clone();
        return b;
    }
}
