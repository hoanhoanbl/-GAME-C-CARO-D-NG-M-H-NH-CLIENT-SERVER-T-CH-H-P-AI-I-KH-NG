package caro.server.model.room;

import caro.server.model.game.GameSession;
import caro.server.model.player.Player;

public class Room {
    public enum Status {
        WAITING, PLAYING, FINISHED
    }

    private final String id;
    private final Player playerX;
    private volatile Player playerO;
    private volatile Status status;
    private volatile GameSession session;
    private final boolean vsBot;
    private final int botLevel;

    public Room(String id, Player playerX) {
        this(id, playerX, null, false, 0);
    }

    public Room(String id, Player playerX, Player playerO, boolean vsBot, int botLevel) {
        this.id = id;
        this.playerX = playerX;
        this.playerO = playerO;
        this.status = (playerO != null) ? Status.PLAYING : Status.WAITING;
        this.vsBot = vsBot;
        this.botLevel = botLevel;
    }

    public String getId() { return id; }
    public Player getPlayerX() { return playerX; }
    public Player getPlayerO() { return playerO; }

    public void setPlayerO(Player playerO) {
        this.playerO = playerO;
        if (playerO != null && this.status == Status.WAITING) {
            this.status = Status.PLAYING;
        }
    }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public GameSession getSession() { return session; }
    public void setSession(GameSession session) { this.session = session; }

    public boolean isVsBot() { return vsBot; }
    public int getBotLevel() { return botLevel; }

    public boolean hasPlayer(Player p) {
        if (p == null) return false;
        return p.equals(playerX) || p.equals(playerO);
    }

    public boolean isFull() {
        return playerO != null;
    }

    public boolean isWaiting() {
        return status == Status.WAITING;
    }

    /**
     * Định dạng trả về cho thông điệp ROOM_LIST theo mục 10.3: id:chủ_phòng:trạng_thái
     */
    public String encode() {
        String ownerName = playerX != null ? playerX.getName() : "Unknown";
        return id + ":" + ownerName + ":" + status.name();
    }
}
