package caro.server.messages;

import caro.common.model.EndReason;
import caro.common.model.GameResult;
import caro.common.model.Symbol;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameMessagesTest {

    @Test
    void gameStart() {
        assertEquals("GAME_START|R1|O|An|X", GameMessages.gameStart("R1", Symbol.O, "An", Symbol.X).format());
    }

    @Test
    void moved() {
        assertEquals("MOVED|X|7|8|O", GameMessages.moved(Symbol.X, 7, 8, Symbol.O).format());
    }

    @Test
    void gameOverFive() {
        GameResult r = GameResult.five(Symbol.X, List.of(new int[] {0, 0}, new int[] {1, 1}, new int[] {2, 2},
                new int[] {3, 3}, new int[] {4, 4}));
        assertEquals("GAME_OVER|X|FIVE|0,0;1,1;2,2;3,3;4,4", GameMessages.gameOver(r).format());
    }

    @Test
    void gameOverDrawAndNoCells() {
        assertEquals("GAME_OVER|DRAW|FULL|", GameMessages.gameOver(GameResult.draw()).format());
        assertEquals("GAME_OVER|O|LEFT|", GameMessages.gameOver(GameResult.win(Symbol.O, EndReason.LEFT)).format());
    }
}
