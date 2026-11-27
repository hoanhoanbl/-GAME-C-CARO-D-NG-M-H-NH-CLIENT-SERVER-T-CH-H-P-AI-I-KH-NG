package caro.client.view;

import caro.client.model.GameModel.Result;
import caro.common.model.EndReason;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResultDialogTest {
    @Test
    void titles() {
        assertEquals("Bạn thắng!", ResultDialog.title(Result.WIN));
        assertEquals("Bạn thua", ResultDialog.title(Result.LOSE));
        assertEquals("Hòa", ResultDialog.title(Result.DRAW));
    }

    @Test
    void reasonFromOwnPointOfView() {
        assertEquals("Đối thủ đã đầu hàng.", ResultDialog.reasonText(Result.WIN, EndReason.SURRENDER));
        assertEquals("Bạn đã đầu hàng.", ResultDialog.reasonText(Result.LOSE, EndReason.SURRENDER));
        assertEquals("Đối thủ đã thoát khỏi ván.", ResultDialog.reasonText(Result.WIN, EndReason.LEFT));
        assertEquals("Đối thủ có 5 quân liên tiếp.", ResultDialog.reasonText(Result.LOSE, EndReason.FIVE));
        assertEquals("Bàn cờ đã kín, không ai có 5 quân.", ResultDialog.reasonText(Result.DRAW, EndReason.FULL));
    }
}
