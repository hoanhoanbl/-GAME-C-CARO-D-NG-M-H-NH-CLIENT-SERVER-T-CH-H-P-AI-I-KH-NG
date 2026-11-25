package vn.edu.vku.caro.client.view;

import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Window;
import vn.edu.vku.caro.common.model.GameResult;
import vn.edu.vku.caro.common.model.Symbol;

import java.util.function.Consumer;

/**
 * Hộp thoại kết quả ván: Thắng/Thua/Hòa kèm lý do và ba lựa chọn. Chỉ hiển thị theo GameResult
 * server báo (R7), không tự kết luận thắng thua và không gửi mạng (R2): lựa chọn trả về cho Controller.
 */
public class ResultDialog {
    public enum Choice { REMATCH, LOBBY, STAY }

    /** Nội dung hiển thị, tách riêng để kiểm thử không cần giao diện. */
    public record Summary(String title, String detail) { }

    private ResultDialog() { }

    public static Summary describe(GameResult result, Symbol mine) {
        Symbol winner = result.winner();
        boolean iWon = winner != null && winner == mine;
        String title = winner == null ? "Hòa" : iWon ? "Bạn thắng!" : "Bạn thua";
        String detail = switch (result.reason()) {
            case FIVE -> iWon ? "Bạn có 5 quân liên tiếp." : "Đối thủ có 5 quân liên tiếp.";
            case SURRENDER -> iWon ? "Đối thủ đã đầu hàng." : "Bạn đã đầu hàng.";
            case LEFT -> iWon ? "Đối thủ đã rời ván." : "Bạn đã rời ván.";
            case FULL -> "Bàn cờ đã đầy, không ai thắng.";
        };
        return new Summary(title, detail);
    }

    /** Nhãn nút về lại: khách không có sảnh nên về màn hình chính. */
    public static String lobbyLabel(boolean guest) { return guest ? "Về màn hình chính" : "Về sảnh"; }

    /** Hiện hộp thoại (không chặn); gọi onChoice đúng một lần khi người chơi chọn hoặc đóng hộp thoại. */
    public static void show(Window owner, GameResult result, Symbol mine, boolean guest, Consumer<Choice> onChoice) {
        Summary s = describe(result, mine);
        ButtonType rematch = new ButtonType("Chơi lại", ButtonData.OK_DONE);
        ButtonType lobby = new ButtonType(lobbyLabel(guest), ButtonData.OTHER);
        ButtonType stay = new ButtonType("Xem bàn cờ", ButtonData.CANCEL_CLOSE);

        Dialog<Choice> dialog = new Dialog<>();
        if (owner != null) dialog.initOwner(owner);
        dialog.setTitle("Kết quả ván cờ");
        dialog.setHeaderText(s.title());
        dialog.setContentText(s.detail());
        dialog.getDialogPane().getButtonTypes().addAll(rematch, lobby, stay);
        dialog.setResultConverter(b -> b == rematch ? Choice.REMATCH : b == lobby ? Choice.LOBBY : Choice.STAY);
        dialog.setOnHidden(e -> {
            Choice c = dialog.getResult();
            onChoice.accept(c == null ? Choice.STAY : c);
        });
        dialog.show();
    }
}
