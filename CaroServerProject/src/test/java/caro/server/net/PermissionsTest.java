package caro.server.net;

import caro.common.protocol.Command;
import caro.server.net.Permissions.State;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PermissionsTest {

    private static final Set<Command> ANON = EnumSet.of(
            Command.REGISTER, Command.LOGIN, Command.PLAY_BOT, Command.PING, Command.QUIT);
    private static final Set<Command> GUEST_EXTRA = EnumSet.of(
            Command.MOVE, Command.SURRENDER, Command.REMATCH, Command.BACK_TO_LOBBY);

    @Test
    void clientCommandsAreExactlyRegisterToQuit() {
        assertEquals(15, Permissions.CLIENT_COMMANDS.size());
        assertTrue(Permissions.isClientCommand(Command.CHAT));
        assertFalse(Permissions.isClientCommand(Command.MOVED));
        assertFalse(Permissions.isClientCommand(Command.PONG));
    }

    @ParameterizedTest
    @EnumSource(Command.class)
    void anonymous(Command cmd) {
        assertEquals(ANON.contains(cmd), Permissions.allowed(State.ANONYMOUS, cmd), cmd.name());
    }

    @ParameterizedTest
    @EnumSource(Command.class)
    void guestInBotGame(Command cmd) {
        boolean expected = ANON.contains(cmd) || GUEST_EXTRA.contains(cmd);
        assertEquals(expected, Permissions.allowed(State.GUEST_IN_BOT_GAME, cmd), cmd.name());
    }

    @ParameterizedTest
    @EnumSource(Command.class)
    void loggedInMayUseEveryClientCommand(Command cmd) {
        assertEquals(Permissions.isClientCommand(cmd), Permissions.allowed(State.LOGGED_IN, cmd), cmd.name());
    }
}
