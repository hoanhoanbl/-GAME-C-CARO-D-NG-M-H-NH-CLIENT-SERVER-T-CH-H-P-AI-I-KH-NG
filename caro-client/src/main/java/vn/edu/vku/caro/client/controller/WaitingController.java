package vn.edu.vku.caro.client.controller;

import javafx.fxml.FXML;
import vn.edu.vku.caro.client.app.Screen;
import vn.edu.vku.caro.client.app.SceneManager;

public class WaitingController {
    @FXML
    private void next() {
        SceneManager.get().show(Screen.GAME);   // tạm thời để thử chuyển màn hình
    }
}
