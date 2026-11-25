package vn.edu.vku.caro.server.model.player;

import vn.edu.vku.caro.common.protocol.Message;

public interface Player {
    String getName();
    boolean isBot();
    boolean isGuest();
    void send(Message msg);
}
