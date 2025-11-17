package typingarena.app;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField; // [추가]
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import typingarena.net.Message;
import typingarena.net.NetClient;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/**
 * 멀티플레이 매칭 창: 서버와 연결해서 게임 타입을 선택하고 자동 매칭을 요청한다.
 * [수정] LandGrabOnlineStage에 닉네임을 전달하도록 수정됨.
 */
public class MultiLobbyStage extends Stage {

    // --- UI 요소 ---
    // (UI 요소는 원본과 동일)
    private final TextField hostField = new TextField("127.0.0.1");
    private final TextField portField = new TextField("7777");
    private final Button connectBtn = new Button("서버 연결");
    private final Button disconnectBtn = new Button("연결 종료");
    private final TextField idField = new TextField();
    private final PasswordField pwField = new PasswordField();
    private final TextField nicknameField = new TextField(defaultNickname());
    private final Button loginBtn = new Button("로그인");
    private final Button registerBtn = new Button("회원가입");
    private VBox gameSelectBox;
    private final Label connectionLabel = new Label("서버에 연결되지 않았습니다.");
    private final Label matchStatusLabel = new Label("매칭할 게임을 선택하세요.");
    private final Button cancelMatchBtn = new Button("매칭 취소");

    // --- 로직 변수 ---
    private NetClient client;
    private String currentGameType;
    private String myNickname = "Player"; // [신규] 로그인 성공 시 닉네임을 저장할 필드

    private TugOfWarOnlineStage tugStage;
    private LandGrabOnlineStage landGrabStage;

    public MultiLobbyStage(Stage owner) {
        initOwner(owner);
        initModality(Modality.NONE);
        setTitle("멀티 플레이 매칭");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(24));

        root.setTop(buildConnectionPane());
        root.setCenter(buildGameSelectPane());
        root.setBottom(buildStatusPane());

        Scene scene = new Scene(root, 640, 520);
        setScene(scene);

