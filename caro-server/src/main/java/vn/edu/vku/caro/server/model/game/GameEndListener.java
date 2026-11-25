package vn.edu.vku.caro.server.model.game;

import vn.edu.vku.caro.common.model.GameResult;

public interface GameEndListener {
    void onGameEnded(GameSession session, GameResult result);
}
