# 🎮 DỰ ÁN GAME CỜ CARO ONLINE (CLIENT - SERVER)

> Hệ thống game Cờ Caro trực tuyến hỗ trợ kết nối mạng Socket TCP, xác thực người dùng bảo mật chuẩn PBKDF2 và quản lý cơ sở dữ liệu MySQL.

---

## 📌 1. Những gì đã hoàn thành & Cách hệ thống hoạt động

### 🔹 Mô hình kiến trúc
Dự án được xây dựng theo mô hình **Client - Server** tách biệt:
- **`caro-server`**: Máy chủ backend viết bằng Java Core, kết nối cơ sở dữ liệu MySQL qua JDBC, lắng nghe trên cổng **`5000`**.
- **`caro-client`**: Giao diện người dùng JavaFX (FXML + CSS), tương tác người dùng và gửi nhận thông điệp qua Socket TCP.

```
[CaroClient (JavaFX)]  <--- Socket TCP (Port 5000) --->  [CaroServer]  <--- JDBC --->  [MySQL (gamecocaro)]
```

### 🔹 Chức năng đã hoàn thành
1. **Quản lý tài khoản trong Cơ sở dữ liệu (MySQL `gamecocaro`)**:
   - Bảng `players` lưu trữ: `id`, `username`, `password_hash`, `password_salt`, `email`, `created_at`, `last_login`.
   - **Bảo mật mật khẩu**: Sử dụng thuật toán **PBKDF2WithHmacSHA256** (65 536 vòng, hash 256-bit). Mỗi tài khoản có **Salt ngẫu nhiên 16 byte riêng biệt**. Tuyệt đối không lưu mật khẩu thô trong CSDL.
2. **Chức năng Đăng ký (Register)**:
   - Kiểm tra định dạng phía Client (Username 3-16 ký tự chữ/số/gạch dưới, Mật khẩu từ 6 ký tự, kiểm tra xác nhận mật khẩu, định dạng email).
   - Kiểm tra trùng tên tài khoản trên Server.
   - Băm mật khẩu và lưu người dùng mới vào MySQL.
3. **Chức năng Đăng nhập (Login)**:
   - Xác thực tên đăng nhập và mật khẩu qua thuật toán băm PBKDF2.
   - Tự động cập nhật thời gian đăng nhập gần nhất (`last_login = NOW()`).
   - Kiểm tra và chống đăng nhập trùng lặp (nếu tài khoản đang online ở nơi khác sẽ bị từ chối).
   - Đăng nhập thành công sẽ lưu thông tin vào `ClientSession` và chuyển tiếp vào màn hình **Sảnh (Lobby)**.
4. **Giao diện Client hoàn chỉnh**:
   - Màn hình Kết nối Server (`connect.fxml`): Nhập IP/Port (mặc định `localhost:5000`).
   - Màn hình Đăng nhập / Đăng ký (`login.fxml`): Tab chuyển đổi mượt mà, thông báo lỗi tiếng Việt trực quan.
   - Màn hình Sảnh (`lobby.fxml`): Chào mừng người chơi kèm nút Đăng xuất.

---

## 🛠 2. Yêu cầu môi trường chạy
- **Java Development Kit (JDK)**: Java 21 hoặc mới hơn (ví dụ: OpenJDK 21, 23, 24, Temurin-21).
- **Maven**: 3.8+ (hoặc dùng Maven tích hợp sẵn trong IntelliJ IDEA).
- **Cơ sở dữ liệu**: MySQL / MariaDB (chạy qua **XAMPP** hoặc **Docker**).
- **IDE khuyên dùng**: IntelliJ IDEA.

---

## 🚀 3. Hướng dẫn chi tiết cho thành viên nhóm lấy code về chạy

### Bước 1: Khởi động Cơ sở dữ liệu (Chọn 1 trong 2 cách)

#### 👉 Cách A: Dùng XAMPP (Phổ biến)
1. Mở **XAMPP Control Panel** -> Bấm **Start** ở module **MySQL** (và **Apache**).
2. Mở trình duyệt vào: `http://localhost/phpmyadmin`
3. Chọn tab **SQL**, copy toàn bộ nội dung file **`db/schema.sql`** dán vào rồi bấm **Go** (hoặc tạo database tên `gamecocaro` rồi import file `schema.sql`).

#### 👉 Cách B: Dùng Docker
Nếu máy có cài Docker, mở Terminal chạy 2 lệnh sau:
```bash
# 1. Chạy container MySQL
docker run -d --name caro-mysql -p 3306:3306 -e MYSQL_DATABASE=gamecocaro -e MYSQL_ALLOW_EMPTY_PASSWORD=yes mysql:8.4

# 2. Import dữ liệu bảng vào container
docker exec -i caro-mysql mysql -u root gamecocaro < db/schema.sql
```

---

### Bước 2: Chạy Server (`caro-server`)
1. Mở IntelliJ IDEA -> chọn **Open** -> chọn **thư mục gốc của repo** (file `pom.xml` cha, gồm 3 module `caro-common`, `caro-server`, `caro-client`). Lần đầu chạy `mvn install -DskipTests` ở thư mục gốc để module chung được cài.
2. Tìm đến file: `src/main/java/vn/edu/vku/caro/server/CaroServer.java`.
3. Nhấp chuột phải vào file -> Chọn **Run 'CaroServer.main()'** (hoặc phím tắt `Ctrl + Shift + F10`).
4. Quan sát Console bên dưới in ra:
   ```text
   [DB] Kết nối MySQL (XAMPP) thành công.
   [SERVER] Đang lắng nghe cổng 5000...
   ```
   *(Để Server tiếp tục chạy nền)*.

---

### Bước 3: Chạy Client (`caro-client`)
1. Dùng cùng project đã mở ở Bước 2 (thư mục gốc repo), hoặc mở thêm một cửa sổ mới.
2. Chạy Client bằng 1 trong các cách sau:
   - **Cách 1**: Mở tab **Maven** ở mép phải -> `caro-client` -> `Plugins` -> `javafx` -> nhấp đúp vào **`javafx:run`**.
   - **Cách 2**: Chạy file `src/main/java/vn/edu/vku/caro/client/app/CaroClientApp.java` (chọn **Run**).
   - **Cách 3**: Mở Terminal gõ: `mvn -pl caro-client javafx:run` (chạy từ thư mục gốc).
3. Cửa sổ giao diện game JavaFX sẽ hiển thị trên màn hình.

---

### Bước 4: Kiểm thử ứng dụng
1. Bấm nút **Kết nối Server** (mặc định `localhost:5000`).
2. **Đăng nhập thử tài khoản có sẵn trong DB**:
   - Tên đăng nhập: `an` (hoặc `binh`)
   - Mật khẩu: `123456`
   - Bấm **Đăng nhập** -> Ứng dụng sẽ vào màn hình **Sảnh**.
3. **Đăng ký tài khoản mới**:
   - Chuyển sang tab **Đăng ký**, điền thông tin và bấm **Tạo tài khoản**.
   - Sau khi báo thành công, nhập mật khẩu và bấm **Đăng nhập**.
4. Vào phpMyAdmin kiểm tra bảng `players` để thấy dữ liệu tài khoản và thời gian đăng nhập được cập nhật tự động.
