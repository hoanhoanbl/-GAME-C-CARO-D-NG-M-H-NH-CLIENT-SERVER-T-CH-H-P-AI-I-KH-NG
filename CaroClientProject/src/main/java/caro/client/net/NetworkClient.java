package caro.client.net;

import caro.common.protocol.Message;
import caro.common.protocol.Protocol;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Kết nối TCP tới server. Mỗi lần {@link #connect} tạo một {@link Connection} riêng; luồng nghe chỉ đóng
 * đúng kết nối của nó, nên kết nối lại không bao giờ bị luồng cũ đóng nhầm.
 *
 * <p>Thông điệp nhận được và mọi thay đổi trạng thái được đẩy sang luồng giao diện ({@code uiExecutor},
 * mặc định {@link Platform#runLater}).
 */
public class NetworkClient {
    private static final NetworkClient INSTANCE = new NetworkClient(Platform::runLater, MessageBus.get());
    public static NetworkClient get() { return INSTANCE; }

    private static final int CONNECT_TIMEOUT_MS = 3000;

    /** Một kết nối cụ thể; bất biến, luồng nghe giữ tham chiếu của riêng nó. */
    private record Connection(Socket socket, BufferedReader in, PrintWriter out) {
        void close() {
            try { socket.close(); } catch (IOException ignored) { }
        }
    }

    private final Executor ui;
    private final MessageBus bus;
    private final ReadOnlyBooleanWrapper connected = new ReadOnlyBooleanWrapper(false);

    private volatile Connection current;
    private volatile String lastHost;
    private volatile int lastPort = Protocol.TCP_PORT;
    private volatile Consumer<String> onConnectionLost;

    /** Cho test: chạy "luồng giao diện" bằng executor tùy ý và MessageBus riêng. */
    NetworkClient(Executor uiExecutor, MessageBus bus) {
        this.ui = Objects.requireNonNull(uiExecutor);
        this.bus = Objects.requireNonNull(bus);
    }

    /**
     * Đóng kết nối cũ (nếu có, không coi là mất kết nối) rồi mở kết nối mới. Có thể chặn tới 3 giây:
     * đừng gọi trên FX thread.
     *
     * @return true nếu kết nối được
     */
    public synchronized boolean connect(String host, int port) {
        closeCurrent();
        lastHost = host;
        lastPort = port;
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            Connection conn = new Connection(socket,
                    new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)),
                    new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true));
            current = conn;
            setConnected(true);

            Thread t = new Thread(() -> listen(conn), "CaroClient-Listener");
            t.setDaemon(true);
            t.start();
            return true;
        } catch (IOException e) {
            System.err.println("[NetworkClient] Kết nối thất bại: " + e.getMessage());
            try { socket.close(); } catch (IOException ignored) { }
            return false;
        }
    }

    /** Kết nối lại tới host/cổng của lần {@link #connect} gần nhất. @return false nếu chưa từng kết nối hoặc thất bại. */
    public boolean reconnect() {
        String host = lastHost;
        return host != null && connect(host, lastPort);
    }

    private void listen(Connection conn) {
        String reason = "Server đã đóng kết nối";
        try {
            String line;
            while ((line = conn.in().readLine()) != null) {
                if (line.isBlank()) continue;
                Message msg;
                try {
                    msg = Message.parse(line);
                } catch (IllegalArgumentException e) {
                    System.err.println("[NetworkClient] Bỏ qua dòng sai định dạng: " + e.getMessage());
                    continue;
                }
                ui.execute(() -> bus.publish(msg));
            }
        } catch (IOException e) {
            reason = "Mất kết nối tới server: " + e.getMessage();
        }
        closeIf(conn, reason);
    }

    /** Luồng nghe kết thúc: chỉ dọn nếu {@code conn} vẫn là kết nối hiện tại (tức là mất ngoài ý muốn). */
    private synchronized void closeIf(Connection conn, String reason) {
        conn.close();
        if (current != conn) return;      // đã bị disconnect()/connect() thay thế: không báo mất kết nối
        current = null;
        setConnected(false);
        System.out.println("[NetworkClient] " + reason);
        Consumer<String> handler = onConnectionLost;
        if (handler != null) ui.execute(() -> handler.accept(reason));
    }

    public void send(Message msg) {
        Connection c = current;
        if (c != null) {
            c.out().println(msg.format());   // PrintWriter tự đồng bộ từng lần println
        } else {
            System.err.println("[NetworkClient] Không thể gửi vì chưa kết nối!");
        }
    }

    /** Ngắt chủ động: không gọi {@link #setOnConnectionLost handler mất kết nối}. */
    public synchronized void disconnect() {
        closeCurrent();
    }

    private void closeCurrent() {
        Connection c = current;
        current = null;
        if (c != null) {
            c.close();
            setConnected(false);
        }
    }

    public boolean isConnected() {
        Connection c = current;
        return c != null && !c.socket().isClosed();
    }

    /** Trạng thái kết nối, cập nhật trên luồng giao diện. */
    public ReadOnlyBooleanProperty connectedProperty() { return connected.getReadOnlyProperty(); }

    /** Gọi trên luồng giao diện khi kết nối mất ngoài ý muốn (server đóng, lỗi mạng), kèm lý do. */
    public void setOnConnectionLost(Consumer<String> handler) { this.onConnectionLost = handler; }

    public String lastHost() { return lastHost; }
    public int lastPort() { return lastPort; }

    private void setConnected(boolean value) {
        ui.execute(() -> connected.set(value));
    }
}
