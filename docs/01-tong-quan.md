# Tổng quan, phạm vi, yêu cầu (mục 1–6)

> Bộ tài liệu dự án Caro · [Mục lục](README.md)

## 1. Tổng quan

Game cờ caro bàn 15x15 chơi trong mạng LAN theo mô hình Client-Server.

- **Người với người:** hai client kết nối tới một server; server quản lý phòng, kiểm tra nước đi, quyết định thắng/thua/hòa.
- **Người với máy:** client chơi với bot AI **chạy trên server** (Minimax + cắt tỉa Alpha-Beta).
- Client viết bằng JavaFX, server là chương trình Java chạy console.

```
 Máy A (Client)              Máy chủ (Server)                Máy B (Client)
┌───────────────┐   TCP   ┌───────────────────────┐   TCP   ┌───────────────┐
│ JavaFX UI     │◀──────▶│ CaroServer             │◀──────▶│ JavaFX UI     │
│ NetworkClient │         │ ├ ClientHandler (n)    │         │ NetworkClient │
└───────────────┘         │ ├ RoomManager          │         └───────────────┘
                          │ ├ GameSession (trọng tài)│
                          │ ├ BotPlayer + AIEngine │
                          │ └ MatchRecorder ──────┼──▶ MySQL (Docker)
                          └───────────────────────┘
```

## 2. Phạm vi

| Làm | Không làm |
|---|---|
| Đăng ký, đăng nhập bằng tài khoản lưu trong MySQL | Quên mật khẩu, đổi mật khẩu, khóa tài khoản |
| Chơi người–người qua LAN theo phòng | Chế độ khán giả / xem trận |
| Chơi với bot trên server, 3 mức độ khó; khách không cần đăng nhập | Mã hóa đường truyền (TLS) |
| Chat trong phòng, đầu hàng, chơi lại | Chơi qua Internet (ngoài LAN) |
| Xử lý mất kết nối | Bảng xếp hạng toàn server (chỉ làm thống kê cá nhân nếu dư thời gian) |
| Lưu người chơi và kết quả trận vào MySQL | Undo nước đi, hẹn giờ mỗi nước |
| Tự tìm server bằng UDP broadcast (mở rộng) | |

## 3. Các quyết định đã chốt

| # | Quyết định | Giá trị |
|---|---|---|
| D1 | Ngôn ngữ, build | Java 21, Maven multi-module, JavaFX 21 |
| D2 | Giao thức chơi | **TCP**, thông điệp văn bản một dòng, UTF-8 |
| D3 | Cổng mặc định | TCP `5000`; UDP tìm server `5001` |
| D4 | Bot AI | **Chạy trên server**, là một `Player` như người thật |
| D5 | Luật thắng | **5 quân liên tiếp trở lên** (ngang, dọc, 2 chéo); không áp dụng luật chặn hai đầu |
| D6 | Hòa | Bàn đầy (225 ô) mà không ai thắng |
| D7 | Đi trước | Quân **X** luôn đi trước. Người tạo phòng cầm X ở ván đầu; chơi lại thì đổi bên. Chơi với bot: người cầm X |
| D8 | Thoát giữa ván | Người còn lại **thắng**, lý do `LEFT` |
| D9 | Tọa độ | `row`, `col` từ 0 đến 14, gốc ở góc trên bên trái |
| D10 | Tài khoản | **Chơi với người phải đăng nhập** (vào sảnh, tạo/vào phòng, chat). **Chơi với máy không cần đăng nhập**: khách bấm "Chơi với máy" ngay ở màn hình đăng nhập, server đặt tên `Khách-<số>`; ván của khách không lưu vào CSDL. Người đã đăng nhập vẫn chơi với máy được từ sảnh, và ván đó được lưu. `username` 3–16 ký tự chữ/số/`_`, không trùng; mật khẩu 6–32 ký tự. Một tài khoản chỉ đăng nhập ở một nơi cùng lúc |
| D11 | Package gốc, mô hình | `caro`; mỗi module chia package theo **MVC** (`model`, `view`, `controller`) + hạ tầng (`net`, `app`), xem mục 7–8 |
| D12 | Lưu mật khẩu | Băm bằng **PBKDF2WithHmacSHA256** (có sẵn trong Java, không cần thư viện) với salt ngẫu nhiên 16 byte, 65 536 vòng. Không bao giờ lưu hay ghi log mật khẩu thô |
| D13 | MySQL | **Bắt buộc** vì đăng nhập cần CSDL. DB không kết nối được → server từ chối đăng nhập với `ERROR\|DB_UNAVAILABLE`, nhưng khách vẫn chơi với máy được |

