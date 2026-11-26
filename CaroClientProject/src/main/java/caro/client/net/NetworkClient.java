package caro.client.net;

import javafx.application.Platform;
import caro.common.protocol.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class NetworkClient {
    private static final NetworkClient INSTANCE = new NetworkClient();
    public static NetworkClient get() { return INSTANCE; }

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread listenerThread;
    private volatile boolean connected = false;

    private NetworkClient() { }

    public synchronized boolean connect(String host, int port) {
        disconnect();
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 3000);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            connected = true;

            listenerThread = new Thread(this::listen, "CaroClient-Listener");
            listenerThread.setDaemon(true);
            listenerThread.start();
            return true;
        } catch (IOException e) {
            System.err.println("[NetworkClient] Kết nối thất bại: " + e.getMessage());
            disconnect();
            return false;
        }
    }

    private void listen() {
        try {
            String line;
            while (connected && (line = in.readLine()) != null) {
                if (line.isBlank()) continue;
                Message msg = Message.parse(line);
                Platform.runLater(() -> MessageBus.get().publish(msg));
            }
        } catch (IOException e) {
            if (connected) {
                System.out.println("[NetworkClient] Mất kết nối tới server: " + e.getMessage());
            }
        } finally {
            disconnect();
        }
    }

    public synchronized void send(Message msg) {
        if (connected && out != null) {
            out.println(msg.format());
        } else {
            System.err.println("[NetworkClient] Không thể gửi vì chưa kết nối!");
        }
    }

    public synchronized void disconnect() {
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
        socket = null;
        out = null;
        in = null;
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
}
