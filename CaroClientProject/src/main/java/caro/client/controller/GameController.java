package caro.client.controller;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import caro.client.app.Screen;
import caro.client.app.SceneManager;
import caro.client.model.ClientSession;
import caro.client.model.GameInfo;
import caro.client.model.GameModel;
import caro.client.net.MessageBus;
import caro.client.view.BoardCanvas;
import caro.common.model.Symbol;
import caro.common.protocol.Command;
import caro.common.protocol.Message;

import java.util.function.Consumer;

/**
 * Màn hình ván cờ: click ô → gửi {@code MOVE}; nhận {@code MOVED}, {@code ERROR} → cập nhật
 * {@link GameModel}. {@link BoardCanvas} và các nhãn tự cập nhật qua binding (R3).
 */
public class GameController {
    @FXML private BoardCanvas boardCanvas;
    @FXML private Label infoLabel;
    @FXML private Label turnLabel;
    @FXML private Label errorLabel;

    private final GameModel model = new GameModel();
    private final Consumer<Message> onMoved = this::handleMoved;
    private final Consumer<Message> onError = this::handleError;
    private boolean subscribed;

    @FXML
    public void initialize() {
        GameInfo info = ClientSession.get().currentGame();
        if (info != null) {
            model.start(parseSymbol(info.mySymbol(), Symbol.X), info.opponentName(),
                    parseSymbol(info.firstSymbol(), Symbol.X));
        } else {
            model.start(Symbol.X, "", Symbol.X);   // mở thẳng màn hình khi chưa có GAME_START
        }

        boardCanvas.setModel(model);
        boardCanvas.setOnCellClicked(this::sendMove);

        infoLabel.textProperty().bind(Bindings.createStringBinding(
                () -> "Bạn cầm " + model.mySymbol.get()
                        + (model.opponentName.get().isEmpty() ? "" : " - Đối thủ: " + model.opponentName.get()),
                model.mySymbol, model.opponentName));
        turnLabel.textProperty().bind(Bindings.createStringBinding(
                () -> model.isMyTurn() ? "Đến lượt bạn (" + model.currentTurn.get() + ")"
                        : "Lượt đối thủ (" + model.currentTurn.get() + ")",
                model.currentTurn, model.mySymbol));
        errorLabel.textProperty().bind(model.errorMessage);

        MessageBus.get().subscribe(Command.MOVED, onMoved);
        MessageBus.get().subscribe(Command.ERROR, onError);
        subscribed = true;

        // Hủy đăng ký khi màn hình bị thay (SceneManager đặt Scene mới → Scene cũ mất Window),
        // kể cả khi controller khác chuyển màn hình.
        boardCanvas.sceneProperty().addListener((obs, oldScene, scene) -> watchScene(scene));
        watchScene(boardCanvas.getScene());
    }

    private void watchScene(Scene scene) {
        if (scene == null) return;
        scene.windowProperty().addListener((obs, oldWindow, window) -> {
            if (oldWindow != null && window == null) dispose();
        });
    }

    private void sendMove(int row, int col) {
        ClientSession.get().send(new Message(Command.MOVE.name(), String.valueOf(row), String.valueOf(col)));
    }

    /** {@code MOVED|symbol|row|col|nextSymbol}. */
    private void handleMoved(Message msg) {
        try {
            if (msg.argCount() != 4) throw new IllegalArgumentException("cần 4 tham số");
            Symbol symbol = Symbol.valueOf(msg.arg(0));
            int row = Integer.parseInt(msg.arg(1));
            int col = Integer.parseInt(msg.arg(2));
            Symbol next = Symbol.valueOf(msg.arg(3));
            model.applyMove(symbol, row, col, next);
        } catch (IllegalArgumentException e) {   // gồm NumberFormatException
            System.err.println("[GameController] MOVED sai định dạng: " + msg.format() + " (" + e.getMessage() + ")");
        }
    }

    /** {@code ERROR|code|message}: hiện thông báo, bàn cờ giữ nguyên. */
    private void handleError(Message msg) {
        String code = msg.argCount() > 0 ? msg.arg(0) : "";
        String text = msg.argCount() > 1 && !msg.arg(1).isBlank() ? msg.arg(1) : code;
        model.errorMessage.set(text.isEmpty() ? "Lỗi không xác định" : text);
    }

    private void dispose() {
        if (!subscribed) return;
        MessageBus.get().unsubscribe(Command.MOVED, onMoved);
        MessageBus.get().unsubscribe(Command.ERROR, onError);
        subscribed = false;
    }

    private static Symbol parseSymbol(String s, Symbol fallback) {
        try {
            Symbol sym = Symbol.valueOf(s);
            return sym == Symbol.EMPTY ? fallback : sym;
        } catch (IllegalArgumentException | NullPointerException e) {
            return fallback;
        }
    }

    @FXML
    private void back() {
        dispose();
        SceneManager.get().show(Screen.CONNECT);
    }
}
