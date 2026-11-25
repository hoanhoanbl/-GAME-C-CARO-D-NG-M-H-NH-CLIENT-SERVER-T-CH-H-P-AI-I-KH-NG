package vn.edu.vku.caro.common.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MessageTest {
    @Test
    void parseAndFormat() {
        Message m = Message.parse("MOVE|7|8");
        assertEquals("MOVE", m.command());
        assertEquals("8", m.arg(1));
        assertEquals("MOVE|7|8", m.format());
    }
}
