# Kiến trúc và cấu trúc mã nguồn (mục 7–8)

> Bộ tài liệu dự án Caro · [Mục lục](README.md)

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
| `docs/*.md` (bộ tài liệu này, xem `docs/README.md`) | H + P (sửa mục nào thì báo) |
| `docs/test-report.md` | P |
| `README.md` (cách build/chạy) | P |

