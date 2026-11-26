package caro.server.model.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;

/** Băm mật khẩu bằng PBKDF2WithHmacSHA256, salt 16 byte, 65 536 vòng, khóa 256 bit (32 byte). */
public class PasswordHasher {
    private static final int ITERATIONS = 65_536;
    private static final int KEY_BITS = 256;
    private static final int SALT_BYTES = 16;

    private final SecureRandom random = new SecureRandom();

    public byte[] newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        return salt;
    }

    public byte[] hash(String password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Không băm được mật khẩu", e);
        } finally {
            spec.clearPassword();
        }
    }

    /** So sánh hằng thời gian để tránh lộ thông tin qua thời gian xử lý. */
    public boolean verify(String password, byte[] salt, byte[] expectedHash) {
        return MessageDigest.isEqual(hash(password, salt), expectedHash);
    }
}
