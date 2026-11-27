package caro.server.controller;

import caro.common.protocol.Command;
import caro.common.protocol.Message;
import caro.server.messages.ErrorMessages;
import caro.server.model.game.GameSession;
import caro.server.model.player.RemotePlayer;
import caro.server.model.room.RoomManager;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Lệnh trong ván cờ (FE-10). Controller chỉ đọc tham số, tìm ván của người gửi rồi giao cho
 * {@link GameSession}; mọi luật (lượt, ô, biên, thắng/hòa) nằm trong {@code GameSession} (R7).
 *
 * <p>Hiện xử lý {@code MOVE} (2.H.1) và {@code SURRENDER} (phía server của 3.H.4).
 * TODO H: {@code PLAY_BOT} (3.H.2), {@code REMATCH} / {@code BACK_TO_LOBBY} (3.H.6), {@code CHAT} (2.H.5).
 */
public class GameCommandController implements CommandController {
    private static final Set<Command> COMMANDS = EnumSet.of(Command.MOVE, Command.SURRENDER);

    private final RoomManager rooms;

    public GameCommandController(RoomManager rooms) {
        this.rooms = Objects.requireNonNull(rooms, "rooms");
    }

    @Override
    public Set<Command> commands() { return COMMANDS; }

    @Override
    public void handle(RemotePlayer from, Message msg) {
        switch (Command.valueOf(msg.command())) {
            case MOVE -> move(from, msg);
            case SURRENDER -> surrender(from, msg);
            default -> from.send(ErrorMessages.error("BAD_MESSAGE", "Lệnh chưa được hỗ trợ: " + msg.command()));
        }
    }

    /** {@code MOVE|row|col}. */
    private void move(RemotePlayer from, Message msg) {
        if (msg.argCount() != 2) {
            from.send(ErrorMessages.error("BAD_MESSAGE", "MOVE|row|col"));
            return;
        }
        int row, col;
        try {
            row = Integer.parseInt(msg.arg(0).trim());
            col = Integer.parseInt(msg.arg(1).trim());
        } catch (NumberFormatException e) {
            from.send(ErrorMessages.error("BAD_MESSAGE", "row và col phải là số nguyên"));
            return;
        }
        GameSession session = sessionOf(from);
        if (session != null) session.handleMove(from, row, col);   // OUT_OF_BOARD, NOT_YOUR_TURN… do GameSession báo
    }

    /** {@code SURRENDER}. */
    private void surrender(RemotePlayer from, Message msg) {
        if (msg.argCount() != 0) {
            from.send(ErrorMessages.error("BAD_MESSAGE", "SURRENDER không có tham số"));
            return;
        }
        GameSession session = sessionOf(from);
        if (session != null) session.handleSurrender(from);
    }

    /** Ván của người gửi, hoặc null (đã gửi {@code ERROR|NO_GAME}). */
    private GameSession sessionOf(RemotePlayer p) {
        GameSession s = rooms.findSessionOf(p);
        if (s == null) p.send(ErrorMessages.error("NO_GAME", "Bạn không ở trong ván nào đang diễn ra"));
        return s;
    }
}
