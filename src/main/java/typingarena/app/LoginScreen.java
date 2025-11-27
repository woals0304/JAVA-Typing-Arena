package typingarena.app;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import typingarena.net.Message;
import typingarena.net.NetClient;

import java.io.IOException;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.Locale;

/**
 * 로그인/회원가입 화면. 성공 시 콜백으로 NetClient와 닉네임을 전달한다.
 */
public class LoginScreen extends Stage {

    private static final String BG_COLOR = "#FDF5E6";
    private static final String CARD_BG = "#FFF3E0";
    private static final String CARD_BORDER = "#D7CCC8";
    private static final String TEXT_MAIN = "#4E342E";
    private static final String ACCENT = "#29B6F6";

    private final TextField hostField = new TextField("127.0.0.1");
    private final TextField portField = new TextField("7777");
    private final TextField idField = new TextField();
    private final PasswordField pwField = new PasswordField();
    private final TextField nicknameField = new TextField();
    private final Button connectBtn = new Button("접속");
    private final Button loginBtn = new Button("로그인");
    private final Button registerBtn = new Button("회원가입");
    private final Label statusLabel = new Label("서버에 먼저 접속하세요.");

    private NetClient client;
    private final BiConsumer<NetClient, String> onLoginSuccess;

    public LoginScreen(Stage owner, BiConsumer<NetClient, String> onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("로그인 / 회원가입");

        VBox card = new VBox(16, buildHeader(), buildForm(), statusLabel);
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: " + CARD_BG + "; -fx-background-radius: 16; -fx-border-color: " + CARD_BORDER + "; -fx-border-radius: 16; -fx-border-width: 2;");

        VBox root = new VBox(card);
        root.setPadding(new Insets(18, 24, 18, 24));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: " + BG_COLOR + ";");

        statusLabel.setStyle("-fx-text-fill: #6D4C41;");

        Scene scene = new Scene(root, 480, 360);
        setScene(scene);

        connectBtn.setOnAction(e -> connect());
        loginBtn.setOnAction(e -> sendLogin());
        registerBtn.setOnAction(e -> sendRegister());

        setOnCloseRequest(e -> {
            if (client != null) {
                try { client.close(); } catch (IOException ignored) {}
            }
        });
    }

