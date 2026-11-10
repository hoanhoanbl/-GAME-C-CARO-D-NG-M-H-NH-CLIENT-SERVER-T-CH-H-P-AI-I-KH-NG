# KẾ HOẠCH DỰ ÁN: GAME CỜ CARO CLIENT-SERVER TÍCH HỢP AI ĐỐI KHÁNG

> Nhóm: **Nguyễn Phan Như Hoàng** (24IT079) · **Nguyễn Phúc Phi** (24IT200) · GVHD: ThS. Nguyễn Ngọc Huyền Trân
> Thời gian: 10/11/2026 → nộp 01/12/2026
> Tài liệu này là **nguồn sự thật duy nhất** của nhóm. Muốn đổi quyết định ở mục 3, 9, 10 thì sửa tài liệu này trước, báo người kia, rồi mới sửa code.

---

## Mục lục

1. Tổng quan
2. Phạm vi
3. Các quyết định đã chốt
4. Actor
5. Use case và danh sách chức năng (FE)
6. Yêu cầu phi chức năng
7. Kiến trúc hệ thống (Client-Server + MVC)
8. Cấu trúc mã nguồn và người sở hữu từng file
9. Hợp đồng giữa hai người (interface dùng chung)
10. Giao thức truyền thông
11. Các luồng xử lý chính
12. Cơ sở dữ liệu
13. Phân công theo giai đoạn
14. Quy trình Git chống conflict
15. Định nghĩa hoàn thành và kiểm thử
16. Rủi ro
17. Việc còn mở

---

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
| D11 | Package gốc, mô hình | `vn.edu.vku.caro`; mỗi module chia package theo **MVC** (`model`, `view`, `controller`) + hạ tầng (`net`, `app`), xem mục 7–8 |
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

## 7. Kiến trúc hệ thống (Client-Server + MVC)

### 7.1 Ba module Maven

```
caro-parent (pom gốc)
├── caro-common   ← dùng chung: luật cờ, giao thức. KHÔNG phụ thuộc JavaFX hay socket.
├── caro-server   ← phụ thuộc caro-common, mysql-connector-j
└── caro-client   ← phụ thuộc caro-common, JavaFX
```

### 7.2 Mô hình kiến trúc: Client-Server + MVC

Cả hai phía đều tổ chức theo **MVC**. Hai module có giao diện khác nhau nên "View" mang nghĩa khác nhau:

| Thành phần | Client (JavaFX) | Server (console) |
|---|---|---|
| **Model** | Trạng thái phía client: `ClientSession`, `GameInfo`, `LobbyModel`, `GameModel`, `ChatModel` (dùng JavaFX `Property`/`ObservableList` để View tự cập nhật) | Toàn bộ nghiệp vụ và dữ liệu: người chơi, phòng, ván cờ (`GameSession`), bot, tài khoản, truy cập MySQL |
| **View** | File `.fxml` + `.css`, và các thành phần vẽ tay (`BoardCanvas`, `ResultDialog`) | Các lớp **tạo thông điệp trả về client** (`AuthMessages`, `LobbyMessages`, `GameMessages`, `ErrorMessages`). Đây là "giao diện" của server, tức chuỗi văn bản client nhận được |
| **Controller** | `ConnectController`, `LoginController`, `LobbyController`, `WaitingController`, `GameController`, `ChatController`: nhận sự kiện người dùng và thông điệp từ server, cập nhật Model | `AuthCommandController`, `LobbyCommandController`, `GameCommandController`: nhận lệnh từ client, gọi Model, chọn View để trả lời |
| Ngoài MVC (hạ tầng) | `net/` (`NetworkClient`, `MessageBus`), `app/` (`CaroClientApp`, `SceneManager`) | `net/` (`ClientHandler`, `CommandDispatcher`, `DiscoveryResponder`), `CaroServer`, `ServerConfig` |

`caro-common` chỉ chứa **Model dùng chung** (`Board`, luật thắng) và định dạng giao thức, nên client và server hiểu luật cờ giống hệt nhau.

Tên controller phía server có hậu tố `CommandController` để không nhầm với controller màn hình phía client.

### 7.3 Luồng MVC phía server

```
 Socket ──▶ net/ClientHandler (1 luồng / client, P) ── đọc dòng → Message.parse
                │
                ▼
          net/CommandDispatcher (P) ── kiểm tra quyền theo trạng thái ── tra bảng Command
                │
   ┌────────────┼───────────────────────────┬──────────────────────────────┐
   ▼            ▼                           ▼                              ▼
 CONTROLLER  AuthCommandController (P)   LobbyCommandController (P)   GameCommandController (H)
             REGISTER, LOGIN, LOGOUT     LIST_ROOMS, CREATE_ROOM,     MOVE, SURRENDER, REMATCH,
                                         JOIN_ROOM, LEAVE_ROOM,       BACK_TO_LOBBY, PLAY_BOT, CHAT
                                         PING, QUIT
   │
   ▼
 MODEL       AuthService ─▶ PlayerDao    RoomManager ──tạo──▶ GameSession ◀── BotPlayer ── AIEngine
             PasswordHasher     │        PlayerRegistry           │
                                ▼                                 ▼ GameEndListener
                             MySQL  ◀──── MatchRecorder ◀─────────┘
   │
   ▼
 VIEW        AuthMessages (P)   LobbyMessages (P)   GameMessages (H)   ErrorMessages (P)
             → Message ──▶ Player.send(...) ──▶ socket
```

Quyền theo trạng thái (dispatcher kiểm tra trước khi gọi controller):

| Trạng thái client | Lệnh được phép |
|---|---|
| Chưa đăng nhập, chưa chơi | `REGISTER`, `LOGIN`, `PLAY_BOT`, `PING`, `QUIT` |
| Khách đang chơi với máy | `MOVE`, `SURRENDER`, `REMATCH`, `BACK_TO_LOBBY`, `PING`, `QUIT` |
| Đã đăng nhập | Mọi lệnh trừ `REGISTER`, `LOGIN` |
| Lệnh ngoài danh sách | Trả `ERROR\|NOT_LOGGED_IN` |

**Thông báo chủ động từ Model.** Một số sự kiện không bắt nguồn từ lệnh của client (bot đánh xong, đối thủ mất kết nối, danh sách phòng đổi). Khi đó Model tạo thông điệp bằng lớp View rồi gửi qua `Player.send(...)`. `Player` đóng vai trò observer của Model (mẫu Observer, thường đi kèm MVC).

### 7.4 Luồng MVC phía client

