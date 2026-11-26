package caro.client.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GameModelTest {
    @Test
    void startsWithX() {
        assertEquals(caro.common.model.Symbol.X, new GameModel().currentTurn.get());
    }
}
