package caro.common.model;

import java.util.List;
import java.util.Objects;

/**
 * Kết quả ván. {@code winner} = null nếu hòa; {@code winLine} chỉ có ô khi thắng bằng {@link EndReason#FIVE}.
 */
public record GameResult(Symbol winner, EndReason reason, List<int[]> winLine) {

    public GameResult {
        Objects.requireNonNull(reason, "reason");
        if (winner == Symbol.EMPTY) throw new IllegalArgumentException("winner phải là X, O hoặc null (hòa)");
        winLine = winLine == null ? List.of() : List.copyOf(winLine);
    }

    /** Thắng do đủ 5 quân. */
    public static GameResult five(Symbol winner, List<int[]> winLine) {
        return new GameResult(Objects.requireNonNull(winner, "winner"), EndReason.FIVE, winLine);
    }

    /** Thắng do đối thủ đầu hàng ({@link EndReason#SURRENDER}) hoặc rời ván ({@link EndReason#LEFT}). */
    public static GameResult win(Symbol winner, EndReason reason) {
        return new GameResult(Objects.requireNonNull(winner, "winner"), reason, List.of());
    }

    /** Hòa do bàn đầy. */
    public static GameResult draw() {
        return new GameResult(null, EndReason.FULL, List.of());
    }

    public boolean isDraw() {
        return winner == null;
    }
}
