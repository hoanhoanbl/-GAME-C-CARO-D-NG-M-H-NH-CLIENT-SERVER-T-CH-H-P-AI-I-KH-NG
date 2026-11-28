package caro.client.controller;

import caro.client.app.SceneManager;
import caro.client.app.Screen;
import caro.client.model.ClientSession;
import caro.client.model.GameInfo;
import caro.common.protocol.Command;
import caro.common.protocol.Message;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * Controller màn hình Phòng chờ (FE-08).
 * Hiển thị mã phòng, thông tin chủ phòng, chờ đối thủ vào phòng.
 * Nhận {@code GAME_START} → lưu {@link GameInfo} và chuyển sang màn hình GAME.
 * Nhận {@code ROOM_LEFT} → xóa thông tin phòng và quay về màn hình LOBBY.
 */
public class WaitingController {

    @FXML private Label roomIdLabel;
    @FXML private Label hostLabel;
    @FXML private Label messageLabel;
    @FXML private Button leaveBtn;

    private final BusSubscriptions subscriptions = new BusSubscriptions();

    @FXML
    public void initialize() {
        ClientSession session = ClientSession.get();
        String roomId = session.getCurrentRoomId();
        roomIdLabel.setText(roomId != null ? roomId : "---");

        String hostName = session.playerName() != null ? session.playerName() : "Bạn";
        hostLabel.setText("Chủ phòng: " + hostName);

        // Đăng ký nhận thông điệp mạng gắn liền với vòng đời giao diện
        subscriptions
                .on(Command.GAME_START, this::onGameStart)
                .on(Command.ROOM_LEFT, this::onRoomLeft)
                .on(Command.ERROR, this::onError)
                .bindTo(leaveBtn);
    }

    @FXML
    public void handleLeaveRoom() {
        messageLabel.setText("");
        ClientSession.get().send(new Message(Command.LEAVE_ROOM.name()));
    }

    private void onGameStart(Message msg) {
        // Định dạng theo mục 10.3: GAME_START|roomId|mySymbol|opponentName|firstSymbol
        if (msg.argCount() >= 4) {
            String roomId = msg.arg(0);
            String mySymbol = msg.arg(1);
            String opponent = msg.arg(2);
            String firstSymbol = msg.arg(3);
            boolean vsBot = "BOT".equalsIgnoreCase(opponent);

            ClientSession.get().setCurrentGame(new GameInfo(roomId, mySymbol, opponent, firstSymbol, vsBot));
            subscriptions.disposeAll();
            SceneManager.get().show(Screen.GAME);
        }
    }

    private void onRoomLeft(Message msg) {
        ClientSession.get().setCurrentRoomId(null);
        subscriptions.disposeAll();
        SceneManager.get().show(Screen.LOBBY);
    }

    private void onError(Message msg) {
        String code = msg.arg(0);
        String text = msg.argCount() > 1 ? msg.arg(1) : "";
        String display = switch (code) {
            case "NOT_IN_ROOM" -> "Bạn không ở trong phòng nào!";
            case "IN_GAME" -> "Ván đấu đang diễn ra!";
            default -> !text.isBlank() ? text : code;
        };
        messageLabel.setText(display);
    }
}
