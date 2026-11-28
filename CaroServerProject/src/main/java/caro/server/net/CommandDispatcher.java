package caro.server.net;

import caro.common.protocol.Command;
import caro.common.protocol.Message;
import caro.server.controller.CommandController;
import caro.server.messages.AuthMessages;
import caro.server.messages.ErrorMessages;
import caro.server.messages.LobbyMessages;
import caro.server.model.auth.AuthService;
import caro.server.model.auth.AuthService.AuthException;
import caro.server.model.dao.PlayerDao.PlayerRecord;
import caro.server.model.game.GameSession;
import caro.server.model.player.PlayerRegistry;
import caro.server.model.player.RemotePlayer;
import caro.server.model.room.RoomManager;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Nhận lệnh từ client, kiểm tra quyền theo trạng thái người gửi, gọi service/controller tương ứng. */
public class CommandDispatcher {
    private final AuthService auth;
    private final RoomManager rooms;
    private final PlayerRegistry registry;
    private final Map<Command, CommandController> controllers = new EnumMap<>(Command.class);

    public CommandDispatcher(AuthService auth, RoomManager rooms, PlayerRegistry registry) {
        this.auth = auth;
        this.rooms = Objects.requireNonNull(rooms, "rooms");
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    /** @return false nếu server nên đóng kết nối (lệnh QUIT). */
    public boolean dispatch(RemotePlayer p, Message msg) {
        Command cmd;
        try {
            cmd = Command.valueOf(msg.command());
        } catch (IllegalArgumentException e) {
            p.send(ErrorMessages.error("BAD_MESSAGE", "Lệnh không xác định"));
            return true;
        }
        if (!Permissions.isClientCommand(cmd)) {
            p.send(ErrorMessages.error("BAD_MESSAGE", "Lệnh chỉ server được gửi: " + cmd));
            return true;
        }
        if (!Permissions.allowed(stateOf(p), cmd)) {
            p.send(ErrorMessages.error("NOT_LOGGED_IN", "Bạn chưa đăng nhập"));
            return true;
        }

        switch (cmd) {
            case PING -> p.send(LobbyMessages.pong());
            case QUIT -> { return false; }
            case REGISTER -> register(p, msg);
            case LOGIN -> login(p, msg);
            case LOGOUT -> logout(p);
            default -> {
                CommandController c = controllers.get(cmd);
                if (c != null) c.handle(p, msg);
                else p.send(ErrorMessages.error("BAD_MESSAGE", "Lệnh chưa được hỗ trợ: " + cmd));
            }
        }
        return true;
    }

    /** Trạng thái người gửi xét theo bảng quyền. */
    Permissions.State stateOf(RemotePlayer p) {
        if (p.isLoggedIn()) return Permissions.State.LOGGED_IN;
        if (p.isGuest() && rooms.findSessionOf(p) != null) return Permissions.State.GUEST_IN_BOT_GAME;
        return Permissions.State.ANONYMOUS;
    }

    /** Đăng ký một controller cho các lệnh của nó. Mỗi lệnh chỉ thuộc một controller. */
    public void register(CommandController c) {
        for (Command cmd : c.commands()) {
            CommandController old = controllers.putIfAbsent(cmd, c);
            if (old != null) {
                throw new IllegalStateException("Lệnh " + cmd + " đã được " + old.getClass().getSimpleName() + " xử lý");
            }
        }
    }

    private void register(RemotePlayer p, Message msg) {
        if (p.isLoggedIn()) { p.send(ErrorMessages.error("ALREADY_LOGGED_IN", "Bạn đã đăng nhập")); return; }
        if (msg.argCount() < 2 || msg.argCount() > 3) {
            p.send(ErrorMessages.error("BAD_MESSAGE", "REGISTER|username|password[|email]"));
            return;
        }
        try {
            String email = msg.argCount() == 3 ? msg.arg(2) : null;
            auth.register(msg.arg(0), msg.arg(1), email);
            p.send(AuthMessages.registered(msg.arg(0)));
            System.out.println("[AUTH] Đăng ký: " + msg.arg(0));
        } catch (AuthException e) {
            p.send(ErrorMessages.error(e.code(), "Đăng ký thất bại"));
        }
    }

    private void login(RemotePlayer p, Message msg) {
        if (p.isLoggedIn()) { p.send(ErrorMessages.error("ALREADY_LOGGED_IN", "Bạn đã đăng nhập")); return; }
        if (msg.argCount() != 2) { p.send(ErrorMessages.error("BAD_MESSAGE", "LOGIN|username|password")); return; }
        try {
            PlayerRecord record = auth.authenticate(msg.arg(0), msg.arg(1));
            p.login(record.username());
            registry.add(p);
            p.send(AuthMessages.welcome(record.username(), record.email()));
            p.send(LobbyMessages.roomList(rooms.encodeRoomList()));
            System.out.println("[AUTH] Đăng nhập: " + record.username());
        } catch (AuthException e) {
            p.send(ErrorMessages.error(e.code(), "Đăng nhập thất bại"));
        }
    }

    private void logout(RemotePlayer p) {
        if (rooms.findRoomOf(p) != null || rooms.findSessionOf(p) != null) {
            p.send(ErrorMessages.error("IN_GAME", "Hãy rời phòng/ván trước khi đăng xuất"));
            return;
        }
        auth.logout(p.getName());
        registry.remove(p);
        System.out.println("[AUTH] Đăng xuất: " + p.getName());
        p.logout();
        p.send(AuthMessages.loggedOut());
    }

    /**
     * Gọi khi socket đóng: kết thúc ván đang chơi (đối thủ thắng {@code LEFT}), rồi dọn trạng thái đăng nhập
     * để tài khoản được đăng nhập lại.
     * TODO P (2.P.3): thay đoạn tìm ván bằng {@code rooms.onDisconnect(p)} khi RoomManager dọn cả phòng.
     */
    public void onDisconnect(RemotePlayer p) {
        GameSession session = rooms.findSessionOf(p);
        if (session != null) session.handleDisconnect(p);

        registry.remove(p);
        if (p.isLoggedIn()) {
            auth.logout(p.getName());
            System.out.println("[AUTH] Mất kết nối, dọn: " + p.getName());
        }
    }
}
