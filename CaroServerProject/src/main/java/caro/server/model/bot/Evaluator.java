package caro.server.model.bot;

import caro.common.model.Board;
import caro.common.model.Symbol;
import caro.common.model.WinChecker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Hàm đánh giá tĩnh thế cờ (docs/05-thiet-ke-ai.md).
 * <p>
 * Mỗi đường (hàng, cột, hai chéo) được tách thành <b>đoạn</b>: dãy ô liên tiếp chỉ gồm quân
 * một bên và ô trống, giới hạn bởi quân đối thủ hoặc biên. Đoạn dưới 5 ô không thể thành đường
 * thắng nên bỏ qua. Trong đoạn, chuỗi quân liền được chấm theo độ dài và số đầu mở; hai chuỗi
 * cách nhau đúng một ô trống được gộp thành một <b>thế gãy</b>.
 * <p>
 * Ngoài điểm, mỗi bên còn được đếm số <b>đe dọa</b> (bốn, bốn mở, ba mở) để chấm thế đôi và
 * để {@link #evaluate} tính theo lượt đi.
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

    /** Điểm và đe dọa của một bên. */
    static final class Stats {
        long score;
        int fives;
        /** Mọi thế bốn: liền hai đầu mở, liền một đầu, gãy. */
        int fours;
        /** Bốn liền hai đầu mở: đối thủ không chặn xuể. */
        int openFours;
        /** Ba mở: liền hoặc gãy, hai đầu ngoài trống. */
        int openThrees;

        int threats() { return fours + openThrees; }
    }

    /**
     * Điểm thế cờ nhìn từ phía {@code me}, dương là {@code me} đang lợi.
     * <ol>
     *   <li>Bên nào đã có 5 quân → ±{@link #FIVE} cho bên đó.</li>
     *   <li>Bên tới lượt có bốn → bên đó thắng ở nước kế: ±FIVE.</li>
     *   <li>Bên còn lại có bốn mở hoặc từ hai bốn → bên tới lượt chặn không xuể: ±FIVE.</li>
     *   <li>Bên tới lượt có ba mở (bên kia không có bốn) → cộng {@link #OPEN_FOUR} cho bên tới lượt.</li>
     *   <li>Điểm của {@code me} − 1,2 × điểm của đối thủ.</li>
     * </ol>
     *
     * @param toMove bên sắp đi trên {@code board}
     */
    public static long evaluate(Board board, Symbol me, Symbol toMove) {
        opponent(me); // kiểm tra me khác EMPTY
        Symbol other = opponent(toMove);
        Stats[] both = statsBoth(board);
        Stats mine = me == Symbol.X ? both[0] : both[1];
        Stats theirs = me == Symbol.X ? both[1] : both[0];
        Stats mover = toMove == me ? mine : theirs;
        Stats waiting = toMove == me ? theirs : mine;

        if (mine.fives > 0) return FIVE;
        if (theirs.fives > 0) return -FIVE;
        if (mover.fours > 0) return toMove == me ? FIVE : -FIVE;
        if (waiting.openFours > 0 || waiting.fours >= 2) return other == me ? FIVE : -FIVE;
        if (mover.openThrees > 0 && waiting.fours == 0) mover.score += OPEN_FOUR;

        return mine.score - theirs.score * DEFENSE_NUM / DEFENSE_DEN;
    }

    /** Tổng điểm của {@code s} trên bàn (đã gồm điểm thế đôi, không trừ điểm đối thủ). */
    public static long score(Board board, Symbol s) {
        return stats(board, s).score;
    }

    /**
     * Tọa độ mọi đường dài từ 5 ô trở lên theo 4 hướng, tính một lần.
     * {@code LINES[k]} = {r0, c0, r1, c1, ...} theo thứ tự dọc đường.
     */
    private static final int[][] LINES = buildLines();

    private static int[][] buildLines() {
        List<int[]> lines = new ArrayList<>();
        for (int[] d : DIRECTIONS) {
            for (int r = 0; r < Board.SIZE; r++) {
                for (int c = 0; c < Board.SIZE; c++) {
                    // Đường bắt đầu ở ô mà ô liền trước (ngược hướng d) nằm ngoài bàn.
                    if (Board.isInside(r - d[0], c - d[1])) continue;
                    int[] buf = new int[2 * Board.SIZE];
                    int n = 0;
                    for (int rr = r, cc = c; Board.isInside(rr, cc); rr += d[0], cc += d[1]) {
                        buf[2 * n] = rr;
                        buf[2 * n + 1] = cc;
                        n++;
                    }
                    if (n >= WinChecker.WIN_LENGTH) lines.add(Arrays.copyOf(buf, 2 * n));
                }
            }
        }
        return lines.toArray(new int[0][]);
    }

    /** Điểm và đe dọa của {@code s}. */
    static Stats stats(Board board, Symbol s) {
        opponent(s); // kiểm tra s khác EMPTY
        Stats[] both = statsBoth(board);
        return s == Symbol.X ? both[0] : both[1];
    }

    /** Quét mỗi đường một lần cho cả hai bên; trả {@code {X, O}}. */
    private static Stats[] statsBoth(Board board) {
        Stats x = new Stats(), o = new Stats();
        Symbol[] line = new Symbol[Board.SIZE];
        for (int[] coords : LINES) {
            int n = coords.length / 2;
            int countX = 0, countO = 0;
            for (int i = 0; i < n; i++) {
                Symbol s = board.get(coords[2 * i], coords[2 * i + 1]);
                line[i] = s;
                if (s == Symbol.X) countX++;
                else if (s == Symbol.O) countO++;
            }
            // Một quân lẻ không có điểm và không gộp được với gì: bỏ qua đường đó cho bên ấy.
            if (countX >= 2) scanLine(line, n, Symbol.X, x);
            if (countO >= 2) scanLine(line, n, Symbol.O, o);
        }
        addDoubleThreatBonus(x);
        addDoubleThreatBonus(o);
        return new Stats[] { x, o };
    }

    private static void addDoubleThreatBonus(Stats st) {
        if (st.openFours == 0 && st.threats() >= 2) st.score += OPEN_FOUR;
    }

    /** Tách {@code line[0..n)} thành đoạn của {@code s}, chấm từng đoạn đủ 5 ô. */
    private static void scanLine(Symbol[] line, int n, Symbol s, Stats st) {
        int i = 0;
        while (i < n) {
            if (line[i] != s && line[i] != Symbol.EMPTY) { i++; continue; }
            int start = i;
            while (i < n && (line[i] == s || line[i] == Symbol.EMPTY)) i++;
            if (i - start >= WinChecker.WIN_LENGTH) scanSegment(line, start, i, s, st);
        }
    }

    /**
     * Chấm đoạn {@code line[a..b)} (chỉ gồm {@code s} và ô trống). Duyệt chuỗi liền từ trái sang phải;
     * chuỗi A và chuỗi kế B cách nhau đúng một ô trống, đều dưới 4 quân, thì gộp thành thế gãy.
     */
    private static void scanSegment(Symbol[] line, int a, int b, Symbol s, Stats st) {
        int i = a;
        while (i < b) {
            if (line[i] != s) { i++; continue; }
            int aStart = i;
            while (i < b && line[i] == s) i++;
            int aLen = i - aStart;          // chuỗi A = [aStart, i)

            // Có chuỗi B ngay sau một ô trống không?
            int bStart = i + 1;
            if (aLen < 4 && bStart < b && line[i] == Symbol.EMPTY && line[bStart] == s) {
                int j = bStart;
                while (j < b && line[j] == s) j++;
                int bLen = j - bStart;
                if (bLen < 4) {
                    scoreBroken(aLen + bLen, aStart > a, j < b, st);
                    i = j;
                    continue;
                }
            }
            scoreRun(aLen, (aStart > a ? 1 : 0) + (i < b ? 1 : 0), st);
        }
    }

    /** Chuỗi liền {@code len} quân, {@code openEnds} đầu mở. */
    private static void scoreRun(int len, int openEnds, Stats st) {
        st.score += patternScore(len, openEnds);
        if (len >= WinChecker.WIN_LENGTH) {
            st.fives++;
        } else if (len == 4 && openEnds > 0) {
            st.fours++;
            if (openEnds == 2) st.openFours++;
        } else if (len == 3 && openEnds == 2) {
            st.openThrees++;
        }
    }

    /**
     * Thế gãy một ô gồm {@code total} quân. Từ 4 quân trở lên: lấp ô trống là thành ≥ 5 (luật D5),
     * chỉ một ô thắng nên chấm như bốn một đầu. 3 quân trở xuống: chấm như chuỗi liền cùng số quân.
     */
    private static void scoreBroken(int total, boolean leftOpen, boolean rightOpen, Stats st) {
        if (total >= 4) {
            st.score += CLOSED_FOUR;
            st.fours++;
            return;
        }
        int openEnds = (leftOpen ? 1 : 0) + (rightOpen ? 1 : 0);
        st.score += patternScore(total, openEnds);
        if (total == 3 && openEnds == 2) st.openThrees++;
    }

    /** Điểm một chuỗi liền theo bảng mẫu. Chuỗi bị chặn cả hai đầu (chưa đủ 5) không còn giá trị. */
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