        setOnShown(e -> connect());
        setOnHidden(e -> disconnect());
        setOnCloseRequest(e -> disconnect());
    }

    // (buildConnectionPane, buildGameSelectPane, createGameButton, buildStatusPane...
    //  ...이하 UI 빌드 메서드는 원본과 동일)

    private VBox buildConnectionPane() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        grid.add(new Label("Host"), 0, 0);
        grid.add(hostField, 1, 0);
        grid.add(new Label("Port"), 2, 0);
        grid.add(portField, 3, 0);
        grid.add(new Label("ID"), 0, 1);
        grid.add(idField, 1, 1);
        GridPane.setColumnSpan(idField, 3);
        grid.add(new Label("Password"), 0, 2);
        grid.add(pwField, 1, 2);
        GridPane.setColumnSpan(pwField, 3);
        grid.add(new Label("Nickname"), 0, 3);
        grid.add(nicknameField, 1, 3);
        GridPane.setColumnSpan(nicknameField, 3);
        nicknameField.setPromptText("회원가입 시에만 입력");

        hostField.setPrefWidth(160);
        portField.setPrefWidth(80);
        idField.setPrefWidth(160);
        pwField.setPrefWidth(160);
        nicknameField.setPrefWidth(160);

        connectBtn.setOnAction(e -> connect());
        disconnectBtn.setOnAction(e -> disconnect());
        loginBtn.setOnAction(e -> handleLogin());
        registerBtn.setOnAction(e -> handleRegister());

        HBox connectionControls = new HBox(10, connectBtn, disconnectBtn);
        connectionControls.setAlignment(Pos.CENTER_LEFT);
        HBox authControls = new HBox(10, loginBtn, registerBtn);
        authControls.setAlignment(Pos.CENTER_LEFT);

        loginBtn.setDisable(true);
        registerBtn.setDisable(true);

        VBox box = new VBox(10, grid, connectionControls, authControls, connectionLabel);
        connectionLabel.setStyle("-fx-text-fill: #555555;");
        return box;
    }

    private VBox buildGameSelectPane() {
        Label title = new Label("자동 매칭 (로그인 필요)");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button tugBtn = createGameButton("줄다리기 (Tug of War)", "TUG_OF_WAR");
        Button castleBtn = createGameButton("성 지키기 (준비 중)", "CASTLE_DEFENSE");
        castleBtn.setDisable(true);
        Button landBtn = createGameButton("땅따먹기 (Land Grab)", "LAND_GRAB");

        gameSelectBox = new VBox(15, title, tugBtn, castleBtn, landBtn);
        gameSelectBox.setPadding(new Insets(16, 0, 16, 0));
        gameSelectBox.setDisable(true);
        return gameSelectBox;
    }

    private Button createGameButton(String label, String gameType) {
        Button btn = new Button(label);
        btn.setPrefWidth(320);
        btn.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        btn.setOnAction(e -> startMatchmaking(gameType));
        return btn;
    }

    private VBox buildStatusPane() {
        cancelMatchBtn.setDisable(true);
        cancelMatchBtn.setOnAction(e -> cancelMatchmaking());
        matchStatusLabel.setStyle("-fx-text-fill: #0078FF;");
        VBox box = new VBox(10, matchStatusLabel, cancelMatchBtn);
        return box;
    }


    // (handleLogin, handleRegister, connect, disconnect, ensureConnected,
    //  startMatchmaking, cancelMatchmaking 메서드는 원본과 동일)

    private void handleLogin() {
        if (client == null) {
            showWarning("연결 오류", "먼저 '서버 연결'을 눌러주세요.");
            return;
        }
        String id = idField.getText().trim();
        String pw = pwField.getText().trim();
        if (id.isEmpty() || pw.isEmpty()) {
            showWarning("입력 오류", "ID와 Password를 모두 입력하세요.");
            return;
        }
        Message msg = Message.of("LOGIN_REQUEST");
        msg.data = Map.of("id", id, "pw", pw);
        client.send(msg);
        connectionLabel.setText("로그인 시도 중...");
    }

    private void handleRegister() {
        if (client == null) {
            showWarning("연결 오류", "먼저 '서버 연결'을 눌러주세요.");
            return;
        }
        String id = idField.getText().trim();
        String pw = pwField.getText().trim();
        String nickname = nicknameField.getText().trim();
        if (id.isEmpty() || pw.isEmpty() || nickname.isEmpty()) {
            showWarning("입력 오류", "ID, Password, Nickname을 모두 입력하세요.");
            return;
        }
        Message msg = Message.of("REGISTER_REQUEST");
        msg.data = Map.of("id", id, "pw", pw, "nickname", nickname);
        client.send(msg);
        connectionLabel.setText("회원가입 요청 중...");
    }

    private void connect() {
        if (client != null) {
            connectionLabel.setText("이미 서버에 연결되어 있습니다.");
            return;
        }
        try {
            int port = Integer.parseInt(portField.getText().trim());
            client = new NetClient(hostField.getText().trim(), port);
            client.setOnMessage(this::handleServerMessage);
            client.connect();
            connectionLabel.setText("서버에 연결되었습니다. 로그인/회원가입을 진행하세요.");
            loginBtn.setDisable(false);
            registerBtn.setDisable(false);
        } catch (NumberFormatException e) {
            connectionLabel.setText("포트 번호가 올바르지 않습니다.");
        } catch (IOException e) {
            connectionLabel.setText("연결 실패: " + e.getMessage());
            client = null;
        }
    }

    private void disconnect() {
        cancelMatchmaking(false);
        if (client != null) {
            try { client.close(); } catch (IOException ignored) {}
            client = null;
        }
        connectionLabel.setText("서버 연결이 종료되었습니다.");
        loginBtn.setDisable(true);
        registerBtn.setDisable(true);
        gameSelectBox.setDisable(true);
        matchStatusLabel.setText("매칭할 게임을 선택하세요.");
    }

    private boolean ensureConnected() {
        if (client != null) return true;
        connect();
        return client != null;
    }

    private void startMatchmaking(String gameType) {
        if (!ensureConnected()) return;
        cancelMatchmaking(false);
        currentGameType = gameType;
        Message msg = Message.of("MATCH_REQUEST");
        msg.data = Map.of("gameType", gameType);
        client.send(msg);
        cancelMatchBtn.setDisable(false);
        matchStatusLabel.setText("[" + gameType + "] 매칭을 찾는 중...");
    }

    private void cancelMatchmaking() {
        cancelMatchmaking(true);
    }

    private void cancelMatchmaking(boolean informServer) {
        if (currentGameType == null) return;
        if (informServer && client != null) {
            Message msg = Message.of("MATCH_CANCEL");
            msg.data = Map.of("gameType", currentGameType);
            client.send(msg);
        }
        currentGameType = null;
        cancelMatchBtn.setDisable(true);
        matchStatusLabel.setText("매칭할 게임을 선택하세요.");
    }

    /**
     * [수정] 로그인/회원가입 응답 처리가 추가된 핸들러
     */
    private void handleServerMessage(Message msg) {
        if (msg == null || msg.type == null) return;
        String type = msg.type.toUpperCase(Locale.ROOT);

        Platform.runLater(() -> {
            switch (type) {
                case "REGISTER_RESPONSE" -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = msg.data;
                    boolean success = (boolean) data.get("success");
                    if (success) {
                        showWarning("회원가입 성공", "회원가입에 성공했습니다. 이제 로그인해 주세요.");
                    } else {
                        showWarning("회원가입 실패", "ID 또는 닉네임이 중복되었거나, 서버 오류입니다.");
                    }
                }

                case "LOGIN_RESPONSE" -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = msg.data;
                    boolean success = (boolean) data.get("success");

                    if (success) {
                        String nickname = (String) data.get("nickname");

                        // [신규] 닉네임 필드에 저장
                        this.myNickname = nickname;

                        connectionLabel.setText(myNickname + "님, 환영합니다! (로그인됨)");
                        connectionLabel.setStyle("-fx-text-fill: #008800;");
                        nicknameField.setText(myNickname);

                        gameSelectBox.setDisable(false);
                        ((Label) gameSelectBox.getChildren().get(0)).setText("자동 매칭");

                        matchStatusLabel.setText("로그인 성공! 매칭할 게임을 선택하세요.");

                        idField.setDisable(true);
                        pwField.setDisable(true);
                        loginBtn.setDisable(true);
                        registerBtn.setDisable(true);
                        hostField.setDisable(true);
                        portField.setDisable(true);

                    } else {
                        showWarning("로그인 실패", "ID 또는 비밀번호가 일치하지 않습니다.");
                        connectionLabel.setText("로그인 실패. 다시 시도하세요.");
                        connectionLabel.setStyle("-fx-text-fill: #AA0000;");
                    }
                }

                case "MATCH_WAITING" -> matchStatusLabel.setText("상대를 찾는 중입니다...");
                case "MATCH_SUCCESS" -> matchStatusLabel.setText("매칭 성공! 게임 시작을 기다리는 중...");
                case "MATCH_CANCELLED" -> {
                    cancelMatchBtn.setDisable(true);
                    matchStatusLabel.setText("매칭이 취소되었습니다.");
                }

                case "MATCH_REQUEST_ERROR", "ERROR" -> {
                    String errorMsg = messageOf(msg);
                    showWarning("오류", errorMsg);
                    matchStatusLabel.setText(errorMsg);
                }

                case "GAME_START_BROADCAST" -> handleGameStart(msg);
                case "GAME_UPDATE_BROADCAST" -> handleGameUpdate(msg);
                case "GAME_END_BROADCAST" -> handleGameEnd(msg);
                case "MATCH_SUCCESS_ERROR" -> showWarning("매칭 오류", messageOf(msg));
                default -> {}
            }
        });
    }


    /**
     * [수정] handleGameStart: 게임 타입에 따라 적절한 Stage를 띄움
     */
    private void handleGameStart(Message msg) {
        String gameType = msg.data != null ? String.valueOf(msg.data.get("gameType")) : null;

        if ("TUG_OF_WAR".equalsIgnoreCase(gameType)) {
            if (tugStage == null) {
                tugStage = new TugOfWarOnlineStage(client);
            }
            tugStage.handleMessage(msg);
            if (!tugStage.isShowing()) tugStage.show();

        } else if ("LAND_GRAB".equalsIgnoreCase(gameType)) {
            if (landGrabStage == null) {
                // [수정] 생성자에 'myNickname' 전달 (오류 수정)
                landGrabStage = new LandGrabOnlineStage(client, myNickname);
            }
            landGrabStage.handleMessage(msg);
            if (!landGrabStage.isShowing()) landGrabStage.show();

        } else {
            showWarning("게임 시작 오류", "알 수 없는 게임 타입입니다: " + gameType);
            return;
        }

        matchStatusLabel.setText("온라인 " + gameType + " 진행 중...");
        cancelMatchBtn.setDisable(true);
        currentGameType = null;
    }

    /**
     * [수정] handleGameUpdate: 모든 활성 게임 Stage에 메시지 전달
     */
    private void handleGameUpdate(Message msg) {
        if (tugStage != null) {
            tugStage.handleMessage(msg);
        }
        if (landGrabStage != null) {
            landGrabStage.handleMessage(msg);
        }
    }

    /**
     * [수정] handleGameEnd: 모든 활성 게임 Stage에 메시지 전달
     */
    private void handleGameEnd(Message msg) {
        if (tugStage != null) {
            tugStage.handleMessage(msg);
        }
        if (landGrabStage != null) {
            landGrabStage.handleMessage(msg);
        }
        matchStatusLabel.setText("경기가 종료되었습니다. (새 게임을 선택하세요)");
    }

    // --- 헬퍼 메서드 (기존과 동일) ---

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setTitle(title);
        alert.initOwner(this);
        alert.show();
    }

    private String messageOf(Message msg) {
        if (msg.data != null && msg.data.get("message") != null) {
            return String.valueOf(msg.data.get("message"));
        }
        return "알 수 없는 오류가 발생했습니다.";
    }

    private String defaultNickname() {
        return "Player" + (int) (Math.random() * 1000);
    }
}