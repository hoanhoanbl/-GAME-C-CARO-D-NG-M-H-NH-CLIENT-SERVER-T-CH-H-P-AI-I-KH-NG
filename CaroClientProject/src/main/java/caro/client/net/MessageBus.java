package caro.client.net;

import caro.common.protocol.Command;
import caro.common.protocol.Message;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class MessageBus {
    private static final MessageBus INSTANCE = new MessageBus();
    public static MessageBus get() { return INSTANCE; }

    private final Map<Command, List<Consumer<Message>>> subscribers = new ConcurrentHashMap<>();

    public void subscribe(Command cmd, Consumer<Message> handler) {
        subscribers.computeIfAbsent(cmd, k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    public void unsubscribe(Command cmd, Consumer<Message> handler) {
        List<Consumer<Message>> list = subscribers.get(cmd);
        if (list != null) {
            list.remove(handler);
        }
    }

    public void publish(Message msg) {
        if (msg == null) return;
        try {
            Command cmd = Command.valueOf(msg.command());
            List<Consumer<Message>> list = subscribers.get(cmd);
            if (list != null) {
                for (Consumer<Message> handler : list) {
                    handler.accept(msg);
                }
            }
        } catch (IllegalArgumentException ignored) {
            // Lệnh không nằm trong enum Command
        }
    }
}
