package caro.client.controller;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import caro.client.app.Screen;
import caro.client.app.SceneManager;
import caro.client.model.ClientSession;
import caro.client.model.GameInfo;
import caro.client.model.GameModel;
import caro.client.view.BoardCanvas;
import caro.client.view.ResultDialog;
import caro.common.model.EndReason;
import caro.common.model.Symbol;
import caro.common.protocol.Command;
import caro.common.protocol.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Màn hình ván cờ: click ô → gửi {@code MOVE}; nhận {@code MOVED}, {@code ERROR}, {@code GAME_OVER}
 * → cập nhật {@link GameModel}. {@link BoardCanvas} và các nhãn tự cập nhật qua binding (R3);
 * hết ván thì mở {@link ResultDialog}.
 */
public class GameController {
    @FXML private BoardCanvas boardCanvas;
    @FXML private Label infoLabel;
    @FXML private Label turnLabel;
    @FXML private Label errorLabel;

    private final GameModel model = new GameModel();
    private final BusSubscriptions subscriptions = new BusSubscriptions();

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
                this::turnText, model.currentTurn, model.mySymbol, model.outcome));
        errorLabel.textProperty().bind(model.errorMessage);

        // Tự hủy đăng ký khi màn hình bị thay, kể cả khi controller khác chuyển màn hình.
        subscriptions.on(Command.MOVED, this::handleMoved)
                .on(Command.ERROR, this::handleError)
                .on(Command.GAME_OVER, this::handleGameOver)
                .bindTo(boardCanvas);
    }

    private String turnText() {
        GameModel.Outcome o = model.outcome.get();
        if (o != null) return "Kết thúc: " + ResultDialog.title(o.resultFor(model.mySymbol.get()));
        return model.isMyTurn() ? "Đến lượt bạn (" + model.currentTurn.get() + ")"
                : "Lượt đối thủ (" + model.currentTurn.get() + ")";
    }

    private void sendMove(int row, int col) {
        if (model.isOver()) return;   // server cũng sẽ trả NO_GAME, chặn sớm cho đỡ hiện lỗi thừa
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

    /** {@code GAME_OVER|winner|reason|cells}: winner = X/O/DRAW, cells = {@code r,c;r,c;…} (có thể rỗng). */
    private void handleGameOver(Message msg) {
        if (model.isOver()) return;   // đã nhận rồi, tránh mở hộp thoại hai lần
        try {
            if (msg.argCount() < 2) throw new IllegalArgumentException("cần ít nhất 2 tham số");
            String w = msg.arg(0);
            Symbol winner = "DRAW".equals(w) ? null : Symbol.valueOf(w);
            EndReason reason = EndReason.valueOf(msg.arg(1));
            List<GameModel.Cell> cells = parseCells(msg.argCount() > 2 ? msg.arg(2) : "");
            model.finish(winner, reason, cells);
        } catch (IllegalArgumentException e) {   // gồm NumberFormatException
            System.err.println("[GameController] GAME_OVER sai định dạng: " + msg.format() + " (" + e.getMessage() + ")");
            return;
        }

        GameModel.Outcome o = model.outcome.get();
        ResultDialog.show(boardCanvas.getScene() == null ? null : boardCanvas.getScene().getWindow(),
                o.resultFor(model.mySymbol.get()), o.reason(), ClientSession.get().isGuest(), this::onResultChoice);
    }

    /**
     * Chơi lại → gửi {@code REMATCH}; rời đi → gửi {@code BACK_TO_LOBBY} (server trả {@code ROOM_LIST}
     * hoặc {@code BACK_TO_LOGIN} rồi màn hình tương ứng tự chuyển); Xem bàn cờ → giữ nguyên.
     */
    private void onResultChoice(ResultDialog.Choice choice) {
        switch (choice) {
            case REMATCH -> ClientSession.get().send(new Message(Command.REMATCH.name()));
            case LEAVE -> ClientSession.get().send(new Message(Command.BACK_TO_LOBBY.name()));
            case CLOSE -> { }
        }
    }

    /** {@code "7,3;7,4"} → [(7,3), (7,4)]; chuỗi rỗng → danh sách rỗng. */
    static List<GameModel.Cell> parseCells(String s) {
        List<GameModel.Cell> cells = new ArrayList<>();
        if (s == null || s.isBlank()) return cells;
        for (String part : s.split(";")) {
            String[] rc = part.split(",");
            if (rc.length != 2) throw new IllegalArgumentException("ô sai định dạng: " + part);
            cells.add(new GameModel.Cell(Integer.parseInt(rc[0].trim()), Integer.parseInt(rc[1].trim())));
        }
        return cells;
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
        subscriptions.disposeAll();
        SceneManager.get().show(Screen.CONNECT);
    }
}
