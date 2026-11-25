package vn.edu.vku.caro.client.view;

import org.junit.jupiter.api.Test;
import vn.edu.vku.caro.common.model.EndReason;
import vn.edu.vku.caro.common.model.GameResult;
import vn.edu.vku.caro.common.model.Symbol;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResultDialogTest {
    private static GameResult r(Symbol w, EndReason why) { return new GameResult(w, why, List.of()); }

    @Test
    void winnerSeesWinTitle() {
        assertEquals("Bạn thắng!", ResultDialog.describe(r(Symbol.X, EndReason.FIVE), Symbol.X).title());
    }

    @Test
    void loserSeesLoseTitle() {
        assertEquals("Bạn thua", ResultDialog.describe(r(Symbol.X, EndReason.FIVE), Symbol.O).title());
    }

    @Test
    void drawHasNoWinner() {
        ResultDialog.Summary s = ResultDialog.describe(r(null, EndReason.FULL), Symbol.O);
        assertEquals("Hòa", s.title());
        assertTrue(s.detail().contains("đầy"));
    }

    @Test
    void reasonTextDependsOnWhoWon() {
        assertTrue(ResultDialog.describe(r(Symbol.O, EndReason.SURRENDER), Symbol.O).detail().contains("Đối thủ đã đầu hàng"));
        assertTrue(ResultDialog.describe(r(Symbol.O, EndReason.SURRENDER), Symbol.X).detail().contains("Bạn đã đầu hàng"));
        assertTrue(ResultDialog.describe(r(Symbol.X, EndReason.LEFT), Symbol.X).detail().contains("rời"));
    }

    @Test
    void guestLobbyButtonSaysMainScreen() {
        assertEquals("Về màn hình chính", ResultDialog.lobbyLabel(true));
        assertEquals("Về sảnh", ResultDialog.lobbyLabel(false));
    }
}
