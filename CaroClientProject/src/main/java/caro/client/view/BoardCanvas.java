package caro.client.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import caro.client.model.GameModel;
import caro.common.model.Board;
import caro.common.model.Symbol;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Vẽ bàn cờ 15x15 theo {@link GameModel} (R2: không gửi mạng, không chứa luật cờ).
 * Tự vẽ lại khi bàn, nước cuối hoặc kết quả ván thay đổi (tô đường thắng theo {@code GAME_OVER});
 * báo ô được click qua {@link #setOnCellClicked}.
 */
public class BoardCanvas extends Canvas {
    public static final double CELL = 38;
    private static final double PAD = 4;

    private static final Color BG = Color.web("#f7e7c4");
    private static final Color GRID = Color.web("#8b6f47");
    private static final Color LAST_MOVE = Color.web("#fff3a0");
    private static final Color X_COLOR = Color.web("#c53030");
    private static final Color O_COLOR = Color.web("#2b6cb0");
    private static final Color WIN_CELL = Color.web("#9ae6b4");
    private static final Color WIN_LINE = Color.web("#2f855a", 0.85);

    private GameModel model;
    private BiConsumer<Integer, Integer> onCellClicked;

    public BoardCanvas() {
        super(Board.SIZE * CELL + 2 * PAD, Board.SIZE * CELL + 2 * PAD);
        setOnMouseClicked(e -> {
            if (onCellClicked == null) return;
            int col = (int) Math.floor((e.getX() - PAD) / CELL);
            int row = (int) Math.floor((e.getY() - PAD) / CELL);
            if (Board.isInside(row, col)) onCellClicked.accept(row, col);
        });
        draw();
    }

    public void setModel(GameModel model) {
        this.model = model;
        model.boardVersionProperty().addListener((obs, o, n) -> draw());
        model.lastMove.addListener((obs, o, n) -> draw());
        model.outcome.addListener((obs, o, n) -> draw());
        draw();
    }

    /** Nhận (row, col) của ô vừa click. */
    public void setOnCellClicked(BiConsumer<Integer, Integer> handler) { this.onCellClicked = handler; }

    private void draw() {
        GraphicsContext g = getGraphicsContext2D();
        g.setFill(BG);
        g.fillRect(0, 0, getWidth(), getHeight());

        if (model != null && model.lastMove.get() != null) {
            GameModel.Cell last = model.lastMove.get();
            g.setFill(LAST_MOVE);
            g.fillRect(PAD + last.col() * CELL, PAD + last.row() * CELL, CELL, CELL);
        }

        List<GameModel.Cell> winLine = model == null || model.outcome.get() == null
                ? List.of() : model.outcome.get().winLine();
        g.setFill(WIN_CELL);
        for (GameModel.Cell c : winLine) {
            g.fillRect(PAD + c.col() * CELL, PAD + c.row() * CELL, CELL, CELL);
        }

        g.setStroke(GRID);
        g.setLineWidth(1);
        for (int i = 0; i <= Board.SIZE; i++) {
            double p = PAD + i * CELL;
            g.strokeLine(PAD, p, PAD + Board.SIZE * CELL, p);
            g.strokeLine(p, PAD, p, PAD + Board.SIZE * CELL);
        }

        if (model == null) return;
        g.setLineWidth(3);
        double m = CELL * 0.22;
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                Symbol s = model.board.get(r, c);
                double x = PAD + c * CELL, y = PAD + r * CELL;
                if (s == Symbol.X) {
                    g.setStroke(X_COLOR);
                    g.strokeLine(x + m, y + m, x + CELL - m, y + CELL - m);
                    g.strokeLine(x + CELL - m, y + m, x + m, y + CELL - m);
                } else if (s == Symbol.O) {
                    g.setStroke(O_COLOR);
                    g.strokeOval(x + m, y + m, CELL - 2 * m, CELL - 2 * m);
                }
            }
        }

        if (winLine.size() >= 2) {
            // Server gửi các ô theo thứ tự dọc đường thắng → nối ô đầu với ô cuối.
            GameModel.Cell a = winLine.get(0), b = winLine.get(winLine.size() - 1);
            g.setStroke(WIN_LINE);
            g.setLineWidth(5);
            g.strokeLine(centerOf(a.col()), centerOf(a.row()), centerOf(b.col()), centerOf(b.row()));
        }
    }

    private static double centerOf(int index) { return PAD + index * CELL + CELL / 2; }
}
