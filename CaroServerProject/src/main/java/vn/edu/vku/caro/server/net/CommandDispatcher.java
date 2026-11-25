package vn.edu.vku.caro.server.net;

import vn.edu.vku.caro.common.protocol.Command;
import vn.edu.vku.caro.common.protocol.Message;
import vn.edu.vku.caro.server.messages.AuthMessages;
import vn.edu.vku.caro.server.messages.ErrorMessages;
import vn.edu.vku.caro.server.messages.LobbyMessages;
import vn.edu.vku.caro.server.model.auth.AuthService;
import vn.edu.vku.caro.server.model.auth.AuthService.AuthException;
import vn.edu.vku.caro.server.model.dao.PlayerDao.PlayerRecord;
import vn.edu.vku.caro.server.model.player.RemotePlayer;

/** Nhận lệnh từ client, kiểm tra trạng thái, gọi service tương ứng. */
public class CommandDispatcher {
    private final AuthService auth;

    public CommandDispatcher(AuthService auth) { this.auth = auth; }

    /** @return false nếu server nên đóng kết nối (lệnh QUIT). */
    public boolean dispatch(RemotePlayer p, Message msg) {
        Command cmd;
        try {
            cmd = Command.valueOf(msg.command());
        } catch (IllegalArgumentException e) {
            p.send(ErrorMessages.error("BAD_MESSAGE", "Lệnh không xác định"));
            return true;
        }

        switch (cmd) {
            case PING -> p.send(LobbyMessages.pong());
            case QUIT -> { return false; }
            case REGISTER -> register(p, msg);
            case LOGIN -> login(p, msg);
            case LOGOUT -> logout(p);
            default -> {
                if (!p.isLoggedIn()) p.send(ErrorMessages.error("NOT_LOGGED_IN", "Bạn chưa đăng nhập"));
                else p.send(ErrorMessages.error("BAD_MESSAGE", "Lệnh chưa được hỗ trợ: " + cmd));
            }
        }
        return true;
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
            p.send(AuthMessages.welcome(record.username(), record.email()));
            p.send(LobbyMessages.roomList(""));       // tạm: chưa có phòng nào
            System.out.println("[AUTH] Đăng nhập: " + record.username());
        } catch (AuthException e) {
            p.send(ErrorMessages.error(e.code(), "Đăng nhập thất bại"));
        }
    }

    private void logout(RemotePlayer p) {
        if (!p.isLoggedIn()) { p.send(ErrorMessages.error("NOT_LOGGED_IN", "Bạn chưa đăng nhập")); return; }
        auth.logout(p.getName());
        System.out.println("[AUTH] Đăng xuất: " + p.getName());
        p.logout();
        p.send(AuthMessages.loggedOut());
    }

    /** Gọi khi socket đóng, để tài khoản được đăng nhập lại. */
    public void onDisconnect(RemotePlayer p) {
        if (p.isLoggedIn()) {
            auth.logout(p.getName());
            System.out.println("[AUTH] Mất kết nối, dọn: " + p.getName());
        }
    }
}
