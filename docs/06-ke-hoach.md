# Kế hoạch, rủi ro, việc còn mở (mục 13, 16, 17)

> Bộ tài liệu dự án Caro · [Mục lục](README.md)

## 13. Phân công theo giai đoạn

Mỗi việc có mã `GĐ.người.số` (ví dụ `1.H.2`) để ghi vào commit và báo cáo tiến độ.

### GĐ0. Chốt thiết kế và dựng khung — làm cùng nhau

| Mã | Việc | Ai | Kết quả |
|---|---|---|---|
| 0.HP.1 | Đọc lại bộ tài liệu trong `docs/`, chốt mục 3, 7.5 (quy tắc MVC), 9, 10, 17 | H + P | Tài liệu được cả hai đồng ý |
| 0.P.1 | Tạo `caro-parent` + 3 module, cấu hình JavaFX, JUnit 5, `.gitignore`, `README.md` cách chạy | P | `mvn clean package` chạy được |
| 0.P.2 | Tạo khung rỗng (chỉ khai báo class, method trả về mặc định) mọi file **của P** ở mục 8 | P | Build được |
| 0.H.1 | Tạo khung rỗng mọi file **của H** ở mục 8, kể cả `game.fxml` có `fx:include` tới `chat.fxml` | H | Build được |
| 0.HP.2 | Viết 3 interface hợp đồng (`Player`, `GameEndListener`, `MatchRecorder`) **cùng nhau, một commit** | H + P | Merge vào `dev` |
| 0.HP.3 | Mỗi người push khung lên `dev` theo thứ tự: P (Maven) → H → P | H + P | `dev` build được, có đủ file ở mục 8 |

**Kết thúc GĐ0:** mọi file trong mục 8 đã tồn tại. Từ đây mỗi người chỉ sửa file của mình, nên gần như không còn conflict.

### GĐ1. Nền tảng song song

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

**Điểm hẹn cuối GĐ1:** client kết nối, đăng ký, đăng nhập, đăng xuất được; tài khoản nằm trong MySQL. `GameSession` qua hết test. Hoàng chạy MySQL bằng Docker theo README và đăng nhập bằng client của Phi; Phi thử `SceneManager` của Hoàng.

### GĐ2. Ghép nối ván người–người

| Mã | Việc | FE | Ai |
|---|---|---|---|
| 2.H.1 | `GameCommandController`: `MOVE`, đăng ký vào dispatcher | FE-10 | H |
| 2.H.2 | `GameController` nối `MessageBus`: nhận `MOVED`, `ERROR` → cập nhật `GameModel`; `BoardCanvas` và nhãn lượt tự cập nhật theo binding | FE-10 | H |
| 2.H.3 | `GAME_OVER`, tô đường thắng, `ResultDialog` | FE-11 | H |
| 2.H.4 | `Evaluator`, `MoveGenerator`, `AIEngine` minimax + JUnit (bot thắng khi có 4 quân, bot chặn đường 4) | FE-16 | H |
| 2.H.5 | `CHAT` / `CHAT_MSG` trong `GameCommandController` + `GameMessages.chatMsg`, `chat.fxml` + `ChatController` + `ChatModel` | FE-15 | H |
| 2.P.1 | `RoomManager`, `Room`, `LIST_ROOMS`, `CREATE_ROOM`, `LobbyMessages`, `LobbyModel` + `lobby.fxml` + `LobbyController` (nút Chơi với máy + ô độ khó tạm thời chỉ gửi lệnh, nút Đăng xuất). **Làm sớm trong GĐ2** để Hoàng ghép ván | FE-07 | P |
| 2.P.2 | `JOIN_ROOM`, `LEAVE_ROOM`, `waiting.fxml` + `WaitingController`; tạo `GameSession` qua factory; xử lý `GAME_START` → chuyển màn hình | FE-08 | P |
| 2.P.3 | Phát hiện socket đóng, `PING/PONG`, dọn registry/phòng (tài khoản được đăng nhập lại), gọi `session.handleDisconnect` | FE-06 | P |
| 2.HP.1 | **Chạy thử trên 2 máy thật**, kiểm tra firewall Windows | — | H + P |

**Mốc M2 (cuối GĐ2):** hai tài khoản trên hai máy đăng nhập, vào cùng phòng, chơi trọn một ván, có chat.

### GĐ3. Bot và hoàn thiện chức năng

