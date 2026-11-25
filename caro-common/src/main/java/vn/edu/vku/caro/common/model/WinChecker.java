package vn.edu.vku.caro.common.model;

import java.util.ArrayList;
import java.util.List;

/** Luật thắng D5: 5 quân trở lên liên tiếp, không áp dụng luật chặn hai đầu. */
public final class WinChecker {
    public static final int WIN_LENGTH = 5;
    private static final int[][] DIRECTIONS = { {0, 1}, {1, 0}, {1, 1}, {1, -1} };

    private WinChecker() { }

    /**
     * Kiểm tra quân vừa đặt ở (row, col). Trả về toàn bộ dãy thắng (từ đầu đến cuối, có thể dài hơn 5),
     * hoặc danh sách rỗng nếu chưa thắng.
     */
    public static List<int[]> findWinLine(Board board, int row, int col) {
        Symbol s = board.get(row, col);
        if (s == Symbol.EMPTY) return List.of();
        for (int[] d : DIRECTIONS) {
            List<int[]> line = new ArrayList<>();
            int r = row, c = col;
            while (Board.inBounds(r - d[0], c - d[1]) && board.get(r - d[0], c - d[1]) == s) { r -= d[0]; c -= d[1]; }
            while (Board.inBounds(r, c) && board.get(r, c) == s) { line.add(new int[] { r, c }); r += d[0]; c += d[1]; }
            if (line.size() >= WIN_LENGTH) return line;
        }
        return List.of();
    }
}
