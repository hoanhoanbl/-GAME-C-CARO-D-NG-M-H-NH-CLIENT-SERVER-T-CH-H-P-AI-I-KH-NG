package caro.server;

import caro.server.model.auth.AuthService;
import caro.server.model.dao.Database;
import caro.server.model.dao.PlayerDao;
import caro.server.net.ClientHandler;
import caro.server.net.CommandDispatcher;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class CaroServer {
    public static void main(String[] args) throws IOException {
        ServerConfig config = ServerConfig.fromArgs(args);

        Database db = new Database();
        System.out.println(db.isAvailable()
                ? "[DB] Kết nối MySQL (XAMPP) thành công."
                : "[DB] Không có MySQL: đăng nhập/đăng ký sẽ bị từ chối.");

        AuthService auth = new AuthService(new PlayerDao(db));
        CommandDispatcher dispatcher = new CommandDispatcher(auth);

        try (ServerSocket server = new ServerSocket(config.port)) {
            System.out.println("[SERVER] Đang lắng nghe cổng " + config.port + "...");
            while (true) {
                Socket socket = server.accept();
                System.out.println("[SERVER] Kết nối mới: " + socket.getRemoteSocketAddress());
                new Thread(new ClientHandler(socket, dispatcher), "client-" + socket.getPort()).start();
            }
        }
    }
}
