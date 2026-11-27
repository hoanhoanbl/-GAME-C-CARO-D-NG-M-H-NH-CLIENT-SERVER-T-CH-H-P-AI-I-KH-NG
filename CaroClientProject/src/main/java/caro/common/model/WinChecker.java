package caro.common.model;

import java.util.ArrayList;
import java.util.List;

/** Luật thắng D5: 5 quân liên tiếp trở lên (ngang, dọc, 2 chéo), không xét chặn hai đầu. */
public final class WinChecker {
    public static final int WIN_LENGTH = 5;

    /** 4 hướng: ngang, dọc, chéo xuôi (\), chéo ngược (/). */
    private static final int[][] DIRECTIONS = { {0, 1}, {1, 0}, {1, 1}, {1, -1} };

    private WinChecker() { }

    /**
     * Kiểm tra quân vừa đặt tại (row, col) có tạo thành đường thắng không.
     *
     * @return các ô {@code {row, col}} của đường thắng, xếp từ đầu này sang đầu kia
     *         (đủ cả đường nếu dài hơn 5); danh sách rỗng nếu chưa thắng hoặc ô trống
     */
    public static List<int[]> findWinLine(Board board, int row, int col) {
        Symbol s = board.get(row, col);
        if (s == Symbol.EMPTY) return List.of();

        for (int[] d : DIRECTIONS) {
            int back = countSame(board, row, col, -d[0], -d[1], s);
            int forward = countSame(board, row, col, d[0], d[1], s);
            if (back + 1 + forward >= WIN_LENGTH) {
                List<int[]> line = new ArrayList<>(back + 1 + forward);
                for (int i = -back; i <= forward; i++) {
                    line.add(new int[] { row + i * d[0], col + i * d[1] });
                }
                return List.copyOf(line);
            }
        }
        return List.of();
    }

    /** Đếm số quân {@code s} liên tiếp đi từ (row, col) theo hướng (dr, dc), không tính ô xuất phát. */
    private static int countSame(Board board, int row, int col, int dr, int dc, Symbol s) {
        int n = 0;
        int r = row + dr, c = col + dc;
        while (Board.isInside(r, c) && board.get(r, c) == s) {
            n++;
            r += dr;
            c += dc;
        }
        return n;
    }
}
