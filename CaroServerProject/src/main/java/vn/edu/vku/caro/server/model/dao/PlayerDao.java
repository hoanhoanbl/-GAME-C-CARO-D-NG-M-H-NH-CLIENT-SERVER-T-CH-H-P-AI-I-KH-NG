package vn.edu.vku.caro.server.model.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Optional;

/** Truy cập bảng players (đăng ký, đăng nhập). */
public class PlayerDao {

    /** Bản ghi tài khoản đọc từ DB. */
    public record PlayerRecord(int id, String username, byte[] hash, byte[] salt, String email) { }

    private final Database db;

    public PlayerDao(Database db) { this.db = db; }

    /** @return true nếu tạo được; false nếu username đã tồn tại. */
    public boolean create(String username, byte[] hash, byte[] salt, String email) throws SQLException {
        String sql = "INSERT INTO players (username, password_hash, password_salt, email) VALUES (?, ?, ?, ?)";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setBytes(2, hash);
            ps.setBytes(3, salt);
            ps.setString(4, (email != null && !email.isBlank()) ? email : null);
            ps.executeUpdate();
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
            return false;   // trùng username (UNIQUE)
        }
    }

    public boolean create(String username, byte[] hash, byte[] salt) throws SQLException {
        return create(username, hash, salt, null);
    }

    public Optional<PlayerRecord> findByUsername(String username) throws SQLException {
        String sql = "SELECT id, username, password_hash, password_salt, email FROM players WHERE username = ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new PlayerRecord(rs.getInt("id"), rs.getString("username"),
                        rs.getBytes("password_hash"), rs.getBytes("password_salt"), rs.getString("email")));
            }
        }
    }

    public void touchLastLogin(int playerId) throws SQLException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE players SET last_login = NOW() WHERE id = ?")) {
            ps.setInt(1, playerId);
            ps.executeUpdate();
        }
    }
}
