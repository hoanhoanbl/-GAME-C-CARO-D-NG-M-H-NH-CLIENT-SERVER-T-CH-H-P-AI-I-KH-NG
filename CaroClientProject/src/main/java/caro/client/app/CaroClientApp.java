package caro.client.app;

import caro.client.net.NetworkClient;
import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class CaroClientApp extends Application {
    @Override
    public void start(Stage stage) {
        stage.setTitle("Cờ Caro");
        SceneManager.get().init(stage);

        // Mất kết nối ngoài ý muốn (server tắt, rớt mạng) → về màn hình kết nối và báo lý do.
        NetworkClient.get().setOnConnectionLost(reason -> {
            SceneManager.get().show(Screen.CONNECT);
            Alert alert = new Alert(Alert.AlertType.WARNING, reason);
            alert.initOwner(stage);
            alert.setTitle("Mất kết nối");
            alert.setHeaderText("Đã mất kết nối tới server");
            alert.show();
        });

        SceneManager.get().show(Screen.CONNECT);
    }

    @Override
    public void stop() {
        NetworkClient.get().disconnect();
    }

    public static void main(String[] args) { launch(args); }
}
