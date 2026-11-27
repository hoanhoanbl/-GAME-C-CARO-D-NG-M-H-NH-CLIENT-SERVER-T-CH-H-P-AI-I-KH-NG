package caro.common.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MessageTest {
    @Test
    void parseAndFormat() {
        Message m = Message.parse("MOVE|7|8");
        assertEquals("MOVE", m.command());
        assertEquals(2, m.argCount());
        assertEquals("7", m.arg(0));
        assertEquals("8", m.arg(1));
        assertEquals("MOVE|7|8", m.format());
    }

    @Test
    void roundTrip() {
        Message m = Message.parse(new Message("MOVE", "7", "8").format());
        assertEquals("MOVE", m.command());
        assertEquals(2, m.argCount());
        assertEquals("8", m.arg(1));
    }

    @Test
    void noArgs() {
        Message m = Message.parse("PING");
        assertEquals("PING", m.command());
        assertEquals(0, m.argCount());
        assertEquals("PING", new Message("PING").format());
    }

    @Test
    void trailingEmptyArgIsKept() {
        Message m = Message.parse("GAME_OVER|X|SURRENDER|");
        assertEquals(3, m.argCount());
        assertEquals("", m.arg(2));
        assertEquals("GAME_OVER|X|SURRENDER|", m.format());
    }

    @Test
    void emptyArgsInMiddle() {
        Message m = Message.parse("ROOM_LIST|");
        assertEquals(1, m.argCount());
        assertEquals("", m.arg(0));
        assertEquals(3, Message.parse("X|||").argCount());
    }

    @Test
    void vietnameseText() {
        String text = "Xin chào, đánh nhanh lên! Ồ ạt ữ";
        Message m = Message.parse(new Message("CHAT", text).format());
        assertEquals(text, m.arg(0));
    }

    @Test
    void pipeInArgIsReplaced() {
        Message m = new Message("CHAT", "a|b");
        assertEquals("CHAT|a/b", m.format());
        assertEquals(1, Message.parse(m.format()).argCount());
    }

    @Test
    void newlineInArgIsReplaced() {
        Message m = new Message("CHAT", "dòng1\ndòng2\r");
        assertFalse(m.format().contains("\n"));
        assertFalse(m.format().contains("\r"));
        assertEquals("dòng1 dòng2 ", m.arg(0));
    }

    @Test
    void nullArgBecomesEmpty() {
        assertEquals("X|", new Message("X", (String) null).format());
    }

    @Test
    void emptyCommandRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Message(""));
        assertThrows(IllegalArgumentException.class, () -> new Message(null));
    }

    @Test
    void nullOrEmptyLineRejected() {
        assertThrows(IllegalArgumentException.class, () -> Message.parse(null));
        assertThrows(IllegalArgumentException.class, () -> Message.parse(""));
    }

    @Test
    void exactlyMaxBytesAccepted() {
        String line = "CHAT|" + "a".repeat(Protocol.MAX_LINE_BYTES - 5);
        assertEquals(Protocol.MAX_LINE_BYTES, line.length());
        assertEquals("CHAT", Message.parse(line).command());
    }

    @Test
    void overMaxBytesWithVietnameseRejected() {
        // "đ" là 2 byte UTF-8: 5 + 2*510 = 1025 byte nhưng chỉ 515 ký tự
        String line = "CHAT|" + "đ".repeat(510);
        assertThrows(IllegalArgumentException.class, () -> Message.parse(line));
    }
}
