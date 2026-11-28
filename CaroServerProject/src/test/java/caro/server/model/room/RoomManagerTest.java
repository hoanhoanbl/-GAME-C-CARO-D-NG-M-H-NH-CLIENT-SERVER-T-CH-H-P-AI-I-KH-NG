package caro.server.model.room;

import caro.common.protocol.Message;
import caro.server.model.game.GameSession;
import caro.server.model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RoomManagerTest {

    private static class DummyPlayer implements Player {
        private final String name;
        final List<Message> sent = new ArrayList<>();

        DummyPlayer(String name) { this.name = name; }
        @Override public String getName() { return name; }
        @Override public boolean isBot() { return false; }
        @Override public boolean isGuest() { return false; }
        @Override public void send(Message msg) { sent.add(msg); }
    }

    private RoomManager roomManager;

    @BeforeEach
    void setUp() {
        roomManager = new RoomManager();
    }

    @Test
    void testCreateRoom() {
        Player p1 = new DummyPlayer("Alice");
        Room room = roomManager.createRoom(p1);

        assertNotNull(room);
        assertTrue(room.getId().startsWith("R"));
        assertEquals(p1, room.getPlayerX());
        assertNull(room.getPlayerO());
        assertTrue(room.isWaiting());
        assertFalse(room.isFull());
        assertTrue(room.hasPlayer(p1));
    }

    @Test
    void testFindRoomOf() {
        Player p1 = new DummyPlayer("Bob");
        Room room = roomManager.createRoom(p1);

        assertEquals(room, roomManager.findRoomOf(p1));
        assertNull(roomManager.findRoomOf(new DummyPlayer("Charlie")));
    }

    @Test
    void testEncodeRoomList() {
        Player p1 = new DummyPlayer("David");
        Room room = roomManager.createRoom(p1);

        String encoded = roomManager.encodeRoomList();
        assertTrue(encoded.contains(room.getId() + ":David:WAITING"));
    }

    @Test
    void testCreateBotRoom() {
        Player human = new DummyPlayer("Eve");
        Player bot = new DummyPlayer("BOT");
        Room botRoom = roomManager.createBotRoom(human, bot, 2);

        assertTrue(botRoom.isVsBot());
        assertEquals(2, botRoom.getBotLevel());
        assertNotNull(botRoom.getSession());

        // Phòng bot không được xuất hiện trong encodeRoomList của sảnh
        String encoded = roomManager.encodeRoomList();
        assertFalse(encoded.contains(botRoom.getId()));
    }

    @Test
    void testStartGame() {
        DummyPlayer p1 = new DummyPlayer("HostPlayer");
        DummyPlayer p2 = new DummyPlayer("GuestPlayer");

        Room room = roomManager.createRoom(p1);
        GameSession session = roomManager.startGame(room, p2);

        assertNotNull(session);
        assertEquals(p2, room.getPlayerO());
        assertEquals(Room.Status.PLAYING, room.getStatus());
        assertTrue(room.isFull());
        assertFalse(room.isWaiting());

        // Cả 2 người chơi đều nhận GAME_START từ session.start()
        assertEquals(1, p1.sent.size());
        assertEquals("GAME_START", p1.sent.get(0).command());
        assertEquals("X", p1.sent.get(0).arg(1));
        assertEquals("GuestPlayer", p1.sent.get(0).arg(2));

        assertEquals(1, p2.sent.size());
        assertEquals("GAME_START", p2.sent.get(0).command());
        assertEquals("O", p2.sent.get(0).arg(1));
        assertEquals("HostPlayer", p2.sent.get(0).arg(2));
    }

    @Test
    void testRemoveRoom() {
        Player p1 = new DummyPlayer("Frank");
        Room room = roomManager.createRoom(p1);
        roomManager.removeRoom(room.getId());

        assertNull(roomManager.findRoomById(room.getId()));
        assertNull(roomManager.findRoomOf(p1));
    }
}