## 4. Actor

| Actor | Loại | Mô tả | Tương tác chính |
|---|---|---|---|
| **Khách** | Chính (người) | Người dùng đã mở client nhưng chưa đăng nhập | Kết nối server, đăng ký, đăng nhập, **chơi với máy** (không lưu kết quả) |
| **Người chơi** | Chính (người) | Khách đã đăng nhập thành công | Tạo/vào phòng, đánh cờ, chat, đầu hàng, chơi lại, chơi với máy, đăng xuất |
| **Người vận hành server** | Chính (người) | Người khởi động chương trình server trên một máy trong LAN (thường là một thành viên nhóm khi demo) | Khởi động/dừng server, cấu hình cổng, xem log |
| **Bot AI** | Phụ (hệ thống) | Đối thủ ảo chạy trong server | Nhận trạng thái bàn cờ, trả về nước đi |
| **MySQL** | Phụ (hệ thống ngoài) | Nơi lưu tài khoản và kết quả trận | Nhận và trả dữ liệu cho server |

"Người chơi" kế thừa "Khách": khách đăng nhập thành công thì trở thành người chơi.

```
                 ┌─────────────────── Hệ thống Caro ───────────────────┐
  Khách ─────────│  (UC-01) Kết nối server                             │
                 │  (UC-02a) Đăng ký   (UC-02b) Đăng nhập ─────────────┼── MySQL
                 │                                                     │
                 │  (UC-08) Đánh cờ          (UC-09) Chat              │
  Người chơi ────│  (UC-18) Đăng xuất                                  │
                 │  (UC-03) Xem DS phòng     (UC-10) Đầu hàng          │
                 │  (UC-04) Tạo phòng        (UC-11) Xem kết quả       │
                 │  (UC-05) Vào phòng        (UC-12) Chơi lại          │
                 │  (UC-06) Rời phòng        (UC-13) Về sảnh / thoát   │
                 │  (UC-07) Chơi với máy ──────────────────────────────┼── Bot AI
                 │      ▲ cả Khách và Người chơi đều dùng được         │
                 │                                                     │
  Người vận ─────│  (UC-14) Khởi động/dừng server, cấu hình cổng       │
  hành server    │  (UC-15) Xem log hoạt động                          │
                 │  (UC-16) Lưu kết quả trận ──────────────────────────┼── MySQL
                 │  (UC-17) Tự tìm server trong LAN (mở rộng)          │
                 └─────────────────────────────────────────────────────┘
```

## 5. Use case và danh sách chức năng (FE)

### 5.1 Đặc tả use case

