package caro.server.model.bot;

import caro.common.model.Board;
import caro.common.model.Symbol;
import caro.common.model.WinChecker;

/**
 * Hàm đánh giá tĩnh thế cờ (docs/05-thiet-ke-ai.md): quét 4 hướng, mỗi chuỗi quân cùng màu
 * liên tiếp được chấm theo độ dài và số đầu mở (ô trống ngay sau hai đầu chuỗi).
 * <p>
 * Điểm thế cờ = điểm của {@code me} − 1,2 × điểm của đối thủ (hệ số &gt; 1 để bot ưu tiên phòng thủ).
 */
public final class Evaluator {
    public static final long FIVE = 1_000_000;
    public static final long OPEN_FOUR = 100_000;
    public static final long CLOSED_FOUR = 10_000;
    public static final long OPEN_THREE = 10_000;
    public static final long CLOSED_THREE = 1_000;
    public static final long OPEN_TWO = 500;
    public static final long CLOSED_TWO = 50;

    /** Hệ số phòng thủ 1,2 viết dạng phân số để tính bằng số nguyên. */
    private static final long DEFENSE_NUM = 6, DEFENSE_DEN = 5;

    /** 4 hướng: ngang, dọc, chéo xuôi (\), chéo ngược (/). */
    private static final int[][] DIRECTIONS = { {0, 1}, {1, 0}, {1, 1}, {1, -1} };

    private Evaluator() { }

    /** Điểm thế cờ nhìn từ phía {@code me}: dương là {@code me} đang lợi. */
    public static long evaluate(Board board, Symbol me) {
        Symbol opp = opponent(me);
        return score(board, me) - score(board, opp) * DEFENSE_NUM / DEFENSE_DEN;
    }

    /** Tổng điểm mọi chuỗi quân {@code s} trên bàn (không trừ điểm đối thủ). */
    public static long score(Board board, Symbol s) {
        long total = 0;
        for (int[] d : DIRECTIONS) {
            for (int r = 0; r < Board.SIZE; r++) {
                for (int c = 0; c < Board.SIZE; c++) {
                    if (board.get(r, c) != s) continue;
                    int pr = r - d[0], pc = c - d[1];
                    // Chỉ chấm một lần cho mỗi chuỗi: bắt đầu ở quân đầu tiên của chuỗi.
                    if (Board.isInside(pr, pc) && board.get(pr, pc) == s) continue;

                    int len = 1;
                    int nr = r + d[0], nc = c + d[1];
                    while (Board.isInside(nr, nc) && board.get(nr, nc) == s) {
                        len++;
                        nr += d[0];
                        nc += d[1];
                    }
                    int openEnds = 0;
                    if (Board.isInside(pr, pc) && board.get(pr, pc) == Symbol.EMPTY) openEnds++;
                    if (Board.isInside(nr, nc) && board.get(nr, nc) == Symbol.EMPTY) openEnds++;
                    total += patternScore(len, openEnds);
                }
            }
        }
        return total;
    }

    /** Điểm một chuỗi theo bảng mẫu. Chuỗi bị chặn cả hai đầu (chưa đủ 5) không còn giá trị. */
    static long patternScore(int len, int openEnds) {
        if (len >= WinChecker.WIN_LENGTH) return FIVE;
        if (openEnds == 0) return 0;
        boolean open = openEnds == 2;
        switch (len) {
            case 4: return open ? OPEN_FOUR : CLOSED_FOUR;
            case 3: return open ? OPEN_THREE : CLOSED_THREE;
            case 2: return open ? OPEN_TWO : CLOSED_TWO;
            default: return 0;
        }
    }

    /** Quân của đối thủ. */
    public static Symbol opponent(Symbol s) {
        switch (s) {
            case X: return Symbol.O;
            case O: return Symbol.X;
            default: throw new IllegalArgumentException("Không có đối thủ cho " + s);
        }
    }
}
