package caro.server.model.player;

import caro.common.protocol.Message;
import caro.server.net.ClientHandler;

public class RemotePlayer implements Player {
    private final ClientHandler handler;
    private volatile String name;      // null = chưa đăng nhập
    private volatile boolean guest;

    public RemotePlayer(ClientHandler handler) { this.handler = handler; }

    public boolean isLoggedIn() { return name != null && !guest; }
    public void login(String name) { this.name = name; this.guest = false; }
    /** Khách chơi với máy (3.H.2 gọi khi nhận {@code PLAY_BOT} từ người chưa đăng nhập). */
    public void enterAsGuest(String guestName) { this.name = guestName; this.guest = true; }
    public void logout() { this.name = null; this.guest = false; }

    @Override public String getName() { return name; }
    @Override public boolean isBot() { return false; }
    @Override public boolean isGuest() { return guest; }
    @Override public void send(Message msg) { handler.send(msg); }
}