```
           (1) người dùng click ô
  VIEW  game.fxml / BoardCanvas ─────────────▶ CONTROLLER  GameController
     ▲                                             │ (2) ClientSession.send(MOVE|r|c)
     │ (5) binding tự vẽ lại                       ▼
     │                                         net/NetworkClient ──▶ Server
  MODEL  GameModel (Board, lượt, nước cuối)         │
     ▲                                             │ (3) MOVED|X|r|c|O
     └──── (4) controller cập nhật model ◀── net/MessageBus (chuyển sang FX thread)
```

Màn hình chuyển theo `app/SceneManager` (H): `CONNECT → LOGIN → LOBBY → WAITING → GAME`.

### 7.5 Quy tắc MVC bắt buộc (để hai người code giống nhau)

| # | Quy tắc |
|---|---|
| R1 | **Model không import `javafx.scene.*`** và không biết socket. Client Model chỉ dùng `javafx.beans.property` / `javafx.collections`. Server Model không import `net/`, trừ `RemotePlayer` |
| R2 | **View không gửi mạng, không chứa luật cờ.** FXML chỉ khai báo giao diện; `BoardCanvas` chỉ vẽ theo `GameModel` |
| R3 | **Controller không vẽ trực tiếp.** Controller cập nhật Model, View tự cập nhật qua binding/listener |
| R4 | Client: chỉ Controller được gọi `ClientSession.send(...)` và `MessageBus.subscribe(...)` |
| R5 | Server: chỉ lớp trong `view/` được ghép chuỗi thông điệp. Controller và Model **không** tự viết `"MOVED\|" + ...` |
| R6 | Mỗi màn hình đúng một bộ: `xxx.fxml` + `xxx.css` + `XxxController.java`. Không controller nào điều khiển hai màn hình |
| R7 | Luật cờ (đúng lượt, ô trống, thắng/hòa) chỉ nằm ở `common/model` và `server/model/game`. Client không tự kết luận thắng thua, chỉ hiển thị theo `GAME_OVER` |

## 8. Cấu trúc mã nguồn và người sở hữu từng file

**Quy tắc:** chỉ chủ sở hữu được sửa file. Người kia cần thay đổi thì nhắn chủ file, hoặc sửa bằng commit riêng có tiền tố `CONTRACT:` sau khi chủ file đồng ý (mục 14).

### 8.1 Cây thư mục

```
(gốc repo)
├── pom.xml                                  P
├── docker-compose.yml                       P
├── db/schema.sql                            P
├── docs/                                    H + P
├── caro-common/src/main/java/vn/edu/vku/caro/common/
│   ├── model/        Symbol, Board, WinChecker, GameResult, EndReason            H
│   └── protocol/     Command, Message, Protocol                                  P
├── caro-server/src/main/java/vn/edu/vku/caro/server/
│   ├── CaroServer.java, ServerConfig.java                                        P
│   ├── net/          ClientHandler, CommandDispatcher, DiscoveryResponder        P
│   ├── controller/   CommandController (interface),
│   │                 AuthCommandController, LobbyCommandController               P
│   │                 GameCommandController                                       H
│   ├── model/
│   │   ├── player/   Player (hợp đồng H+P), RemotePlayer, PlayerRegistry         P
│   │   ├── auth/     AuthService, PasswordHasher                                 P
│   │   ├── room/     Room, RoomManager                                           P
│   │   ├── game/     GameSession, GameSessionFactory                             H
│   │   │             GameEndListener (hợp đồng H+P)
│   │   ├── bot/      BotPlayer, AIEngine, Evaluator, MoveGenerator, BotExecutor  H
│   │   └── dao/      Database, PlayerDao, MatchDao,
│   │                 NoOpMatchRecorder, MySqlMatchRecorder                       P
│   │                 MatchRecorder (hợp đồng H+P)
│   └── view/         AuthMessages, LobbyMessages, ErrorMessages                  P
│                     GameMessages                                                H
└── caro-client/
    ├── src/main/java/vn/edu/vku/caro/client/
    │   ├── app/          CaroClientApp, SceneManager, Screen                     H
    │   ├── net/          NetworkClient, MessageBus                               P
    │   ├── model/        ClientSession, GameInfo, LobbyModel, RoomInfo           P
    │   │                 GameModel, ChatModel, ChatLine                          H
    │   ├── view/         BoardCanvas, ResultDialog                               H
    │   └── controller/   ConnectController, LoginController,
    │                     LobbyController, WaitingController                      P
    │                     GameController, ChatController                          H
    └── src/main/resources/vn/edu/vku/caro/client/view/
        ├── connect.fxml, login.fxml, lobby.fxml, waiting.fxml                    P
        ├── game.fxml, chat.fxml                                                  H
        └── css/  base.css, game.css, chat.css (H) · connect.css, login.css, lobby.css (P)
```

Đường dẫn rút gọn trong các bảng dưới: `…/` = `src/main/java/vn/edu/vku/caro/`, `res/` = `src/main/resources/vn/edu/vku/caro/client/view/`.

### 8.2 caro-common (Model dùng chung + giao thức)

| File | MVC | Chủ | Ghi chú |
|---|---|---|---|
| `pom.xml` (gốc + 3 module) | — | P | Tạo ở GĐ0; thêm thư viện → báo người kia |
| `…/common/model/Symbol.java` | Model | H | enum `X, O, EMPTY` |
| `…/common/model/Board.java` | Model | H | Mảng 15x15, `place`, `get`, `isEmpty`, `isFull`, `copy` |
| `…/common/model/WinChecker.java` | Model | H | `findWinLine(board, row, col)` trả về danh sách ô thắng hoặc rỗng |
| `…/common/model/GameResult.java` | Model | H | `winner` (X/O/null nếu hòa), `reason`, `winLine` |
| `…/common/model/EndReason.java` | Model | H | enum `FIVE, SURRENDER, LEFT, FULL` |
| `…/common/protocol/Command.java` | — | P | enum mọi lệnh ở mục 10. **File dùng chung**, chỉ P sửa |
| `…/common/protocol/Message.java` | — | P | `parse(String)`, `format()`, `arg(i)` |
| `…/common/protocol/Protocol.java` | — | P | Hằng số: cổng, dấu phân cách, độ dài tối đa |
| `src/test/…/BoardTest.java`, `WinCheckerTest.java` | test | H | |
| `src/test/…/MessageTest.java` | test | P | |

### 8.3 caro-server

