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
import vn.edu.vku.caro.client.view.BoardCanvas;
import vn.edu.vku.caro.common.model.Symbol;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Khung màn hình ván cờ (1.H.4). Chưa nối mạng: nếu chưa có GameInfo từ server thì chạy chế độ thử
 * với dữ liệu giả, hai bên luân phiên đi trên cùng máy. Nối MessageBus ở 2.H.2.
 */
public class GameController implements Initializable {
    @FXML private BoardCanvas boardCanvas;
    @FXML private Label turnLabel;
    @FXML private Label infoLabel;

    private final GameModel model = new GameModel();
    private boolean demo;

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

        boardCanvas.setOnCellClicked(this::onCellClicked);
    }

    private void onCellClicked(int row, int col) {
        if (model.finished.get() || !model.board.isEmpty(row, col)) return;
        if (demo) {
            Symbol me = model.currentTurn.get();
            model.applyMoved(me, row, col, me == Symbol.X ? Symbol.O : Symbol.X);
            return;
        }
        // TODO 2.H.1/2.H.2: gửi MOVE|row|col qua ClientSession, chờ MOVED từ server.
    }

    @FXML
    private void back() { SceneManager.get().show(Screen.CONNECT); }
}
