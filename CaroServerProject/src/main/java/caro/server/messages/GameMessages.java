package caro.server.messages;

import caro.common.protocol.Message;

public final class GameMessages {
    private GameMessages() { }
    public static Message gameStart(String roomId, String mySymbol, String opponent, String first) {
        return new Message("GAME_START", roomId, mySymbol, opponent, first);
    }
    public static Message moved(String symbol, int row, int col, String next) {
        return new Message("MOVED", symbol, String.valueOf(row), String.valueOf(col), next);
    }
    public static Message gameOver(String winner, String reason, String cells) {
        return new Message("GAME_OVER", winner, reason, cells);
    }
    public static Message rematchRequested(String fromName) { return new Message("REMATCH_REQUESTED", fromName); }
    public static Message chatMsg(String fromName, String text) { return new Message("CHAT_MSG", fromName, text); }
    public static Message backToLogin() { return new Message("BACK_TO_LOGIN"); }
    public static Message opponentLeftRoom() { return new Message("OPPONENT_LEFT_ROOM"); }
}
