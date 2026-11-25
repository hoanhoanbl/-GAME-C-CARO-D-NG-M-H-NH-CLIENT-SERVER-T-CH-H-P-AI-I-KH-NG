package vn.edu.vku.caro.client.controller;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import vn.edu.vku.caro.client.app.SceneManager;
import vn.edu.vku.caro.client.app.Screen;
import vn.edu.vku.caro.client.model.ClientSession;
import vn.edu.vku.caro.client.model.GameInfo;
import vn.edu.vku.caro.client.model.GameModel;
import vn.edu.vku.caro.client.net.MessageBus;
import vn.edu.vku.caro.client.view.BoardCanvas;
import vn.edu.vku.caro.common.model.Symbol;
import vn.edu.vku.caro.common.protocol.Command;
import vn.edu.vku.caro.common.protocol.Message;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Màn hình ván cờ. Nhận MOVED, GAME_OVER, ERROR từ MessageBus và cập nhật {@link GameModel}; View tự vẽ lại
 * qua binding (R3). Click ô → gửi MOVE|row|col, không tự đặt quân: chờ server báo MOVED (R7).
 * Nếu chưa có GameInfo (mở thẳng bằng -Dcaro.screen=GAME) thì chạy chế độ thử, hai bên luân phiên trên một máy.
 */
public class GameController implements Initializable {
    @FXML private BoardCanvas boardCanvas;
    @FXML private Label turnLabel;
    @FXML private Label infoLabel;
    @FXML private Label statusLabel;

    private final GameModel model = new GameModel();
    private boolean demo;

    private final Consumer<Message> onMoved = m -> handle(m, model::applyMoved);
    private final Consumer<Message> onGameOver = m -> handle(m, model::applyGameOver);
    private final Consumer<Message> onError = m -> handle(m, model::applyError);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        GameInfo info = ClientSession.get().currentGame();
        demo = info == null;
        if (demo) {
            infoLabel.setText("Chế độ thử (chưa nối mạng)");
        } else {
            model.mySymbol.set(Symbol.valueOf(info.mySymbol()));
            model.currentTurn.set(Symbol.valueOf(info.firstSymbol()));
            model.opponentName.set(info.opponentName());
            infoLabel.setText("Đối thủ: " + info.opponentName() + " · Bạn là " + info.mySymbol());
        }

        boardCanvas.setModel(model);
        turnLabel.textProperty().bind(Bindings.createStringBinding(() -> {
            if (model.finished.get()) return "Ván đã kết thúc";
            if (demo) return "Lượt của " + model.currentTurn.get();
            return model.isMyTurn() ? "Đến lượt bạn" : "Đang chờ đối thủ";
        }, model.currentTurn, model.finished, model.mySymbol));

        statusLabel.textProperty().bind(model.statusMessage);

        boardCanvas.setOnCellClicked(this::onCellClicked);
        if (!demo) subscribeWhileShown();
    }

    /** Đăng ký nhận thông điệp; tự hủy khi cửa sổ chuyển sang màn hình khác để không xử lý trùng. */
    private void subscribeWhileShown() {
        MessageBus bus = MessageBus.get();
        bus.subscribe(Command.MOVED, onMoved);
        bus.subscribe(Command.GAME_OVER, onGameOver);
        bus.subscribe(Command.ERROR, onError);
        boardCanvas.sceneProperty().addListener((o, oldScene, scene) -> {
            if (scene != null) {
                scene.windowProperty().addListener((o2, oldWin, win) -> {
                    if (oldWin != null && win == null) unsubscribe();
                });
            }
        });
    }

    private void unsubscribe() {
        MessageBus bus = MessageBus.get();
        bus.unsubscribe(Command.MOVED, onMoved);
        bus.unsubscribe(Command.GAME_OVER, onGameOver);
        bus.unsubscribe(Command.ERROR, onError);
    }

    /** Thông điệp sai định dạng không được làm hỏng màn hình: báo ra statusMessage. */
    private void handle(Message m, Consumer<Message> apply) {
        try {
            apply.accept(m);
        } catch (RuntimeException e) {
            model.statusMessage.set("Thông điệp từ server không hợp lệ: " + m.format());
        }
    }

    private void onCellClicked(int row, int col) {
        if (model.finished.get() || !model.board.isEmpty(row, col)) return;
        if (demo) {
            Symbol me = model.currentTurn.get();
            model.applyMoved(me, row, col, me == Symbol.X ? Symbol.O : Symbol.X);
            return;
        }
        if (!model.isMyTurn()) {
            model.statusMessage.set("Chưa đến lượt bạn");
            return;
        }
        ClientSession.get().send(new Message("MOVE", String.valueOf(row), String.valueOf(col)));
    }

    @FXML
    private void back() { SceneManager.get().show(Screen.CONNECT); }
}
