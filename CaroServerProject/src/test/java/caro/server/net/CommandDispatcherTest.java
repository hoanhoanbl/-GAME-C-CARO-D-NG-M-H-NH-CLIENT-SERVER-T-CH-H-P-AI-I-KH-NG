package caro.server.net;

import caro.common.protocol.Message;
import caro.server.controller.GameCommandController;
import caro.server.model.auth.AuthService;
import caro.server.model.auth.AuthService.AuthException;
import caro.server.model.dao.PlayerDao;
import caro.server.model.game.GameSession;
import caro.server.model.player.Player;
import caro.server.model.player.PlayerRegistry;
import caro.server.model.player.RemotePlayer;
import caro.server.model.room.RoomManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CommandDispatcherTest {

    /** RemotePlayer không có socket: ghi lại thông điệp. */
    static class CapturingPlayer extends RemotePlayer {
        final List<String> lines = new ArrayList<>();
        CapturingPlayer() { super(new ClientHandler(null, null)); }
        @Override public synchronized void send(Message msg) { lines.add(msg.format()); }
        synchronized String last() { return lines.isEmpty() ? null : lines.get(lines.size() - 1); }
    }

    static class FakeDao extends PlayerDao {
        private final Map<String, PlayerRecord> players = new HashMap<>();
        FakeDao() { super(null); }
        @Override public boolean create(String u, byte[] h, byte[] s, String email) {
            if (players.containsKey(u.toLowerCase())) return false;
            players.put(u.toLowerCase(), new PlayerRecord(players.size() + 1, u, h, s, email));
            return true;
        }
        @Override public Optional<PlayerRecord> findByUsername(String u) {
            return Optional.ofNullable(players.get(u.toLowerCase()));
        }
        @Override public void touchLastLogin(int id) { }
    }

    /** RoomManager giả: biết một ván dựng sẵn. */
    static class FakeRooms extends RoomManager {
        GameSession session;
        @Override public GameSession findSessionOf(Player p) {
            return session != null && session.symbolOf(p) != null && !session.isFinished() ? session : null;
        }
    }

    private AuthService auth;
    private FakeRooms rooms;
    private PlayerRegistry registry;
    private CommandDispatcher dispatcher;

    @BeforeEach
    void setUp() throws AuthException {
        auth = new AuthService(new FakeDao());
        auth.register("anan", "123456");
        auth.register("binh", "123456");
        rooms = new FakeRooms();
        registry = new PlayerRegistry();
        dispatcher = new CommandDispatcher(auth, rooms, registry);
        dispatcher.register(new GameCommandController(rooms));
    }

    private CapturingPlayer loggedIn(String name) {
        CapturingPlayer p = new CapturingPlayer();
        dispatcher.dispatch(p, Message.parse("LOGIN|" + name + "|123456"));
        assertTrue(p.isLoggedIn(), "đăng nhập " + name);
        p.lines.clear();
        return p;
    }

    private GameSession startGame(Player x, Player o) {
        GameSession s = new GameSession("R1", x, o, null, null);
        rooms.session = s;
        s.start();
        return s;
    }

    @Test
    void anonymousCannotCreateRoom() {
        CapturingPlayer anon = new CapturingPlayer();
        assertTrue(dispatcher.dispatch(anon, Message.parse("CREATE_ROOM")));
        assertTrue(anon.last().startsWith("ERROR|NOT_LOGGED_IN|"));
    }

    @Test
    void anonymousMayPing() {
        CapturingPlayer anon = new CapturingPlayer();
        dispatcher.dispatch(anon, Message.parse("PING"));
        assertEquals("PONG", anon.last());
    }

    @Test
    void guestInBotGameMayMove() {
        CapturingPlayer guest = new CapturingPlayer();
        guest.enterAsGuest("Khách-1");
        CapturingPlayer other = new CapturingPlayer();
        startGame(guest, other);
        dispatcher.dispatch(guest, Message.parse("MOVE|7|7"));
        assertEquals("MOVED|X|7|7|O", guest.last());
    }

    @Test
    void guestOutsideGameCannotMove() {
        CapturingPlayer guest = new CapturingPlayer();
        guest.enterAsGuest("Khách-1");
        dispatcher.dispatch(guest, Message.parse("MOVE|7|7"));
        assertTrue(guest.last().startsWith("ERROR|NOT_LOGGED_IN|"));
    }

    @Test
    void clientSendingServerCommandIsBadMessage() {
        CapturingPlayer an = loggedIn("anan");
        dispatcher.dispatch(an, Message.parse("MOVED|X|7|7|O"));
        assertTrue(an.last().startsWith("ERROR|BAD_MESSAGE|"));
    }

    @Test
    void logoutDuringGameIsRejected() {
        CapturingPlayer an = loggedIn("anan");
        CapturingPlayer binh = loggedIn("binh");
        startGame(an, binh);
        dispatcher.dispatch(an, Message.parse("LOGOUT"));
        assertTrue(an.last().startsWith("ERROR|IN_GAME|"));
        assertTrue(an.isLoggedIn());
    }

    @Test
    void loginAddsToRegistryAndLogoutRemoves() {
        CapturingPlayer an = loggedIn("anan");
        assertSame(an, registry.find("ANAN").orElseThrow());
        dispatcher.dispatch(an, Message.parse("LOGOUT"));
        assertEquals("LOGGED_OUT", an.last());
        assertTrue(registry.find("anan").isEmpty());
    }

    @Test
    void disconnectEndsGameAndCleansUp() {
        CapturingPlayer an = loggedIn("anan");
        CapturingPlayer binh = loggedIn("binh");
        GameSession s = startGame(an, binh);
        binh.lines.clear();

        dispatcher.onDisconnect(an);

        assertTrue(s.isFinished());
        assertEquals("GAME_OVER|O|LEFT|", binh.last());
        assertTrue(registry.find("anan").isEmpty());

        CapturingPlayer again = new CapturingPlayer();
        dispatcher.dispatch(again, Message.parse("LOGIN|anan|123456"));
        assertTrue(again.isLoggedIn(), "đăng nhập lại được sau khi rớt mạng");
    }

    @Test
    void registerWithTooManyArgsIsBadMessage() {
        CapturingPlayer anon = new CapturingPlayer();
        dispatcher.dispatch(anon, Message.parse("REGISTER|an2|123456|a@b.com|thua"));
        assertTrue(anon.last().startsWith("ERROR|BAD_MESSAGE|"));
    }

    @Test
    void registerWithoutEmail() {
        CapturingPlayer anon = new CapturingPlayer();
        dispatcher.dispatch(anon, Message.parse("REGISTER|an2|123456"));
        assertEquals("REGISTERED|an2", anon.last());
    }

    @Test
    void quitClosesConnection() {
        assertFalse(dispatcher.dispatch(new CapturingPlayer(), Message.parse("QUIT")));
    }
}
