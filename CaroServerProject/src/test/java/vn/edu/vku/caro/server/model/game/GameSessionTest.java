package vn.edu.vku.caro.server.model.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.edu.vku.caro.common.model.EndReason;
import vn.edu.vku.caro.common.model.GameResult;
import vn.edu.vku.caro.common.model.Symbol;
import vn.edu.vku.caro.common.protocol.Message;
import vn.edu.vku.caro.server.model.dao.MatchRecorder;
import vn.edu.vku.caro.server.model.player.Player;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameSessionTest {
    static class FakePlayer implements Player {
        final String name;
        final boolean guest;
        final List<Message> inbox = new ArrayList<>();
        FakePlayer(String name, boolean guest) { this.name = name; this.guest = guest; }
        public String getName() { return name; }
        public boolean isBot() { return false; }
        public boolean isGuest() { return guest; }
        public void send(Message m) { inbox.add(m); }
        Message last() { return inbox.get(inbox.size() - 1); }
    }

    FakePlayer a, b;
    GameSession s;
    GameResult ended;
    int recorded;

    @BeforeEach
    void setUp() { setUp(false); }

    void setUp(boolean guestA) {
        a = new FakePlayer("A", guestA);
        b = new FakePlayer("B", false);
        recorded = 0;
        ended = null;
        MatchRecorder rec = (px, po, bot, lvl, r, n, t0, t1) -> recorded++;
        s = new GameSession("r1", a, b, (sess, r) -> ended = r, rec);
        s.start();
    }

    @Test void startSendsGameStartToBoth() {
        assertEquals("GAME_START", a.last().command());
        assertEquals("X", a.last().arg(1));
        assertEquals("O", b.last().arg(1));
        assertEquals("A", b.last().arg(2));
    }

    @Test void moveBroadcastsMovedWithNextTurn() {
        s.handleMove(a, 7, 7);
        assertEquals("MOVED", b.last().command());
        assertEquals("X", a.last().arg(0));
        assertEquals("O", b.last().arg(3));
    }

    @Test void wrongTurnIsRejected() {
        s.handleMove(b, 7, 7);
        assertEquals("NOT_YOUR_TURN", b.last().arg(0));
    }

    @Test void occupiedAndOutOfBoardRejected() {
        s.handleMove(a, 7, 7);
        s.handleMove(b, 7, 7);
        assertEquals("CELL_OCCUPIED", b.last().arg(0));
        s.handleMove(b, -1, 3);
        assertEquals("OUT_OF_BOARD", b.last().arg(0));
        s.handleMove(b, 15, 3);
        assertEquals("OUT_OF_BOARD", b.last().arg(0));
    }

    private void playFiveForX() {
        for (int i = 0; i < 4; i++) { s.handleMove(a, 0, i); s.handleMove(b, 1, i); }
        s.handleMove(a, 0, 4);
    }

    @Test void fiveInARowEndsGameAndRecords() {
        playFiveForX();
        assertTrue(s.isFinished());
        assertEquals(Symbol.X, ended.winner());
        assertEquals(EndReason.FIVE, ended.reason());
        assertEquals("GAME_OVER", b.last().command());
        assertEquals("0,0;0,1;0,2;0,3;0,4", b.last().arg(2));
        assertEquals(1, recorded);
    }

    @Test void moveAfterGameOverIsRejected() {
        playFiveForX();
        s.handleMove(b, 5, 5);
        assertEquals("NO_GAME", b.last().arg(0));
    }

    @Test void surrenderGivesWinToOpponent() {
        s.handleSurrender(a);
        assertEquals(Symbol.O, ended.winner());
        assertEquals(EndReason.SURRENDER, ended.reason());
    }

    @Test void disconnectGivesWinToOpponent() {
        s.handleDisconnect(b);
        assertEquals(Symbol.X, ended.winner());
        assertEquals(EndReason.LEFT, ended.reason());
    }

    @Test void guestGameIsNotRecorded() {
        setUp(true);
        s.handleSurrender(a);
        assertEquals(0, recorded);
        assertNotNull(ended);
    }

    @Test void recorderFailureStillEndsGame() {
        s = new GameSession("r2", a, b, (sess, r) -> ended = r,
                (px, po, bot, lvl, r, n, t0, t1) -> { throw new RuntimeException("DB down"); });
        s.start();
        s.handleSurrender(a);
        assertTrue(s.isFinished());
        assertNotNull(ended);
    }
}
