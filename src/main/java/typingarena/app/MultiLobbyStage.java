package typingarena.app;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import typingarena.app.LandGrabOnlineStage;
import typingarena.net.Message;
import typingarena.net.NetClient;

import java.util.Locale;
import java.util.Map;

/**
 * 멀티플레이 매칭 창: 로그인 완료 후 매칭 요청만 담당.
 * GAME_START_BROADCAST를 수신하면 로비를 닫고 실제 게임 스테이지를 연다.
 */
public class MultiLobbyStage extends Stage {

    private static final String BG_COLOR = "#FDF5E6";
    private static final String CARD_BG = "#FFF3E0";
    private static final String CARD_BORDER = "#D7CCC8";
    private static final String TEXT_MAIN = "#4E342E";
    private static final String ACCENT = "#29B6F6";

    private final NetClient client;
    private final String myNickname;

    private final Label matchStatusLabel = new Label("매칭할 게임을 선택하세요.");
    private final Button tugBtn = new Button("줄다리기 (Tug of War)");
    private final Button landBtn = new Button("땅따먹기 (Land Grab)");
    private final Button cancelBtn = new Button("매칭 취소");

    private String currentGameType;
    private TugOfWarOnlineStage tugStage;
    private LandGrabOnlineStage landGrabStage;

    public MultiLobbyStage(Stage owner, NetClient client, String nickname) {
        this.client = client;
        this.myNickname = nickname != null ? nickname : "Player";
        initOwner(owner);
        initModality(Modality.NONE);
        setTitle("멀티 플레이 매칭");

        client.setOnMessage(this::handleServerMessage);

        BorderPane card = new BorderPane();
        card.setPadding(new Insets(18));
        card.setCenter(buildCenter());
        card.setBottom(buildStatus());
        card.setStyle("-fx-background-color: " + CARD_BG + "; -fx-background-radius: 16; -fx-border-color: " + CARD_BORDER + "; -fx-border-radius: 16; -fx-border-width: 2;");

        BorderPane root = new BorderPane(card);
        root.setPadding(new Insets(18));
        root.setStyle("-fx-background-color: " + BG_COLOR + ";");

        Scene scene = new Scene(root, 440, 320);
        setScene(scene);
        setOnCloseRequest(e -> cancelMatchmaking(true));
    }

    private VBox buildCenter() {
        tugBtn.setPrefWidth(280);
        landBtn.setPrefWidth(280);
        tugBtn.setOnAction(e -> startMatch("TUG_OF_WAR"));
        landBtn.setOnAction(e -> startMatch("LAND_GRAB"));

        styleAccent(tugBtn);
        stylePrimary(landBtn);

        VBox box = new VBox(12, tugBtn, landBtn);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private VBox buildStatus() {
        cancelBtn.setDisable(true);
        cancelBtn.setOnAction(e -> cancelMatchmaking(true));
        styleSecondary(cancelBtn);
        matchStatusLabel.setPadding(new Insets(10, 0, 6, 0));
        matchStatusLabel.setStyle("-fx-text-fill: " + TEXT_MAIN + "; -fx-font-weight: bold;");
        VBox box = new VBox(6, matchStatusLabel, cancelBtn);
        box.setPadding(new Insets(10));
        return box;
    }

    private void startMatch(String gameType) {
        currentGameType = gameType;
        matchStatusLabel.setText("[" + gameType + "] 매칭 요청 전송...");
        cancelBtn.setDisable(false);
        Message msg = Message.of("MATCH_REQUEST");
        msg.data = Map.of("gameType", gameType);
        client.send(msg);
    }

    private void cancelMatchmaking(boolean informServer) {
        if (currentGameType != null && informServer) {
            Message msg = Message.of("MATCH_CANCEL");
            msg.data = Map.of("gameType", currentGameType);
            client.send(msg);
        }
        currentGameType = null;
        matchStatusLabel.setText("매칭할 게임을 선택하세요.");
        cancelBtn.setDisable(true);
    }

    private void handleServerMessage(Message msg) {
        if (msg == null || msg.type == null) return;
        String type = msg.type.toUpperCase(Locale.ROOT);
        Platform.runLater(() -> {
            switch (type) {
                case "MATCH_WAITING" -> matchStatusLabel.setText("상대를 찾는 중...");
                case "MATCH_SUCCESS" -> matchStatusLabel.setText("매칭 성공! 게임 시작 신호 대기...");
                case "MATCH_CANCELLED" -> {
                    matchStatusLabel.setText("매칭이 취소되었습니다.");
                    cancelBtn.setDisable(true);
                    currentGameType = null;
                }
                case "GAME_START_BROADCAST" -> handleGameStart(msg);
                case "GAME_UPDATE_BROADCAST" -> forwardToGames(msg);
                case "GAME_END_BROADCAST" -> forwardToGames(msg);
                case "MATCH_REQUEST_ERROR", "ERROR" -> matchStatusLabel.setText(messageOf(msg));
                default -> {}
            }
        });
    }

    private void forwardToGames(Message msg) {
        if (tugStage != null) tugStage.handleMessage(msg);
        if (landGrabStage != null) landGrabStage.handleMessage(msg);
    }

    private void handleGameStart(Message msg) {
        Object g = msg.data != null ? msg.data.get("gameType") : null;
        String gameType = g != null ? String.valueOf(g) : "";

        if ("TUG_OF_WAR".equalsIgnoreCase(gameType)) {
            if (tugStage == null) tugStage = new TugOfWarOnlineStage(client);
            tugStage.handleMessage(msg);
            tugStage.show();
        } else if ("LAND_GRAB".equalsIgnoreCase(gameType)) {
            if (landGrabStage == null) landGrabStage = new LandGrabOnlineStage(client, myNickname);
            landGrabStage.handleMessage(msg);
            landGrabStage.show();
        } else {
            matchStatusLabel.setText("알 수 없는 게임 타입: " + gameType);
            return;
        }

        cancelBtn.setDisable(true);
        currentGameType = null;
        // 로비 창 닫기
        close();
    }

    private String messageOf(Message msg) {
        if (msg.data != null && msg.data.get("message") != null) {
            return String.valueOf(msg.data.get("message"));
        }
        return "알 수 없는 오류가 발생했습니다.";
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
