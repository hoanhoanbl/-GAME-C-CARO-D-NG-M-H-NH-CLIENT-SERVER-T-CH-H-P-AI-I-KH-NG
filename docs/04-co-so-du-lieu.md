# Cơ sở dữ liệu (mục 12)

> Bộ tài liệu dự án Caro · [Mục lục](README.md)

## 12. Cơ sở dữ liệu (FE-20, FE-27)

Bot không có tài khoản: ván với bot lưu `player_o_id = NULL`, `vs_bot = TRUE`.
Ván của **khách** với bot **không lưu**: `GameSession` chỉ gọi `MatchRecorder.record` khi không có người chơi nào `isGuest()`.

**Bảng `players`** (tài khoản)

| Cột | Kiểu | Ghi chú |
|---|---|---|
| `id` | INT, PK, tự tăng | |
| `username` | VARCHAR(16), UNIQUE, NOT NULL | Dùng làm tên hiển thị trong game |
| `password_hash` | VARBINARY(32), NOT NULL | Kết quả PBKDF2 |
| `password_salt` | VARBINARY(16), NOT NULL | Salt ngẫu nhiên riêng từng tài khoản |
| `created_at` | DATETIME | Ngày đăng ký |
| `last_login` | DATETIME, NULL | |

**Bảng `matches`**

| Cột | Kiểu | Ghi chú |
|---|---|---|
| `id` | INT, PK, tự tăng | |
| `player_x_id` | INT, FK → players | |
| `player_o_id` | INT, FK → players, NULL | NULL nếu đối thủ là bot |
| `vs_bot` | BOOLEAN | |
| `bot_level` | TINYINT, NULL | |
| `winner` | ENUM('X','O','DRAW') | |
| `reason` | ENUM('FIVE','SURRENDER','LEFT','FULL') | |
| `move_count` | INT | |
| `started_at`, `ended_at` | DATETIME | |

Thống kê (FE-21) tính bằng truy vấn trên `matches`, không lưu cột riêng.