| File | MVC | Chủ | Ghi chú |
|---|---|---|---|
| `…/server/CaroServer.java` | hạ tầng | P | `main`, vòng accept, **đăng ký controller**: đúng 3 dòng `register(new AuthCommandController…)`, `register(new LobbyCommandController…)`, `register(new GameCommandController…)` |
| `…/server/ServerConfig.java` | hạ tầng | P | Cổng, thông tin DB |
| `…/server/net/ClientHandler.java` | hạ tầng | P | Luồng đọc/ghi một client |
| `…/server/net/CommandDispatcher.java` | hạ tầng | P | Bảng quyền theo trạng thái (mục 7.3) |
| `…/server/net/DiscoveryResponder.java` | hạ tầng | P | FE-22 |
| `…/server/controller/CommandController.java` | Controller | P | interface (mục 9) |
| `…/server/controller/AuthCommandController.java` | Controller | P | `REGISTER`, `LOGIN`, `LOGOUT` |
| `…/server/controller/LobbyCommandController.java` | Controller | P | `LIST_ROOMS`, `CREATE_ROOM`, `JOIN_ROOM`, `LEAVE_ROOM`, `PING`, `QUIT` |
| `…/server/controller/GameCommandController.java` | Controller | H | `MOVE`, `SURRENDER`, `REMATCH`, `BACK_TO_LOBBY`, `PLAY_BOT`, `CHAT` |
| `…/server/model/player/Player.java` | Model | **H + P (hợp đồng)** | interface (mục 9), chốt ở GĐ0 |
| `…/server/model/player/RemotePlayer.java` | Model | P | Bọc `ClientHandler` |
| `…/server/model/player/PlayerRegistry.java` | Model | P | Danh sách online, cấp tên `Khách-<số>` |
| `…/server/model/auth/AuthService.java` | Model | P | Kiểm tra định dạng, gọi `PlayerDao`, chặn đăng nhập trùng |
| `…/server/model/auth/PasswordHasher.java` | Model | P | PBKDF2 + salt, so sánh bằng `MessageDigest.isEqual` |
| `…/server/model/room/Room.java` | Model | P | |
| `…/server/model/room/RoomManager.java` | Model | P | Cài `GameEndListener` |
| `…/server/model/game/GameSession.java` | Model | H | Trọng tài |
| `…/server/model/game/GameSessionFactory.java` | Model | H | `create(roomId, x, o, listener, recorder)` |
| `…/server/model/game/GameEndListener.java` | Model | **H + P (hợp đồng)** | interface |
| `…/server/model/bot/BotPlayer.java` | Model | H | |
| `…/server/model/bot/AIEngine.java` | Model | H | |
| `…/server/model/bot/Evaluator.java` | Model | H | |
| `…/server/model/bot/MoveGenerator.java` | Model | H | |
| `…/server/model/bot/BotExecutor.java` | Model | H | Thread pool cho bot |
| `…/server/model/dao/Database.java`, `PlayerDao.java` | Model | P | Làm ở GĐ1 cho đăng nhập |
| `…/server/model/dao/MatchRecorder.java` | Model | **H + P (hợp đồng)** | interface |
| `…/server/model/dao/NoOpMatchRecorder.java` | Model | P | Dùng cho test và trong GĐ1–2 khi chưa có `MySqlMatchRecorder` |
| `…/server/model/dao/MySqlMatchRecorder.java`, `MatchDao.java` | Model | P | Làm ở GĐ3 |
| `…/server/view/AuthMessages.java` | View | P | `registered(...)`, `welcome(...)`, `loggedOut()` |
| `…/server/view/LobbyMessages.java` | View | P | `roomList(...)`, `roomCreated(...)`, `roomLeft()`, `pong()` |
| `…/server/view/ErrorMessages.java` | View | P | `error(code, text)` |
| `…/server/view/GameMessages.java` | View | H | `gameStart(...)`, `moved(...)`, `gameOver(...)`, `rematchRequested(...)`, `chatMsg(...)`, `backToLogin()`, `opponentLeftRoom()` |
| `src/test/…/GameSessionTest.java`, `AIEngineTest.java`, `EvaluatorTest.java`, `GameMessagesTest.java` | test | H | Dùng `FakePlayer` trong thư mục test |
| `src/test/…/RoomManagerTest.java`, `CommandDispatcherTest.java`, `PasswordHasherTest.java`, `AuthServiceTest.java` | test | P | `AuthServiceTest` dùng `PlayerDao` giả, không cần MySQL |
| `docker-compose.yml`, `db/schema.sql` (gốc repo) | — | P | |

### 8.4 caro-client

| File | MVC | Chủ | Ghi chú |
|---|---|---|---|
| `…/client/app/CaroClientApp.java` | hạ tầng | H | `main`, khởi tạo `SceneManager` |
| `…/client/app/SceneManager.java`, `Screen.java` | hạ tầng | H | enum `Screen { CONNECT, LOGIN, LOBBY, WAITING, GAME }` — **thêm màn hình mới phải báo H** |
| `…/client/net/NetworkClient.java` | hạ tầng | P | Luồng đọc riêng |
| `…/client/net/MessageBus.java` | hạ tầng | P | `subscribe(Command, Consumer<Message>)`, callback chạy trên FX thread |
| `…/client/model/ClientSession.java` | Model | P | Singleton: tên, có phải khách không, `NetworkClient`, `GameInfo` hiện tại |
| `…/client/model/GameInfo.java` | Model | P | `roomId`, `mySymbol`, `opponentName`, `firstSymbol`, `vsBot` |
| `…/client/model/LobbyModel.java`, `RoomInfo.java` | Model | P | `ObservableList<RoomInfo>` cho bảng phòng |
| `…/client/model/GameModel.java` | Model | H | `Board`, `currentTurn` (property), `lastMove`, `winLine`, `result` |
| `…/client/model/ChatModel.java`, `ChatLine.java` | Model | H | `ObservableList<ChatLine>` |
| `…/client/view/BoardCanvas.java` | View | H | Vẽ bàn cờ theo `GameModel`, phát sự kiện click ô |
| `…/client/view/ResultDialog.java` | View | H | Thắng/Thua/Hòa + lý do, nút Chơi lại / Về sảnh (khách: Về màn hình chính) |
| `res/connect.fxml`, `res/css/connect.css` | View | P | Ô IP, cổng, nút Kết nối, nút Tìm server (FE-22) |
| `res/login.fxml`, `res/css/login.css` | View | P | Form Đăng nhập / Đăng ký; khu "Chơi với máy (không cần đăng nhập)" có ô độ khó |
| `res/lobby.fxml`, `res/css/lobby.css` | View | P | Bảng phòng, nút Tạo phòng, Chơi với máy + ô độ khó, Đăng xuất, tên người đang đăng nhập |
| `res/waiting.fxml` | View | P | "Đang chờ đối thủ", nút Rời phòng |
| `res/game.fxml`, `res/css/game.css` | View | H | Chừa sẵn `<fx:include source="chat.fxml"/>` từ GĐ0 |
| `res/chat.fxml`, `res/css/chat.css` | View | H | |
| `res/css/base.css` | View | H | Màu, font chung; người kia chỉ đọc |
| `…/client/controller/ConnectController.java` | Controller | P | |
| `…/client/controller/LoginController.java` | Controller | P | Xử lý `REGISTERED`, `WELCOME`, và `GAME_START` của khách |
| `…/client/controller/LobbyController.java` | Controller | P | Xử lý `ROOM_LIST`, `ROOM_CREATED`, `GAME_START`, `LOGGED_OUT` |
| `…/client/controller/WaitingController.java` | Controller | P | Xử lý `GAME_START`, `ROOM_LEFT` |
| `…/client/controller/GameController.java` | Controller | H | Xử lý `MOVED`, `GAME_OVER`, `REMATCH_REQUESTED`, `OPPONENT_LEFT_ROOM`, `BACK_TO_LOGIN`; ẩn chat khi đấu bot |
| `…/client/controller/ChatController.java` | Controller | H | Xử lý `CHAT_MSG` |
| `src/test/…/GameModelTest.java` | test | H | |
| `src/test/…/LobbyModelTest.java` | test | P | |