    private VBox buildHeader() {
        Label title = new Label("Typing Arena 로그인");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_MAIN + ";");
        Label subtitle = new Label("서버 접속 후 로그인하거나 회원가입하세요.");
        subtitle.setStyle("-fx-text-fill: #6D4C41;");
        VBox box = new VBox(6, title, subtitle);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private VBox buildForm() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);

        grid.add(new Label("Host"), 0, 0);
        grid.add(hostField, 1, 0);
        grid.add(new Label("Port"), 0, 1);
        grid.add(portField, 1, 1);
        grid.add(new Label("ID"), 0, 2);
        grid.add(idField, 1, 2);
        grid.add(new Label("Password"), 0, 3);
        grid.add(pwField, 1, 3);
        grid.add(new Label("Nickname(회원가입)"), 0, 4);
        grid.add(nicknameField, 1, 4);

        hostField.setPrefWidth(160);
        portField.setPrefWidth(100);
        idField.setPrefWidth(180);
        pwField.setPrefWidth(180);
        nicknameField.setPrefWidth(180);

        stylePrimary(connectBtn);
        styleAccent(loginBtn);
        styleSecondary(registerBtn);

        HBox btnRow = new HBox(10, connectBtn, loginBtn, registerBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        loginBtn.setDisable(true);
        registerBtn.setDisable(true);

        VBox box = new VBox(12, grid, btnRow);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void connect() {
        if (client != null) {
            statusLabel.setText("이미 연결됨.");
            statusLabel.setStyle("-fx-text-fill: #0078FF;");
            return;
        }
        try {
            int port = Integer.parseInt(portField.getText().trim());
            client = new NetClient(hostField.getText().trim(), port);
            client.setOnMessage(this::handleServerMessage);
            client.connect();
            statusLabel.setText("서버 연결 성공. 로그인/회원가입을 진행하세요.");
            statusLabel.setStyle("-fx-text-fill: #008800;");
            loginBtn.setDisable(false);
            registerBtn.setDisable(false);
        } catch (Exception e) {
            statusLabel.setText("연결 실패: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #AA0000;");
            client = null;
        }
    }

    private void sendLogin() {
        if (client == null) { statusLabel.setText("먼저 접속하세요."); return; }
        String id = idField.getText().trim();
        String pw = pwField.getText().trim();
        if (id.isEmpty() || pw.isEmpty()) {
            statusLabel.setText("ID/비밀번호를 입력하세요.");
            return;
        }
        Message msg = Message.of("LOGIN_REQUEST");
        msg.data = Map.of("id", id, "pw", pw);
        client.send(msg);
        statusLabel.setText("로그인 요청 전송...");
    }

    private void sendRegister() {
        if (client == null) { statusLabel.setText("먼저 접속하세요."); return; }
        String id = idField.getText().trim();
        String pw = pwField.getText().trim();
        String nick = nicknameField.getText().trim();
        if (id.isEmpty() || pw.isEmpty() || nick.isEmpty()) {
            statusLabel.setText("ID/비밀번호/닉네임을 모두 입력하세요.");
            return;
        }
        Message msg = Message.of("REGISTER_REQUEST");
        msg.data = Map.of("id", id, "pw", pw, "nickname", nick);
        client.send(msg);
        statusLabel.setText("회원가입 요청 전송...");
    }

    private void handleServerMessage(Message msg) {
        if (msg == null || msg.type == null) return;
        String type = msg.type.toUpperCase(Locale.ROOT);
        switch (type) {
            case "REGISTER_RESPONSE" -> handleRegisterResponse(msg);
            case "LOGIN_RESPONSE" -> handleLoginResponse(msg);
            default -> {}
        }
    }

    private void handleRegisterResponse(Message msg) {
        Map<String, Object> data = msg.data;
        boolean success = data != null && Boolean.TRUE.equals(data.get("success"));
        Platform.runLater(() -> {
            Alert alert = new Alert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
            alert.setTitle("회원가입 결과");
            alert.setHeaderText(null);
            alert.setContentText(success ? "회원가입 성공! 이제 로그인하세요." : "회원가입 실패: 중복 또는 서버 오류");
            alert.initOwner(this);
            alert.show();
            statusLabel.setText(success ? "회원가입 성공" : "회원가입 실패");
        });
    }

    private void handleLoginResponse(Message msg) {
        Map<String, Object> data = msg.data;
        boolean success = data != null && Boolean.TRUE.equals(data.get("success"));
        Platform.runLater(() -> {
            if (success) {
                String nickname = data.get("nickname") != null ? String.valueOf(data.get("nickname")) : idField.getText().trim();
                statusLabel.setText("로그인 성공: " + nickname);
                if (onLoginSuccess != null) onLoginSuccess.accept(client, nickname);
                close();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "로그인 실패: ID/비밀번호를 확인하세요.");
                alert.initOwner(this);
                alert.show();
                statusLabel.setText("로그인 실패");
            }
        });
    }

    private void stylePrimary(Button btn) {
        btn.setStyle("-fx-background-color: linear-gradient(to right, #FFD54F, #FFB300); -fx-text-fill: " + TEXT_MAIN + "; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-color: " + CARD_BORDER + "; -fx-border-radius: 10; -fx-border-width: 1;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: linear-gradient(to right, #FFE082, #FFC107); -fx-text-fill: " + TEXT_MAIN + "; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-color: " + CARD_BORDER + "; -fx-border-radius: 10; -fx-border-width: 1;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: linear-gradient(to right, #FFD54F, #FFB300); -fx-text-fill: " + TEXT_MAIN + "; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-color: " + CARD_BORDER + "; -fx-border-radius: 10; -fx-border-width: 1;"));
    }

    private void styleAccent(Button btn) {
        btn.setStyle("-fx-background-color: " + ACCENT + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-color: #1B5E20; -fx-border-radius: 10; -fx-border-width: 1;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #4FC3F7; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-color: #1B5E20; -fx-border-radius: 10; -fx-border-width: 1;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: " + ACCENT + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-color: #1B5E20; -fx-border-radius: 10; -fx-border-width: 1;"));
    }

    private void styleSecondary(Button btn) {
        btn.setStyle("-fx-background-color: #FFF; -fx-text-fill: " + TEXT_MAIN + "; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-color: " + CARD_BORDER + "; -fx-border-radius: 10; -fx-border-width: 1;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #FFF8E1; -fx-text-fill: " + TEXT_MAIN + "; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-color: " + CARD_BORDER + "; -fx-border-radius: 10; -fx-border-width: 1;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #FFF; -fx-text-fill: " + TEXT_MAIN + "; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-color: " + CARD_BORDER + "; -fx-border-radius: 10; -fx-border-width: 1;"));
    }
}
