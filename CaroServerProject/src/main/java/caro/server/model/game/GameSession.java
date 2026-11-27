package caro.server.model.game;

import caro.common.model.Board;
import caro.common.model.EndReason;
import caro.common.model.GameResult;
import caro.common.model.Symbol;
import caro.common.model.WinChecker;
import caro.common.protocol.Message;
import caro.server.messages.ErrorMessages;
import caro.server.messages.GameMessages;
import caro.server.model.bot.BotPlayer;
import caro.server.model.dao.MatchRecorder;
import caro.server.model.player.Player;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Trọng tài một ván cờ (FE-09). Là nơi duy nhất quyết định nước đi hợp lệ và thắng/thua/hòa (R7).
 *
 * <p>Thread-safe: {@code handleMove} có thể được gọi đồng thời từ luồng {@code ClientHandler} của hai người
 * và từ {@code BotExecutor}. Trạng thái ván được khóa bằng {@code this}; {@link GameEndListener} và
 * {@link MatchRecorder} được gọi <b>ngoài</b> khóa để tránh deadlock với {@code RoomManager}.
 */
public class GameSession {
    /** X luôn đi trước. */
    public static final Symbol FIRST = Symbol.X;

    private final String roomId;
    private final Player playerX;
    private final Player playerO;
    private final GameEndListener listener;
    private final MatchRecorder recorder;

    private Board board = new Board();
    private Symbol turn = FIRST;
    private int moveCount;
    private boolean started;
    private GameResult result;          // != null khi ván đã kết thúc
    private Instant startedAt;

    public GameSession(String roomId, Player x, Player o, GameEndListener listener, MatchRecorder recorder) {
        this.roomId = Objects.requireNonNull(roomId, "roomId");
        this.playerX = Objects.requireNonNull(x, "x");
        this.playerO = Objects.requireNonNull(o, "o");
        this.listener = listener;
        this.recorder = recorder;
    }

    /** Gửi {@code GAME_START} cho cả hai, bắt đầu tính giờ. Gọi lần hai không có tác dụng. */
    public void start() {
        synchronized (this) {
            if (started) return;
            started = true;
            startedAt = Instant.now();
        }
        playerX.send(GameMessages.gameStart(roomId, Symbol.X, playerO.getName(), FIRST));
        playerO.send(GameMessages.gameStart(roomId, Symbol.O, playerX.getName(), FIRST));
    }

    /**
     * Xử lý {@code MOVE|row|col}. Nước sai → chỉ người gửi nhận {@code ERROR}, bàn cờ không đổi.
     * Nước đúng → cả hai nhận {@code MOVED}; nếu ván kết thúc thì cả hai nhận thêm {@code GAME_OVER}.
     */
    public void handleMove(Player p, int row, int col) {
        GameResult ended;
        synchronized (this) {
            Symbol s = symbolOf(p);
            if (s == null || !started || result != null) {
                p.send(ErrorMessages.error("NO_GAME", "Bạn không ở trong ván nào đang diễn ra"));
                return;
            }
            if (s != turn) {
                p.send(ErrorMessages.error("NOT_YOUR_TURN", "Chưa đến lượt bạn"));
                return;
            }
            if (!Board.isInside(row, col)) {
                p.send(ErrorMessages.error("OUT_OF_BOARD", "Ô nằm ngoài bàn cờ"));
                return;
            }
            if (!board.isEmpty(row, col)) {
                p.send(ErrorMessages.error("CELL_OCCUPIED", "Ô đã có quân"));
                return;
            }

            board.place(row, col, s);
            moveCount++;
            turn = opposite(s);
            broadcast(GameMessages.moved(s, row, col, turn));

            List<int[]> winLine = WinChecker.findWinLine(board, row, col);
            if (!winLine.isEmpty()) {
                ended = finish(GameResult.five(s, winLine), true);
            } else if (moveCount == Board.SIZE * Board.SIZE) {   // mỗi nước chiếm đúng 1 ô trống → đủ 225 nước là kín bàn
                ended = finish(GameResult.draw(), true);
            } else {
                return;
            }
        }
        afterGameEnded(ended);
    }

