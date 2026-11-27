package caro.client.app;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Mọi màn hình nạp được FXML + controller (fx:id, onAction, stylesheet đúng đường dẫn). */
class FxmlLoadTest {
    private static boolean fxAvailable;

    @BeforeAll
    static void startFx() throws InterruptedException {
        CountDownLatch ready = new CountDownLatch(1);
        try {
            Platform.startup(ready::countDown);
        } catch (IllegalStateException alreadyStarted) {
            ready.countDown();
        } catch (UnsupportedOperationException | Error noDisplay) {
            return;
        }
        fxAvailable = ready.await(5, TimeUnit.SECONDS);
    }

    @Test
    void everyScreenLoads() throws Exception {
        assumeTrue(fxAvailable, "không có JavaFX");
        for (Screen s : Screen.values()) load(s.name().toLowerCase() + ".fxml");
        load("chat.fxml");
    }

    private static void load(String file) throws Exception {
        URL url = FxmlLoadTest.class.getResource("/caro/client/view/" + file);
        assertNotNull(url, file);
        AtomicReference<Throwable> err = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try { FXMLLoader.load(url); } catch (Throwable t) { err.set(t); } finally { done.countDown(); }
        });
        assertTrue(done.await(5, TimeUnit.SECONDS));
        if (err.get() != null) fail("Không nạp được " + file + ": " + err.get(), err.get());
    }
}
