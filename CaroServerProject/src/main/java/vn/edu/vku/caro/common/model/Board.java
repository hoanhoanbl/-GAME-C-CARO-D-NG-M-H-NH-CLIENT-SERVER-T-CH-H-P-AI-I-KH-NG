package vn.edu.vku.caro.common.model;

public class Board {
    public static final int SIZE = 15;
    private final Symbol[][] cells = new Symbol[SIZE][SIZE];

    public Board() {
        for (Symbol[] row : cells) java.util.Arrays.fill(row, Symbol.EMPTY);
    }

    public Symbol get(int row, int col) { return cells[row][col]; }
    public boolean isEmpty(int row, int col) { return cells[row][col] == Symbol.EMPTY; }
    public void place(int row, int col, Symbol s) { cells[row][col] = s; }   // TODO H: kiểm tra biên

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
