package vn.edu.vku.caro.server.model.bot;

import vn.edu.vku.caro.common.protocol.Message;
import vn.edu.vku.caro.server.model.player.Player;

public class BotPlayer implements Player {
    private final int level;
    public BotPlayer(int level) { this.level = level; }
    @Override public String getName() { return "BOT"; }
    @Override public boolean isBot() { return true; }
    @Override public boolean isGuest() { return false; }
    @Override public void send(Message msg) { /* TODO H: khi tới lượt thì nhờ BotExecutor tính nước */ }
}