| Mã | Việc | FE | Ai |
|---|---|---|---|
| 3.H.1 | Alpha-Beta, sắp xếp nước đi, thắng ngay/chặn ngay | FE-17 | H |
| 3.H.2 | `BotPlayer`, `BotExecutor`, `PLAY_BOT` cho cả khách và người đăng nhập (gọi `RoomManager.createBotRoom` của P); không ghi CSDL nếu có khách; `BACK_TO_LOBBY` của khách trả `BACK_TO_LOGIN`; `GameController` ẩn chat khi đấu bot | FE-18 | H |
| 3.P.0 | `RoomManager.createBotRoom` (phòng ẩn cho ván với bot); bảng quyền khách trong `CommandDispatcher`; `PlayerRegistry` cấp tên `Khách-<số>`; `login.fxml` + `LoginController` thêm ô độ khó + nút "Chơi với máy", nhận `GAME_START` → `SceneManager.show(GAME)`. **Làm đầu GĐ3** để H ghép | FE-18 | P |
| 3.H.3 | Ba mức độ khó | FE-19 | H |
| 3.H.4 | `SURRENDER` + nút Đầu hàng | FE-12 | H |
| 3.H.5 | `handleDisconnect` → `GAME_OVER` lý do `LEFT` + thông báo client | FE-13 | H |
| 3.H.6 | `REMATCH`, `BACK_TO_LOBBY`, đổi bên | FE-14 | H |
| 3.P.1 | Bảng `matches`, `MatchDao`, `MySqlMatchRecorder` (lỗi ghi DB → log, ván vẫn kết thúc bình thường) | FE-20 | P |
| 3.P.2 | Thống kê cá nhân ở sảnh (nếu kịp) | FE-21 | P |
| 3.P.3 | UDP tìm server (nếu kịp) | FE-22 | P |
| 3.P.4 | Viết `docs/test-report.md` với kịch bản mục 15, bắt đầu chạy | FE-24 | P |

**Mốc M3 (cuối GĐ3): đóng băng chức năng.** Sau mốc này chỉ sửa lỗi.

### GĐ4. Kiểm thử, báo cáo, nộp

| Mã | Việc | Ai |
|---|---|---|
| 4.HP.1 | Chạy toàn bộ kịch bản mục 15 trên 2–3 máy; lỗi ở file của ai thì người đó sửa | H + P |
| 4.H.1 | Đo thời gian bot theo độ sâu, đo độ trễ nước đi (bảng số cho báo cáo) | H |
| 4.H.2 | Báo cáo: luật cờ, thiết kế ván cờ, thuật toán AI, giao diện bàn cờ, kết quả đo | H |
| 4.P.1 | Báo cáo: tổng quan, actor/use case, kiến trúc Client-Server, TCP/UDP, giao thức, CSDL, kiểm thử | P |
| 4.HP.2 | Ảnh chụp màn hình, slide, video demo | H + P |
| 4.HP.3 | Nộp lên hệ thống | H + P |

### Tổng quan tiến độ

```
Hoàng  GĐ0 [khung]│Board,Session   │Game UI,Chat,Minimax│Bot,Đầu hàng,Rematch│Đo+Báo cáo
Phi    GĐ0 [Maven]│Mạng,MySQL,Login│Sảnh,Phòng,MấtKN    │Lưu trận,UDP,Test   │Test+Báo cáo
                  ▲ hẹn cuối GĐ1 ▲ M2 cuối GĐ2     ▲ M3 cuối GĐ3       ▲ nộp
```

## 16. Rủi ro

| Rủi ro | Ảnh hưởng | Cách giảm |
|---|---|---|
| Firewall Windows chặn cổng 5000/5001 | Không chơi được qua 2 máy | Thử 2 máy ngay GĐ2 (2.HP.1); ghi hướng dẫn mở cổng vào README |
| Hợp đồng ở mục 9 thay đổi giữa chừng | Conflict, sửa lại cả hai bên | Chốt kỹ ở GĐ0; đổi thì theo quy tắc `CONTRACT:` |
| Bot quá chậm ở độ sâu cao | Đơ ván | Giới hạn ô ứng viên, alpha-beta, đo sớm ở GĐ3 |
| Cập nhật UI từ luồng mạng | Lỗi JavaFX, giao diện đơ | `MessageBus` luôn chuyển callback sang FX thread |
| Một người trễ việc | Người kia bị chặn ở điểm hẹn | Mỗi bên có cách tự test độc lập (`FakePlayer`, dữ liệu giả, telnet); cắt FE mức X trước |
| Docker/MySQL khó cài trên máy demo | **Không đăng nhập được = không chơi với người được** (chơi với máy vẫn được nhờ chế độ khách) | Chỉ máy chạy server cần MySQL; cài và chạy thử Docker trên máy demo ngay GĐ1; chuẩn bị sẵn tài khoản demo trong `schema.sql`; ghi hướng dẫn cài MySQL trực tiếp (không Docker) vào README làm phương án B |
| Phi bị dồn việc ở GĐ1 (mạng + MySQL + đăng nhập) | Trễ điểm hẹn cuối GĐ1 | Hoàng không phụ thuộc đăng nhập ở GĐ1 (test bằng `FakePlayer`); nếu trễ thì lùi 1.P.7 (giao diện đăng ký) sang đầu GĐ2, giữ `LOGIN` chạy được trước |

## 17. Việc còn mở (cần chốt ở GĐ0)

- [x] Luật thắng: **giữ D5** (5 quân trở lên liên tiếp, không áp dụng luật chặn hai đầu).
- [x] Có đăng nhập không? → **Có**, bắt buộc trước khi vào sảnh (D10). MySQL vì vậy thành bắt buộc (D13).
- [x] Chơi với máy có cho khách chưa đăng nhập không? → **Có**. Chơi với người mới phải đăng nhập (D10).
- [x] FE-22 (UDP tìm server): **giữ mức X**. Nhập tay IP vẫn dùng được, broadcast dễ bị firewall/wifi trường chặn; làm ở GĐ3 nếu kịp.
- [x] Merge: **qua Pull Request**, người còn lại review trước khi vào `dev`.
