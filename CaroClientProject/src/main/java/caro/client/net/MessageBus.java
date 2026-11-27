package caro.client.net;

import caro.common.protocol.Command;
import caro.common.protocol.Message;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Phân phối thông điệp từ server tới các handler đã đăng ký theo lệnh. Handler chạy trên FX thread. */
public class MessageBus {
    private static final MessageBus INSTANCE = new MessageBus();
    public static MessageBus get() { return INSTANCE; }

    private final Map<Command, List<Consumer<Message>>> subscribers = new ConcurrentHashMap<>();

    /** Cho test; ứng dụng dùng {@link #get()}. */
    MessageBus() { }

    public void subscribe(Command cmd, Consumer<Message> handler) {
        subscribers.computeIfAbsent(cmd, k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    public void unsubscribe(Command cmd, Consumer<Message> handler) {
        List<Consumer<Message>> list = subscribers.get(cmd);
        if (list != null) list.remove(handler);
    }

    /** Số handler đang đăng ký cho lệnh (cho test và gỡ lỗi rò handler). */
    public int subscriberCount(Command cmd) {
        List<Consumer<Message>> list = subscribers.get(cmd);
        return list == null ? 0 : list.size();
    }

    /** Gọi mọi handler của lệnh; một handler ném lỗi không ngăn các handler còn lại. */
    public void publish(Message msg) {
        if (msg == null) return;
        Command cmd;
        try {
            cmd = Command.valueOf(msg.command());
        } catch (IllegalArgumentException e) {
            System.err.println("[MessageBus] Lệnh không xác định: " + msg.command());
            return;
        }
        List<Consumer<Message>> list = subscribers.get(cmd);
        if (list == null) return;
        for (Consumer<Message> handler : list) {
            try {
                handler.accept(msg);
            } catch (RuntimeException e) {
                System.err.println("[MessageBus] Handler của " + cmd + " lỗi: " + e);
                e.printStackTrace();
            }
        }
    }
}
