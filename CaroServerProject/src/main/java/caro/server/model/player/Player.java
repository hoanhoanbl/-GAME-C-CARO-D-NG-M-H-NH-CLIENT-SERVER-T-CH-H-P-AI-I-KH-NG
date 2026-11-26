package caro.server.model.player;

import caro.common.protocol.Message;

public interface Player {
    String getName();
    boolean isBot();
    boolean isGuest();
    void send(Message msg);
}
