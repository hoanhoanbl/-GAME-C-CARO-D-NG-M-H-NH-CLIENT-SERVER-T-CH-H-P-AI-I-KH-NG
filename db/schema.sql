-- =====================================================
--  CSDL GAME CỜ CARO (MySQL / MariaDB trên XAMPP hoặc Docker)
-- =====================================================
CREATE DATABASE IF NOT EXISTS gamecocaro
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE gamecocaro;

-- Bảng tài khoản người chơi
CREATE TABLE IF NOT EXISTS players (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(16)   NOT NULL UNIQUE,    -- tên đăng nhập
    password_hash VARBINARY(32) NOT NULL,           -- mật khẩu băm PBKDF2WithHmacSHA256 (32 byte)
    password_salt VARBINARY(16) NOT NULL,           -- salt ngẫu nhiên 16 byte riêng biệt
    email         VARCHAR(100)  NULL,               -- email người chơi (tuỳ chọn)
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login    DATETIME NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Kết quả trận đấu
CREATE TABLE IF NOT EXISTS matches (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    player_x_id INT NOT NULL,
    player_o_id INT NULL,                       -- NULL nếu đối thủ là bot
    vs_bot      BOOLEAN NOT NULL DEFAULT FALSE,
    bot_level   TINYINT NULL,
    winner      ENUM('X','O','DRAW') NOT NULL,
    reason      ENUM('FIVE','SURRENDER','LEFT','FULL') NOT NULL,
    move_count  INT NOT NULL,
    started_at  DATETIME NOT NULL,
    ended_at    DATETIME NOT NULL,
    CONSTRAINT fk_match_x FOREIGN KEY (player_x_id) REFERENCES players(id),
    CONSTRAINT fk_match_o FOREIGN KEY (player_o_id) REFERENCES players(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tài khoản mẫu để kiểm thử (Mật khẩu cả 2 tài khoản đều là: 123456)
INSERT IGNORE INTO players (id, username, password_hash, password_salt, email) VALUES
    (1, 'an', UNHEX('6ca9b1bab487bb700aa564caed82085857534019ae0128f42a974a1345768c93'), UNHEX('a1b2c3d4e5f60718293a4b5c6d7e8f90'), 'an@example.com'),
    (2, 'binh', UNHEX('53faffdc24d4bb03db0aed8c5ae4ffb07cd129bd81422c5291b3cdc6dafc4991'), UNHEX('0f1e2d3c4b5a69788796a5b4c3d2e1f0'), 'binh@example.com');
