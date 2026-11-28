package caro.client.controller;

import caro.client.app.SceneManager;
import caro.client.app.Screen;
import caro.client.model.ClientSession;
import caro.client.model.GameInfo;
import caro.client.model.LobbyModel;
import caro.client.model.RoomInfo;
import caro.client.model.User;
import caro.client.net.MessageBus;
import caro.common.protocol.Command;
import caro.common.protocol.Message;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.function.Consumer;

/**
 * Controller màn hình Sảnh (FE-07).
 */
public class LobbyController {

    @FXML private Label welcomeLabel;
    @FXML private TableView<RoomInfo> roomsTable;
    @FXML private TableColumn<RoomInfo, String> roomIdCol;
    @FXML private TableColumn<RoomInfo, String> ownerCol;
    @FXML private TableColumn<RoomInfo, String> statusCol;
    @FXML private ComboBox<String> difficultyComboBox;
    @FXML private Label messageLabel;

    private final LobbyModel lobbyModel = new LobbyModel();

    private final Consumer<Message> roomListHandler = this::onRoomList;
    private final Consumer<Message> roomCreatedHandler = this::onRoomCreated;
    private final Consumer<Message> gameStartHandler = this::onGameStart;
    private final Consumer<Message> loggedOutHandler = this::onLoggedOut;
    private final Consumer<Message> errorHandler = this::onError;

    @FXML
    public void initialize() {
        ClientSession session = ClientSession.get();
        User user = session.getUser();
        if (user != null) {
            String emailStr = (user.getEmail() != null && !user.getEmail().isBlank()) ? " (" + user.getEmail() + ")" : "";
            welcomeLabel.setText("Xin chào: " + user.getName() + emailStr + (session.isGuest() ? " [Khách]" : ""));
        } else if (session.playerName() != null) {
            welcomeLabel.setText("Xin chào: " + session.playerName() + (session.isGuest() ? " [Khách]" : ""));
        }

        // Cấu hình bảng danh sách phòng
        roomIdCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        ownerCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOwner()));
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatusDisplay()));
        roomsTable.setItems(lobbyModel.getRooms());

        // Nhấp đúp chuột vào phòng để vào nhanh
        roomsTable.setRowFactory(tv -> {
            TableRow<RoomInfo> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    handleJoinRoom();
                }
            });
            return row;
        });

        // Cấu hình danh sách độ khó bot
        difficultyComboBox.setItems(FXCollections.observableArrayList(
                "1 - Dễ (Heuristic)",
                "2 - Trung bình (Minimax)",
                "3 - Khó (Alpha-Beta)"
        ));
        difficultyComboBox.getSelectionModel().select(0);

        // Đăng ký nhận thông điệp từ MessageBus
        MessageBus bus = MessageBus.get();
        bus.unsubscribe(Command.ROOM_LIST, roomListHandler);
        bus.unsubscribe(Command.ROOM_CREATED, roomCreatedHandler);
        bus.unsubscribe(Command.GAME_START, gameStartHandler);
        bus.unsubscribe(Command.LOGGED_OUT, loggedOutHandler);
        bus.unsubscribe(Command.ERROR, errorHandler);

        bus.subscribe(Command.ROOM_LIST, roomListHandler);
        bus.subscribe(Command.ROOM_CREATED, roomCreatedHandler);
        bus.subscribe(Command.GAME_START, gameStartHandler);
        bus.subscribe(Command.LOGGED_OUT, loggedOutHandler);
        bus.subscribe(Command.ERROR, errorHandler);

        // Tự động yêu cầu danh sách phòng khi vào sảnh
        handleRefresh();
    }

    @FXML
    public void handleCreateRoom() {
        messageLabel.setText("");
        ClientSession.get().send(new Message("CREATE_ROOM"));
    }

    @FXML
    public void handleJoinRoom() {
        messageLabel.setStyle("-fx-text-fill: #e53e3e;");
        RoomInfo selected = roomsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            messageLabel.setText("Vui lòng chọn một phòng trong danh sách!");
            return;
        }
        if (!selected.isWaiting()) {
            messageLabel.setText("Phòng " + selected.getId() + " hiện đã đầy hoặc đang thi đấu!");
            return;
        }
        messageLabel.setText("");
        ClientSession.get().send(new Message("JOIN_ROOM", selected.getId()));
    }

    @FXML
    public void handleRefresh() {
        ClientSession.get().send(new Message("LIST_ROOMS"));
    }

    @FXML
    public void handlePlayWithBot() {
        int level = difficultyComboBox.getSelectionModel().getSelectedIndex() + 1;
        messageLabel.setStyle("-fx-text-fill: #3182ce;");
        messageLabel.setText("Đang khởi tạo ván đấu với máy (Độ khó: " + level + ")...");
        ClientSession.get().send(new Message("PLAY_BOT", String.valueOf(level)));
    }

    @FXML
    public void handleLogout() {
        ClientSession.get().send(new Message("LOGOUT"));
        ClientSession.get().setUser(null);
        ClientSession.get().setPlayerName(null);
        SceneManager.get().show(Screen.LOGIN);
    }

    private void onRoomList(Message msg) {
        String encoded = msg.argCount() > 0 ? msg.arg(0) : "";
        lobbyModel.updateFromEncoded(encoded);
    }

    private void onRoomCreated(Message msg) {
        String roomId = msg.arg(0);
        ClientSession.get().setCurrentRoomId(roomId);
        SceneManager.get().show(Screen.WAITING);
    }

    private void onGameStart(Message msg) {
        // GAME_START|roomId|mySymbol|opponentName|firstSymbol
        if (msg.argCount() >= 4) {
            String roomId = msg.arg(0);
            String mySymbol = msg.arg(1);
            String opponent = msg.arg(2);
            String firstSymbol = msg.arg(3);
            boolean vsBot = "BOT".equalsIgnoreCase(opponent);
            ClientSession.get().setCurrentGame(new GameInfo(roomId, mySymbol, opponent, firstSymbol, vsBot));
            SceneManager.get().show(Screen.GAME);
        }
    }

    private void onLoggedOut(Message msg) {
        ClientSession.get().setUser(null);
        ClientSession.get().setPlayerName(null);
        SceneManager.get().show(Screen.LOGIN);
    }

    private void onError(Message msg) {
        String code = msg.arg(0);
        String text = msg.argCount() > 1 ? msg.arg(1) : "";
        String display;
        switch (code) {
            case "ALREADY_IN_ROOM" -> display = "Bạn hiện đang ở trong một phòng khác!";
            case "ROOM_NOT_FOUND" -> display = "Phòng không tồn tại hoặc đã bị hủy!";
            case "ROOM_FULL" -> display = "Phòng đã đủ 2 người chơi!";
            case "NOT_LOGGED_IN" -> display = "Phiên đăng nhập đã hết hạn!";
            default -> display = !text.isBlank() ? text : code;
        }
        messageLabel.setStyle("-fx-text-fill: #e53e3e;");
        messageLabel.setText(display);
    }
}
