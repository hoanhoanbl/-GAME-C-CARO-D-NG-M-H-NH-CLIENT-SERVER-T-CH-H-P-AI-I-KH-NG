# Hợp đồng, giao thức, luồng xử lý (mục 9–11)

> Bộ tài liệu dự án Caro · [Mục lục](README.md)

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
- Lệnh không xác định, sai số tham số hoặc dòng dài quá 1024 byte → server trả `ERROR|BAD_MESSAGE|…`, **không** ngắt kết nối. Lỗi nội bộ khi xử lý lệnh hợp lệ → `ERROR|SERVER_ERROR|…`, cũng không ngắt kết nối.
- `Message` tự làm sạch mọi tham số khi tạo: `|` → `/`, ký tự CR/LF → khoảng trắng, nên một tham số không làm lệch số tham số hay tách dòng.
- Khách (chưa đăng nhập) chỉ được gửi `REGISTER`, `LOGIN`, `PLAY_BOT`, `PING`, `QUIT`, và trong ván với máy thì thêm `MOVE`, `SURRENDER`, `REMATCH`, `BACK_TO_LOBBY`. Lệnh khác → `ERROR|NOT_LOGGED_IN|…` (bảng quyền ở mục 7.2).
- Mật khẩu không được chứa `|`. Mật khẩu đi qua TCP dạng văn bản thường (không mã hóa đường truyền); chấp nhận trong LAN và ghi rõ hạn chế này trong báo cáo.
- Server **không** ghi log dòng `REGISTER`/`LOGIN` nguyên văn (che mật khẩu thành `***`).

### 10.2 Client → Server

| Lệnh | Tham số | Trạng thái hợp lệ | Phản hồi khi thành công | Mã lỗi | Handler (chủ) |
|---|---|---|---|---|---|
| `REGISTER` | `username\|password[\|email]` (email tùy chọn) | Chưa đăng nhập | `REGISTERED\|username` (không tự đăng nhập) | `USERNAME_TAKEN`, `USERNAME_INVALID`, `PASSWORD_INVALID`, `DB_UNAVAILABLE` | AuthCommandController (P) |
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
| `WELCOME` | `username[\|email]` (có email nếu tài khoản có) | Đăng nhập thành công | LoginController (P) |
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

