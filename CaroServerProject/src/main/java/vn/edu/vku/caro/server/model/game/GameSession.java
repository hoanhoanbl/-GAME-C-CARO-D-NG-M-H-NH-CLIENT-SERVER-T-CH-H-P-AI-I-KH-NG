package vn.edu.vku.caro.server.model.game;

import vn.edu.vku.caro.common.model.Board;
import vn.edu.vku.caro.common.model.EndReason;
import vn.edu.vku.caro.common.model.GameResult;
import vn.edu.vku.caro.common.model.Symbol;
import vn.edu.vku.caro.common.model.WinChecker;
import vn.edu.vku.caro.common.protocol.Message;
import vn.edu.vku.caro.server.messages.ErrorMessages;
import vn.edu.vku.caro.server.messages.GameMessages;
import vn.edu.vku.caro.server.model.dao.MatchRecorder;
import vn.edu.vku.caro.server.model.player.Player;

import java.time.Instant;
import java.util.List;

/** Trọng tài ván cờ: giữ bàn, lượt đi, kiểm tra nước đi, phát hiện thắng/hòa. X đi trước. */
public class GameSession {
    private final String roomId;
    private final Player x;
    private final Player o;
    private final GameEndListener listener;
    private final MatchRecorder recorder;
    private final Board board = new Board();

    private Symbol turn = Symbol.X;
    private int moveCount;
    private boolean finished;
    private Instant startedAt;

    public GameSession(String roomId, Player x, Player o, GameEndListener listener, MatchRecorder recorder) {
        this.roomId = roomId;
        this.x = x;
        this.o = o;
        this.listener = listener;
        this.recorder = recorder;
    }

    public synchronized void start() {
        startedAt = Instant.now();
        x.send(GameMessages.gameStart(roomId, "X", o.getName(), "X"));
        o.send(GameMessages.gameStart(roomId, "O", x.getName(), "X"));
    }

    public synchronized void handleMove(Player p, int row, int col) {
        Symbol mine = symbolOf(p);
        if (mine == null || finished) { p.send(ErrorMessages.error("NO_GAME", "Không có ván đang chơi")); return; }
        if (mine != turn) { p.send(ErrorMessages.error("NOT_YOUR_TURN", "Chưa đến lượt bạn")); return; }
        if (!Board.inBounds(row, col)) { p.send(ErrorMessages.error("OUT_OF_BOARD", "Nước đi ngoài bàn cờ")); return; }
        if (!board.isEmpty(row, col)) { p.send(ErrorMessages.error("CELL_OCCUPIED", "Ô đã có quân")); return; }

        board.place(row, col, mine);
        moveCount++;
        List<int[]> winLine = WinChecker.findWinLine(board, row, col);
        boolean won = !winLine.isEmpty();
        boolean draw = !won && board.isFull();
        turn = other(mine);

        String next = (won || draw) ? "" : turn.name();
        broadcast(GameMessages.moved(mine.name(), row, col, next));
        if (won) finish(new GameResult(mine, EndReason.FIVE, winLine));
        else if (draw) finish(new GameResult(null, EndReason.FULL, List.of()));
    }

    public synchronized void handleSurrender(Player p) {
        Symbol mine = symbolOf(p);
        if (mine == null || finished) { p.send(ErrorMessages.error("NO_GAME", "Không có ván đang chơi")); return; }
        finish(new GameResult(other(mine), EndReason.SURRENDER, List.of()));
    }

    /** Một bên mất kết nối: bên còn lại thắng với lý do LEFT. */
    public synchronized void handleDisconnect(Player p) {
        Symbol mine = symbolOf(p);
        if (mine == null || finished) return;
        finish(new GameResult(other(mine), EndReason.LEFT, List.of()));
    }

    public synchronized void handleRematch(Player p) { /* TODO H (3.H.6) */ }

    public synchronized boolean isFinished() { return finished; }

    private void finish(GameResult result) {
        finished = true;
        broadcast(GameMessages.gameOver(
                result.winner() == null ? "DRAW" : result.winner().name(),
                result.reason().name(),
                formatCells(result.winLine())));
        // Ván có khách không ghi CSDL; lỗi ghi DB không được làm hỏng việc kết thúc ván.
        if (recorder != null && !x.isGuest() && !o.isGuest()) {
            try {
                recorder.record(x.getName(), o.getName(), x.isBot() || o.isBot(), 0,
                        result, moveCount, startedAt, Instant.now());
            } catch (RuntimeException e) {
                System.err.println("[GameSession] Không ghi được ván " + roomId + ": " + e.getMessage());
            }
        }
        if (listener != null) listener.onGameEnded(this, result);
    }

    private void broadcast(Message m) { x.send(m); o.send(m); }

    private Symbol symbolOf(Player p) { return p == x ? Symbol.X : p == o ? Symbol.O : null; }
    private static Symbol other(Symbol s) { return s == Symbol.X ? Symbol.O : Symbol.X; }

    private static String formatCells(List<int[]> cells) {
        StringBuilder sb = new StringBuilder();
        for (int[] c : cells) { if (sb.length() > 0) sb.append(';'); sb.append(c[0]).append(',').append(c[1]); }
        return sb.toString();
    }
}
