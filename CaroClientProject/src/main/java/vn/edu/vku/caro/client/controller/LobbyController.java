package vn.edu.vku.caro.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import vn.edu.vku.caro.client.app.SceneManager;
import vn.edu.vku.caro.client.app.Screen;
import vn.edu.vku.caro.client.model.ClientSession;
import vn.edu.vku.caro.client.model.User;
import vn.edu.vku.caro.common.protocol.Message;

public class LobbyController {

    @FXML
    private Label welcomeLabel;

    @FXML
    public void initialize() {
        ClientSession session = ClientSession.get();
        User user = session.getUser();
        if (user != null) {
            String emailStr = (user.getEmail() != null && !user.getEmail().isBlank()) ? " (" + user.getEmail() + ")" : "";
            welcomeLabel.setText("Xin chào: " + user.getName() + emailStr + (session.isGuest() ? " [Khách]" : ""));
        } else if (session.playerName() != null) {
            welcomeLabel.setText("Xin chào: " + session.playerName());
        }
    }

    @FXML
    private void handleLogout() {
        ClientSession.get().send(new Message("LOGOUT"));
        ClientSession.get().setUser(null);
        ClientSession.get().setPlayerName(null);
        SceneManager.get().show(Screen.LOGIN);
    }

    @FXML
    private void next() {
        SceneManager.get().show(Screen.WAITING);
    }
}
