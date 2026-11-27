package caro.server.model.bot;

import caro.common.model.Board;
import caro.common.model.Symbol;
import caro.common.model.WinChecker;

import java.util.List;

/**
 * Tìm nước đi cho bot bằng Minimax (FE-16). Alpha-Beta và sắp xếp nước là FE-17.
 * <p>
 * Nút lá chấm bằng {@link Evaluator#evaluate}; nước vừa tạo 5 quân là nút kết thúc, chấm
 * ±{@link #WIN_SCORE} cộng độ sâu còn lại để ưu tiên thắng sớm, thua muộn.
 * Bàn được đặt/gỡ quân tại chỗ khi duyệt nên sau khi gọi bàn trở lại như cũ.
 */
public class AIEngine {
    /** Lớn hơn mọi điểm {@link Evaluator} có thể trả về. */
    public static final long WIN_SCORE = 1_000_000_000L;

    /**
     * @param board bàn hiện tại (không bị thay đổi sau khi gọi)
     * @param me    quân của bot
     * @param depth số nửa nước nhìn trước, ≥ 1 (1 = chỉ xét nước của bot)
     * @return ô {@code {row, col}} tốt nhất; ô giữa nếu bàn trống
     * @throws IllegalArgumentException nếu {@code depth < 1} hoặc {@code me} là EMPTY
     * @throws IllegalStateException    nếu bàn đã đầy
     */
    public int[] bestMove(Board board, Symbol me, int depth) {
        if (depth < 1) throw new IllegalArgumentException("depth phải ≥ 1: " + depth);
        Symbol opp = Evaluator.opponent(me);

        List<int[]> moves = MoveGenerator.candidates(board);
        if (moves.isEmpty()) throw new IllegalStateException("Bàn đã đầy, không còn nước đi");

        int[] best = null;
        long bestScore = Long.MIN_VALUE;
        for (int[] m : moves) {
            board.place(m[0], m[1], me);
            long score;
            if (!WinChecker.findWinLine(board, m[0], m[1]).isEmpty()) {
                score = WIN_SCORE + depth;
            } else {
                score = minimax(board, depth - 1, false, me, opp);
            }
            board.place(m[0], m[1], Symbol.EMPTY);

            if (score > bestScore) {
                bestScore = score;
                best = m;
            }
        }
        return best;
    }

    /**
     * @param maximizing true nếu tới lượt bot ({@code me}), false nếu tới lượt đối thủ
     * @return điểm thế cờ nhìn từ phía {@code me}
     */
    private long minimax(Board board, int depth, boolean maximizing, Symbol me, Symbol opp) {
        if (depth == 0) return Evaluator.evaluate(board, me, maximizing ? me : opp);

        List<int[]> moves = MoveGenerator.candidates(board);
        if (moves.isEmpty()) return 0; // bàn đầy: hòa

        Symbol toMove = maximizing ? me : opp;
        long best = maximizing ? Long.MIN_VALUE : Long.MAX_VALUE;
        for (int[] m : moves) {
            board.place(m[0], m[1], toMove);
            long score;
            if (!WinChecker.findWinLine(board, m[0], m[1]).isEmpty()) {
                score = maximizing ? WIN_SCORE + depth : -(WIN_SCORE + depth);
            } else {
                score = minimax(board, depth - 1, !maximizing, me, opp);
            }
            board.place(m[0], m[1], Symbol.EMPTY);

            best = maximizing ? Math.max(best, score) : Math.min(best, score);
        }
        return best;
    }
}
