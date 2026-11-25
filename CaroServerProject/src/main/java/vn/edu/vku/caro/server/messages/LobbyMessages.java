package vn.edu.vku.caro.server.messages;

import vn.edu.vku.caro.common.protocol.Message;

public final class LobbyMessages {
    private LobbyMessages() { }
    public static Message roomList(String encoded) { return new Message("ROOM_LIST", encoded); }
    public static Message roomCreated(String roomId) { return new Message("ROOM_CREATED", roomId); }
    public static Message roomLeft() { return new Message("ROOM_LEFT"); }
    public static Message pong() { return new Message("PONG"); }
}
