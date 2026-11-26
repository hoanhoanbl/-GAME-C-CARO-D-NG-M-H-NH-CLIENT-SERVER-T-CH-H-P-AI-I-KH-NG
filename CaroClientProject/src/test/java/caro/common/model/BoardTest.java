package caro.common.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BoardTest {
    @Test
    void newBoardIsEmpty() {
        assertTrue(new Board().isEmpty(7, 7));
    }
}
