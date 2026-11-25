package vn.edu.vku.caro.client.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import vn.edu.vku.caro.client.model.GameModel;
import vn.edu.vku.caro.common.model.Board;
import vn.edu.vku.caro.common.model.Symbol;

import java.util.function.BiConsumer;

/** Chỉ vẽ theo GameModel và báo ô được bấm; không chứa luật cờ, không gửi mạng (R2). */
public class BoardCanvas extends Canvas {
    private static final double CELL = 40;
    private static final double PAD = 10;

    private GameModel model;
    private BiConsumer<Integer, Integer> onCellClicked;

    public BoardCanvas() {
        super(PAD * 2 + CELL * Board.SIZE, PAD * 2 + CELL * Board.SIZE);
        setOnMouseClicked(e -> {
            int col = (int) Math.floor((e.getX() - PAD) / CELL);
            int row = (int) Math.floor((e.getY() - PAD) / CELL);
            if (Board.inBounds(row, col) && onCellClicked != null) onCellClicked.accept(row, col);
        });
        redraw();
    }

    public void setModel(GameModel model) {
        this.model = model;
        model.revision.addListener((o, a, b) -> redraw());
        redraw();
    }

    public void setOnCellClicked(BiConsumer<Integer, Integer> handler) { this.onCellClicked = handler; }

    public void redraw() {
        GraphicsContext g = getGraphicsContext2D();
        g.setFill(Color.web("#f3deb0"));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setStroke(Color.web("#7a5c2e"));
        g.setLineWidth(1);
        for (int i = 0; i < Board.SIZE; i++) {
            double p = PAD + CELL / 2 + i * CELL;
            g.strokeLine(PAD + CELL / 2, p, PAD + CELL / 2 + (Board.SIZE - 1) * CELL, p);
            g.strokeLine(p, PAD + CELL / 2, p, PAD + CELL / 2 + (Board.SIZE - 1) * CELL);
        }
        if (model == null) return;

        for (int[] c : model.winLine()) {
            g.setFill(Color.web("#ffe066", 0.7));
            g.fillRect(PAD + c[1] * CELL, PAD + c[0] * CELL, CELL, CELL);
        }
        drawWinLine(g);
        int[] last = model.lastMove();
        if (last != null) {
            g.setStroke(Color.web("#e03131"));
            g.setLineWidth(2);
            g.strokeRect(PAD + last[1] * CELL + 2, PAD + last[0] * CELL + 2, CELL - 4, CELL - 4);
        }
        g.setFont(Font.font("System", 28));
        g.setTextAlign(TextAlignment.CENTER);
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                Symbol s = model.board.get(r, c);
                if (s == Symbol.EMPTY) continue;
                g.setFill(s == Symbol.X ? Color.web("#1c7ed6") : Color.web("#c92a2a"));
                g.fillText(s.name(), PAD + c * CELL + CELL / 2, PAD + r * CELL + CELL / 2 + 10);
            }
        }
    }

    /** Gạch một đường đậm xuyên qua các ô thắng, từ ô đầu tới ô cuối. */
    private void drawWinLine(GraphicsContext g) {
        java.util.List<int[]> line = model.winLine();
        if (line.size() < 2) return;
        int[] a = line.get(0);
        int[] b = line.get(line.size() - 1);
        g.setStroke(Color.web("#f08c00"));
        g.setLineWidth(5);
        g.setLineCap(StrokeLineCap.ROUND);
        g.strokeLine(PAD + a[1] * CELL + CELL / 2, PAD + a[0] * CELL + CELL / 2,
                PAD + b[1] * CELL + CELL / 2, PAD + b[0] * CELL + CELL / 2);
    }
}
