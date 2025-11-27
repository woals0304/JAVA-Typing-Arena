package typingarena.app;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import typingarena.net.Message;
import typingarena.net.NetClient;

import java.util.Locale;
import java.util.Map;

/**
 * 멀티 매칭 UI를 메인 창 내부에 표시하기 위한 패널.
 * GAME_START_BROADCAST 수신 시 게임 스테이지를 열고, 로비는 그대로 유지하거나 상위 콜백으로 전환한다.
 */
public class MultiLobbyPane extends BorderPane {

    private static final String BG_COLOR = "#FDF5E6";
    private static final String CARD_BG = "#FFF3E0";
    private static final String CARD_BORDER = "#D7CCC8";
    private static final String TEXT_MAIN = "#4E342E";
    private static final String ACCENT = "#29B6F6";

    private final Font titleFont = Font.font("Malgun Gothic", FontWeight.EXTRA_BOLD, 18);
    private final Font labelFont = Font.font("Malgun Gothic", FontWeight.BOLD, 13);
    private final Font buttonFont = Font.font("Malgun Gothic", FontWeight.BOLD, 14);

    private final NetClient client;
    private final String myNickname;
    private final Runnable onBack;

    private final Label matchStatusLabel = new Label("매칭할 게임을 선택하세요.");
    private final Button tugBtn = new Button("줄다리기 (Tug of War)");
    private final Button landBtn = new Button("땅따먹기 (Land Grab)");
    private final Button castleBtn = new Button("성 지키기 (준비 중)");
    private final Button cancelBtn = new Button("매칭 취소");
    private final Button backBtn = new Button("◀ 돌아가기");

    private String currentGameType;
    private TugOfWarOnlineStage tugStage;
    private LandGrabOnlineStage landGrabStage;

    public MultiLobbyPane(NetClient client, String nickname, Runnable onBack) {
        this.client = client;
        this.myNickname = nickname != null ? nickname : "Player";
        this.onBack = onBack;

        client.setOnMessage(this::handleServerMessage);

        BorderPane card = new BorderPane();
        card.setPadding(new Insets(18));
        card.setTop(buildHeader());
        card.setCenter(buildCenter());
        card.setBottom(buildStatus());
        card.setStyle("-fx-background-color: " + CARD_BG + "; -fx-background-radius: 16; -fx-border-color: " + CARD_BORDER + "; -fx-border-radius: 16; -fx-border-width: 2;");

        setPadding(new Insets(18));
        setStyle("-fx-background-color: " + BG_COLOR + ";");
        setCenter(card);
    }

    private HBox buildHeader() {
        Label title = new Label("멀티 매칭");
        title.setFont(titleFont);
        title.setStyle("-fx-text-fill: " + TEXT_MAIN + ";");
        backBtn.setOnAction(e -> {
            cancelMatchmaking(true);
            if (onBack != null) onBack.run();
        });
        styleSecondary(backBtn);
        backBtn.setFont(buttonFont);
        HBox box = new HBox(10, backBtn, title);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(0, 0, 8, 0));
        return box;
    }

    private VBox buildCenter() {
        tugBtn.setPrefWidth(280);
        landBtn.setPrefWidth(280);
        castleBtn.setPrefWidth(280);
        tugBtn.setOnAction(e -> startMatch("TUG_OF_WAR"));
        landBtn.setOnAction(e -> startMatch("LAND_GRAB"));
        castleBtn.setDisable(true);

        styleAccent(tugBtn);
        stylePrimary(landBtn);
        styleSecondary(castleBtn);
        tugBtn.setFont(buttonFont);
        landBtn.setFont(buttonFont);
        castleBtn.setFont(buttonFont);

        VBox box = new VBox(12, tugBtn, landBtn, castleBtn);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private VBox buildStatus() {
        cancelBtn.setDisable(true);
        cancelBtn.setOnAction(e -> cancelMatchmaking(true));
        styleSecondary(cancelBtn);
        cancelBtn.setFont(buttonFont);
        matchStatusLabel.setPadding(new Insets(10, 0, 6, 0));
        matchStatusLabel.setStyle("-fx-text-fill: " + TEXT_MAIN + "; -fx-font-weight: bold;");
        matchStatusLabel.setFont(labelFont);
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
    }

    private String messageOf(Message msg) {
        if (msg.data != null && msg.data.get("message") != null) {
            return String.valueOf(msg.data.get("message"));
        }
        return "알 수 없는 오류가 발생했습니다.";
    }

    private void stylePrimary(Button btn) {
        String base = "-fx-background-color: linear-gradient(to right, #FFD54F, #FFB300); -fx-text-fill: " + TEXT_MAIN + "; -fx-font-weight: bold; -fx-padding: 10 16; -fx-background-radius: 10; -fx-border-color: " + CARD_BORDER + "; -fx-border-radius: 10; -fx-border-width: 1;";
        String hover = "-fx-background-color: linear-gradient(to right, #FFE082, #FFC107); -fx-text-fill: " + TEXT_MAIN + "; -fx-font-weight: bold; -fx-padding: 10 16; -fx-background-radius: 10; -fx-border-color: " + CARD_BORDER + "; -fx-border-radius: 10; -fx-border-width: 1;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
    }

    private void styleAccent(Button btn) {
        String base = "-fx-background-color: " + ACCENT + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 16; -fx-background-radius: 10; -fx-border-color: #1B5E20; -fx-border-radius: 10; -fx-border-width: 1;";
        String hover = "-fx-background-color: #4FC3F7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 16; -fx-background-radius: 10; -fx-border-color: #1B5E20; -fx-border-radius: 10; -fx-border-width: 1;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
    }

    private void styleSecondary(Button btn) {
        String base = "-fx-background-color: #FFF; -fx-text-fill: " + TEXT_MAIN + "; -fx-font-weight: bold; -fx-padding: 10 16; -fx-background-radius: 10; -fx-border-color: " + CARD_BORDER + "; -fx-border-radius: 10; -fx-border-width: 1;";
        String hover = "-fx-background-color: #FFF8E1; -fx-text-fill: " + TEXT_MAIN + "; -fx-font-weight: bold; -fx-padding: 10 16; -fx-background-radius: 10; -fx-border-color: " + CARD_BORDER + "; -fx-border-radius: 10; -fx-border-width: 1;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
    }
}
