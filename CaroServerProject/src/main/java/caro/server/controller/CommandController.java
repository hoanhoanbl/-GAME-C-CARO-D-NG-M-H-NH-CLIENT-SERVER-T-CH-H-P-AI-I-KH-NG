package caro.server.controller;

import caro.common.protocol.Command;
import caro.common.protocol.Message;
import caro.server.model.player.RemotePlayer;

import java.util.Set;

/** Hợp đồng H + P (mục 9): một nhóm lệnh client → server, đăng ký vào {@code CommandDispatcher}. */
public interface CommandController {
    /** Các lệnh controller này xử lý. */
    Set<Command> commands();

    void handle(RemotePlayer from, Message msg);
}
