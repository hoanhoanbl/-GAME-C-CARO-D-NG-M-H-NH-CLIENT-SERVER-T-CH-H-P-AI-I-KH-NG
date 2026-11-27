package caro.server.model.player;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Danh sách người chơi đang online (đã đăng nhập), tra theo tên không phân biệt hoa thường; cấp tên {@code Khách-<số>}.
 *
 * <p>Chốt "một tài khoản chỉ đăng nhập một nơi" vẫn nằm ở {@code AuthService}; registry chỉ phục vụ tra cứu
 * và gửi cho mọi người ở sảnh. {@code CommandDispatcher} là nơi duy nhất thêm/xóa.
 */
public class PlayerRegistry {
    private final ConcurrentHashMap<String, RemotePlayer> online = new ConcurrentHashMap<>();
    private final AtomicInteger guestCounter = new AtomicInteger();

    public String nextGuestName() { return "Khách-" + guestCounter.incrementAndGet(); }

    /** Thêm người đã đăng nhập. @return false nếu tên đã có người khác giữ. */
    public boolean add(RemotePlayer p) {
        String key = key(p.getName());
        if (key == null) throw new IllegalArgumentException("Người chơi chưa có tên");
        RemotePlayer old = online.putIfAbsent(key, p);
        return old == null || old == p;
    }

    /** Xóa đúng người này (không xóa nhầm người khác cùng tên). */
    public void remove(RemotePlayer p) {
        String key = key(p.getName());
        if (key != null) online.remove(key, p);
    }

    public Optional<RemotePlayer> find(String name) {
        String key = key(name);
        return key == null ? Optional.empty() : Optional.ofNullable(online.get(key));
    }

    /** Ảnh chụp danh sách người online. */
    public Collection<RemotePlayer> online() { return List.copyOf(online.values()); }

    private static String key(String name) { return name == null ? null : name.toLowerCase(Locale.ROOT); }
}
