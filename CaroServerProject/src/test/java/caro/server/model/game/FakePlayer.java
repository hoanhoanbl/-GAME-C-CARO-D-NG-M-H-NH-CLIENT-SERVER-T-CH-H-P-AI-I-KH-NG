package caro.server.model.game;

import caro.common.protocol.Message;
import caro.server.model.player.Player;

import java.util.ArrayList;
import java.util.List;

/** Người chơi giả cho test: ghi lại mọi thông điệp nhận được thay vì gửi qua socket. */
class FakePlayer implements Player {
    private final String name;
    private final boolean bot;
    private final boolean guest;
    final List<Message> inbox = new ArrayList<>();

    FakePlayer(String name) { this(name, false, false); }

    FakePlayer(String name, boolean bot, boolean guest) {
        this.name = name;
        this.bot = bot;
        this.guest = guest;
    }

    static FakePlayer guest(String name) { return new FakePlayer(name, false, true); }

    @Override public String getName() { return name; }
    @Override public boolean isBot() { return bot; }
    @Override public boolean isGuest() { return guest; }
    @Override public synchronized void send(Message msg) { inbox.add(msg); }

    synchronized Message last() { return inbox.isEmpty() ? null : inbox.get(inbox.size() - 1); }

    /** Dạng chuỗi giao thức của thông điệp cuối, ví dụ {@code MOVED|X|7|7|O}. */
    String lastLine() {
        Message m = last();
        return m == null ? null : m.format();
    }

    synchronized List<String> lines() { return inbox.stream().map(Message::format).toList(); }

    synchronized void clear() { inbox.clear(); }
}
