package vn.edu.vku.caro.server.model.dao;

import vn.edu.vku.caro.common.model.GameResult;

import java.time.Instant;

public interface MatchRecorder {
    void record(String playerX, String playerO, boolean vsBot, int botLevel,
                GameResult result, int moveCount, Instant startedAt, Instant endedAt);
}