### 8.5 Tài liệu

| File | Chủ |
|---|---|
| `docs/KE_HOACH_DU_AN.md` (tài liệu này) | H + P (sửa mục nào thì báo) |
| `docs/test-report.md` | P |
| `README.md` (cách build/chạy) | P |

## 9. Hợp đồng giữa hai người (interface dùng chung)

Chốt ở GĐ0. Sau GĐ0, **không đổi chữ ký** nếu chưa thống nhất. Đây là chữ ký mô tả thiết kế, chưa phải code hoàn chỉnh.

```java
// caro-server: model/player/Player.java
public interface Player {
    String getName();
    boolean isBot();
    boolean isGuest();                    // true: khách chưa đăng nhập (chỉ chơi với máy, không lưu CSDL)
    void send(Message msg);               // RemotePlayer: ghi ra socket. BotPlayer: tự phản ứng khi tới lượt.
}

// caro-server: controller/CommandController.java
public interface CommandController {
    Set<Command> commands();               // các lệnh controller này xử lý
    void handle(RemotePlayer from, Message msg);
}

// caro-server: model/game/GameEndListener.java
public interface GameEndListener {
    void onGameEnded(GameSession session, GameResult result);
}

// caro-server: model/dao/MatchRecorder.java
public interface MatchRecorder {
    void record(String playerX, String playerO, boolean vsBot, int botLevel,
                GameResult result, int moveCount, Instant startedAt, Instant endedAt);
}

// caro-server: model/game/GameSession.java (H cài đặt, P gọi)
public class GameSession {
    public GameSession(String roomId, Player x, Player o, GameEndListener listener, MatchRecorder recorder);
    public void start();                              // gửi GAME_START cho cả hai (qua GameMessages)
    public void handleMove(Player p, int row, int col);
    public void handleSurrender(Player p);
    public void handleDisconnect(Player p);           // P gọi khi socket đóng
    public void handleRematch(Player p);
    public boolean isFinished();
}

// caro-server: model/room/RoomManager.java (P cài đặt, H gọi)
public GameSession findSessionOf(Player p);           // GameCommandController dùng để tìm ván của người gửi (cả ván với bot)
public Room findRoomOf(Player p);
public Room createBotRoom(Player human, Player bot, int level);  // GameCommandController gọi khi nhận PLAY_BOT;
                                                      // RoomManager tạo phòng ẩn (không hiện ở sảnh) và GameSession,
                                                      // nhờ đó MOVE/SURRENDER/mất kết nối đi chung một đường với ván người–người

// caro-server: view/ErrorMessages.java (P cài đặt, H dùng)
public static Message error(String code, String text);
```

```java
// caro-client: net/MessageBus.java (P cài đặt, H dùng — chỉ trong Controller)
public void subscribe(Command cmd, Consumer<Message> handler);   // handler chạy trên FX thread
public void unsubscribe(Command cmd, Consumer<Message> handler);

// caro-client: model/ClientSession.java (P cài đặt, H dùng)
public static ClientSession get();
public String playerName();
public boolean isGuest();
public void send(Message msg);
public GameInfo currentGame();          // P điền khi nhận GAME_START

// caro-client: app/SceneManager.java (H cài đặt, P dùng)
public void show(Screen screen);
```

**Ai xử lý `GAME_START` trên client?** `LoginController` (khách), `LobbyController`, `WaitingController` (đều của P) nhận, điền `ClientSession.currentGame()`, rồi gọi `SceneManager.show(GAME)`. `GameController` (H) đọc `currentGame()` khi mở màn hình và khởi tạo `GameModel`.

## 10. Giao thức truyền thông

### 10.1 Định dạng

- Mỗi thông điệp là **một dòng** UTF-8 kết thúc bằng `\n`, dài tối đa 1024 byte.
- Dạng: `LỆNH|tham_số_1|tham_số_2|…`
- Trong nội dung chat, client thay `|` bằng `/` và bỏ ký tự xuống dòng trước khi gửi.
- Lệnh không xác định hoặc sai số tham số → server trả `ERROR|BAD_MESSAGE|…`, **không** ngắt kết nối.
- Khách (chưa đăng nhập) chỉ được gửi `REGISTER`, `LOGIN`, `PLAY_BOT`, `PING`, `QUIT`, và trong ván với máy thì thêm `MOVE`, `SURRENDER`, `REMATCH`, `BACK_TO_LOBBY`. Lệnh khác → `ERROR|NOT_LOGGED_IN|…` (bảng quyền ở mục 7.2).
- Mật khẩu không được chứa `|`. Mật khẩu đi qua TCP dạng văn bản thường (không mã hóa đường truyền); chấp nhận trong LAN và ghi rõ hạn chế này trong báo cáo.
- Server **không** ghi log dòng `REGISTER`/`LOGIN` nguyên văn (che mật khẩu thành `***`).

### 10.2 Client → Server

