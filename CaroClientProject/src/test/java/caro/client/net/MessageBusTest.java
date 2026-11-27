package caro.client.net;

import caro.common.protocol.Command;
import caro.common.protocol.Message;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class MessageBusTest {

    @Test
    void failingHandlerDoesNotBlockOthers() {
        MessageBus bus = new MessageBus();
        List<Message> got = new ArrayList<>();
        bus.subscribe(Command.MOVED, m -> { throw new IllegalStateException("lỗi thử"); });
        bus.subscribe(Command.MOVED, got::add);
        bus.publish(Message.parse("MOVED|X|7|7|O"));
        assertEquals(1, got.size());
    }

    @Test
    void unsubscribeUnknownHandlerIsHarmless() {
        MessageBus bus = new MessageBus();
        Consumer<Message> h = m -> { };
        assertDoesNotThrow(() -> bus.unsubscribe(Command.MOVED, h));
        bus.subscribe(Command.MOVED, h);
        bus.unsubscribe(Command.MOVED, h);
        assertEquals(0, bus.subscriberCount(Command.MOVED));
    }

    @Test
    void unknownCommandIsIgnored() {
        MessageBus bus = new MessageBus();
        assertDoesNotThrow(() -> bus.publish(new Message("KHONG_CO")));
    }
}
