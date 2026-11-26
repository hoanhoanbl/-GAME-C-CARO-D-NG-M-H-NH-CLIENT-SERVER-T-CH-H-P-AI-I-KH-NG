package caro.client.app;

import javafx.application.Application;
import javafx.stage.Stage;

public class CaroClientApp extends Application {
    @Override
    public void start(Stage stage) {
        stage.setTitle("Cờ Caro");
        SceneManager.get().init(stage);
        SceneManager.get().show(Screen.CONNECT);
    }

    public static void main(String[] args) { launch(args); }
}