| Lệnh | Tham số | Trạng thái hợp lệ | Phản hồi khi thành công | Mã lỗi | Handler (chủ) |
|---|---|---|---|---|---|
| `REGISTER` | `username\|password` | Chưa đăng nhập | `REGISTERED\|username` (không tự đăng nhập) | `USERNAME_TAKEN`, `USERNAME_INVALID`, `PASSWORD_INVALID`, `DB_UNAVAILABLE` | AuthCommandController (P) |
| `LOGIN` | `username\|password` | Chưa đăng nhập | `WELCOME\|username` rồi `ROOM_LIST` | `INVALID_CREDENTIALS`, `ALREADY_LOGGED_IN`, `DB_UNAVAILABLE` | AuthCommandController (P) |
| `LOGOUT` | — | Ở sảnh | `LOGGED_OUT` | `IN_GAME` | AuthCommandController (P) |
| `LIST_ROOMS` | — | Ở sảnh | `ROOM_LIST` | — | LobbyCommandController (P) |
| `CREATE_ROOM` | — | Ở sảnh | `ROOM_CREATED\|roomId`; mọi người ở sảnh nhận `ROOM_LIST` mới | `ALREADY_IN_ROOM` | LobbyCommandController (P) |
| `JOIN_ROOM` | `roomId` | Ở sảnh | Cả hai nhận `GAME_START` | `ROOM_NOT_FOUND`, `ROOM_FULL` | LobbyCommandController (P) |
| `LEAVE_ROOM` | — | Đang chờ trong phòng | `ROOM_LEFT` | `NOT_IN_ROOM` | LobbyCommandController (P) |
| `PLAY_BOT` | `level` (1–3) | Ở sảnh, **hoặc khách chưa đăng nhập** | `GAME_START` (đối thủ tên `BOT`, người cầm X). Khách được server đặt tên `Khách-<số>` | `INVALID_LEVEL`, `ALREADY_IN_GAME` | GameCommandController (H) |
| `MOVE` | `row\|col` | Trong ván, đến lượt | Cả hai nhận `MOVED` | `NO_GAME`, `NOT_YOUR_TURN`, `OUT_OF_BOARD`, `CELL_OCCUPIED` | GameCommandController (H) |
| `SURRENDER` | — | Trong ván | Cả hai nhận `GAME_OVER` | `NO_GAME` | GameCommandController (H) |
| `REMATCH` | — | Ván vừa kết thúc | Đối thủ nhận `REMATCH_REQUESTED`; khi cả hai đồng ý: `GAME_START` | `OPPONENT_GONE` | GameCommandController (H) |
| `BACK_TO_LOBBY` | — | Ván đã kết thúc | Người đã đăng nhập: `ROOM_LIST`; đối thủ (nếu còn) nhận `OPPONENT_LEFT_ROOM`. Khách: `BACK_TO_LOGIN` | — | GameCommandController (H) |
| `CHAT` | `text` | Trong phòng | Cả hai nhận `CHAT_MSG` | `NOT_IN_ROOM` | GameCommandController (H) |
| `PING` | — | Bất kỳ | `PONG` | — | LobbyCommandController (P) |
| `QUIT` | — | Bất kỳ | Server đóng socket | — | LobbyCommandController (P) |

### 10.3 Server → Client

| Lệnh | Tham số | Ý nghĩa | Client xử lý (chủ) |
|---|---|---|---|
| `REGISTERED` | `username` | Đăng ký thành công | LoginController (P) |
| `WELCOME` | `username` | Đăng nhập thành công | LoginController (P) |
| `LOGGED_OUT` | — | Đã đăng xuất | LobbyController (P) |
| `BACK_TO_LOGIN` | — | Khách kết thúc ván với máy, quay về màn hình đăng nhập | GameController (H) gọi `SceneManager.show(LOGIN)` |
| `ROOM_LIST` | `id:chủ_phòng:trạng_thái;…` (rỗng nếu không có) | `trạng_thái` = `WAITING` / `PLAYING` | LobbyController (P) |
| `ROOM_CREATED` | `roomId` | Đã tạo phòng, chờ đối thủ | LobbyController (P) |
| `ROOM_LEFT` | — | Đã rời phòng | WaitingController (P) |
| `GAME_START` | `roomId\|mySymbol\|opponentName\|firstSymbol` | Bắt đầu ván | Lobby/WaitingController (P) → GameController (H) |
| `MOVED` | `symbol\|row\|col\|nextSymbol` | Một nước đi hợp lệ | GameController (H) |
| `GAME_OVER` | `winner\|reason\|cells` | `winner` = `X`/`O`/`DRAW`; `reason` = `FIVE`/`SURRENDER`/`LEFT`/`FULL`; `cells` = `r,c;r,c;…` (rỗng nếu không có) | GameController (H) |
| `REMATCH_REQUESTED` | `fromName` | Đối thủ muốn chơi lại | GameController (H) |
| `OPPONENT_LEFT_ROOM` | — | Đối thủ về sảnh sau ván | GameController (H) |
| `CHAT_MSG` | `fromName\|text` | Tin nhắn chat | ChatController (H) |
| `ERROR` | `code\|message` | Lỗi | Màn hình hiện tại hiển thị |
| `PONG` | — | Trả lời PING | NetworkClient (P) |

### 10.4 UDP tìm server (FE-22, mở rộng)

- Client gửi broadcast tới `255.255.255.255:5001` nội dung `CARO_DISCOVER`.
- Server trả unicast `CARO_SERVER|<tcpPort>|<tên máy>`; client lấy IP từ địa chỉ nguồn gói tin.

## 11. Các luồng xử lý chính

### 11.1 Hai người chơi một ván

```
Client A                     Server                         Client B
  │ LOGIN|an|*** ───────────▶│ AuthService kiểm tra mật khẩu   │
  │◀── WELCOME|an, ROOM_LIST │◀─────────────── LOGIN|binh|*** ─│
  │ CREATE_ROOM ────────────▶│ RoomManager tạo R1 (An = X)     │
  │◀── ROOM_CREATED|R1       │──── ROOM_LIST (R1 WAITING) ────▶│
  │                          │◀──────────────── JOIN_ROOM|R1 ──│
  │                          │ RoomManager → GameSessionFactory│
  │◀ GAME_START|R1|X|Bình|X ─│── GAME_START|R1|O|An|X ────────▶│
  │ MOVE|7|7 ───────────────▶│ GameSession kiểm tra            │
  │◀── MOVED|X|7|7|O ────────│──── MOVED|X|7|7|O ─────────────▶│
  │                          │◀──────────────────── MOVE|7|8 ──│
  │       … lặp lại …        │                                 │
  │                          │ WinChecker thấy 5 quân          │
  │◀ GAME_OVER|X|FIVE|cells ─│── GAME_OVER|X|FIVE|cells ──────▶│
  │                          │ MatchRecorder.record(…)         │
```

### 11.2 Chơi với máy

```
Client                         Server
  │ PLAY_BOT|2 ───────────────▶│ GameCommandController (H) tạo BotPlayer(level 2)
  │                            │ → RoomManager.createBotRoom(người, bot, 2) (P)
  │◀── GAME_START|B7|X|BOT|X ──│ GameSession(người = X, bot = O)
  │ MOVE|7|7 ─────────────────▶│ hợp lệ → MOVED gửi cho người và BotPlayer
  │◀── MOVED|X|7|7|O ──────────│ BotPlayer thấy tới lượt O
  │                            │ BotExecutor chạy AIEngine.bestMove(board.copy(), O, depth)
  │                            │ xong → session.handleMove(bot, r, c)
  │◀── MOVED|O|6|8|X ──────────│
```

