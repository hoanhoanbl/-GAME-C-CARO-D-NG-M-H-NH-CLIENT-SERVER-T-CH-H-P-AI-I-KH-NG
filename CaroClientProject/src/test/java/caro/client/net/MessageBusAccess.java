package caro.client.net;

/** Cho test ở gói khác tạo MessageBus riêng (constructor chỉ mở trong gói). */
public final class MessageBusAccess {
    private MessageBusAccess() { }
    public static MessageBus newBus() { return new MessageBus(); }
}
