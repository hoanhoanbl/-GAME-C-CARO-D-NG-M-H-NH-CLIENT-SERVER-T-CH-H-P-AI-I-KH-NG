package caro.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import caro.client.app.SceneManager;
import caro.client.app.Screen;
import caro.client.net.NetworkClient;

public class ConnectController {

    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private Label statusLabel;
    @FXML private Button connectButton;

    @FXML
    public void initialize() {
        NetworkClient net = NetworkClient.get();
        if (net.lastHost() != null) {          // quay lại màn hình này: điền server đã dùng
            hostField.setText(net.lastHost());
            portField.setText(String.valueOf(net.lastPort()));
        }
    }

    @FXML
    private void handleConnect() {
        String host = hostField.getText().trim();
        if (host.isEmpty()) host = "localhost";

        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            showStatus("#e53e3e", "Cổng (port) phải là số nguyên!");
            return;
        }
        if (port < 1 || port > 65535) {
            showStatus("#e53e3e", "Cổng phải trong khoảng 1–65535!");
            return;
        }

        showStatus("#3182ce", "Đang kết nối tới " + host + ":" + port + "...");
        connectButton.setDisable(true);

        // connect có thể chặn tới 3 giây: chạy trên luồng nền để giao diện không bị treo.
        String h = host;
        Thread t = new Thread(() -> {
            boolean ok = NetworkClient.get().connect(h, port);
            Platform.runLater(() -> onConnectResult(ok));
        }, "CaroClient-Connect");
        t.setDaemon(true);
        t.start();
    }

    private void onConnectResult(boolean ok) {
        connectButton.setDisable(false);
        if (ok) {
            showStatus("#38a169", "Kết nối thành công!");
            SceneManager.get().show(Screen.LOGIN);
        } else {
            showStatus("#e53e3e", "Không thể kết nối tới server. Vui lòng kiểm tra CaroServer!");
        }
    }

    private void showStatus(String color, String text) {
        statusLabel.setStyle("-fx-text-fill: " + color + ";");
        statusLabel.setText(text);
    }

    @FXML
    private void next() {
        SceneManager.get().show(Screen.LOGIN);
    }
}
