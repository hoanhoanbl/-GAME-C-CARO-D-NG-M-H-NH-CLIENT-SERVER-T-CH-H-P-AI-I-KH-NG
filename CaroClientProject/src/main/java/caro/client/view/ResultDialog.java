package caro.client.view;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Window;
import caro.client.model.GameModel.Result;
import caro.common.model.EndReason;

import java.util.function.Consumer;

/**
 * Hộp thoại kết quả ván: Thắng/Thua/Hòa + lý do, nút Chơi lại / Về sảnh (khách: Về màn hình chính)
 * / Xem bàn cờ. Chỉ hiển thị và báo nút được chọn qua callback; không gửi mạng (R2).
 */
public final class ResultDialog {
    /** Nút người chơi bấm. {@link #CLOSE} cũng dùng khi đóng hộp thoại bằng nút X của cửa sổ. */
    public enum Choice { REMATCH, LEAVE, CLOSE }

    private ResultDialog() { }

    /**
     * Mở hộp thoại không chặn (không dùng showAndWait) để bàn cờ kịp vẽ đường thắng phía sau
     * và MessageBus không bị giữ lại giữa chừng.
     *
     * @param owner    cửa sổ cha, có thể null
     * @param guest    true nếu là khách: nút rời đi ghi "Về màn hình chính"
     * @param onChoice nhận nút được chọn đúng một lần
     */
    public static void show(Window owner, Result result, EndReason reason, boolean guest, Consumer<Choice> onChoice) {
        ButtonType rematch = new ButtonType("Chơi lại", ButtonBar.ButtonData.YES);
        ButtonType leave = new ButtonType(guest ? "Về màn hình chính" : "Về sảnh", ButtonBar.ButtonData.NO);
        ButtonType close = new ButtonType("Xem bàn cờ", ButtonBar.ButtonData.CANCEL_CLOSE);

        Dialog<Choice> dialog = new Dialog<>();
        if (owner != null) dialog.initOwner(owner);
        dialog.setTitle("Kết thúc ván");
        dialog.setHeaderText(title(result));
        dialog.setContentText(reasonText(result, reason));
        dialog.getDialogPane().getButtonTypes().setAll(rematch, leave, close);
        dialog.setResultConverter(bt -> bt == rematch ? Choice.REMATCH : bt == leave ? Choice.LEAVE : Choice.CLOSE);
        dialog.setOnHidden(e -> onChoice.accept(dialog.getResult() == null ? Choice.CLOSE : dialog.getResult()));
        dialog.show();
    }

    /** Dòng tiêu đề lớn của hộp thoại. */
    public static String title(Result result) {
        return switch (result) {
            case WIN -> "Bạn thắng!";
            case LOSE -> "Bạn thua";
            case DRAW -> "Hòa";
        };
    }

    /** Lý do kết thúc, viết theo góc nhìn của mình. */
    public static String reasonText(Result result, EndReason reason) {
        boolean win = result == Result.WIN;
        return switch (reason) {
            case FIVE -> win ? "Bạn có 5 quân liên tiếp." : "Đối thủ có 5 quân liên tiếp.";
            case SURRENDER -> win ? "Đối thủ đã đầu hàng." : "Bạn đã đầu hàng.";
            case LEFT -> win ? "Đối thủ đã thoát khỏi ván." : "Bạn đã rời ván.";
            case FULL -> "Bàn cờ đã kín, không ai có 5 quân.";
        };
    }
}
