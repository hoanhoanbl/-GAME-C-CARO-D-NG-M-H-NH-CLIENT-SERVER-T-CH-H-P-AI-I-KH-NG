package vn.edu.vku.caro.server.messages;

import vn.edu.vku.caro.common.protocol.Message;

public final class AuthMessages {
    private AuthMessages() { }
    public static Message registered(String username) { return new Message("REGISTERED", username); }
    public static Message welcome(String username) { return new Message("WELCOME", username); }
    public static Message welcome(String username, String email) {
        return email != null && !email.isBlank()
                ? new Message("WELCOME", username, email)
                : new Message("WELCOME", username);
    }
    public static Message loggedOut() { return new Message("LOGGED_OUT"); }
}