| UC | Tên | Actor | Tiền điều kiện | Luồng chính | Ngoại lệ | FE |
|---|---|---|---|---|---|---|
| UC-01 | Kết nối server | Khách | Server đang chạy | Nhập IP, cổng → bấm Kết nối → mở socket → hiện form đăng nhập | Sai IP/cổng, server tắt → báo lỗi, ở lại màn hình | FE-05 |
| UC-02a | Đăng ký | Khách, MySQL | Đã kết nối | Chọn Đăng ký → nhập username, mật khẩu, nhập lại mật khẩu → gửi `REGISTER` → server băm mật khẩu, lưu DB → `REGISTERED` → quay về form đăng nhập, điền sẵn username | Username đã có → `USERNAME_TAKEN`; sai định dạng → `USERNAME_INVALID`/`PASSWORD_INVALID`; hai mật khẩu không khớp → client báo, không gửi; DB lỗi → `DB_UNAVAILABLE` | FE-27 |
| UC-02b | Đăng nhập | Khách, MySQL | Đã kết nối, đã có tài khoản | Nhập username, mật khẩu → gửi `LOGIN` → server so mật khẩu băm → `WELCOME` + `ROOM_LIST` → vào sảnh | Sai username hoặc mật khẩu → `INVALID_CREDENTIALS` (không nói rõ sai cái nào); đang đăng nhập nơi khác → `ALREADY_LOGGED_IN`; DB lỗi → `DB_UNAVAILABLE` | FE-27 |
| UC-18 | Đăng xuất | Người chơi | Ở sảnh | Bấm Đăng xuất → gửi `LOGOUT` → server xóa khỏi danh sách online → client về form đăng nhập (giữ kết nối) | Đang trong ván → nút bị ẩn | FE-27 |
| UC-03 | Xem danh sách phòng | Người chơi | Ở sảnh | Sảnh hiển thị phòng (mã, chủ phòng, trạng thái), tự cập nhật | Không có phòng → hiển thị "Chưa có phòng" | FE-07 |
| UC-04 | Tạo phòng | Người chơi | Ở sảnh | Bấm Tạo phòng → server tạo phòng, người tạo cầm X → màn hình chờ | Đang ở phòng khác → lỗi | FE-07 |
| UC-05 | Vào phòng | Người chơi | Ở sảnh, phòng đang chờ | Chọn phòng → Vào → server bắt đầu ván, cả hai sang màn hình bàn cờ | Phòng đầy/không tồn tại → lỗi | FE-08 |
| UC-06 | Rời phòng | Người chơi | Đang chờ trong phòng | Bấm Rời → phòng bị xóa → về sảnh | — | FE-08 |
| UC-07 | Chơi với máy | Khách hoặc Người chơi, Bot AI | Khách: đã kết nối, đang ở màn hình đăng nhập. Người chơi: ở sảnh | Chọn độ khó → bấm Chơi với máy → server tạo ván với bot (người cầm X) → chơi → hết ván: Chơi lại, hoặc về màn hình trước đó (khách về màn hình đăng nhập, người chơi về sảnh) | Khách không có chat; ván của khách không lưu CSDL | FE-18, FE-19 |
| UC-08 | Đánh cờ | Người chơi, Bot AI | Đang trong ván, đến lượt | Click ô trống → gửi `MOVE` → server kiểm tra → gửi `MOVED` cho cả hai → đổi lượt | Không đến lượt, ô đã có quân → server trả `ERROR`, client không vẽ | FE-09, FE-10 |
| UC-09 | Chat | Người chơi | Đang trong phòng | Nhập tin → gửi → cả hai thấy tin | Tin rỗng → không gửi | FE-15 |
| UC-10 | Đầu hàng | Người chơi | Đang trong ván | Bấm Đầu hàng → xác nhận → ván kết thúc, đối thủ thắng | — | FE-12 |
| UC-11 | Xem kết quả | Người chơi | Ván kết thúc | Tô đường thắng, hiện hộp thoại Thắng/Thua/Hòa + lý do | — | FE-11 |
| UC-12 | Chơi lại | Người chơi | Ván vừa kết thúc, cùng phòng | Bấm Chơi lại → chờ đối thủ đồng ý → ván mới, đổi bên | Đối thủ đã rời → báo, về sảnh | FE-14 |
| UC-13 | Về sảnh / thoát | Người chơi | — | Về sảnh hoặc đóng ứng dụng → server dọn dữ liệu | Thoát giữa ván → đối thủ thắng (D8) | FE-13, FE-06 |
| UC-14 | Khởi động server | Người vận hành | — | Chạy server với cổng mặc định hoặc tham số → log "Đang lắng nghe…" | Cổng bận → báo lỗi và thoát | FE-23 |
| UC-15 | Xem log | Người vận hành | Server đang chạy | Console in: kết nối, đăng ký, đăng nhập (không in mật khẩu), tạo phòng, bắt đầu/kết thúc ván, lỗi | — | FE-23 |
| UC-16 | Lưu kết quả | MySQL | Ván kết thúc | Server ghi người chơi và trận vào CSDL | DB không kết nối được → ghi log, game vẫn chạy | FE-20 |
| UC-17 | Tự tìm server | Người chơi | Cùng LAN | Bấm Tìm server → client broadcast → server trả lời → tự điền IP/cổng | Không tìm thấy sau 3 giây → báo, nhập tay | FE-22 |

### 5.2 Danh sách chức năng

