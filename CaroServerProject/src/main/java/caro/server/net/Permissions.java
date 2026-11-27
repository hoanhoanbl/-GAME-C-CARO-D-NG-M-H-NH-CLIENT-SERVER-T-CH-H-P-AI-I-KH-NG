package caro.server.net;

import caro.common.protocol.Command;

import java.util.EnumSet;
import java.util.Set;

/**
 * Bảng quyền gửi lệnh theo trạng thái người gửi (mục 10.1, bảng quyền mục 7.2).
 * Hàm thuần, không phụ thuộc socket hay phòng: {@link CommandDispatcher} tính {@link State} rồi tra ở đây.
 */
public final class Permissions {
    /** Trạng thái người gửi xét theo quyền. */
    public enum State {
        /** Chưa đăng nhập, không ở trong ván nào. */
        ANONYMOUS,
        /** Khách (chưa đăng nhập) đang trong ván với máy. */
        GUEST_IN_BOT_GAME,
        /** Đã đăng nhập bằng tài khoản. */
        LOGGED_IN
    }

    /** Các lệnh client → server (theo thứ tự khai báo trong {@link Command}, từ REGISTER tới QUIT). */
    public static final Set<Command> CLIENT_COMMANDS = EnumSet.range(Command.REGISTER, Command.QUIT);

    private static final Set<Command> ANONYMOUS_ALLOWED =
            EnumSet.of(Command.REGISTER, Command.LOGIN, Command.PLAY_BOT, Command.PING, Command.QUIT);

    private static final Set<Command> GUEST_IN_GAME_ALLOWED;
    static {
        Set<Command> s = EnumSet.copyOf(ANONYMOUS_ALLOWED);
        s.addAll(EnumSet.of(Command.MOVE, Command.SURRENDER, Command.REMATCH, Command.BACK_TO_LOBBY));
        GUEST_IN_GAME_ALLOWED = s;
    }

    private Permissions() { }

    /** true nếu {@code cmd} là lệnh client được phép gửi lên server. */
    public static boolean isClientCommand(Command cmd) {
        return CLIENT_COMMANDS.contains(cmd);
    }

    /** true nếu người ở trạng thái {@code state} được gửi {@code cmd}. Lệnh server → client luôn false. */
    public static boolean allowed(State state, Command cmd) {
        if (!isClientCommand(cmd)) return false;
        return switch (state) {
            case LOGGED_IN -> true;
            case GUEST_IN_BOT_GAME -> GUEST_IN_GAME_ALLOWED.contains(cmd);
            case ANONYMOUS -> ANONYMOUS_ALLOWED.contains(cmd);
        };
    }
}
