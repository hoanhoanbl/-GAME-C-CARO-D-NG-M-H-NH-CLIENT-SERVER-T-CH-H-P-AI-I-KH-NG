package caro.common.protocol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Message {
    private final String command;
    private final List<String> args;

    public Message(String command, String... args) {
        this.command = command;
        this.args = new ArrayList<>(Arrays.asList(args));
    }

    public static Message parse(String line) {
        String[] parts = line.split("\\|", -1);
        return new Message(parts[0], Arrays.copyOfRange(parts, 1, parts.length));
    }

    public String format() {
        return args.isEmpty() ? command : command + "|" + String.join("|", args);
    }

    public String command() { return command; }
    public String arg(int i) { return args.get(i); }
    public int argCount() { return args.size(); }
}
