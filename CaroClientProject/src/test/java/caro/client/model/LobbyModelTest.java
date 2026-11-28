package caro.client.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LobbyModelTest {

    @Test
    void testUpdateFromEncoded() {
        LobbyModel model = new LobbyModel();
        String encoded = "R1:an:WAITING;R2:binh:PLAYING";
        model.updateFromEncoded(encoded);

        assertEquals(2, model.getRooms().size());

        RoomInfo r1 = model.getRooms().get(0);
        assertEquals("R1", r1.getId());
        assertEquals("an", r1.getOwner());
        assertEquals("WAITING", r1.getStatus());
        assertTrue(r1.isWaiting());

        RoomInfo r2 = model.getRooms().get(1);
        assertEquals("R2", r2.getId());
        assertEquals("binh", r2.getOwner());
        assertEquals("PLAYING", r2.getStatus());
        assertFalse(r2.isWaiting());
    }

    @Test
    void testUpdateFromEmptyString() {
        LobbyModel model = new LobbyModel();
        model.updateFromEncoded("R1:an:WAITING");
        assertEquals(1, model.getRooms().size());

        model.updateFromEncoded("");
        assertTrue(model.getRooms().isEmpty());

        model.updateFromEncoded(null);
        assertTrue(model.getRooms().isEmpty());
    }
}
