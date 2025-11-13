package typingarena.app;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
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
 */
public class MultiLobbyStage extends Stage {

    private final TextField hostField = new TextField("127.0.0.1");
    private final TextField portField = new TextField("7777");
    private final TextField nicknameField = new TextField(defaultNickname());
    private final Label connectionLabel = new Label("서버에 연결되지 않았습니다.");
    private final Label matchStatusLabel = new Label("매칭할 게임을 선택하세요.");
    private final Button cancelMatchBtn = new Button("매칭 취소");

    private NetClient client;
    private String currentGameType;

    // [신규] 각 온라인 게임 Stage를 관리
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

        Scene scene = new Scene(root, 640, 420);
        setScene(scene);

        setOnShown(e -> connect());
        setOnHidden(e -> disconnect());
        setOnCloseRequest(e -> disconnect());
    }

    private VBox buildConnectionPane() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(6);

        grid.add(new Label("Host"), 0, 0);
        grid.add(hostField, 1, 0);
        grid.add(new Label("Port"), 2, 0);
        grid.add(portField, 3, 0);
        grid.add(new Label("Nickname"), 0, 1);
        grid.add(nicknameField, 1, 1);
        GridPane.setColumnSpan(nicknameField, 3);

        hostField.setPrefWidth(160);
        portField.setPrefWidth(80);
        nicknameField.setPrefWidth(160);

        Button connectBtn = new Button("연결");
        connectBtn.setOnAction(e -> connect());
        Button disconnectBtn = new Button("연결 종료");
        disconnectBtn.setOnAction(e -> disconnect());
        HBox controls = new HBox(10, connectBtn, disconnectBtn);
        controls.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(8, grid, controls, connectionLabel);
        connectionLabel.setStyle("-fx-text-fill: #555555;");
        return box;
    }

    private VBox buildGameSelectPane() {
        Label title = new Label("자동 매칭");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button tugBtn = createGameButton("줄다리기 (Tug of War)", "TUG_OF_WAR");

        Button castleBtn = createGameButton("성 지키기 (준비 중)", "CASTLE_DEFENSE");
        castleBtn.setDisable(true);

        // [수정] 땅따먹기 버튼 활성화
        Button landBtn = createGameButton("땅따먹기 (Land Grab)", "LAND_GRAB");
        // landBtn.setDisable(true); // [제거]

        VBox box = new VBox(15, title, tugBtn, castleBtn, landBtn);
        box.setPadding(new Insets(16, 0, 16, 0));
        return box;
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

    // (connect, disconnect, ensureConnected, startMatchmaking, cancelMatchmaking
    //  ... 메서드는 원본과 동일)

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
            connectionLabel.setText("서버에 연결되었습니다.");
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
        String nickname = nicknameField.getText().trim().isEmpty()
                ? defaultNickname()
                : nicknameField.getText().trim();
        Message msg = Message.of("MATCH_REQUEST");
        msg.data = Map.of("gameType", gameType, "nickname", nickname);
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


    private void handleServerMessage(Message msg) {
        if (msg == null || msg.type == null) return;
        String type = msg.type.toUpperCase(Locale.ROOT);
        Platform.runLater(() -> {
            switch (type) {
                case "MATCH_WAITING" -> matchStatusLabel.setText("상대를 찾는 중입니다...");
                case "MATCH_SUCCESS" -> matchStatusLabel.setText("매칭 성공! 게임 시작을 기다리는 중...");
                case "MATCH_CANCELLED" -> {
                    cancelMatchBtn.setDisable(true);
                    matchStatusLabel.setText("매칭이 취소되었습니다.");
                }
                case "MATCH_REQUEST_ERROR" -> showWarning("매칭 오류", messageOf(msg));
                case "GAME_START_BROADCAST" -> handleGameStart(msg);
                case "GAME_UPDATE_BROADCAST" -> handleGameUpdate(msg);
                case "GAME_END_BROADCAST" -> handleGameEnd(msg);
                case "MATCH_SUCCESS_ERROR" -> showWarning("매칭 오류", messageOf(msg));
                default -> {}
            }
        });
    }

    // [제거] ensureTugStage(), isTugMessage() 메서드 제거

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
                landGrabStage = new LandGrabOnlineStage(client);
            }
            landGrabStage.handleMessage(msg);
            if (!landGrabStage.isShowing()) landGrabStage.show();

        } else {
            // (지원하지 않는 게임 타입)
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
        matchStatusLabel.setText("경기가 종료되었습니다.");
    }

    // (showWarning, messageOf, defaultNickname 헬퍼 메서드는 원본과 동일)

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