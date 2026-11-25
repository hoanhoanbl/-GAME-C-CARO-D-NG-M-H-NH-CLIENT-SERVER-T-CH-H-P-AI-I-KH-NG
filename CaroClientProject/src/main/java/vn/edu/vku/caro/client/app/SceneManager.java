package vn.edu.vku.caro.client.app;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;

/** Chuyển giữa 5 màn hình: nạp FXML, gắn base.css và css riêng của màn hình (nếu có). */
public class SceneManager {
    private static final String VIEW_DIR = "/vn/edu/vku/caro/client/view/";
    private static final SceneManager INSTANCE = new SceneManager();
    private Stage stage;

    private SceneManager() { }
    public static SceneManager get() { return INSTANCE; }
    public void init(Stage stage) { this.stage = stage; }

    public void show(Screen screen) {
        String name = screen.name().toLowerCase();
        try {
            Region root = FXMLLoader.load(getClass().getResource(VIEW_DIR + name + ".fxml"));
            double w = stage.getScene() == null ? 900 : stage.getScene().getWidth();
            double h = stage.getScene() == null ? 650 : stage.getScene().getHeight();
            Scene scene = new Scene(root, w, h);
            addCss(scene, "css/base.css");
            addCss(scene, "css/" + name + ".css");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void addCss(Scene scene, String path) {
        URL url = getClass().getResource(VIEW_DIR + path);
        if (url != null) scene.getStylesheets().add(url.toExternalForm());
    }
}
