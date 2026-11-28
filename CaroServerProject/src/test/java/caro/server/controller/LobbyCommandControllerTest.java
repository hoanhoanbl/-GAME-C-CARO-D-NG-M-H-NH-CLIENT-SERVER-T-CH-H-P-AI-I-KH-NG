package caro.server.controller;

import caro.common.protocol.Command;
import caro.common.protocol.Message;
import caro.server.net.ClientHandler;
import caro.server.model.player.PlayerRegistry;
import caro.server.model.player.RemotePlayer;
import caro.server.model.room.Room;
import caro.server.model.room.RoomManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LobbyCommandControllerTest {

    private static class TestPlayer extends RemotePlayer {
        final List<Message> inbox = new ArrayList<>();
        private final String name;

        TestPlayer(String name) {
            super(new ClientHandler(null, null));
            this.name = name;
        }

        @Override public String getName() { return name; }
        @Override public boolean isLoggedIn() { return true; }
        @Override public synchronized void send(Message msg) { inbox.add(msg); }

        Message last() {
            return inbox.isEmpty() ? null : inbox.get(inbox.size() - 1);
        }
    }

    private RoomManager roomManager;
    private PlayerRegistry registry;
    private LobbyCommandController controller;

    @BeforeEach
    void setUp() {
        roomManager = new RoomManager();
        registry = new PlayerRegistry();
        controller = new LobbyCommandController(roomManager, registry);
    }

    @Test
    void testCreateRoomSuccess() {
        TestPlayer p1 = new TestPlayer("Alice");
        registry.add(p1);

        controller.handle(p1, new Message("CREATE_ROOM"));

        assertEquals("ROOM_CREATED", p1.last().command());
        String roomId = p1.last().arg(0);
        assertNotNull(roomManager.findRoomById(roomId));
    }

    @Test
    void testCreateRoomWhenAlreadyInRoom() {
        TestPlayer p1 = new TestPlayer("Alice");
        registry.add(p1);

        controller.handle(p1, new Message("CREATE_ROOM"));
        controller.handle(p1, new Message("CREATE_ROOM"));

        assertEquals("ERROR", p1.last().command());
        assertEquals("ALREADY_IN_ROOM", p1.last().arg(0));
    }

    @Test
    void testJoinRoomSuccess() {
        TestPlayer host = new TestPlayer("Host");
        TestPlayer guest = new TestPlayer("Guest");
        registry.add(host);
        registry.add(guest);

        controller.handle(host, new Message("CREATE_ROOM"));
        String roomId = host.last().arg(0);

        controller.handle(guest, new Message("JOIN_ROOM", roomId));

        // Cả 2 người chơi đều nhận GAME_START
        assertEquals("GAME_START", host.last().command());
        assertEquals("X", host.last().arg(1));
        assertEquals("Guest", host.last().arg(2));

        assertEquals("GAME_START", guest.last().command());
        assertEquals("O", guest.last().arg(1));
        assertEquals("Host", guest.last().arg(2));

        Room room = roomManager.findRoomById(roomId);
        assertEquals(Room.Status.PLAYING, room.getStatus());
    }

    @Test
    void testJoinRoomNotFound() {
        TestPlayer guest = new TestPlayer("Guest");
        registry.add(guest);

        controller.handle(guest, new Message("JOIN_ROOM", "NON_EXISTENT"));

        assertEquals("ERROR", guest.last().command());
        assertEquals("ROOM_NOT_FOUND", guest.last().arg(0));
    }

    @Test
    void testJoinRoomAlreadyFull() {
        TestPlayer host = new TestPlayer("Host");
        TestPlayer guest1 = new TestPlayer("Guest1");
        TestPlayer guest2 = new TestPlayer("Guest2");
        registry.add(host);
        registry.add(guest1);
        registry.add(guest2);

        controller.handle(host, new Message("CREATE_ROOM"));
        String roomId = host.last().arg(0);

        controller.handle(guest1, new Message("JOIN_ROOM", roomId));
        controller.handle(guest2, new Message("JOIN_ROOM", roomId));

        assertEquals("ERROR", guest2.last().command());
        assertEquals("ROOM_FULL", guest2.last().arg(0));
    }

    @Test
    void testLeaveRoomSuccess() {
        TestPlayer host = new TestPlayer("Host");
        registry.add(host);

        controller.handle(host, new Message("CREATE_ROOM"));
        String roomId = host.last().arg(0);

        controller.handle(host, new Message("LEAVE_ROOM"));

        assertEquals("ROOM_LEFT", host.last().command());
        assertNull(roomManager.findRoomById(roomId));
    }

    @Test
    void testLeaveRoomNotInRoom() {
        TestPlayer player = new TestPlayer("Player");
        registry.add(player);

        controller.handle(player, new Message("LEAVE_ROOM"));

        assertEquals("ERROR", player.last().command());
        assertEquals("NOT_IN_ROOM", player.last().arg(0));
    }
}
