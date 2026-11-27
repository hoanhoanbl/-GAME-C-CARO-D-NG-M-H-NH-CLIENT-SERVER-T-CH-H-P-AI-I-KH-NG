package caro.server.messages;

import caro.common.model.GameResult;
import caro.common.model.Symbol;
import caro.common.protocol.Message;

import java.util.List;
import java.util.stream.Collectors;

/** "View" của ván cờ phía server: tạo các thông điệp gửi về client (mục 10.3). */
public final class GameMessages {
    public static final String DRAW = "DRAW";

    private GameMessages() { }

    public static Message gameStart(String roomId, String mySymbol, String opponent, String first) {
        return new Message("GAME_START", roomId, mySymbol, opponent, first);
    }

    public static Message gameStart(String roomId, Symbol mySymbol, String opponent, Symbol first) {
        return gameStart(roomId, mySymbol.name(), opponent, first.name());
    }

    public static Message moved(String symbol, int row, int col, String next) {
        return new Message("MOVED", symbol, String.valueOf(row), String.valueOf(col), next);
    }

    public static Message moved(Symbol symbol, int row, int col, Symbol next) {
        return moved(symbol.name(), row, col, next.name());
    }

    public static Message gameOver(String winner, String reason, String cells) {
        return new Message("GAME_OVER", winner, reason, cells);
    }

    /** {@code GAME_OVER|X/O/DRAW|reason|r,c;r,c;…} (cells rỗng nếu không có đường thắng). */
    public static Message gameOver(GameResult result) {
        String winner = result.isDraw() ? DRAW : result.winner().name();
        return gameOver(winner, result.reason().name(), formatCells(result.winLine()));
    }

    /** Ô dạng {@code r,c;r,c;…}. */
    public static String formatCells(List<int[]> cells) {
        return cells.stream().map(c -> c[0] + "," + c[1]).collect(Collectors.joining(";"));
    }

    public static Message rematchRequested(String fromName) { return new Message("REMATCH_REQUESTED", fromName); }
    public static Message chatMsg(String fromName, String text) { return new Message("CHAT_MSG", fromName, text); }
    public static Message backToLogin() { return new Message("BACK_TO_LOGIN"); }
    public static Message opponentLeftRoom() { return new Message("OPPONENT_LEFT_ROOM"); }
}
