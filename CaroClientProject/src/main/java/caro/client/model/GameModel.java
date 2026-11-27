package caro.client.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import caro.common.model.Board;
import caro.common.model.EndReason;
import caro.common.model.Symbol;

import java.util.List;

/**
 * Trạng thái ván cờ phía client. Chỉ phản ánh những gì server báo về ({@code GAME_START},
 * {@code MOVED}, {@code ERROR}, {@code GAME_OVER}); không tự kiểm tra luật hay kết luận thắng thua (R7).
 * View lắng nghe các property để tự vẽ lại (R3).
 */
public class GameModel {
    /** Một ô trên bàn. */
    public record Cell(int row, int col) { }

    /** Kết quả của mình so với người thắng server báo về. */
    public enum Result { WIN, LOSE, DRAW }

    /**
     * Ván đã kết thúc theo {@code GAME_OVER}. {@code winner} = null nếu hòa;
     * {@code winLine} là các ô của đường thắng (rỗng nếu không thắng bằng 5 quân).
     */
    public record Outcome(Symbol winner, EndReason reason, List<Cell> winLine) {
        public Outcome {
            if (reason == null) throw new IllegalArgumentException("reason không được null");
            if (winner == Symbol.EMPTY) throw new IllegalArgumentException("winner phải là X, O hoặc null (hòa)");
            winLine = winLine == null ? List.of() : List.copyOf(winLine);
        }

        public boolean isDraw() { return winner == null; }

        /** Thắng/Thua/Hòa nhìn từ phía người cầm {@code me}. */
        public Result resultFor(Symbol me) {
            if (isDraw()) return Result.DRAW;
            return winner == me ? Result.WIN : Result.LOSE;
        }
    }

    public final Board board = new Board();
    public final ObjectProperty<Symbol> currentTurn = new SimpleObjectProperty<>(Symbol.X);
    public final ObjectProperty<Symbol> mySymbol = new SimpleObjectProperty<>(Symbol.X);
    public final StringProperty opponentName = new SimpleStringProperty("");
    /** Nước vừa đi, null khi chưa có nước nào. */
    public final ObjectProperty<Cell> lastMove = new SimpleObjectProperty<>();
    /** Thông báo lỗi gần nhất từ server, rỗng nếu không có. */
    public final StringProperty errorMessage = new SimpleStringProperty("");
    /** Kết quả ván, null khi ván đang diễn ra. */
    public final ObjectProperty<Outcome> outcome = new SimpleObjectProperty<>();

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
        outcome.set(null);
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

    /**
     * Kết thúc ván theo {@code GAME_OVER|winner|reason|cells}: ghi kết quả và đường thắng, xóa lỗi.
     *
     * @throws IllegalArgumentException nếu {@code reason} null, winner là EMPTY hoặc ô đường thắng ngoài bàn
     */
    public void finish(Symbol winner, EndReason reason, List<Cell> winLine) {
        Outcome o = new Outcome(winner, reason, winLine);
        for (Cell c : o.winLine()) {
            if (!Board.isInside(c.row(), c.col())) {
                throw new IllegalArgumentException("Ô (" + c.row() + ", " + c.col() + ") nằm ngoài bàn");
            }
        }
        errorMessage.set("");
        outcome.set(o);
    }

    public boolean isOver() { return outcome.get() != null; }

    /** Chỉ còn ý nghĩa khi ván chưa kết thúc. */
    public boolean isMyTurn() { return !isOver() && currentTurn.get() == mySymbol.get(); }
}
