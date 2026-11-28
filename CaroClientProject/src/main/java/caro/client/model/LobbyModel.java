package caro.client.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Model quản lý danh sách phòng phía Client (FE-07).
 */
public class LobbyModel {
    private final ObservableList<RoomInfo> rooms = FXCollections.observableArrayList();

    public ObservableList<RoomInfo> getRooms() {
        return rooms;
    }

    /**
     * Cập nhật danh sách từ chuỗi mã hóa ROOM_LIST: id:chủ_phòng:trạng_thái;…
     */
    public void updateFromEncoded(String encoded) {
        rooms.clear();
        if (encoded == null || encoded.isBlank()) return;
        String[] items = encoded.split(";");
        for (String item : items) {
            if (item.isBlank()) continue;
            String[] parts = item.split(":");
            if (parts.length >= 3) {
                rooms.add(new RoomInfo(parts[0], parts[1], parts[2]));
            }
        }
    }
}