    /** {@code SURRENDER}: người còn lại thắng, cả hai nhận {@code GAME_OVER}. */
    public void handleSurrender(Player p) {
        GameResult ended;
        synchronized (this) {
            Symbol s = symbolOf(p);
            if (s == null || !started || result != null) {
                p.send(ErrorMessages.error("NO_GAME", "Bạn không ở trong ván nào đang diễn ra"));
                return;
            }
            ended = finish(GameResult.win(opposite(s), EndReason.SURRENDER), true);
        }
        afterGameEnded(ended);
    }

    /**
     * Người chơi mất kết nối (P gọi). Nếu ván đang diễn ra: người còn lại thắng với lý do {@code LEFT};
     * chỉ người còn lại nhận {@code GAME_OVER} vì socket của người rời đã đóng. Ván đã kết thúc thì bỏ qua.
     */
    public void handleDisconnect(Player p) {
        GameResult ended;
        synchronized (this) {
            Symbol s = symbolOf(p);
            if (s == null || !started || result != null) return;
            ended = finish(GameResult.win(opposite(s), EndReason.LEFT), false);
            playerOf(opposite(s)).send(GameMessages.gameOver(ended));
        }
        afterGameEnded(ended);
    }

    /** {@code REMATCH}. TODO H (3.H.6, FE-14): đổi bên khi cả hai đồng ý. */
    public void handleRematch(Player p) { }

    public synchronized boolean isFinished() { return result != null; }

    // ---- truy vấn (cho controller, bot, test) ----

    public String getRoomId() { return roomId; }
    public Player getPlayerX() { return playerX; }
    public Player getPlayerO() { return playerO; }
    public synchronized Symbol currentTurn() { return turn; }
    public synchronized int moveCount() { return moveCount; }
    public synchronized GameResult result() { return result; }

    /** Bản sao bàn cờ; sửa bản sao không ảnh hưởng ván (bot dùng để tính nước). */
    public synchronized Board boardSnapshot() { return board.copy(); }

    /** Ký hiệu của người chơi trong ván, hoặc null nếu không thuộc ván này. */
    public Symbol symbolOf(Player p) {
        if (p == playerX) return Symbol.X;
        if (p == playerO) return Symbol.O;
        return null;
    }

    // ---- nội bộ ----

    private Player playerOf(Symbol s) { return s == Symbol.X ? playerX : playerO; }

    private static Symbol opposite(Symbol s) { return s == Symbol.X ? Symbol.O : Symbol.X; }

    private void broadcast(Message msg) {
        playerX.send(msg);
        playerO.send(msg);
    }

    /** Chốt kết quả (gọi trong khóa). {@code notifyBoth}: gửi GAME_OVER cho cả hai. */
    private GameResult finish(GameResult r, boolean notifyBoth) {
        result = r;
        if (notifyBoth) broadcast(GameMessages.gameOver(r));
        return r;
    }

    /** Ghi CSDL và báo listener, ngoài khóa. Lỗi ở đây chỉ log, không ảnh hưởng ván đã kết thúc. */
    private void afterGameEnded(GameResult r) {
        Instant endedAt = Instant.now();
        boolean hasGuest = playerX.isGuest() || playerO.isGuest();
        if (recorder != null && !hasGuest) {
            try {
                Player bot = playerX.isBot() ? playerX : playerO.isBot() ? playerO : null;
                int botLevel = bot instanceof BotPlayer b ? b.getLevel() : 0;
                int moves;
                synchronized (this) { moves = moveCount; }
                recorder.record(playerX.getName(), playerO.getName(), bot != null, botLevel,
                        r, moves, startedAt, endedAt);
            } catch (RuntimeException e) {
                System.out.println("[SERVER] Lỗi ghi kết quả ván " + roomId + ": " + e.getMessage());
            }
        }
        if (listener != null) {
            try {
                listener.onGameEnded(this, r);
            } catch (RuntimeException e) {
                System.out.println("[SERVER] Lỗi GameEndListener ván " + roomId + ": " + e.getMessage());
            }
        }
    }
}
