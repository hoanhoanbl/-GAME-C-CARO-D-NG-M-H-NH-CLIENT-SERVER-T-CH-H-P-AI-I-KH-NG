package caro.server.model.game;

import caro.common.model.Board;
import caro.common.model.EndReason;
import caro.common.model.GameResult;
import caro.common.model.Symbol;
import caro.server.model.dao.MatchRecorder;
import caro.server.model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class GameSessionTest {

    /** Ghi lại các lần gọi record. */
    static class FakeRecorder implements MatchRecorder {
        final List<Object[]> calls = new ArrayList<>();
        @Override
        public void record(String playerX, String playerO, boolean vsBot, int botLevel,
                           GameResult result, int moveCount, Instant startedAt, Instant endedAt) {
            calls.add(new Object[] { playerX, playerO, vsBot, botLevel, result, moveCount, startedAt, endedAt });
        }
    }

    private FakePlayer an;
    private FakePlayer binh;
    private FakeRecorder recorder;
    private List<GameResult> ended;
    private GameSession session;

    @BeforeEach
    void setUp() {
        an = new FakePlayer("An");
        binh = new FakePlayer("Bình");
        recorder = new FakeRecorder();
        ended = new ArrayList<>();
        session = new GameSession("R1", an, binh, (s, r) -> ended.add(r), recorder);
        session.start();
        an.clear();
        binh.clear();
    }

    /** X và O đánh xen kẽ theo danh sách nước {r, c}; X đi trước. */
    private void play(int[]... moves) {
        for (int i = 0; i < moves.length; i++) {
            Player p = i % 2 == 0 ? an : binh;
            session.handleMove(p, moves[i][0], moves[i][1]);
        }
    }

    // ---------- bắt đầu ván ----------

    @Test
    void startSendsGameStartToBoth() {
        FakePlayer x = new FakePlayer("An"), o = new FakePlayer("Bình");
        GameSession s = new GameSession("R9", x, o, null, null);
        s.start();
        assertEquals(List.of("GAME_START|R9|X|Bình|X"), x.lines());
        assertEquals(List.of("GAME_START|R9|O|An|X"), o.lines());
        assertEquals(Symbol.X, s.currentTurn());
        assertFalse(s.isFinished());

        s.start();   // gọi lại không gửi thêm
        assertEquals(1, x.inbox.size());
    }

    @Test
    void moveBeforeStartIsRejected() {
        FakePlayer x = new FakePlayer("An"), o = new FakePlayer("Bình");
        GameSession s = new GameSession("R9", x, o, null, null);
        s.handleMove(x, 7, 7);
        assertTrue(x.lastLine().startsWith("ERROR|NO_GAME|"));
        assertEquals(0, s.moveCount());
    }

    // ---------- nước đi ----------

    @Test
    void validMoveIsBroadcastAndSwitchesTurn() {
        session.handleMove(an, 7, 7);
        assertEquals(List.of("MOVED|X|7|7|O"), an.lines());
        assertEquals(List.of("MOVED|X|7|7|O"), binh.lines());
        assertEquals(Symbol.O, session.currentTurn());
        assertEquals(Symbol.X, session.boardSnapshot().get(7, 7));

        session.handleMove(binh, 7, 8);
        assertEquals("MOVED|O|7|8|X", an.lastLine());
        assertEquals(Symbol.X, session.currentTurn());
        assertEquals(2, session.moveCount());
    }

    @Test
    void notYourTurn() {   // T-05
        session.handleMove(binh, 7, 7);
        assertTrue(binh.lastLine().startsWith("ERROR|NOT_YOUR_TURN|"));
        assertTrue(an.inbox.isEmpty(), "người kia không nhận gì");
        assertTrue(session.boardSnapshot().isEmpty(7, 7), "bàn cờ không đổi");
        assertEquals(Symbol.X, session.currentTurn());
    }

    @Test
    void outOfBoard() {
        session.handleMove(an, -1, 0);
        assertTrue(an.lastLine().startsWith("ERROR|OUT_OF_BOARD|"));
        session.handleMove(an, 0, Board.SIZE);
        assertTrue(an.lastLine().startsWith("ERROR|OUT_OF_BOARD|"));
        assertEquals(Symbol.X, session.currentTurn());
        assertTrue(binh.inbox.isEmpty());
    }

    @Test
    void cellOccupied() {
        session.handleMove(an, 7, 7);
        session.handleMove(binh, 7, 7);
        assertTrue(binh.lastLine().startsWith("ERROR|CELL_OCCUPIED|"));
        assertEquals(Symbol.O, session.currentTurn(), "vẫn lượt O");
        assertEquals(Symbol.X, session.boardSnapshot().get(7, 7));
    }

    @Test
    void outsiderGetsNoGame() {
        FakePlayer stranger = new FakePlayer("Lạ");
        session.handleMove(stranger, 7, 7);
        assertTrue(stranger.lastLine().startsWith("ERROR|NO_GAME|"));
        assertTrue(an.inbox.isEmpty());
        assertTrue(session.boardSnapshot().isEmpty(7, 7));
    }

    @Test
    void boardSnapshotIsACopy() {
        session.boardSnapshot().place(0, 0, Symbol.O);
        assertTrue(session.boardSnapshot().isEmpty(0, 0));
    }

    // ---------- thắng / hòa ----------

    @Test
    void fiveInARowWins() {
        // X: (7,3)…(7,7); O: (8,3)…(8,6)
        play(new int[] {7, 3}, new int[] {8, 3},
             new int[] {7, 4}, new int[] {8, 4},
             new int[] {7, 5}, new int[] {8, 5},
             new int[] {7, 6}, new int[] {8, 6},
             new int[] {7, 7});

        assertTrue(session.isFinished());
        String over = "GAME_OVER|X|FIVE|7,3;7,4;7,5;7,6;7,7";
        assertEquals(List.of("MOVED|X|7|7|O", over), an.lines().subList(an.inbox.size() - 2, an.inbox.size()));
        assertEquals(over, binh.lastLine());

        GameResult r = session.result();
        assertEquals(Symbol.X, r.winner());
        assertEquals(EndReason.FIVE, r.reason());
        assertEquals(List.of(r), ended, "listener được gọi đúng 1 lần");
    }

    @Test
    void oCanWinToo() {
        // X rải rác, O đủ 5 theo cột 0
        play(new int[] {0, 14}, new int[] {0, 0},
             new int[] {2, 14}, new int[] {1, 0},
             new int[] {4, 14}, new int[] {2, 0},
             new int[] {6, 14}, new int[] {3, 0},
             new int[] {8, 14}, new int[] {4, 0});
        assertEquals("GAME_OVER|O|FIVE|0,0;1,0;2,0;3,0;4,0", an.lastLine());
    }

    @Test
    void noMovesAfterGameOver() {
        fiveInARowWins();
        int before = binh.inbox.size();
        session.handleMove(binh, 0, 0);
        assertTrue(binh.lastLine().startsWith("ERROR|NO_GAME|"));
        assertEquals(before + 1, binh.inbox.size());
        assertTrue(session.boardSnapshot().isEmpty(0, 0));
        assertEquals(1, ended.size());
    }

    @Test
    void fullBoardIsDraw() {
        // Mẫu XXOO lệch 2 cột mỗi hàng: không có 5 quân liền. Mỗi hàng 15 ô nên X và O
        // không bằng nhau theo hàng; đánh theo từng lượt bằng cách lấy lần lượt ô X / ô O chưa đánh.
        List<int[]> xs = new ArrayList<>(), os = new ArrayList<>();
        for (int r = 0; r < Board.SIZE; r++)
            for (int c = 0; c < Board.SIZE; c++)
                (((c + 2 * r) / 2) % 2 == 0 ? xs : os).add(new int[] {r, c});
        assertEquals(113, xs.size(), "X đi trước nên có 113 ô");
        assertEquals(112, os.size());

        for (int i = 0; i < os.size(); i++) {
            session.handleMove(an, xs.get(i)[0], xs.get(i)[1]);
            session.handleMove(binh, os.get(i)[0], os.get(i)[1]);
        }
        assertFalse(session.isFinished(), "còn 1 ô");
        session.handleMove(an, xs.get(112)[0], xs.get(112)[1]);

        assertTrue(session.isFinished());
        assertEquals("GAME_OVER|DRAW|FULL|", an.lastLine());
        assertEquals("GAME_OVER|DRAW|FULL|", binh.lastLine());
        assertTrue(session.result().isDraw());
        assertEquals(225, session.moveCount());
    }

    // ---------- đầu hàng / mất kết nối ----------

    @Test
    void surrenderGivesWinToOpponent() {
        session.handleMove(an, 7, 7);
        session.handleSurrender(an);
        assertEquals("GAME_OVER|O|SURRENDER|", an.lastLine());
        assertEquals("GAME_OVER|O|SURRENDER|", binh.lastLine());
        assertEquals(1, ended.size());

        session.handleSurrender(binh);   // đã kết thúc
        assertTrue(binh.lastLine().startsWith("ERROR|NO_GAME|"));
        assertEquals(1, ended.size());
    }

    @Test
    void disconnectNotifiesOnlyRemainingPlayer() {
        session.handleMove(an, 7, 7);
        an.clear();
        binh.clear();

        session.handleDisconnect(binh);
        assertEquals(List.of("GAME_OVER|X|LEFT|"), an.lines());
        assertTrue(binh.inbox.isEmpty(), "không gửi cho người đã rời");
        assertEquals(EndReason.LEFT, session.result().reason());
        assertEquals(1, ended.size());

        session.handleDisconnect(an);    // ván đã kết thúc → bỏ qua
        assertEquals(1, ended.size());
    }

    // ---------- ghi CSDL ----------

    @Test
    void recordsFinishedGame() {
        fiveInARowWins();
        assertEquals(1, recorder.calls.size());
        Object[] c = recorder.calls.get(0);
        assertEquals("An", c[0]);
        assertEquals("Bình", c[1]);
        assertEquals(false, c[2]);
        assertEquals(0, c[3]);
        assertEquals(EndReason.FIVE, ((GameResult) c[4]).reason());
        assertEquals(9, c[5]);
        assertNotNull(c[6]);
        assertFalse(((Instant) c[7]).isBefore((Instant) c[6]));
    }

    @Test
    void guestGameIsNotRecorded() {
        FakePlayer guest = FakePlayer.guest("Khách-1");
        FakePlayer bot = new FakePlayer("BOT", true, false);
        FakeRecorder rec = new FakeRecorder();
        GameSession s = new GameSession("B1", guest, bot, null, rec);
        s.start();
        s.handleSurrender(guest);
        assertTrue(s.isFinished());
        assertTrue(rec.calls.isEmpty());
    }

    @Test
    void recorderFailureDoesNotBreakGame() {
        GameSession s = new GameSession("R2", an, binh, (x, r) -> ended.add(r),
                (px, po, vb, lv, r, n, st, en) -> { throw new IllegalStateException("DB down"); });
        s.start();
        s.handleSurrender(binh);
        assertTrue(s.isFinished());
        assertEquals("GAME_OVER|X|SURRENDER|", an.lastLine());
        assertEquals(1, ended.size(), "listener vẫn được gọi");
    }

    // ---------- đồng thời ----------

    @Test
    void concurrentMovesOnSameCellOnlyOneWins() throws Exception {
        // Hai luồng cùng gửi nhiều lần nước của X vào cùng ô: chỉ đúng 1 nước được chấp nhận.
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch go = new CountDownLatch(1);
        for (int i = 0; i < 50; i++) {
            pool.submit(() -> { go.await(); session.handleMove(an, 7, 7); return null; });
        }
        go.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(1, session.moveCount());
        assertEquals(1, binh.lines().stream().filter(l -> l.startsWith("MOVED|")).count());
        assertEquals(Symbol.O, session.currentTurn());
    }
}
