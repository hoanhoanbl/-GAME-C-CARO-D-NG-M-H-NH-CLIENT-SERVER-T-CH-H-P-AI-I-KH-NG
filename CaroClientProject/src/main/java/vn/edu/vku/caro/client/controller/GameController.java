package vn.edu.vku.caro.client.controller;

import javafx.fxml.FXML;
import vn.edu.vku.caro.client.app.Screen;
import vn.edu.vku.caro.client.app.SceneManager;

public class GameController {
    @FXML
    private void back() { SceneManager.get().show(Screen.CONNECT); }
}
