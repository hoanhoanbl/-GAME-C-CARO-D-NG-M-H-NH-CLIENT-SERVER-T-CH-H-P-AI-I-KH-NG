package vn.edu.vku.caro.server.model.game;

import vn.edu.vku.caro.server.model.dao.MatchRecorder;
import vn.edu.vku.caro.server.model.player.Player;

public final class GameSessionFactory {
    private GameSessionFactory() { }

    public static GameSession create(String roomId, Player x, Player o, GameEndListener listener, MatchRecorder recorder) {
        return new GameSession(roomId, x, o, listener, recorder);
    }
}
