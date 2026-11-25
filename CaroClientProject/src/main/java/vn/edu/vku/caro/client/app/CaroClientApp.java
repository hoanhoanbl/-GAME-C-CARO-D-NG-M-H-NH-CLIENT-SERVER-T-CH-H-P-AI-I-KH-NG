package vn.edu.vku.caro.client.app;

import javafx.application.Application;
import javafx.stage.Stage;

public class CaroClientApp extends Application {
    @Override
    public void start(Stage stage) {
        stage.setTitle("Cờ Caro");
        SceneManager.get().init(stage);
        // -Dcaro.screen=GAME để mở thẳng một màn hình khi phát triển
        SceneManager.get().show(Screen.valueOf(System.getProperty("caro.screen", "CONNECT")));
    }

    public static void main(String[] args) { launch(args); }
}
