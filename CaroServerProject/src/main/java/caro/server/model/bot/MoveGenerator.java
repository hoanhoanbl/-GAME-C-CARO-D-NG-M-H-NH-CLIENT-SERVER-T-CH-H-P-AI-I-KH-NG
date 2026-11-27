package caro.server.model.bot;

import caro.common.model.Board;
import caro.common.model.Symbol;

import java.util.ArrayList;
import java.util.List;

/**
 * Sinh ô ứng viên cho bot: chỉ xét ô trống cách một quân đã đánh tối đa {@link #RADIUS} ô
 * (theo cả hàng và cột). Bàn trống thì chỉ có ô giữa.
 */
public final class MoveGenerator {
    public static final int RADIUS = 2;
    public static final int CENTER = Board.SIZE / 2;

    private MoveGenerator() { }

    /**
     * @return các ô {@code {row, col}} theo thứ tự hàng rồi cột; rỗng nếu bàn đầy;
     *         {@code [{7, 7}]} nếu bàn trống
     */
    public static List<int[]> candidates(Board board) {
        boolean[][] near = new boolean[Board.SIZE][Board.SIZE];
        boolean hasStone = false;
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (board.get(r, c) == Symbol.EMPTY) continue;
                hasStone = true;
                markAround(near, r, c);
            }
        }
        if (!hasStone) return List.of(new int[] { CENTER, CENTER });

        List<int[]> result = new ArrayList<>();
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (near[r][c] && board.isEmpty(r, c)) result.add(new int[] { r, c });
            }
        }
        return result;
    }

    /** Đánh dấu mọi ô trong hình vuông bán kính RADIUS quanh (row, col), cắt theo biên bàn. */
    private static void markAround(boolean[][] near, int row, int col) {
        int r0 = Math.max(0, row - RADIUS), r1 = Math.min(Board.SIZE - 1, row + RADIUS);
        int c0 = Math.max(0, col - RADIUS), c1 = Math.min(Board.SIZE - 1, col + RADIUS);
        for (int r = r0; r <= r1; r++) {
            for (int c = c0; c <= c1; c++) near[r][c] = true;
        }
    }
}
