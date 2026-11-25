package vn.edu.vku.caro.server.model.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.edu.vku.caro.server.model.auth.AuthService.AuthException;
import vn.edu.vku.caro.server.model.dao.PlayerDao;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest {

    private static class FakePlayerDao extends PlayerDao {
        private final Map<String, PlayerRecord> players = new HashMap<>();
        private int idCounter = 1;

        public FakePlayerDao() { super(null); }

        @Override
        public boolean create(String username, byte[] hash, byte[] salt, String email) {
            if (players.containsKey(username.toLowerCase())) return false;
            players.put(username.toLowerCase(), new PlayerRecord(idCounter++, username, hash, salt, email));
            return true;
        }

        @Override
        public Optional<PlayerRecord> findByUsername(String username) {
            return Optional.ofNullable(players.get(username.toLowerCase()));
        }

        @Override
        public void touchLastLogin(int playerId) { }
    }

    private AuthService auth;

    @BeforeEach
    void setUp() {
        auth = new AuthService(new FakePlayerDao());
    }

    @Test
    void testRegisterSuccessAndLogin() throws AuthException {
        auth.register("player1", "password123", "p1@gmail.com");
        String name = auth.login("player1", "password123");
        assertEquals("player1", name);
    }

    @Test
    void testRegisterDuplicateUsername() throws AuthException {
        auth.register("user_one", "password123");
        AuthException ex = assertThrows(AuthException.class, () -> auth.register("user_one", "different_pass"));
        assertEquals("USERNAME_TAKEN", ex.code());
    }

    @Test
    void testRegisterInvalidUsername() {
        assertThrows(AuthException.class, () -> auth.register("ab", "password123")); // quá ngắn
        assertThrows(AuthException.class, () -> auth.register("user@name", "password123")); // chứa ký tự đặc biệt
    }

    @Test
    void testRegisterInvalidPassword() {
        assertThrows(AuthException.class, () -> auth.register("valid_user", "123")); // quá ngắn (<6)
        assertThrows(AuthException.class, () -> auth.register("valid_user", "pass|word")); // chứa '|'
    }

    @Test
    void testLoginWrongPassword() throws AuthException {
        auth.register("my_user", "correctpass");
        AuthException ex = assertThrows(AuthException.class, () -> auth.login("my_user", "wrongpass"));
        assertEquals("INVALID_CREDENTIALS", ex.code());
    }

    @Test
    void testLoginAlreadyLoggedInAndLogout() throws AuthException {
        auth.register("online_user", "password123");
        auth.login("online_user", "password123");

        AuthException ex = assertThrows(AuthException.class, () -> auth.login("online_user", "password123"));
        assertEquals("ALREADY_LOGGED_IN", ex.code());

        auth.logout("online_user");
        assertDoesNotThrow(() -> auth.login("online_user", "password123"));
    }
}