Bot **không bao giờ** chạy trên luồng của `ClientHandler`.

### 11.3 Mất kết nối giữa ván

```
ClientHandler(B) đọc lỗi/EOF
  → PlayerRegistry.remove(B)                          (P)
  → RoomManager.onDisconnect(B)                       (P)
      → session.handleDisconnect(B)                   (H)
          → A nhận GAME_OVER|<symbol A>|LEFT|
          → GameEndListener → RoomManager xóa phòng   (P)
          → MatchRecorder.record(...)                 (P)
```

### 11.4 Trạng thái phòng

```
 (CREATE_ROOM)            (JOIN_ROOM)              (GAME_OVER)
 ────────────▶ WAITING ───────────────▶ PLAYING ─────────────▶ FINISHED
                  │                        ▲                       │
          LEAVE_ROOM│                       │ cả hai REMATCH       │ BACK_TO_LOBBY / thoát
                  ▼                        └───────────────────────┤
               (xóa phòng)                                         ▼
                                                              (xóa phòng)
```

### 11.5 Gợi ý thiết kế AI (FE-16, FE-17)

- **Ô ứng viên:** chỉ xét ô trống cách quân đã đánh ≤ 2 ô. Bàn trống thì đánh ô giữa (7,7).
- **Hàm đánh giá:** quét 4 hướng, đếm chuỗi quân cùng màu và số đầu mở:

| Mẫu | Điểm (tham khảo) |
|---|---|
| 5 quân | 1 000 000 |
| 4 quân, hai đầu mở | 100 000 |
| 4 quân, một đầu mở | 10 000 |
| 3 quân, hai đầu mở | 10 000 |
| 3 quân, một đầu mở | 1 000 |
| 2 quân, hai đầu mở | 500 |
| 2 quân, một đầu mở | 50 |

  Điểm thế cờ = điểm của bot − (hệ số 1,2 × điểm của người). Hệ số > 1 để bot ưu tiên phòng thủ.
- **Độ khó:** mức 1 = độ sâu 1 + chọn ngẫu nhiên trong top 3; mức 2 = độ sâu 2; mức 3 = độ sâu 3–4 (chỉnh theo kết quả đo FE-25).
- **Trước khi tìm kiếm:** nếu có nước thắng ngay thì đánh; nếu đối thủ có nước thắng ngay thì chặn.

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

## 13. Phân công theo giai đoạn

Mỗi việc có mã `GĐ.người.số` (ví dụ `1.H.2`) để ghi vào commit và báo cáo tiến độ.

### GĐ0. Chốt thiết kế và dựng khung (10/11 – 12/11) — làm cùng nhau

| Mã | Việc | Ai | Kết quả |
|---|---|---|---|
| 0.HP.1 | Đọc lại tài liệu này, chốt mục 3, 7.5 (quy tắc MVC), 9, 10, 17 | H + P | Tài liệu được cả hai đồng ý |
| 0.P.1 | Tạo `caro-parent` + 3 module, cấu hình JavaFX, JUnit 5, `.gitignore`, `README.md` cách chạy | P | `mvn clean package` chạy được |
| 0.P.2 | Tạo khung rỗng (chỉ khai báo class, method trả về mặc định) mọi file **của P** ở mục 8 | P | Build được |
| 0.H.1 | Tạo khung rỗng mọi file **của H** ở mục 8, kể cả `game.fxml` có `fx:include` tới `chat.fxml` | H | Build được |
| 0.HP.2 | Viết 3 interface hợp đồng (`Player`, `GameEndListener`, `MatchRecorder`) **cùng nhau, một commit** | H + P | Merge vào `main` |
| 0.HP.3 | Mỗi người push khung lên `main` theo thứ tự: P (Maven) → H → P | H + P | `main` build được, có đủ file ở mục 8 |

**Kết thúc GĐ0:** mọi file trong mục 8 đã tồn tại. Từ đây mỗi người chỉ sửa file của mình, nên gần như không còn conflict.

### GĐ1. Nền tảng song song (13/11 – 17/11)

| Mã | Việc | FE | Ai |
|---|---|---|---|
| 1.H.1 | `Board`, `Symbol`, `WinChecker`, `GameResult`, `EndReason` + JUnit (thắng ngang/dọc/2 chéo, ở biên, 6 quân, hòa) | FE-02 | H |
| 1.H.2 | `CaroClientApp`, `Screen`, `SceneManager` chuyển được 5 màn hình (dùng FXML trống của P), `base.css` | FE-04 | H |
| 1.H.3 | `GameSession` + `GameMessages`: lượt, kiểm tra nước, gửi `MOVED`, phát hiện thắng/hòa + `GameSessionTest` với `FakePlayer` | FE-09 | H |
| 1.H.4 | `GameModel`, `game.fxml`, `BoardCanvas`, `GameController` (khung): vẽ bàn theo `GameModel`, click được, chạy thử bằng dữ liệu giả (chưa nối mạng) + `GameModelTest` | FE-10 | H |
| 1.P.1 | `Command`, `Message`, `Protocol` + `MessageTest` (tham số rỗng, ký tự tiếng Việt, sai định dạng) | FE-03 | P |
| 1.P.2 | `CaroServer`, `ServerConfig`, `ClientHandler`, `CommandDispatcher`, log console | FE-05, FE-23 | P |
| 1.P.3 | `PlayerRegistry`, `RemotePlayer`, chặn lệnh khi chưa đăng nhập | FE-05 | P |
| 1.P.4 | `NetworkClient`, `MessageBus`, `ClientSession`, `connect.fxml` + `ConnectController` | FE-05 | P |
| 1.P.5 | `docker-compose.yml`, `schema.sql` (bảng `players`), `Database`, `PlayerDao`; ghi cách chạy MySQL vào README | FE-20 | P |
| 1.P.6 | `PasswordHasher`, `AuthService`, `AuthCommandController` (`REGISTER`, `LOGIN`, `LOGOUT`), `AuthMessages`, `ErrorMessages` + `PasswordHasherTest`, `AuthServiceTest` | FE-27 | P |
| 1.P.7 | `login.fxml` + `LoginController`: form đăng nhập, form đăng ký, báo lỗi từng mã | FE-27 | P |

**Điểm hẹn 17/11:** client kết nối, đăng ký, đăng nhập, đăng xuất được; tài khoản nằm trong MySQL. `GameSession` qua hết test. Hoàng chạy MySQL bằng Docker theo README và đăng nhập bằng client của Phi; Phi thử `SceneManager` của Hoàng.

### GĐ2. Ghép nối ván người–người (18/11 – 22/11)

