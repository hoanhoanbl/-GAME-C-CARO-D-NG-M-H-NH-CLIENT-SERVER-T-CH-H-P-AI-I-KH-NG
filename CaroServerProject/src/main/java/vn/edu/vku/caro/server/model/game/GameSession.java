package vn.edu.vku.caro.server.model.game;

import vn.edu.vku.caro.server.model.dao.MatchRecorder;
import vn.edu.vku.caro.server.model.player.Player;

/** Trọng tài ván cờ. TODO H (FE-09). */
public class GameSession {
    public GameSession(String roomId, Player x, Player o, GameEndListener listener, MatchRecorder recorder) { }
    public void start() { }
    public void handleMove(Player p, int row, int col) { }
    public void handleSurrender(Player p) { }
    public void handleDisconnect(Player p) { }
    public void handleRematch(Player p) { }
    public boolean isFinished() { return false; }
}
