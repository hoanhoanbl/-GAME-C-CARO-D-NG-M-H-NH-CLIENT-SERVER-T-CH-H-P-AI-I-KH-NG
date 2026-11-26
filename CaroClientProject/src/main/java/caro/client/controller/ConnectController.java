package caro.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import caro.client.app.SceneManager;
import caro.client.app.Screen;
import caro.client.net.NetworkClient;

public class ConnectController {

    @FXML
    private TextField hostField;

    @FXML
    private TextField portField;

    @FXML
    private Label statusLabel;

    @FXML
    private void handleConnect() {
        String host = hostField.getText().trim();
        if (host.isEmpty()) host = "localhost";

        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            statusLabel.setStyle("-fx-text-fill: #e53e3e;");
            statusLabel.setText("Cổng (port) phải là số nguyên!");
            return;
        }

        statusLabel.setStyle("-fx-text-fill: #3182ce;");
        statusLabel.setText("Đang kết nối tới " + host + ":" + port + "...");

        boolean ok = NetworkClient.get().connect(host, port);
        if (ok) {
            statusLabel.setStyle("-fx-text-fill: #38a169;");
            statusLabel.setText("Kết nối thành công!");
            SceneManager.get().show(Screen.LOGIN);
        } else {
            statusLabel.setStyle("-fx-text-fill: #e53e3e;");
            statusLabel.setText("Không thể kết nối tới server. Vui lòng kiểm tra CaroServer!");
        }
    }

    @FXML
    private void next() {
        SceneManager.get().show(Screen.LOGIN);
    }
}
