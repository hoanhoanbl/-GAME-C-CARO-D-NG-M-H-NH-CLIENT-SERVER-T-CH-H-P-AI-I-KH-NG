package caro.client.controller;

import caro.client.model.GameModel.Cell;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GameControllerTest {
    @Test
    void parseCellsReadsWinLine() {
        assertEquals(List.of(new Cell(7, 3), new Cell(8, 4), new Cell(9, 5)),
                GameController.parseCells("7,3;8,4;9,5"));
    }

    @Test
    void parseCellsEmpty() {
        assertTrue(GameController.parseCells("").isEmpty());
        assertTrue(GameController.parseCells(null).isEmpty());
    }

    @Test
    void parseCellsRejectsGarbage() {
        assertThrows(IllegalArgumentException.class, () -> GameController.parseCells("7;8"));
        assertThrows(IllegalArgumentException.class, () -> GameController.parseCells("a,b"));
    }
}
