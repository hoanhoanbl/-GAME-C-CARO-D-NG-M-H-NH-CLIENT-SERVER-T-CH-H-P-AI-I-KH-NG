package caro.server.net;

import caro.common.protocol.Protocol;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * Đọc từng dòng nhưng không bao giờ giữ quá {@link Protocol#MAX_LINE_BYTES} của một dòng trong bộ nhớ
 * (thay cho {@code BufferedReader.readLine()} vốn không giới hạn). Dòng quá dài được bỏ tới hết {@code \n}
 * và trả về dạng {@link Line#tooLong()} để server báo {@code BAD_MESSAGE} rồi đọc tiếp.
 */
public class LineReader {
    /** Một dòng đọc được; {@code text} = null khi {@code tooLong}. */
    public record Line(String text, boolean tooLong) { }

    private final Reader in;
    private final int maxBytes;

    public LineReader(Reader in) { this(in, Protocol.MAX_LINE_BYTES); }

    public LineReader(Reader in, int maxBytes) {
        this.in = in;
        this.maxBytes = maxBytes;
    }

    /**
     * @return dòng tiếp theo (đã bỏ {@code \r} cuối), hoặc null khi hết luồng mà không đọc được ký tự nào
     */
    public Line readLine() throws IOException {
        // Số ký tự luôn ≤ số byte UTF-8, nên giữ tối đa maxBytes + 1 ký tự (chừa chỗ cho '\r') là đủ để quyết định.
        StringBuilder sb = new StringBuilder();
        boolean overflow = false;
        boolean any = false;
        int ch;
        while ((ch = in.read()) != -1) {
            any = true;
            if (ch == '\n') break;
            if (overflow) continue;
            if (sb.length() > maxBytes) overflow = true;   // đã có maxBytes + 1 ký tự và còn thêm
            else sb.append((char) ch);
        }
        if (!any) return null;
        if (overflow) return new Line(null, true);

        int n = sb.length();
        if (n > 0 && sb.charAt(n - 1) == '\r') sb.setLength(n - 1);
        String text = sb.toString();
        if (text.getBytes(StandardCharsets.UTF_8).length > maxBytes) return new Line(null, true);
        return new Line(text, false);
    }
}
