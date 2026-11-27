package caro.client.net;

import caro.common.protocol.Command;
import caro.common.protocol.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class NetworkClientTest {

    private ServerSocket server;
    private final BlockingQueue<Socket> accepted = new LinkedBlockingQueue<>();
    private MessageBus bus;
    private NetworkClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new ServerSocket(0);
        Thread t = new Thread(() -> {
            try {
                while (true) accepted.add(server.accept());
            } catch (IOException ignored) { }
        });
        t.setDaemon(true);
        t.start();
        bus = new MessageBus();
        client = new NetworkClient(Runnable::run, bus);   // "luồng giao diện" = luồng gọi
    }

    @AfterEach
    void tearDown() throws IOException {
        client.disconnect();
        server.close();
        for (Socket s : accepted) s.close();
    }

    private static PrintWriter writer(Socket s) throws IOException {
        return new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    private static BufferedReader reader(Socket s) throws IOException {
        return new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
    }

    @Test
    void reconnectIsNotClosedByOldListener() throws Exception {
        List<String> lost = new CopyOnWriteArrayList<>();
        client.setOnConnectionLost(lost::add);

        assertTrue(client.connect("localhost", server.getLocalPort()));
        Socket first = accepted.poll(2, TimeUnit.SECONDS);
        assertTrue(client.connect("localhost", server.getLocalPort()));
        Socket second = accepted.poll(2, TimeUnit.SECONDS);
        assertNotNull(first);
        assertNotNull(second);

        // Chờ luồng nghe cũ chắc chắn đã thoát (server thấy EOF ở socket cũ).
        assertEquals(-1, first.getInputStream().read());
        Thread.sleep(200);

        assertTrue(client.isConnected(), "kết nối mới vẫn mở");
        assertTrue(client.connectedProperty().get());
        assertTrue(lost.isEmpty(), "kết nối lại chủ động không phải mất kết nối");

        client.send(new Message("PING"));
        assertEquals("PING", reader(second).readLine());

        BlockingQueue<Message> got = new LinkedBlockingQueue<>();
        bus.subscribe(Command.PONG, got::add);
        writer(second).println("PONG");
        assertNotNull(got.poll(2, TimeUnit.SECONDS));
    }

    @Test
    void serverCloseReportsConnectionLost() throws Exception {
        BlockingQueue<String> lost = new LinkedBlockingQueue<>();
        client.setOnConnectionLost(lost::add);

        assertTrue(client.connect("localhost", server.getLocalPort()));
        accepted.poll(2, TimeUnit.SECONDS).close();

        assertNotNull(lost.poll(2, TimeUnit.SECONDS));
        assertFalse(client.isConnected());
        assertFalse(client.connectedProperty().get());
    }

    @Test
    void userDisconnectDoesNotReportLost() throws Exception {
        List<String> lost = new CopyOnWriteArrayList<>();
        client.setOnConnectionLost(lost::add);
        assertTrue(client.connect("localhost", server.getLocalPort()));
        client.disconnect();
        Thread.sleep(200);
        assertTrue(lost.isEmpty());
        assertFalse(client.isConnected());
    }

    @Test
    void malformedLineDoesNotStopListener() throws Exception {
        assertTrue(client.connect("localhost", server.getLocalPort()));
        Socket s = accepted.poll(2, TimeUnit.SECONDS);
        BlockingQueue<Message> got = new LinkedBlockingQueue<>();
        bus.subscribe(Command.PONG, got::add);
        PrintWriter w = writer(s);
        w.println("|thiếu lệnh");
        w.println("PONG");
        assertNotNull(got.poll(2, TimeUnit.SECONDS));
        assertTrue(client.isConnected());
    }

    @Test
    void reconnectUsesLastHostAndPort() throws Exception {
        assertFalse(client.reconnect(), "chưa từng kết nối");
        assertTrue(client.connect("localhost", server.getLocalPort()));
        client.disconnect();
        assertTrue(client.reconnect());
        assertEquals(server.getLocalPort(), client.lastPort());
    }
}
