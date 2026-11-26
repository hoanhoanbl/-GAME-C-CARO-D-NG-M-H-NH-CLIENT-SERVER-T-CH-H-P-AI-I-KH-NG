package caro.server.model.player;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Danh sách người chơi online, cấp tên Khách-<số>. */
public class PlayerRegistry {
    private final ConcurrentHashMap<String, RemotePlayer> online = new ConcurrentHashMap<>();
    private final AtomicInteger guestCounter = new AtomicInteger();

    public String nextGuestName() { return "Khách-" + guestCounter.incrementAndGet(); }
}
