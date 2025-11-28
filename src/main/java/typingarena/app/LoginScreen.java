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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
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

    private final Font titleFont = Font.font("Malgun Gothic", FontWeight.EXTRA_BOLD, 20);
    private final Font subtitleFont = Font.font("Malgun Gothic", FontWeight.NORMAL, 13);
    private final Font labelFont = Font.font("Malgun Gothic", FontWeight.BOLD, 13);
    private final Font buttonFont = Font.font("Malgun Gothic", FontWeight.BOLD, 14);

    private final TextField hostField = new TextField("127.0.0.1");
    private final TextField portField = new TextField("7777");
    private enum Mode { LOGIN, REGISTER }

    private final TextField idField = new TextField();
    private final PasswordField pwField = new PasswordField();
    private final Label nicknameLabel = new Label("Nickname(회원가입)");
    private final TextField nicknameField = new TextField();
    private final Button connectBtn = new Button("접속");
    private final Button modeLoginBtn = new Button("로그인");
    private final Button modeRegisterBtn = new Button("회원가입");
    private final Button submitBtn = new Button("로그인");
    private final Label statusLabel = new Label("서버에 먼저 접속하세요.");
    private Mode mode = Mode.LOGIN;

    private NetClient client;
    private final BiConsumer<NetClient, String> onLoginSuccess;

    public LoginScreen(Stage owner, BiConsumer<NetClient, String> onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("로그인 / 회원가입");

        VBox card = new VBox(18, buildHeader(), buildForm(), statusLabel);
        card.setPadding(new Insets(24));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: " + CARD_BG + "; -fx-background-radius: 18; -fx-border-color: " + CARD_BORDER + "; -fx-border-radius: 18; -fx-border-width: 2;");

        BorderPane root = new BorderPane(card);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: " + BG_COLOR + ";");

        statusLabel.setStyle("-fx-text-fill: #6D4C41;");
        statusLabel.setFont(subtitleFont);

        Scene scene = new Scene(root, 560, 500);
        setScene(scene);

        connectBtn.setOnAction(e -> connect());
        modeLoginBtn.setOnAction(e -> switchMode(Mode.LOGIN));
        modeRegisterBtn.setOnAction(e -> switchMode(Mode.REGISTER));
        submitBtn.setOnAction(e -> {
            if (mode == Mode.LOGIN) sendLogin();
            else sendRegister();
        });

        setOnCloseRequest(e -> {
            if (client != null) {
                try { client.close(); } catch (IOException ignored) {}
            }
        });
    }

    private VBox buildHeader() {
        Label title = new Label("타자 연습 게임");
        title.setFont(titleFont);
        title.setStyle("-fx-text-fill: " + TEXT_MAIN + ";");
        Label subtitle = new Label("서버 접속 후 로그인하거나 회원가입하세요.");
        subtitle.setFont(subtitleFont);
        subtitle.setStyle("-fx-text-fill: #6D4C41;");
        VBox box = new VBox(6, title, subtitle);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private VBox buildForm() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);

        Label hostLabel = new Label("Host");
        Label portLabel = new Label("Port");
        Label idLabel = new Label("ID");
        Label pwLabel = new Label("Password");

        grid.add(hostLabel, 0, 0);
        grid.add(hostField, 1, 0);
        grid.add(portLabel, 0, 1);
        grid.add(portField, 1, 1);
        grid.add(idLabel, 0, 2);
        grid.add(idField, 1, 2);
        grid.add(pwLabel, 0, 3);
        grid.add(pwField, 1, 3);
        grid.add(nicknameLabel, 0, 4);
        grid.add(nicknameField, 1, 4);

        hostField.setPrefWidth(160);
        portField.setPrefWidth(100);
        idField.setPrefWidth(180);
        pwField.setPrefWidth(180);
        nicknameField.setPrefWidth(180);
        grid.getChildren().stream()
                .filter(n -> n instanceof Label)
                .map(n -> (Label) n)
                .forEach(l -> l.setFont(labelFont));
        hostField.setFont(subtitleFont);
        portField.setFont(subtitleFont);
        idField.setFont(subtitleFont);
        pwField.setFont(subtitleFont);
        nicknameField.setFont(subtitleFont);

        stylePrimary(connectBtn);
        styleAccent(modeLoginBtn);
        styleSecondary(modeRegisterBtn);
        styleAccent(submitBtn);
        connectBtn.setFont(buttonFont);
        modeLoginBtn.setFont(buttonFont);
        modeRegisterBtn.setFont(buttonFont);
        submitBtn.setFont(buttonFont);
        connectBtn.setMinWidth(110);
        modeLoginBtn.setMinWidth(110);
        modeRegisterBtn.setMinWidth(110);
        submitBtn.setMinWidth(140);

        HBox modeRow = new HBox(8, modeLoginBtn, modeRegisterBtn);
        modeRow.setAlignment(Pos.CENTER_LEFT);
        modeRow.setPadding(new Insets(6, 0, 6, 0));

        HBox btnRow = new HBox(10, connectBtn, submitBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        submitBtn.setDisable(true);
        modeLoginBtn.setDisable(true);
        modeRegisterBtn.setDisable(true);

        VBox box = new VBox(12, modeRow, grid, btnRow);
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
        submitBtn.setDisable(false);
        modeLoginBtn.setDisable(false);
        modeRegisterBtn.setDisable(false);
        switchMode(Mode.LOGIN);
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

    private void switchMode(Mode mode) {
        this.mode = mode;
        boolean isRegister = mode == Mode.REGISTER;
        nicknameLabel.setVisible(isRegister);
        nicknameLabel.setManaged(isRegister);
        nicknameField.setVisible(isRegister);
        nicknameField.setManaged(isRegister);
        submitBtn.setText(isRegister ? "회원가입" : "로그인");
        modeLoginBtn.setDisable(!isConnected());
        modeRegisterBtn.setDisable(!isConnected());
        statusLabel.setText(isRegister ? "회원가입 모드입니다." : "로그인 모드입니다.");
    }

    private boolean isConnected() {
        return client != null;
    }

    private void stylePrimary(Button btn) {
        btn.setStyle("-fx-background-color: linear-gradient(to right, #FFD54F, #FFB300); -fx-text-fill: " + TEXT_MAIN + "; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-color: " + CARD_BORDER + "; -fx-border-radius: 10; -fx-border-width: 1;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: linear-gradient(to right, #FFE082, #FFC107); -fx-text-fill: " + TEXT_MAIN + "; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-color: " + CARD_BORDER + "; -fx-border-radius: 10; -fx-border-width: 1;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: linear-gradient(to right, #FFD54F, #FFB300); -fx-text-fill: " + TEXT_MAIN + "; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-color: " + CARD_BORDER + "; -fx-border-radius: 10; -fx-border-width: 1;"));
    }

    private void styleAccent(Button btn) {
        String base = "-fx-background-color: linear-gradient(to right, #4FC3F7, " + ACCENT + "); -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 16; -fx-background-radius: 10; -fx-border-color: #1B5E20; -fx-border-radius: 10; -fx-border-width: 1;";
        String hover = "-fx-background-color: linear-gradient(to right, #81D4FA, #4FC3F7); -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 16; -fx-background-radius: 10; -fx-border-color: #1B5E20; -fx-border-radius: 10; -fx-border-width: 1;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
    }

    private void styleSecondary(Button btn) {
        btn.setStyle("-fx-background-color: #FFF; -fx-text-fill: " + TEXT_MAIN + "; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-color: " + CARD_BORDER + "; -fx-border-radius: 10; -fx-border-width: 1;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #FFF8E1; -fx-text-fill: " + TEXT_MAIN + "; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-color: " + CARD_BORDER + "; -fx-border-radius: 10; -fx-border-width: 1;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #FFF; -fx-text-fill: " + TEXT_MAIN + "; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-color: " + CARD_BORDER + "; -fx-border-radius: 10; -fx-border-width: 1;"));
    }
}
