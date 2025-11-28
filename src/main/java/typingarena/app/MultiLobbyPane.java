package typingarena.app;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import typingarena.net.Message;
import typingarena.net.NetClient;

import java.io.InputStream;
import java.util.Locale;
import java.util.Map;

/**
 * 멀티 매칭 UI를 메인 창 내부에 표시하기 위한 패널.
 * GAME_START_BROADCAST 수신 시 게임 스테이지를 열고, 로비는 그대로 유지하거나 상위 콜백으로 전환한다.
 */
public class MultiLobbyPane extends BorderPane {

    private static final String CARD_BORDER = "#D7CCC8";
    private static final String TEXT_MAIN = "#4E342E";
    private static final String ACCENT = "#29B6F6";
    private static final String FRAME_DARK = "#3E2723";

    private String gameFontFamily = "Malgun Gothic";
    private Font titleFont;
    private Font labelFont;
    private Font buttonFont;
    private Font overlineFont;

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

        initFonts();
        client.setOnMessage(this::handleServerMessage);

        BorderPane card = new BorderPane();
        card.setPadding(new Insets(22, 26, 22, 26));
        card.setTop(buildHeader());
        card.setCenter(buildCenter());
        card.setBottom(buildStatus());
        card.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, rgba(255,243,224,0.96), rgba(250,220,180,0.96)); -fx-background-radius: 22; -fx-border-color: #5D4037; -fx-border-radius: 22; -fx-border-width: 3; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 18, 0.24, 0, 8);");

        StackPane frame = new StackPane();
        frame.setPadding(new Insets(16));
        frame.setStyle("-fx-background-color: linear-gradient(" + FRAME_DARK + " 0%, #2A1B14 100%); -fx-background-radius: 26; -fx-border-color: #5D4037; -fx-border-width: 2; -fx-border-radius: 26; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 28, 0.35, 0, 14);");
        frame.getChildren().add(card);

        setPadding(new Insets(28));
        setStyle("-fx-background-color: radial-gradient(center 50% 50%, radius 80%, #4A2F23, #1D120D 90%);");
        setCenter(frame);
    }

    private VBox buildHeader() {
        Label overline = new Label("LIVE MATCHING READY");
        overline.setFont(overlineFont);
        overline.setStyle("-fx-text-fill: #FFD54F; -fx-letter-spacing: 0.8;");

        Label title = new Label("멀티플레이 타자 미니게임 로비");
        title.setFont(titleFont);
        title.setStyle("-fx-text-fill: linear-gradient(#FFE082, #FFB300); -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 0, 0, 0, 3);");
        StackPane titlePlate = new StackPane(title);
        titlePlate.setPadding(new Insets(10, 18, 10, 18));
        titlePlate.setStyle("-fx-background-color: linear-gradient(#5D4037, #3E2723); -fx-background-radius: 18; -fx-border-color: #FFB300; -fx-border-radius: 18; -fx-border-width: 2;");
        titlePlate.setEffect(new DropShadow(12, Color.web("#00000044")));

        backBtn.setOnAction(e -> {
            cancelMatchmaking(true);
            if (onBack != null) onBack.run();
        });
        styleSecondary(backBtn);
        backBtn.setFont(buttonFont);

        HBox titleRow = new HBox(12, backBtn, titlePlate);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(6, overline, titleRow);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(0, 0, 10, 0));
        return box;
    }

    private VBox buildCenter() {
        tugBtn.setText("⚔ 줄다리기 (Tug of War)");
        landBtn.setText("🧭 땅따먹기 (Land Grab)");
        castleBtn.setText("🛡 성 지키기 (준비 중)");

        tugBtn.setPrefWidth(320);
        landBtn.setPrefWidth(320);
        castleBtn.setPrefWidth(320);
        tugBtn.setOnAction(e -> startMatch("TUG_OF_WAR"));
        landBtn.setOnAction(e -> startMatch("LAND_GRAB"));
        castleBtn.setDisable(true);

        styleAccent(tugBtn);
        stylePrimary(landBtn);
        styleSecondary(castleBtn);
        tugBtn.setFont(buttonFont);
        landBtn.setFont(buttonFont);
        castleBtn.setFont(buttonFont);

        Label sectionTitle = new Label("매치 타입 선택");
        sectionTitle.setFont(gameFont(16, FontWeight.EXTRA_BOLD));
        sectionTitle.setStyle("-fx-text-fill: " + TEXT_MAIN + ";");

        VBox buttons = new VBox(12, tugBtn, landBtn, castleBtn);
        buttons.setAlignment(Pos.CENTER);

        VBox box = new VBox(10, sectionTitle, buttons);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(14, 12, 14, 12));
        box.setStyle("-fx-background-color: rgba(255,255,255,0.82); -fx-background-radius: 16; -fx-border-color: rgba(93,64,55,0.35); -fx-border-radius: 16; -fx-border-width: 1.5; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 12, 0.18, 0, 6);");
        return box;
    }

    private VBox buildStatus() {
        cancelBtn.setDisable(true);
        cancelBtn.setOnAction(e -> cancelMatchmaking(true));
        styleSecondary(cancelBtn);
        cancelBtn.setFont(buttonFont);
        matchStatusLabel.setPadding(new Insets(6, 0, 6, 0));
        matchStatusLabel.setStyle("-fx-text-fill: " + TEXT_MAIN + "; -fx-font-weight: bold;");
        matchStatusLabel.setFont(labelFont);

        StackPane statusPlate = new StackPane(matchStatusLabel);
        statusPlate.setPadding(new Insets(10, 16, 10, 16));
        statusPlate.setStyle("-fx-background-color: linear-gradient(#FFF8E1, #FFE082); -fx-background-radius: 12; -fx-border-color: #D18816; -fx-border-radius: 12; -fx-border-width: 2;");
        statusPlate.setEffect(new DropShadow(12, Color.web("#00000033")));

        HBox row = new HBox(12, statusPlate, cancelBtn);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(8, row);
        box.setPadding(new Insets(8, 0, 0, 0));
        return box;
    }

    private void initFonts() {
        try (InputStream is = getClass().getResourceAsStream("/fonts/CookieRun Regular.otf")) {
            Font loaded = is != null ? Font.loadFont(is, 22) : null;
            if (loaded != null) {
                gameFontFamily = loaded.getFamily();
            }
        } catch (Exception ignored) {}
        titleFont = gameFont(26, FontWeight.EXTRA_BOLD);
        labelFont = gameFont(13, FontWeight.BOLD);
        buttonFont = gameFont(15, FontWeight.BOLD);
        overlineFont = gameFont(12, FontWeight.EXTRA_BOLD);
    }

    private Font gameFont(double size, FontWeight weight) {
        return Font.font(gameFontFamily, weight, size);
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
        String base = "-fx-background-color: linear-gradient(#FFEE58, #FBC02D), linear-gradient(#FBC02D, #F57F17); -fx-text-fill: " + TEXT_MAIN + "; -fx-font-weight: bold; -fx-padding: 12 18; -fx-background-radius: 14; -fx-border-color: #8D6E63; -fx-border-radius: 14; -fx-border-width: 1.5;";
        String hover = "-fx-background-color: linear-gradient(#FFF59D, #FBC02D), linear-gradient(#FFECB3, #FFC107); -fx-text-fill: " + TEXT_MAIN + "; -fx-font-weight: bold; -fx-padding: 12 18; -fx-background-radius: 14; -fx-border-color: #8D6E63; -fx-border-radius: 14; -fx-border-width: 1.5;";
        btn.setStyle(base);
        btn.setEffect(new DropShadow(12, Color.web("#00000033")));
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
    }

    private void styleAccent(Button btn) {
        String base = "-fx-background-color: linear-gradient(#64B5F6, #1E88E5), linear-gradient(" + ACCENT + ", #0D47A1); -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12 18; -fx-background-radius: 14; -fx-border-color: #0D47A1; -fx-border-radius: 14; -fx-border-width: 1.5;";
        String hover = "-fx-background-color: linear-gradient(#90CAF9, #42A5F5), linear-gradient(#29B6F6, #1565C0); -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12 18; -fx-background-radius: 14; -fx-border-color: #0D47A1; -fx-border-radius: 14; -fx-border-width: 1.5;";
        btn.setStyle(base);
        btn.setEffect(new DropShadow(12, Color.web("#00000033")));
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
    }

    private void styleSecondary(Button btn) {
        String base = "-fx-background-color: linear-gradient(#F5F0E6, #E8DCC8); -fx-text-fill: " + TEXT_MAIN + "; -fx-font-weight: bold; -fx-padding: 10 16; -fx-background-radius: 12; -fx-border-color: " + CARD_BORDER + "; -fx-border-radius: 12; -fx-border-width: 1.2;";
        String hover = "-fx-background-color: linear-gradient(#FFF8E1, #E8DCC8); -fx-text-fill: " + TEXT_MAIN + "; -fx-font-weight: bold; -fx-padding: 10 16; -fx-background-radius: 12; -fx-border-color: " + CARD_BORDER + "; -fx-border-radius: 12; -fx-border-width: 1.2;";
        btn.setStyle(base);
        btn.setEffect(new DropShadow(10, Color.web("#00000022")));
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
    }
}
