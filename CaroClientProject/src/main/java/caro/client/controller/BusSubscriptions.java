package caro.client.controller;

import caro.client.net.MessageBus;
import caro.common.protocol.Command;
import caro.common.protocol.Message;
import javafx.scene.Node;
import javafx.scene.Scene;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Các handler {@link MessageBus} của một controller, gắn với vòng đời màn hình: đăng ký ngay khi gọi
 * {@link #on}, tự hủy hết khi Scene chứa node gốc mất Window ({@code SceneManager} đặt Scene mới).
 *
 * <pre>{@code
 * subs.on(Command.MOVED, this::handleMoved).on(Command.ERROR, this::handleError).bindTo(boardCanvas);
 * }</pre>
 */
public final class BusSubscriptions {
    private record Entry(Command cmd, Consumer<Message> handler) { }

    private final MessageBus bus;
    private final List<Entry> entries = new ArrayList<>();
    private boolean bound;

    public BusSubscriptions() { this(MessageBus.get()); }

    BusSubscriptions(MessageBus bus) { this.bus = Objects.requireNonNull(bus); }

    /** Đăng ký handler ngay. */
    public BusSubscriptions on(Command cmd, Consumer<Message> handler) {
        bus.subscribe(cmd, handler);
        entries.add(new Entry(cmd, handler));
        return this;
    }

    /** Tự {@link #disposeAll()} khi Scene của {@code node} mất Window. Gọi lại lần hai không có tác dụng. */
    public BusSubscriptions bindTo(Node node) {
        if (bound) return this;
        bound = true;
        node.sceneProperty().addListener((obs, oldScene, scene) -> watch(scene));
        watch(node.getScene());
        return this;
    }

    private void watch(Scene scene) {
        if (scene == null) return;
        scene.windowProperty().addListener((obs, oldWindow, window) -> {
            if (oldWindow != null && window == null) disposeAll();
        });
    }

    /** Hủy mọi handler đã đăng ký qua đối tượng này. */
    public void disposeAll() {
        for (Entry e : entries) bus.unsubscribe(e.cmd(), e.handler());
        entries.clear();
    }

    /** Số handler còn đăng ký (cho test). */
    int size() { return entries.size(); }
}
