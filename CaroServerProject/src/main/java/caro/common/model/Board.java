package caro.common.model;

import java.util.Arrays;
import java.util.Objects;

/** Bàn cờ 15x15. Chỉ lưu trạng thái ô, không biết lượt hay luật thắng (xem {@link WinChecker}). */
public class Board {
    public static final int SIZE = 15;
    private final Symbol[][] cells = new Symbol[SIZE][SIZE];

    public Board() {
        for (Symbol[] row : cells) Arrays.fill(row, Symbol.EMPTY);
    }

    /** true nếu (row, col) nằm trong bàn. */
    public static boolean isInside(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }

    public Symbol get(int row, int col) {
        checkInside(row, col);
        return cells[row][col];
    }

    public boolean isEmpty(int row, int col) {
        return get(row, col) == Symbol.EMPTY;
    }

    /**
     * Đặt quân vào ô. Không kiểm tra ô trống hay lượt (việc của {@code GameSession});
     * đặt {@link Symbol#EMPTY} để gỡ quân (bot dùng khi duyệt cây).
     *
     * @throws IllegalArgumentException nếu ô nằm ngoài bàn hoặc {@code s} là null
     */
    public void place(int row, int col, Symbol s) {
        checkInside(row, col);
        cells[row][col] = Objects.requireNonNull(s, "symbol");
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

    private static void checkInside(int row, int col) {
        if (!isInside(row, col)) {
            throw new IllegalArgumentException("Ô (" + row + ", " + col + ") nằm ngoài bàn " + SIZE + "x" + SIZE);
        }
    }
}
