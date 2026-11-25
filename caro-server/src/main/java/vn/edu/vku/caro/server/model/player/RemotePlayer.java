package vn.edu.vku.caro.server.model.player;

import vn.edu.vku.caro.common.protocol.Message;
import vn.edu.vku.caro.server.net.ClientHandler;

public class RemotePlayer implements Player {
    private final ClientHandler handler;
    private volatile String name;      // null = chưa đăng nhập
    private volatile boolean guest;

    public RemotePlayer(ClientHandler handler) { this.handler = handler; }

    public boolean isLoggedIn() { return name != null && !guest; }
    public void login(String name) { this.name = name; this.guest = false; }
    public void logout() { this.name = null; }

    @Override public String getName() { return name; }
    @Override public boolean isBot() { return false; }
    @Override public boolean isGuest() { return guest; }
    @Override public void send(Message msg) { handler.send(msg); }
}