Ưu tiên: **M** bắt buộc · **S** nên có · **X** mở rộng (chỉ làm khi xong hết M và S).
Người phụ trách: **H** = Hoàng, **P** = Phi.

| FE | Tên | Server | Client | Ai | Ưu tiên | Phụ thuộc | Giai đoạn |
|---|---|---|---|---|---|---|---|
| FE-01 | Khởi tạo dự án và khung mã | Maven 3 module, khung các class ở mục 8 | Khung các màn hình | H + P | M | — | GĐ0 |
| FE-02 | Luật cờ | — (`common`) `Board`, `WinChecker`, JUnit | — | H | M | FE-01 | GĐ1 |
| FE-03 | Giao thức | — (`common`) `Command`, `Message` parse/format, JUnit | — | P | M | FE-01 | GĐ1 |
| FE-04 | Khung ứng dụng client | — | `CaroClientApp`, `SceneManager`, chuyển màn hình | H | M | FE-01 | GĐ1 |
| FE-05 | Kết nối | `CaroServer` accept, `ClientHandler`, `CommandDispatcher`, `PlayerRegistry` (danh sách online), chặn mọi lệnh khi chưa đăng nhập (`NOT_LOGGED_IN`) | `connect.fxml` + `ConnectController` (IP, cổng), `NetworkClient`, `MessageBus`, `ClientSession` | P | M | FE-03 | GĐ1 |
| FE-27 | Đăng ký, đăng nhập, đăng xuất | `AuthService`, `PasswordHasher` (PBKDF2), `REGISTER`, `LOGIN`, `LOGOUT`, chặn đăng nhập trùng | `login.fxml` + `LoginController` (form đăng nhập + chuyển sang form đăng ký), nút Đăng xuất ở sảnh | P | M | FE-05, FE-20 | GĐ1 |
| FE-06 | Lỗi và mất kết nối tầng mạng | Phát hiện socket đóng, `PING/PONG`, dọn người chơi, gọi hook ván cờ | Báo mất kết nối, quay về màn hình kết nối | P | M | FE-05 | GĐ2 |
| FE-07 | Sảnh: danh sách và tạo phòng | `RoomManager`, `LIST_ROOMS`, `CREATE_ROOM`, đẩy `ROOM_LIST` | `lobby.fxml` + `LobbyController` + `LobbyModel` gồm danh sách phòng, nút Tạo phòng, ô chọn độ khó + nút Chơi với máy | P | M | FE-05 | GĐ1 |
| FE-08 | Vào/rời phòng, bắt đầu ván | `JOIN_ROOM`, `LEAVE_ROOM`, tạo `GameSession` qua `GameSessionFactory` | Màn hình chờ đối thủ, nút Rời | P | M | FE-07, FE-09 | GĐ2 |
| FE-09 | Trọng tài ván cờ | `GameSession`: kiểm tra lượt, ô, biên; cập nhật `Board`; gửi `MOVED`; phát hiện kết thúc | — | H | M | FE-02 | GĐ1 |
| FE-10 | Bàn cờ trên client | `GameCommandController` xử lý `MOVE` | `game.fxml` + `BoardCanvas` + `GameController` + `GameModel`: vẽ 15x15, click gửi `MOVE`, nhận `GAME_START`/`MOVED`, hiển thị lượt, tô nước cuối | H | M | FE-04, FE-09 | GĐ1–2 |
| FE-11 | Kết thúc ván | Gửi `GAME_OVER` kèm đường thắng | Tô đường thắng, `ResultDialog` | H | M | FE-10 | GĐ2 |
| FE-12 | Đầu hàng | `SURRENDER` | Nút Đầu hàng có xác nhận | H | S | FE-11 | GĐ3 |
| FE-13 | Đối thủ thoát giữa ván | `GameSession.handleDisconnect` → `GAME_OVER` lý do `LEFT` | Hiện "Đối thủ đã thoát" | H | M | FE-06, FE-11 | GĐ3 |
| FE-14 | Chơi lại | `REMATCH`, `REMATCH_REQUESTED`, reset bàn, đổi bên | Nút Chơi lại / Về sảnh trong `ResultDialog` | H | S | FE-11 | GĐ3 |
| FE-15 | Chat trong phòng | `CHAT` → `CHAT_MSG` cho người trong phòng (trong `GameCommandController`) | `chat.fxml` + `ChatController` + `ChatModel` (nhúng vào `game.fxml`) | H | S | FE-08 | GĐ2 |
| FE-16 | AI: hàm đánh giá + Minimax | `Evaluator`, `MoveGenerator`, `AIEngine` minimax, JUnit | — | H | M | FE-02 | GĐ2 |
| FE-17 | AI: Alpha-Beta + tối ưu | Cắt tỉa, sắp xếp nước, thắng ngay/chặn ngay | — | H | M | FE-16 | GĐ3 |
| FE-18 | Chơi với máy | `BotPlayer`, thread pool riêng, `PLAY_BOT` cho cả khách và người đã đăng nhập; không lưu ván của khách | Nút ở sảnh (FE-07, P) và ở màn hình đăng nhập cho khách (3.P.0, P); `GameController` ẩn chat khi đối thủ là bot (H) | H | M | FE-09, FE-17 | GĐ3 |
| FE-19 | Độ khó bot | Mức 1/2/3 ánh xạ độ sâu | Ô chọn ở sảnh và màn hình đăng nhập (P) | H | S | FE-18 | GĐ3 |
| FE-20 | MySQL | GĐ1: `docker-compose.yml`, `schema.sql`, `Database`, `PlayerDao` (phục vụ đăng nhập). GĐ3: `MatchDao`, `MySqlMatchRecorder` (lưu kết quả trận) | — | P | **M** | FE-01 | GĐ1 + GĐ3 |
| FE-21 | Thống kê cá nhân | Lấy thắng/thua/hòa từ DB, gửi kèm `WELCOME` | Hiển thị ở sảnh | P | X | FE-20 | GĐ3 |
| FE-22 | Tự tìm server (UDP) | `DiscoveryResponder` | Nút Tìm server trong `connect.fxml` (`ConnectController`) | P | X | FE-05 | GĐ3 |
| FE-23 | Vận hành server | Đọc cổng từ tham số, log ra console | — | P | M | FE-05 | GĐ1 |
| FE-24 | Kiểm thử hệ thống | Kịch bản ở mục 15, chạy trên 2–3 máy | | P chủ trì, H cùng | M | tất cả | GĐ4 |
| FE-25 | Đo hiệu năng | Thời gian bot theo độ sâu, độ trễ nước đi | | H | S | FE-18 | GĐ4 |
| FE-26 | Báo cáo, slide, demo | | | H + P | M | — | GĐ4 |

