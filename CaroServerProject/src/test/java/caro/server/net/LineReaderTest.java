package caro.server.net;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

class LineReaderTest {

    private static LineReader reader(String s) { return new LineReader(new StringReader(s), 1024); }

    @Test
    void normalLines() throws IOException {
        LineReader r = reader("PING\nMOVE|7|8\n");
        assertEquals("PING", r.readLine().text());
        assertEquals("MOVE|7|8", r.readLine().text());
        assertNull(r.readLine());
    }

    @Test
    void crlfIsStripped() throws IOException {
        LineReader.Line l = reader("PING\r\n").readLine();
        assertFalse(l.tooLong());
        assertEquals("PING", l.text());
    }

    @Test
    void exactlyMaxBytesAccepted() throws IOException {
        String line = "a".repeat(1024);
        LineReader.Line l = reader(line + "\r\n").readLine();
        assertFalse(l.tooLong());
        assertEquals(line, l.text());
    }

    @Test
    void overMaxBytesRejected() throws IOException {
        assertTrue(reader("a".repeat(1025) + "\n").readLine().tooLong());
    }

    @Test
    void multiByteOverMaxRejected() throws IOException {
        // 513 ký tự "đ" = 1026 byte UTF-8 nhưng chỉ 513 ký tự
        assertTrue(reader("đ".repeat(513) + "\n").readLine().tooLong());
        assertFalse(reader("đ".repeat(512) + "\n").readLine().tooLong());
    }

    @Test
    void veryLongLineThenValidLine() throws IOException {
        LineReader r = reader("x".repeat(5000) + "\nPING\n");
        assertTrue(r.readLine().tooLong());
        assertEquals("PING", r.readLine().text());
        assertNull(r.readLine());
    }

    @Test
    void eofMidLineReturnsPartial() throws IOException {
        LineReader r = reader("PI");
        assertEquals("PI", r.readLine().text());
        assertNull(r.readLine());
    }

    @Test
    void emptyStreamReturnsNull() throws IOException {
        assertNull(reader("").readLine());
    }
}