| Mã | Việc | FE | Ai |
|---|---|---|---|
| 2.H.1 | `GameCommandController`: `MOVE`, đăng ký vào dispatcher | FE-10 | H |
| 2.H.2 | `GameController` nối `MessageBus`: nhận `MOVED`, `ERROR` → cập nhật `GameModel`; `BoardCanvas` và nhãn lượt tự cập nhật theo binding | FE-10 | H |
| 2.H.3 | `GAME_OVER`, tô đường thắng, `ResultDialog` | FE-11 | H |
| 2.H.4 | `Evaluator`, `MoveGenerator`, `AIEngine` minimax + JUnit (bot thắng khi có 4 quân, bot chặn đường 4) | FE-16 | H |
| 2.H.5 | `CHAT` / `CHAT_MSG` trong `GameCommandController` + `GameMessages.chatMsg`, `chat.fxml` + `ChatController` + `ChatModel` | FE-15 | H |
| 2.P.1 | `RoomManager`, `Room`, `LIST_ROOMS`, `CREATE_ROOM`, `LobbyMessages`, `LobbyModel` + `lobby.fxml` + `LobbyController` (nút Chơi với máy + ô độ khó tạm thời chỉ gửi lệnh, nút Đăng xuất). **Xong trước 19/11** để Hoàng ghép ván | FE-07 | P |
| 2.P.2 | `JOIN_ROOM`, `LEAVE_ROOM`, `waiting.fxml` + `WaitingController`; tạo `GameSession` qua factory; xử lý `GAME_START` → chuyển màn hình | FE-08 | P |
| 2.P.3 | Phát hiện socket đóng, `PING/PONG`, dọn registry/phòng (tài khoản được đăng nhập lại), gọi `session.handleDisconnect` | FE-06 | P |
| 2.HP.1 | **Chạy thử trên 2 máy thật**, kiểm tra firewall Windows | — | H + P |

**Mốc M2 (22/11):** hai tài khoản trên hai máy đăng nhập, vào cùng phòng, chơi trọn một ván, có chat.

### GĐ3. Bot và hoàn thiện chức năng (23/11 – 27/11)

| Mã | Việc | FE | Ai |
|---|---|---|---|
| 3.H.1 | Alpha-Beta, sắp xếp nước đi, thắng ngay/chặn ngay | FE-17 | H |
| 3.H.2 | `BotPlayer`, `BotExecutor`, `PLAY_BOT` cho cả khách và người đăng nhập (gọi `RoomManager.createBotRoom` của P); không ghi CSDL nếu có khách; `BACK_TO_LOBBY` của khách trả `BACK_TO_LOGIN`; `GameController` ẩn chat khi đấu bot | FE-18 | H |
| 3.P.0 | `RoomManager.createBotRoom` (phòng ẩn cho ván với bot); bảng quyền khách trong `CommandDispatcher`; `PlayerRegistry` cấp tên `Khách-<số>`; `login.fxml` + `LoginController` thêm ô độ khó + nút "Chơi với máy", nhận `GAME_START` → `SceneManager.show(GAME)`. **Làm trước 24/11** để H ghép | FE-18 | P |
| 3.H.3 | Ba mức độ khó | FE-19 | H |
| 3.H.4 | `SURRENDER` + nút Đầu hàng | FE-12 | H |
| 3.H.5 | `handleDisconnect` → `GAME_OVER` lý do `LEFT` + thông báo client | FE-13 | H |
| 3.H.6 | `REMATCH`, `BACK_TO_LOBBY`, đổi bên | FE-14 | H |
| 3.P.1 | Bảng `matches`, `MatchDao`, `MySqlMatchRecorder` (lỗi ghi DB → log, ván vẫn kết thúc bình thường) | FE-20 | P |
| 3.P.2 | Thống kê cá nhân ở sảnh (nếu kịp) | FE-21 | P |
| 3.P.3 | UDP tìm server (nếu kịp) | FE-22 | P |
| 3.P.4 | Viết `docs/test-report.md` với kịch bản mục 15, bắt đầu chạy | FE-24 | P |

**Mốc M3 (27/11): đóng băng chức năng.** Sau ngày này chỉ sửa lỗi.

### GĐ4. Kiểm thử, báo cáo, nộp (28/11 – 01/12)

| Mã | Việc | Ai |
|---|---|---|
| 4.HP.1 | Chạy toàn bộ kịch bản mục 15 trên 2–3 máy; lỗi ở file của ai thì người đó sửa | H + P |
| 4.H.1 | Đo thời gian bot theo độ sâu, đo độ trễ nước đi (bảng số cho báo cáo) | H |
| 4.H.2 | Báo cáo: luật cờ, thiết kế ván cờ, thuật toán AI, giao diện bàn cờ, kết quả đo | H |
| 4.P.1 | Báo cáo: tổng quan, actor/use case, kiến trúc Client-Server, TCP/UDP, giao thức, CSDL, kiểm thử | P |
| 4.HP.2 | Ảnh chụp màn hình, slide, video demo | H + P |
| 4.HP.3 | Nộp lên hệ thống ngày **01/12** | H + P |

### Tổng quan tiến độ

```
           10/11  13/11       18/11       23/11       28/11  01/12
Hoàng  GĐ0 [khung]│Board,Session   │Game UI,Chat,Minimax│Bot,Đầu hàng,Rematch│Đo+Báo cáo
Phi    GĐ0 [Maven]│Mạng,MySQL,Login│Sảnh,Phòng,MấtKN    │Lưu trận,UDP,Test   │Test+Báo cáo
                  ▲ hẹn 17/11   ▲ M2 22/11      ▲ M3 27/11           ▲ nộp
```

## 14. Quy trình Git chống conflict

1. **`main` luôn build được.** Sau GĐ0 không push thẳng lên `main`.
2. **Nhánh:** `hoang/fe-09-game-session`, `phi/fe-07-lobby`. Một nhánh một FE, sống ≤ 3 ngày.
3. **Commit:** `FE-09: kiểm tra nước đi đúng lượt`. Mỗi commit một ý.
4. **Trước khi merge:**
   ```bash
   git fetch origin
   git rebase origin/main
   mvn clean test
   ```
   Rồi merge vào `main` (qua Pull Request để người kia xem nhanh, hoặc merge local nếu cả hai đồng ý).
5. **Chỉ sửa file của mình** theo bảng mục 8.
6. **File hợp đồng** (`Player`, `GameEndListener`, `MatchRecorder`, `Command`, các `pom.xml`, `Screen`, `ErrorMessages`, 3 dòng đăng ký controller trong `CaroServer`):
   - Nhắn người kia trước.
   - Sửa trong **commit riêng** có tiền tố `CONTRACT:`, không trộn với code tính năng.
   - Merge ngay, người kia rebase ngay.
