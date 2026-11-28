package caro.server.model.room;

import caro.common.model.GameResult;
import caro.server.model.dao.NoOpMatchRecorder;
import caro.server.model.game.GameEndListener;
import caro.server.model.game.GameSession;
import caro.server.model.game.GameSessionFactory;
import caro.server.model.player.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Quản lý danh sách phòng chơi (FE-07).
 * Cài {@link GameEndListener} để cập nhật trạng thái phòng khi ván kết thúc.
 */
public class RoomManager implements GameEndListener {
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger();

    public Room createRoom(Player creator) {
        String id = "R" + idCounter.incrementAndGet();
        Room room = new Room(id, creator);
        rooms.put(id, room);
        return room;
    }

    public Room createBotRoom(Player human, Player bot, int level) {
        String id = "BOT_" + idCounter.incrementAndGet();
        Room room = new Room(id, human, bot, true, level);
        GameSession session = GameSessionFactory.create(id, human, bot, this, new NoOpMatchRecorder());
        room.setSession(session);
        rooms.put(id, room);
        return room;
    }

    public Room findRoomById(String id) {
        if (id == null) return null;
        return rooms.get(id);
    }

    public Room findRoomOf(Player p) {
        if (p == null) return null;
        for (Room r : rooms.values()) {
            if (r.hasPlayer(p)) return r;
        }
        return null;
    }

    public GameSession findSessionOf(Player p) {
        Room r = findRoomOf(p);
        return r != null ? r.getSession() : null;
    }

    public void removeRoom(String id) {
        if (id != null) rooms.remove(id);
    }

    public Collection<Room> allRooms() {
        return rooms.values();
    }

    /**
     * Danh sách phòng ở sảnh (chỉ hiện phòng người–người, không hiện phòng bot ẩn).
     * Định dạng theo mục 10.3: id:chủ_phòng:trạng_thái;… (rỗng nếu không có phòng nào).
     */
    public String encodeRoomList() {
        List<String> list = new ArrayList<>();
        for (Room r : rooms.values()) {
            if (!r.isVsBot()) {
                list.add(r.encode());
            }
        }
        return String.join(";", list);
    }

    @Override
    public void onGameEnded(GameSession session, GameResult result) {
        if (session == null) return;
        for (Room r : rooms.values()) {
            if (r.getSession() == session) {
                r.setStatus(Room.Status.FINISHED);
                break;
            }
        }
    }
}
