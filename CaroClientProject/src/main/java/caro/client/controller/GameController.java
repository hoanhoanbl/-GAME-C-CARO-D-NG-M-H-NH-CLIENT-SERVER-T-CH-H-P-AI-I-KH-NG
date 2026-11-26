package caro.client.controller;

import javafx.fxml.FXML;
import caro.client.app.Screen;
import caro.client.app.SceneManager;

public class GameController {
    @FXML
    private void back() { SceneManager.get().show(Screen.CONNECT); }
}
