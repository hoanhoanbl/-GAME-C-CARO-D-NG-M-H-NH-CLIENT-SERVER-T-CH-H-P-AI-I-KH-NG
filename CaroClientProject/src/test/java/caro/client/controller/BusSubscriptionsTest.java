package caro.client.controller;

import caro.client.net.MessageBusAccess;
import caro.client.net.MessageBus;
import caro.common.protocol.Command;
import caro.common.protocol.Message;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class BusSubscriptionsTest {

    private static boolean fxAvailable;

    @BeforeAll
    static void startFx() throws InterruptedException {
        CountDownLatch ready = new CountDownLatch(1);
        try {
            Platform.startup(ready::countDown);
        } catch (IllegalStateException alreadyStarted) {
            ready.countDown();
        } catch (UnsupportedOperationException | Error noDisplay) {
            return;   // máy không có màn hình: bỏ qua các test cần Stage
        }
        fxAvailable = ready.await(5, TimeUnit.SECONDS);
    }

    /** Chạy trên FX thread và chờ xong. */
    private static void fx(Runnable r) throws Exception {
        AtomicReference<Throwable> err = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try { r.run(); } catch (Throwable t) { err.set(t); } finally { done.countDown(); }
        });
        assertTrue(done.await(5, TimeUnit.SECONDS));
        if (err.get() != null) throw new AssertionError(err.get());
    }

    @Test
    void handlersRemovedWhenSceneLosesWindow() throws Exception {
        assumeTrue(fxAvailable, "không có JavaFX");
        MessageBus bus = MessageBusAccess.newBus();
        List<Message> got = new ArrayList<>();

        fx(() -> {
            Stage stage = new Stage();
            Pane node = new StackPane();
            stage.setScene(new Scene(node));
            BusSubscriptions subs = new BusSubscriptions(bus).on(Command.WELCOME, got::add).bindTo(node);
            subs.bindTo(node);                                     // gọi lần hai không đăng ký trùng
            assertEquals(1, bus.subscriberCount(Command.WELCOME));

            bus.publish(Message.parse("WELCOME|anan"));
            assertEquals(1, got.size());

            stage.setScene(new Scene(new StackPane()));           // như SceneManager.show(...)
            assertEquals(0, bus.subscriberCount(Command.WELCOME));
            bus.publish(Message.parse("WELCOME|anan"));
            assertEquals(1, got.size(), "handler cũ không còn được gọi");
        });
    }

    @Test
    void reopeningScreenKeepsOneHandler() throws Exception {
        assumeTrue(fxAvailable, "không có JavaFX");
        MessageBus bus = MessageBusAccess.newBus();
        List<String> calls = new ArrayList<>();

        fx(() -> {
            Stage stage = new Stage();
            for (int i = 0; i < 2; i++) {                          // mở màn hình đăng nhập hai lần
                Pane root = new StackPane();
                stage.setScene(new Scene(root));
                int n = i;
                new BusSubscriptions(bus).on(Command.WELCOME, m -> calls.add("controller" + n)).bindTo(root);
            }
            bus.publish(Message.parse("WELCOME|anan"));
            assertEquals(List.of("controller1"), calls);
        });
    }

    @Test
    void disposeAllWithoutFx() {
        MessageBus bus = MessageBusAccess.newBus();
        BusSubscriptions subs = new BusSubscriptions(bus).on(Command.MOVED, m -> { }).on(Command.ERROR, m -> { });
        assertEquals(2, subs.size());
        subs.disposeAll();
        assertEquals(0, bus.subscriberCount(Command.MOVED));
        assertEquals(0, bus.subscriberCount(Command.ERROR));
    }
}
