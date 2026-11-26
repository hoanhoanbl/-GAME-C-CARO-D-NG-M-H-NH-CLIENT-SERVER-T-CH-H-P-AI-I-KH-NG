package caro.server.messages;

import caro.common.protocol.Message;

public final class ErrorMessages {
    private ErrorMessages() { }
    public static Message error(String code, String text) { return new Message("ERROR", code, text); }
}
