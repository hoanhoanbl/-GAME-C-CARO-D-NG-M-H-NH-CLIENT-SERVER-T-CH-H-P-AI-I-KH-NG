package vn.edu.vku.caro.client.model;

import vn.edu.vku.caro.client.net.NetworkClient;
import vn.edu.vku.caro.common.protocol.Message;

public class ClientSession {
    private static final ClientSession INSTANCE = new ClientSession();
    public static ClientSession get() { return INSTANCE; }

    private User user;
    private String playerName;
    private boolean guest;
    private GameInfo currentGame;

    private ClientSession() { }

    public User getUser() { return user; }
    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            this.playerName = user.getName();
        }
    }

    public String playerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public boolean isGuest() { return guest; }
    public void setGuest(boolean guest) { this.guest = guest; }

    public GameInfo currentGame() { return currentGame; }
    public void setCurrentGame(GameInfo currentGame) { this.currentGame = currentGame; }

    public void send(Message msg) {
        NetworkClient.get().send(msg);
    }
}
