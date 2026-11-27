package caro.client.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import caro.common.model.Board;
import caro.common.model.Symbol;

/**
 * Trạng thái ván cờ phía client. Chỉ phản ánh những gì server báo về ({@code GAME_START},
 * {@code MOVED}, {@code ERROR}); không tự kiểm tra luật hay kết luận thắng thua (R7).
 * View lắng nghe các property để tự vẽ lại (R3).
 */
public class GameModel {
    /** Một ô trên bàn. */
    public record Cell(int row, int col) { }

    public final Board board = new Board();
    public final ObjectProperty<Symbol> currentTurn = new SimpleObjectProperty<>(Symbol.X);
    public final ObjectProperty<Symbol> mySymbol = new SimpleObjectProperty<>(Symbol.X);
    public final StringProperty opponentName = new SimpleStringProperty("");
    /** Nước vừa đi, null khi chưa có nước nào. */
    public final ObjectProperty<Cell> lastMove = new SimpleObjectProperty<>();
    /** Thông báo lỗi gần nhất từ server, rỗng nếu không có. */
    public final StringProperty errorMessage = new SimpleStringProperty("");

    /** Tăng sau mỗi thay đổi trên {@link #board} để View biết cần vẽ lại (Board không phải property). */
    private final ReadOnlyIntegerWrapper boardVersion = new ReadOnlyIntegerWrapper(0);

    public ReadOnlyIntegerProperty boardVersionProperty() { return boardVersion.getReadOnlyProperty(); }

    /** Bắt đầu ván mới theo {@code GAME_START}: xóa bàn, đặt quân của mình và lượt đi đầu. */
    public void start(Symbol mine, String opponent, Symbol first) {
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) board.place(r, c, Symbol.EMPTY);
        }
        mySymbol.set(mine);
        opponentName.set(opponent == null ? "" : opponent);
        currentTurn.set(first);
        lastMove.set(null);
        errorMessage.set("");
        boardVersion.set(boardVersion.get() + 1);
    }

    /**
     * Ghi nước đi server đã chấp nhận ({@code MOVED|symbol|row|col|next}).
     *
     * @throws IllegalArgumentException nếu ô nằm ngoài bàn hoặc symbol là null/EMPTY
     */
    public void applyMove(Symbol symbol, int row, int col, Symbol next) {
        if (symbol == null || symbol == Symbol.EMPTY) {
            throw new IllegalArgumentException("symbol phải là X hoặc O");
        }
        board.place(row, col, symbol);
        lastMove.set(new Cell(row, col));
        currentTurn.set(next);
        errorMessage.set("");
        boardVersion.set(boardVersion.get() + 1);
    }

    public boolean isMyTurn() { return currentTurn.get() == mySymbol.get(); }
}
