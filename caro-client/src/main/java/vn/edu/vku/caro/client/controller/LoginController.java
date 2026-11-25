package vn.edu.vku.caro.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import vn.edu.vku.caro.client.app.SceneManager;
import vn.edu.vku.caro.client.app.Screen;
import vn.edu.vku.caro.client.model.ClientSession;
import vn.edu.vku.caro.client.model.User;
import vn.edu.vku.caro.client.net.MessageBus;
import vn.edu.vku.caro.client.net.NetworkClient;
import vn.edu.vku.caro.common.protocol.Command;
import vn.edu.vku.caro.common.protocol.Message;

import java.util.function.Consumer;
import java.util.regex.Pattern;

public class LoginController {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,16}$");

    @FXML private Button loginTabBtn;
    @FXML private Button registerTabBtn;

    @FXML private TextField usernameField;
    @FXML private VBox emailBox;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private VBox confirmPasswordBox;
    @FXML private PasswordField confirmPasswordField;

    @FXML private Label messageLabel;
    @FXML private Button actionBtn;

    private boolean isRegisterMode = false;

    private final Consumer<Message> welcomeHandler = this::onWelcome;
    private final Consumer<Message> registeredHandler = this::onRegistered;
    private final Consumer<Message> errorHandler = this::onError;

    @FXML
    public void initialize() {
        MessageBus bus = MessageBus.get();
        bus.unsubscribe(Command.WELCOME, welcomeHandler);
        bus.unsubscribe(Command.REGISTERED, registeredHandler);
        bus.unsubscribe(Command.ERROR, errorHandler);

        bus.subscribe(Command.WELCOME, welcomeHandler);
        bus.subscribe(Command.REGISTERED, registeredHandler);
        bus.subscribe(Command.ERROR, errorHandler);

        showLoginView();
    }

    @FXML
    public void showLoginView() {
        isRegisterMode = false;
        loginTabBtn.setStyle("-fx-pref-height: 36px; -fx-background-color: #3182ce; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
        registerTabBtn.setStyle("-fx-pref-height: 36px; -fx-background-color: transparent; -fx-text-fill: #4a5568; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");

        emailBox.setVisible(false);
        emailBox.setManaged(false);
        confirmPasswordBox.setVisible(false);
        confirmPasswordBox.setManaged(false);

        actionBtn.setText("Đăng nhập");
        passwordField.clear();
        confirmPasswordField.clear();
    }

    @FXML
    public void showRegisterView() {
        isRegisterMode = true;
        registerTabBtn.setStyle("-fx-pref-height: 36px; -fx-background-color: #3182ce; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
        loginTabBtn.setStyle("-fx-pref-height: 36px; -fx-background-color: transparent; -fx-text-fill: #4a5568; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");

        emailBox.setVisible(true);
        emailBox.setManaged(true);
        confirmPasswordBox.setVisible(true);
        confirmPasswordBox.setManaged(true);

        actionBtn.setText("Tạo tài khoản");
        messageLabel.setText("");
    }

    @FXML
    public void handleAction() {
        messageLabel.setStyle("-fx-text-fill: #e53e3e;");
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty()) {
            messageLabel.setText("Vui lòng nhập tên đăng nhập!");
            usernameField.requestFocus();
            return;
        }

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            messageLabel.setText("Tên đăng nhập phải từ 3-16 ký tự, chỉ gồm chữ, số và gạch dưới (_)");
            return;
        }

        if (password.isEmpty()) {
            messageLabel.setText("Vui lòng nhập mật khẩu!");
            passwordField.requestFocus();
            return;
        }

        if (isRegisterMode) {
            if (password.length() < 6 || password.length() > 32) {
                messageLabel.setText("Mật khẩu phải từ 6 đến 32 ký tự!");
                return;
            }
            if (password.contains("|")) {
                messageLabel.setText("Mật khẩu không được chứa ký tự '|'!");
                return;
            }
            String confirmPassword = confirmPasswordField.getText();
            if (!password.equals(confirmPassword)) {
                messageLabel.setText("Xác nhận mật khẩu không khớp!");
                confirmPasswordField.requestFocus();
                return;
            }

            String email = emailField.getText().trim();
            if (!email.isEmpty() && !email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                messageLabel.setText("Email không đúng định dạng!");
                emailField.requestFocus();
                return;
            }

            if (!ensureConnected()) return;

            messageLabel.setStyle("-fx-text-fill: #3182ce;");
            messageLabel.setText("Đang đăng ký tài khoản...");
            if (!email.isEmpty()) {
                ClientSession.get().send(new Message("REGISTER", username, password, email));
            } else {
                ClientSession.get().send(new Message("REGISTER", username, password));
            }
        } else {
            if (!ensureConnected()) return;

            messageLabel.setStyle("-fx-text-fill: #3182ce;");
            messageLabel.setText("Đang đăng nhập...");
            ClientSession.get().send(new Message("LOGIN", username, password));
        }
    }

    private boolean ensureConnected() {
        if (!NetworkClient.get().isConnected()) {
            boolean reconnected = NetworkClient.get().connect("localhost", 5000);
            if (!reconnected) {
                messageLabel.setStyle("-fx-text-fill: #e53e3e;");
                messageLabel.setText("Chưa kết nối tới CaroServer! Hãy bật server hoặc chọn 'Đổi Server'.");
                return false;
            }
        }
        return true;
    }

    private void onWelcome(Message msg) {
        String username = msg.arg(0);
        String email = msg.argCount() > 1 ? msg.arg(1) : "";
        User user = new User(username, passwordField.getText(), email);
        ClientSession.get().setUser(user);
        ClientSession.get().setGuest(false);

        SceneManager.get().show(Screen.LOBBY);
    }

    private void onRegistered(Message msg) {
        String registeredUser = msg.arg(0);
        showLoginView();
        usernameField.setText(registeredUser);
        messageLabel.setStyle("-fx-text-fill: #38a169;");
        messageLabel.setText("Đăng ký thành công tài khoản '" + registeredUser + "'! Mời bạn đăng nhập.");
    }

    private void onError(Message msg) {
        String code = msg.arg(0);
        String text = msg.argCount() > 1 ? msg.arg(1) : "";

        String displayMsg;
        switch (code) {
            case "USERNAME_INVALID" -> displayMsg = "Tên đăng nhập không hợp lệ (3-16 ký tự, chỉ gồm chữ, số, _)";
            case "PASSWORD_INVALID" -> displayMsg = "Mật khẩu không hợp lệ (6-32 ký tự, không chứa '|')";
            case "USERNAME_TAKEN" -> displayMsg = "Tên đăng nhập đã tồn tại! Vui lòng chọn tên khác.";
            case "INVALID_CREDENTIALS" -> displayMsg = "Tên đăng nhập hoặc mật khẩu không chính xác!";
            case "ALREADY_LOGGED_IN" -> displayMsg = "Tài khoản này hiện đang đăng nhập ở một nơi khác!";
            case "DB_UNAVAILABLE" -> displayMsg = "Lỗi kết nối cơ sở dữ liệu MySQL trên server!";
            case "NOT_LOGGED_IN" -> displayMsg = "Bạn chưa đăng nhập!";
            default -> displayMsg = !text.isEmpty() ? text : code;
        }

        messageLabel.setStyle("-fx-text-fill: #e53e3e;");
        messageLabel.setText(displayMsg);
    }

    @FXML
    public void handlePlayWithBot() {
        User guest = new User("Khách");
        ClientSession.get().setUser(guest);
        ClientSession.get().setGuest(true);
        SceneManager.get().show(Screen.LOBBY);
    }

    @FXML
    public void backToConnect() {
        SceneManager.get().show(Screen.CONNECT);
    }
}