7. **Thêm lệnh giao thức mới:** sửa mục 10 trong tài liệu này trước → P thêm vào `Command` (thêm ở cuối enum) → mỗi người cài phần của mình.
8. **FXML:** mỗi màn hình một file, không bao giờ hai người cùng sửa một file FXML. Chat được nhúng bằng `fx:include`.
9. **Không commit:** `target/`, `.idea/`, `*.iml`, `.vscode/`, file mật khẩu DB (dùng biến môi trường hoặc `server.properties` nằm trong `.gitignore` + file mẫu `server.properties.example`).
10. **Họp nhanh 10 phút mỗi tối:** hôm nay xong gì, mai làm gì, có cần sửa hợp đồng không.

## 15. Định nghĩa hoàn thành và kiểm thử

**Một việc được coi là xong khi:** chạy được, có test (với lớp logic), đã merge vào `main`, `main` vẫn build, và người kia đã chạy thử ít nhất một lần.

### Kịch bản kiểm thử hệ thống (FE-24)

| Mã | Kịch bản | Kết quả mong đợi |
|---|---|---|
| T-01 | Kết nối sai IP / server tắt | Báo lỗi, không treo |
| T-02a | Đăng ký username đã tồn tại | `USERNAME_TAKEN` |
| T-02b | Đăng nhập sai mật khẩu / username không tồn tại | Cùng một lỗi `INVALID_CREDENTIALS` |
| T-02c | Cùng một tài khoản đăng nhập trên hai máy | Máy thứ hai nhận `ALREADY_LOGGED_IN` |
| T-02d | Gửi `CREATE_ROOM` khi chưa đăng nhập (qua telnet) | `NOT_LOGGED_IN` |
| T-02e | Mất kết nối rồi đăng nhập lại cùng tài khoản | Đăng nhập được (phiên cũ đã được dọn) |
| T-02f | Kiểm tra bảng `players` và log server | Không thấy mật khẩu thô |
| T-02g | Khách bấm "Chơi với máy" không đăng nhập, chơi hết ván | Chơi được; hết ván về màn hình đăng nhập; bảng `matches` không có dòng mới |
| T-02h | Khách gửi `CREATE_ROOM` / `CHAT` (qua telnet) | `NOT_LOGGED_IN` |
| T-02i | Người đã đăng nhập chơi với máy từ sảnh | Chơi được; hết ván về sảnh; bảng `matches` có dòng mới với `vs_bot = TRUE` |
| T-03 | Tạo phòng, client khác thấy phòng mới | `ROOM_LIST` cập nhật tức thì |
| T-04 | Hai người vào cùng một phòng cùng lúc | Chỉ một người vào được, người kia nhận `ROOM_FULL` |
| T-05 | Đánh khi không đến lượt | `NOT_YOUR_TURN`, bàn cờ không đổi |
| T-06 | Đánh vào ô đã có quân / ngoài bàn | `CELL_OCCUPIED` / `OUT_OF_BOARD` |
| T-07 | Thắng ngang, dọc, chéo xuôi, chéo ngược, sát biên | `GAME_OVER` đúng người thắng, tô đúng 5 ô |
| T-08 | Bàn đầy không ai thắng | `GAME_OVER\|DRAW\|FULL` |
| T-09 | Đầu hàng | Đối thủ thắng, lý do `SURRENDER` |
| T-10 | Tắt client giữa ván | Đối thủ thắng, lý do `LEFT`; phòng bị xóa |
| T-11 | Rút dây mạng một máy giữa ván | Như T-10 sau tối đa 15 giây (PING timeout) |
| T-12 | Chơi lại | Ván mới, đổi bên X/O |
| T-13 | Bot thắng ngay khi có 4 quân mở | Bot đánh nước thắng |
| T-14 | Người có 4 quân, bot phải chặn | Bot chặn |
| T-15 | 10 client, 5 ván cùng lúc, 1 ván với bot mức 3 | Các ván không chậm lẫn nhau |
| T-16 | Gửi chuỗi rác qua telnet | `ERROR\|BAD_MESSAGE`, server vẫn chạy |
| T-17 | Tắt MySQL giữa chừng | Ván đang chơi vẫn chạy, đăng nhập mới nhận `DB_UNAVAILABLE`, khách vẫn chơi với máy được, log báo lỗi DB |
| T-18 | Chat có tiếng Việt và ký tự `\|` | Hiển thị đúng (`\|` thành `/`) |

## 16. Rủi ro

| Rủi ro | Ảnh hưởng | Cách giảm |
|---|---|---|
| Firewall Windows chặn cổng 5000/5001 | Không chơi được qua 2 máy | Thử 2 máy ngay GĐ2 (2.HP.1); ghi hướng dẫn mở cổng vào README |
| Hợp đồng ở mục 9 thay đổi giữa chừng | Conflict, sửa lại cả hai bên | Chốt kỹ ở GĐ0; đổi thì theo quy tắc `CONTRACT:` |
| Bot quá chậm ở độ sâu cao | Đơ ván | Giới hạn ô ứng viên, alpha-beta, đo sớm ở GĐ3 |
| Cập nhật UI từ luồng mạng | Lỗi JavaFX, giao diện đơ | `MessageBus` luôn chuyển callback sang FX thread |
| Một người trễ việc | Người kia bị chặn ở điểm hẹn | Mỗi bên có cách tự test độc lập (`FakePlayer`, dữ liệu giả, telnet); cắt FE mức X trước |
| Docker/MySQL khó cài trên máy demo | **Không đăng nhập được = không chơi với người được** (chơi với máy vẫn được nhờ chế độ khách) | Chỉ máy chạy server cần MySQL; cài và chạy thử Docker trên máy demo ngay GĐ1; chuẩn bị sẵn tài khoản demo trong `schema.sql`; ghi hướng dẫn cài MySQL trực tiếp (không Docker) vào README làm phương án B |
| Phi bị dồn việc ở GĐ1 (mạng + MySQL + đăng nhập) | Trễ điểm hẹn 17/11 | Hoàng không phụ thuộc đăng nhập ở GĐ1 (test bằng `FakePlayer`); nếu trễ thì lùi 1.P.7 (giao diện đăng ký) sang 18/11, giữ `LOGIN` chạy được trước |

## 17. Việc còn mở (cần chốt ở GĐ0)

- [ ] Luật thắng: giữ D5 (5 quân trở lên, không chặn hai đầu) hay áp dụng luật chặn hai đầu?
- [x] Có đăng nhập không? → **Có**, bắt buộc trước khi vào sảnh (D10). MySQL vì vậy thành bắt buộc (D13).
- [x] Chơi với máy có cho khách chưa đăng nhập không? → **Có**. Chơi với người mới phải đăng nhập (D10).
- [ ] FE-22 (UDP tìm server) để mức X hay nâng lên S (vì đề cương có ghi)?
- [ ] Merge qua Pull Request hay merge local?
