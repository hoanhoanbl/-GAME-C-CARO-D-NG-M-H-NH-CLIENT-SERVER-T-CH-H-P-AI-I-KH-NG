package caro.client.model;

public record RoomInfo(String id, String owner, String status) {
    public String getId() { return id; }
    public String getOwner() { return owner; }
    public String getStatus() { return status; }

    public boolean isWaiting() {
        return "WAITING".equalsIgnoreCase(status);
    }

    public String getStatusDisplay() {
        return isWaiting() ? "Đang chờ" : "Đang thi đấu";
    }
}
