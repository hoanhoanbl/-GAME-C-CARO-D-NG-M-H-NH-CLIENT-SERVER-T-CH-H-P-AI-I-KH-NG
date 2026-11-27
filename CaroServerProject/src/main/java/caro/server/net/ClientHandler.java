package caro.server.net;

import caro.common.protocol.Message;
import caro.common.protocol.Protocol;
import caro.server.messages.ErrorMessages;
import caro.server.model.player.RemotePlayer;

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
            LineReader reader = new LineReader(in);
            LineReader.Line read;
            boolean keepOpen = true;
            while (keepOpen && (read = reader.readLine()) != null) {
                if (read.tooLong()) {
                    System.out.println("[SERVER] <- (dòng quá " + Protocol.MAX_LINE_BYTES + " byte, bỏ qua)");
                    send(ErrorMessages.error("BAD_MESSAGE", "Dòng quá dài"));
                    continue;
                }
                String line = read.text();
                line = line.replace("\uFEFF", "").trim();
                if (line.isEmpty()) continue;
                System.out.println("[SERVER] <- " + mask(line));
                Message msg;
                try {
                    msg = Message.parse(line);
                } catch (IllegalArgumentException e) {
                    send(ErrorMessages.error("BAD_MESSAGE", "Thông điệp không hợp lệ"));
                    continue;
                }
                try {
                    keepOpen = dispatcher.dispatch(player, msg);
                } catch (RuntimeException e) {
                    // Lỗi lập trình: log đủ để sửa, giữ kết nối.
                    System.out.println("[SERVER] Lỗi khi xử lý " + msg.command() + ":");
                    e.printStackTrace(System.out);
                    send(ErrorMessages.error("SERVER_ERROR", "Lỗi máy chủ khi xử lý " + msg.command()));
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
