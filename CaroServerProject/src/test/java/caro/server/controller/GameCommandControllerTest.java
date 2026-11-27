package caro.server.controller;

import caro.common.model.Symbol;
import caro.common.protocol.Command;
import caro.common.protocol.Message;
import caro.server.model.game.GameSession;
import caro.server.model.player.Player;
import caro.server.model.player.RemotePlayer;
import caro.server.model.room.RoomManager;
import caro.server.net.ClientHandler;
import caro.server.net.CommandDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GameCommandControllerTest {

    /** RemotePlayer không có socket: ghi lại thông điệp thay vì gửi đi. */
    static class CapturingPlayer extends RemotePlayer {
        final List<String> lines = new ArrayList<>();

        CapturingPlayer(String name) {
            super(new ClientHandler(null, null));
            login(name);
        }

        @Override public synchronized void send(Message msg) { lines.add(msg.format()); }

        synchronized String last() { return lines.isEmpty() ? null : lines.get(lines.size() - 1); }
    }

    /** RoomManager giả: chỉ biết một ván dựng sẵn (RoomManager thật là việc 2.P.1 của P). */
    static class FakeRooms extends RoomManager {
        GameSession session;
        @Override public GameSession findSessionOf(Player p) {
            return session != null && session.symbolOf(p) != null ? session : null;
        }
    }

    private CapturingPlayer an;
    private CapturingPlayer binh;
    private FakeRooms rooms;
    private GameCommandController controller;
    private GameSession session;

    @BeforeEach
    void setUp() {
        an = new CapturingPlayer("an");
        binh = new CapturingPlayer("binh");
        rooms = new FakeRooms();
        session = new GameSession("R1", an, binh, null, null);
        rooms.session = session;
        session.start();
        an.lines.clear();
        binh.lines.clear();
        controller = new GameCommandController(rooms);
    }

    private void send(CapturingPlayer p, String line) { controller.handle(p, Message.parse(line)); }

    @Test
    void handlesMoveAndSurrender() {
        assertEquals(Set.of(Command.MOVE, Command.SURRENDER), controller.commands());
    }

    @Test
    void moveIsForwardedToSession() {
        send(an, "MOVE|7|7");
        assertEquals("MOVED|X|7|7|O", an.last());
        assertEquals("MOVED|X|7|7|O", binh.last());
        assertEquals(Symbol.X, session.boardSnapshot().get(7, 7));
    }

    @Test
    void sessionErrorsReachSender() {
        send(binh, "MOVE|7|7");
        assertTrue(binh.last().startsWith("ERROR|NOT_YOUR_TURN|"));
        send(an, "MOVE|15|0");
        assertTrue(an.last().startsWith("ERROR|OUT_OF_BOARD|"));
        assertEquals(0, session.moveCount());
    }

    @Test
    void badMoveArguments() {
        for (String line : List.of("MOVE", "MOVE|7", "MOVE|7|7|7", "MOVE|a|7", "MOVE|7|", "MOVE|1.5|2")) {
            an.lines.clear();
            send(an, line);
            assertEquals(1, an.lines.size(), line);
            assertTrue(an.last().startsWith("ERROR|BAD_MESSAGE|"), line + " → " + an.last());
        }
        assertTrue(binh.lines.isEmpty());
        assertEquals(0, session.moveCount());
    }

    @Test
    void moveWithSpacesIsAccepted() {
        send(an, "MOVE| 3 | 4 ");
        assertEquals("MOVED|X|3|4|O", an.last());
    }

    @Test
    void noGameWhenNotInRoom() {
        CapturingPlayer stranger = new CapturingPlayer("la");
        send(stranger, "MOVE|7|7");
        assertTrue(stranger.last().startsWith("ERROR|NO_GAME|"));
        send(stranger, "SURRENDER");
        assertTrue(stranger.last().startsWith("ERROR|NO_GAME|"));
        assertTrue(an.lines.isEmpty());
    }

    @Test
    void surrender() {
        send(an, "SURRENDER");
        assertEquals("GAME_OVER|O|SURRENDER|", an.last());
        assertEquals("GAME_OVER|O|SURRENDER|", binh.last());
        assertTrue(session.isFinished());
    }

    @Test
    void surrenderWithArgumentsIsRejected() {
        send(an, "SURRENDER|x");
        assertTrue(an.last().startsWith("ERROR|BAD_MESSAGE|"));
        assertFalse(session.isFinished());
    }

    // ---------- qua CommandDispatcher ----------

    @Test
    void dispatcherRoutesMoveToController() {
        CommandDispatcher dispatcher = new CommandDispatcher(null);
        dispatcher.register(controller);
        assertTrue(dispatcher.dispatch(an, Message.parse("MOVE|7|7")));
        assertEquals("MOVED|X|7|7|O", binh.last());
    }

    @Test
    void dispatcherStillRequiresLogin() {
        CommandDispatcher dispatcher = new CommandDispatcher(null);
        dispatcher.register(controller);
        CapturingPlayer anon = new CapturingPlayer("x");
        anon.logout();
        dispatcher.dispatch(anon, Message.parse("MOVE|7|7"));
        assertTrue(anon.last().startsWith("ERROR|NOT_LOGGED_IN|"));
    }

    @Test
    void dispatcherRejectsDuplicateRegistration() {
        CommandDispatcher dispatcher = new CommandDispatcher(null);
        dispatcher.register(controller);
        assertThrows(IllegalStateException.class, () -> dispatcher.register(new GameCommandController(rooms)));
    }
}
