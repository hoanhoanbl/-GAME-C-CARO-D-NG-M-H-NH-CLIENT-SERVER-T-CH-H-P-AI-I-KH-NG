package caro.server.model.dao;

import caro.common.model.GameResult;

import java.time.Instant;

public class MySqlMatchRecorder implements MatchRecorder {
    @Override
    public void record(String playerX, String playerO, boolean vsBot, int botLevel,
                       GameResult result, int moveCount, Instant startedAt, Instant endedAt) {
        // TODO P: ghi qua MatchDao; lỗi DB chỉ log
    }
}
