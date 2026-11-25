# Caro Server

Server console, kiến trúc phân lớp đơn giản (không dùng MVC):

```
net/       ClientHandler (đọc/ghi socket), CommandDispatcher (nhận lệnh, gọi service)
model/     auth, player, room, game, bot, dao  (nghiệp vụ + truy cập MySQL)
messages/  hàm tạo chuỗi thông điệp gửi cho client
```

Package `vn.edu.vku.caro.common` nằm trong module `caro-common` (dùng chung với client và server).

## Chạy
Trong IntelliJ: chạy `vn.edu.vku.caro.server.CaroServer` (hoặc `mvn exec:java`).
Cổng mặc định 5000, truyền cổng khác qua tham số dòng lệnh.

## Cơ sở dữ liệu (XAMPP)
1. XAMPP Control Panel -> Start **MySQL** (và Apache nếu dùng phpMyAdmin).
2. Mở http://localhost/phpmyadmin -> tab **SQL** -> dán nội dung `db/schema.sql` (ở thư mục gốc repo) -> **Go**.
3. Mặc định server kết nối `localhost:3306`, database `caro`, user `root`, mật khẩu rỗng.
   Khác thì đặt biến môi trường `CARO_DB_URL`, `CARO_DB_USER`, `CARO_DB_PASSWORD`.

Tài khoản demo: `an` / `binh`, mật khẩu `123456`.

## Thử nhanh
- `AuthDemo` (chạy trực tiếp): thử đăng ký/đăng nhập với MySQL, không cần client.
- `telnet localhost 5000` rồi gõ `LOGIN|an|123456`, `PING`, `QUIT`.
