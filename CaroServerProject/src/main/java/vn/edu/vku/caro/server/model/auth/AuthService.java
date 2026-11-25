package vn.edu.vku.caro.server.model.auth;

import vn.edu.vku.caro.server.model.dao.PlayerDao;
import vn.edu.vku.caro.server.model.dao.PlayerDao.PlayerRecord;

import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/** Nghiệp vụ đăng ký / đăng nhập / đăng xuất. */
public class AuthService {

    /** Lỗi nghiệp vụ; code chính là mã gửi cho client trong ERROR|code|... */
    public static class AuthException extends Exception {
        private final String code;
        public AuthException(String code) { super(code); this.code = code; }
        public String code() { return code; }
    }

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{3,16}$");
    private static final byte[] DUMMY_SALT = new byte[16];

    private final PlayerDao dao;
    private final PasswordHasher hasher = new PasswordHasher();
    private final Set<String> online = ConcurrentHashMap.newKeySet();   // tên (chữ thường) đang đăng nhập

    public AuthService(PlayerDao dao) { this.dao = dao; }

    public void register(String username, String password, String email) throws AuthException {
        if (username == null || !USERNAME.matcher(username).matches())
            throw new AuthException("USERNAME_INVALID");
        if (password == null || password.length() < 6 || password.length() > 32 || password.contains("|"))
            throw new AuthException("PASSWORD_INVALID");

        byte[] salt = hasher.newSalt();
        byte[] hash = hasher.hash(password, salt);
        boolean created;
        try {
            created = dao.create(username, hash, salt, email);
        } catch (SQLException e) {
            System.out.println("[AUTH] Lỗi DB khi đăng ký: " + e.getMessage());
            throw new AuthException("DB_UNAVAILABLE");
        }
        if (!created) throw new AuthException("USERNAME_TAKEN");
    }

    public void register(String username, String password) throws AuthException {
        register(username, password, null);
    }

    /** Xác thực và trả về PlayerRecord */
    public PlayerRecord authenticate(String username, String password) throws AuthException {
        if (username == null || password == null) throw new AuthException("INVALID_CREDENTIALS");

        Optional<PlayerRecord> found;
        try {
            found = dao.findByUsername(username);
        } catch (SQLException e) {
            System.out.println("[AUTH] Lỗi DB khi đăng nhập: " + e.getMessage());
            throw new AuthException("DB_UNAVAILABLE");
        }
        if (found.isEmpty()) {
            hasher.hash(password, DUMMY_SALT);              // tốn thời gian giống trường hợp có tài khoản
            throw new AuthException("INVALID_CREDENTIALS"); // không nói rõ sai username hay mật khẩu
        }
        PlayerRecord p = found.get();
        if (!hasher.verify(password, p.salt(), p.hash()))
            throw new AuthException("INVALID_CREDENTIALS");
        if (!online.add(p.username().toLowerCase()))
            throw new AuthException("ALREADY_LOGGED_IN");

        try {
            dao.touchLastLogin(p.id());
        } catch (SQLException e) {
            System.out.println("[AUTH] Không cập nhật được last_login: " + e.getMessage());
        }
        return p;
    }

    /** @return tên đúng như lưu trong DB, dùng làm tên hiển thị. */
    public String login(String username, String password) throws AuthException {
        return authenticate(username, password).username();
    }

    /** Gọi khi đăng xuất hoặc khi socket đóng. */
    public void logout(String username) {
        if (username != null) online.remove(username.toLowerCase());
    }
}
