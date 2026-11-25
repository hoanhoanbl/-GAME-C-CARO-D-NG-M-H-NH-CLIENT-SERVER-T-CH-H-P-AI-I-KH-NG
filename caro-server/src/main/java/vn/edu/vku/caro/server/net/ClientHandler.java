package vn.edu.vku.caro.server.net;

import vn.edu.vku.caro.common.protocol.Message;
import vn.edu.vku.caro.server.messages.ErrorMessages;
import vn.edu.vku.caro.server.model.player.RemotePlayer;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Một luồng cho mỗi client: đọc từng dòng, giao cho CommandDispatcher. */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final CommandDispatcher dispatcher;
    private final RemotePlayer player = new RemotePlayer(this);
    private volatile PrintWriter out;

    public ClientHandler(Socket socket, CommandDispatcher dispatcher) {
        this.socket = socket;
        this.dispatcher = dispatcher;
    }

    public synchronized void send(Message msg) {
        PrintWriter w = out;
        if (w != null) w.println(msg.format());
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter w = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
            this.out = w;
            String line;
            boolean keepOpen = true;
            while (keepOpen && (line = in.readLine()) != null) {
                line = line.replace("\uFEFF", "").trim();
                if (line.isEmpty()) continue;
                System.out.println("[SERVER] <- " + mask(line));
                try {
                    keepOpen = dispatcher.dispatch(player, Message.parse(line));
                } catch (Exception e) {
                    send(ErrorMessages.error("BAD_MESSAGE", "Thông điệp không hợp lệ"));
                }
            }
        } catch (IOException e) {
            System.out.println("[SERVER] Mất kết nối: " + e.getMessage());
        } finally {
            dispatcher.onDisconnect(player);
            try { socket.close(); } catch (IOException ignored) { }
            System.out.println("[SERVER] Đã đóng kết nối " + socket.getRemoteSocketAddress());
        }
    }

    /** Không bao giờ ghi mật khẩu vào log. */
    private static String mask(String line) {
        if (line.startsWith("REGISTER|") || line.startsWith("LOGIN|")) {
            String[] p = line.split("\\|", -1);
            return p[0] + "|" + (p.length > 1 ? p[1] : "") + "|***";
        }
        return line;
    }
}
