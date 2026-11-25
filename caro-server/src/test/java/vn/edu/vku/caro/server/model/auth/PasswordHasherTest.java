package vn.edu.vku.caro.server.model.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void testSaltLengthAndRandomness() {
        byte[] salt1 = hasher.newSalt();
        byte[] salt2 = hasher.newSalt();
        assertEquals(16, salt1.length);
        assertEquals(16, salt2.length);
        assertFalse(MessageDigestEquals(salt1, salt2), "Hai salt tạo ra phải khác nhau");
    }

    @Test
    void testHashLength() {
        byte[] salt = hasher.newSalt();
        byte[] hash = hasher.hash("123456", salt);
        assertEquals(32, hash.length, "Hash PBKDF2 256-bit phải có độ dài 32 byte");
    }

    @Test
    void testVerifyCorrectPassword() {
        byte[] salt = hasher.newSalt();
        byte[] hash = hasher.hash("mypassword", salt);
        assertTrue(hasher.verify("mypassword", salt, hash), "Mật khẩu đúng phải xác thực thành công");
    }

    @Test
    void testVerifyWrongPassword() {
        byte[] salt = hasher.newSalt();
        byte[] hash = hasher.hash("mypassword", salt);
        assertFalse(hasher.verify("wrongpassword", salt, hash), "Mật khẩu sai phải bị từ chối");
    }

    private boolean MessageDigestEquals(byte[] a, byte[] b) {
        return java.security.MessageDigest.isEqual(a, b);
    }
}
