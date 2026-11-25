package vn.edu.vku.caro.client.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import vn.edu.vku.caro.common.model.Board;
import vn.edu.vku.caro.common.model.Symbol;

import java.util.ArrayList;
import java.util.List;

/**
 * Trạng thái ván cờ phía client. Chỉ ghi lại những gì server báo (R7): không tự kiểm tra luật thắng.
 * {@link #revision} tăng mỗi khi bàn đổi để View vẽ lại.
 */
public class GameModel {
    public final Board board = new Board();
    public final ObjectProperty<Symbol> currentTurn = new SimpleObjectProperty<>(Symbol.X);
    public final ObjectProperty<Symbol> mySymbol = new SimpleObjectProperty<>(Symbol.X);
    public final StringProperty opponentName = new SimpleStringProperty("");
    public final BooleanProperty finished = new SimpleBooleanProperty(false);
    public final IntegerProperty revision = new SimpleIntegerProperty(0);

    private int[] lastMove;
    private List<int[]> winLine = List.of();

    public boolean isMyTurn() { return !finished.get() && currentTurn.get() == mySymbol.get(); }

    public int[] lastMove() { return lastMove; }
    public List<int[]> winLine() { return winLine; }

    /** Áp dụng MOVED|symbol|row|col|next. next rỗng nếu ván đã kết thúc. */
    public void applyMoved(Symbol symbol, int row, int col, Symbol next) {
        board.place(row, col, symbol);
        lastMove = new int[] { row, col };
        if (next != null && next != Symbol.EMPTY) currentTurn.set(next);
        bump();
    }

    /** Áp dụng GAME_OVER: cells dạng "r,c;r,c;…", có thể rỗng. */
    public void applyGameOver(String cells) {
        winLine = parseCells(cells);
        finished.set(true);
        bump();
    }

    public static List<int[]> parseCells(String cells) {
        List<int[]> out = new ArrayList<>();
        if (cells == null || cells.isBlank()) return out;
        for (String part : cells.split(";")) {
            String[] rc = part.split(",");
            out.add(new int[] { Integer.parseInt(rc[0].trim()), Integer.parseInt(rc[1].trim()) });
        }
        return out;
    }

    private void bump() { revision.set(revision.get() + 1); }
}
