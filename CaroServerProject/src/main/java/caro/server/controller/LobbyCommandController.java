package caro.server.controller;

import caro.common.protocol.Command;
import caro.common.protocol.Message;
import caro.server.messages.ErrorMessages;
import caro.server.messages.LobbyMessages;
import caro.server.model.player.PlayerRegistry;
import caro.server.model.player.RemotePlayer;
import caro.server.model.room.Room;
import caro.server.model.room.RoomManager;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Controller xử lý các lệnh sảnh/phòng ở GĐ2 (FE-07).
 * Xử lý: {@code LIST_ROOMS}, {@code CREATE_ROOM}.
 */
public class LobbyCommandController implements CommandController {
    private static final Set<Command> COMMANDS = EnumSet.of(Command.LIST_ROOMS, Command.CREATE_ROOM);

    private final RoomManager rooms;
    private final PlayerRegistry registry;

    public LobbyCommandController(RoomManager rooms, PlayerRegistry registry) {
        this.rooms = Objects.requireNonNull(rooms, "rooms");
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public Set<Command> commands() {
        return COMMANDS;
    }

    @Override
    public void handle(RemotePlayer from, Message msg) {
        switch (Command.valueOf(msg.command())) {
            case LIST_ROOMS -> listRooms(from);
            case CREATE_ROOM -> createRoom(from);
            default -> from.send(ErrorMessages.error("BAD_MESSAGE", "Lệnh chưa được hỗ trợ: " + msg.command()));
        }
    }

    private void listRooms(RemotePlayer from) {
        from.send(LobbyMessages.roomList(rooms.encodeRoomList()));
    }

    private void createRoom(RemotePlayer from) {
        if (rooms.findRoomOf(from) != null) {
            from.send(ErrorMessages.error("ALREADY_IN_ROOM", "Bạn đang ở trong phòng"));
            return;
        }
        Room room = rooms.createRoom(from);
        from.send(LobbyMessages.roomCreated(room.getId()));

        // Cập nhật danh sách phòng mới cho tất cả người chơi đang ở sảnh
        broadcastRoomList();
    }

    public void broadcastRoomList() {
        String encoded = rooms.encodeRoomList();
        for (RemotePlayer p : registry.online()) {
            if (rooms.findRoomOf(p) == null) {
                p.send(LobbyMessages.roomList(encoded));
            }
        }
    }
}