**Khối lượng:** Hoàng 14 FE (ván cờ, chat và bot, nặng về thuật toán), Phi 10 FE (mạng, tài khoản, phòng, CSDL, nhiều màn hình hơn) và chủ trì kiểm thử FE-24. Cả hai cùng làm FE-01 và FE-26. Chat được chuyển sang Hoàng để bù phần đăng nhập + MySQL mà Phi phải làm sớm.

## 6. Yêu cầu phi chức năng

| Mã | Yêu cầu | Cách kiểm tra |
|---|---|---|
| NF-01 | Server chịu ít nhất 10 client và 5 ván đồng thời | Mở 10 client trên 2–3 máy |
| NF-02 | Độ trễ từ lúc gửi `MOVE` đến lúc nhận `MOVED` < 100 ms trong LAN | FE-25 đo và ghi bảng |
| NF-03 | Bot mức 3 trả nước đi trong ≤ 2 giây | FE-25 đo |
| NF-04 | Giao diện không đơ khi chờ mạng hoặc bot | Mạng chạy luồng riêng, cập nhật UI bằng `Platform.runLater` |
| NF-05 | Thông điệp sai không làm sập server | Gửi rác qua telnet, server trả `ERROR` và vẫn chạy |
| NF-06 | Một client mất kết nối không ảnh hưởng ván khác | Rút mạng một máy khi đang có 2 ván |
| NF-07 | Server không sập khi MySQL mất kết nối: đăng nhập mới bị từ chối (`DB_UNAVAILABLE`), các ván đang chơi vẫn tiếp tục | Tắt container MySQL giữa chừng |
| NF-08 | Mật khẩu chỉ lưu dạng băm có salt, không xuất hiện trong log | Xem bảng `players` và log server |

