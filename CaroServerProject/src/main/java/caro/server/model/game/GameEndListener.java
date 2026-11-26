package caro.server.model.game;

import caro.common.model.GameResult;

public interface GameEndListener {
    void onGameEnded(GameSession session, GameResult result);
}
