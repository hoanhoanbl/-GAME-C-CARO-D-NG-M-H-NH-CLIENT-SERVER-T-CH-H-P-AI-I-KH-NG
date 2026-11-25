package vn.edu.vku.caro.server.model.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Kết nối MySQL/MariaDB của XAMPP.
 * Mặc định: localhost:3306, database "caro", user "root", mật khẩu rỗng.
 * Có thể đổi bằng biến môi trường CARO_DB_URL, CARO_DB_USER, CARO_DB_PASSWORD.
 */
public class Database {
    private static final String URL = env("CARO_DB_URL",
            "jdbc:mysql://localhost:3306/gamecocaro?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh");
    private static final String USER = env("CARO_DB_USER", "root");
    private static final String PASSWORD = env("CARO_DB_PASSWORD", "");

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return v != null ? v : fallback;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /** Dùng lúc khởi động server để báo DB có sẵn sàng không. */
    public boolean isAvailable() {
        try (Connection c = getConnection()) {
            return c.isValid(2);
        } catch (SQLException e) {
            System.out.println("[DB] Không kết nối được: " + e.getMessage());
            return false;
        }
    }
}
