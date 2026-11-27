package caro.client.app;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;

public class SceneManager {
    private static final SceneManager INSTANCE = new SceneManager();
    private Stage stage;

    private SceneManager() { }
    public static SceneManager get() { return INSTANCE; }
    public void init(Stage stage) { this.stage = stage; }

    public void show(Screen screen) {
        String file = screen.name().toLowerCase() + ".fxml";
        java.net.URL url = getClass().getResource("/caro/client/view/" + file);
        if (url == null) throw new IllegalStateException("Thiếu file giao diện " + file + " cho màn hình " + screen);
        try {
            Region root = FXMLLoader.load(url);
            Scene scene = new Scene(root, 900, 650);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
