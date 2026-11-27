package caro.common.protocol;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Một thông điệp giao thức dạng {@code LỆNH|tham_số_1|…} (mục 10.1).
 *
 * <p>Tham số được làm sạch ngay khi tạo: {@code |} → {@code /}, {@code \r}/{@code \n} → khoảng trắng,
 * nên một tham số không bao giờ làm lệch số tham số hay tách dòng ở phía nhận.
 */
public class Message {
    private final String command;
    private final List<String> args;

    /**
     * @throws IllegalArgumentException nếu {@code command} null hoặc rỗng
     */
    public Message(String command, String... args) {
        if (command == null || command.isEmpty()) throw new IllegalArgumentException("Lệnh không được rỗng");
        this.command = sanitize(command);
        List<String> list = new ArrayList<>(args == null ? 0 : args.length);
        if (args != null) for (String a : args) list.add(sanitize(a));
        this.args = list;
    }

    /**
     * Tách một dòng nhận được thành thông điệp. Giữ cả tham số rỗng (kể cả ở cuối).
     *
     * @throws IllegalArgumentException nếu dòng null, rỗng hoặc dài quá {@link Protocol#MAX_LINE_BYTES} byte UTF-8
     */
    public static Message parse(String line) {
        if (line == null || line.isEmpty()) throw new IllegalArgumentException("Dòng rỗng");
        if (line.getBytes(StandardCharsets.UTF_8).length > Protocol.MAX_LINE_BYTES) {
            throw new IllegalArgumentException("Dòng dài quá " + Protocol.MAX_LINE_BYTES + " byte");
        }
        String[] parts = line.split("\\|", -1);
        return new Message(parts[0], Arrays.copyOfRange(parts, 1, parts.length));
    }

    public String format() {
        return args.isEmpty() ? command : command + "|" + String.join("|", args);
    }

    public String command() { return command; }
    public String arg(int i) { return args.get(i); }
    public int argCount() { return args.size(); }

    @Override
    public String toString() { return format(); }

    /** {@code null} → "", {@code |} → {@code /}, {@code \r}, {@code \n} → khoảng trắng. */
    private static String sanitize(String s) {
        if (s == null) return "";
        return s.replace('|', '/').replace('\r', ' ').replace('\n', ' ');
    }
}
