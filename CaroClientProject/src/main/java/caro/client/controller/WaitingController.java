package caro.client.controller;

import javafx.fxml.FXML;
import caro.client.app.Screen;
import caro.client.app.SceneManager;

public class WaitingController {
    @FXML
    private void next() {
        SceneManager.get().show(Screen.GAME);   // tạm thời để thử chuyển màn hình
    }
}
