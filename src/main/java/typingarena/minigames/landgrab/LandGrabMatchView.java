package typingarena.minigames.landgrab;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class LandGrabMatchView {

    private final StackPane mainStack = new StackPane();
    private final BorderPane gameRoot = new BorderPane();
    private final LandGrabPanel landGrabPanel;

    // --- HUD Components ---
    private final Text txtTime = new Text("60.0s");
    private final Text txtMyName = new Text("Player");
    private final Text txtMyScore = new Text("0");
    private final Text txtOppName = new Text("Opponent");
    private final Text txtAiScore = new Text("0");

    // 콤보 UI
    private final Label lblComboCount = new Label("0");
    private final Label lblComboLabel = new Label("COMBO");
    private final StackPane comboBadgePane = new StackPane();
    private final Polygon comboHexagon = new Polygon();
    private final VBox comboTextBox = new VBox(-5);

    // 하단 입력창
    private final Label lblStatus = new Label("Ready?");
    private final TextField inputField = new TextField();
    private final HBox controlBox = new HBox(10);

    // --- Game Over Overlay ---
    private final VBox gameOverOverlay = new VBox(20);
    private final Label lblResultTitle = new Label("VICTORY");
    private final Label lblResultReason = new Label("");
    private final Label lblResultScore = new Label("");
    private final Button btnRematch = new Button("재경기 신청");
    private final Button btnQuit = new Button("나가기");
    private final Label lblRematchNoti = new Label("상대방이 재경기를 원합니다!");

    // 애니메이션 객체
    private FadeTransition blinkAnimation;

    public LandGrabMatchView() {
        this.landGrabPanel = new LandGrabPanel();

        gameRoot.setStyle("-fx-background-color: #FFF3E0;");

        // 상단 헤더
        StackPane headerContainer = createStyledHeader();
        gameRoot.setTop(headerContainer);

        // 좌우 패널
        VBox leftPanel = createLeftInfoPanel();
        leftPanel.setMinWidth(220);
        gameRoot.setLeft(leftPanel);

        VBox rightPanel = createRightInfoPanel();
        rightPanel.setMinWidth(220);
        gameRoot.setRight(rightPanel);

        // 중앙 게임판
        StackPane centerWrapper = new StackPane(landGrabPanel);
        centerWrapper.setPadding(new Insets(10));
        centerWrapper.setEffect(new DropShadow(25, Color.rgb(0,0,0,0.25)));
        centerWrapper.setMinSize(0, 0);
        gameRoot.setCenter(centerWrapper);

        // 하단 입력바 (여기서 호출됨)
        setupInputBar();
        gameRoot.setBottom(controlBox);

        // 오버레이
        setupGameOverOverlay();

        mainStack.getChildren().addAll(gameRoot, gameOverOverlay);
    }

    // [누락되었던 메서드 복구 완료]
    private void setupInputBar() {
        inputField.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 18));
        inputField.setPromptText("단어를 입력하세요...");
        inputField.setStyle("-fx-background-radius: 30; -fx-background-color: white; -fx-border-color: #BA68C8; -fx-border-width: 2px; -fx-border-radius: 30; -fx-padding: 8 20 8 20; -fx-text-fill: #333;");
        HBox.setHgrow(inputField, Priority.ALWAYS);
        controlBox.setAlignment(Pos.CENTER);
        controlBox.setPadding(new Insets(15, 20, 15, 20));
        controlBox.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-background-radius: 20 20 0 0;");
        controlBox.getChildren().add(inputField);
    }

    private StackPane createStyledHeader() {
        StackPane bg = new StackPane();
        bg.setMaxHeight(90);
        bg.setStyle("-fx-background-color: rgba(255,255,255,0.85); -fx-background-radius: 0 0 40 40; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 15, 0, 0, 5);");

        HBox content = new HBox(40);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(10, 50, 15, 50));

        VBox timeBox = new VBox(0);
        timeBox.setAlignment(Pos.CENTER);
        Text lblTimeTitle = new Text("TIME");
        lblTimeTitle.setFont(Font.font("Impact", 16)); lblTimeTitle.setFill(Color.GRAY);

        txtTime.setFont(Font.font("CookieRun Regular", 32));
        txtTime.setFill(Color.web("#FF6D00"));
        txtTime.setEffect(new DropShadow(2, Color.WHITE));
        timeBox.getChildren().addAll(lblTimeTitle, txtTime);

        HBox scoreBoard = new HBox(20);
        scoreBoard.setAlignment(Pos.CENTER);
        StackPane myScorePane = createScoreBadge(txtMyName, txtMyScore, Color.web("#0288D1"));
        StackPane oppScorePane = createScoreBadge(txtOppName, txtAiScore, Color.web("#D32F2F"));

        Text txtVS = new Text("VS");
        txtVS.setFont(Font.font("Impact", 48));
        txtVS.setFill(new LinearGradient(0,0,0,1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.LIGHTGRAY), new Stop(1, Color.GRAY)));
        txtVS.setEffect(new DropShadow(3, Color.rgb(0,0,0,0.3)));

        scoreBoard.getChildren().addAll(myScorePane, txtVS, oppScorePane);
        setupRhythmComboUI();
        content.getChildren().addAll(timeBox, scoreBoard, comboBadgePane);

        StackPane root = new StackPane(bg, content);
        lblStatus.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 18));
        lblStatus.setTextFill(Color.web("#E65100"));
        lblStatus.setTranslateY(60);

        root.getChildren().add(lblStatus);
        StackPane.setAlignment(lblStatus, Pos.BOTTOM_CENTER);
        StackPane.setMargin(lblStatus, new Insets(0,0,-30,0));

        return root;
    }

    private StackPane createScoreBadge(Text nameTxt, Text scoreTxt, Color themeColor) {
        StackPane badge = new StackPane();
        Circle bg = new Circle(40, Color.WHITE);
        bg.setStroke(themeColor); bg.setStrokeWidth(3); bg.setEffect(new DropShadow(5, Color.rgb(0,0,0,0.1)));
        VBox box = new VBox(-2); box.setAlignment(Pos.CENTER);
        nameTxt.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 14)); nameTxt.setFill(Color.DARKGRAY);
        scoreTxt.setFont(Font.font("Impact", 34)); scoreTxt.setFill(themeColor);
        scoreTxt.setStroke(null); scoreTxt.setEffect(new DropShadow(2, 2, 2, Color.rgb(0,0,0,0.2)));
        box.getChildren().addAll(scoreTxt, nameTxt);
        badge.getChildren().addAll(bg, box);
        return badge;
    }

    private void setupGameOverOverlay() {
        gameOverOverlay.setAlignment(Pos.CENTER);
        gameOverOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85);");
        gameOverOverlay.setVisible(false);

        lblResultTitle.setFont(Font.font("Impact", 70));
        lblResultTitle.setEffect(new Glow(0.8));

        lblResultReason.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 20));
        lblResultReason.setTextFill(Color.LIGHTGRAY);

        lblResultScore.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 28));
        lblResultScore.setTextFill(Color.WHITE);

        styleButton(btnRematch, "#4CAF50");
        styleButton(btnQuit, "#F44336");

        lblRematchNoti.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 22));
        lblRematchNoti.setTextFill(Color.rgb(0, 255, 255));
        lblRematchNoti.setStyle("-fx-effect: dropshadow(gaussian, cyan, 15, 0.5, 0, 0);");
        lblRematchNoti.setVisible(false);

        blinkAnimation = new FadeTransition(Duration.seconds(0.4), lblRematchNoti);
        blinkAnimation.setFromValue(1.0); blinkAnimation.setToValue(0.2);
        blinkAnimation.setAutoReverse(true);
        blinkAnimation.setCycleCount(FadeTransition.INDEFINITE);

        HBox btnBox = new HBox(30, btnQuit, btnRematch);
        btnBox.setAlignment(Pos.CENTER);

        gameOverOverlay.getChildren().addAll(lblResultTitle, lblResultReason, lblResultScore, lblRematchNoti, btnBox);
    }

    private void styleButton(Button btn, String colorHex) {
        btn.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 18));
        btn.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: white; -fx-background-radius: 30; -fx-padding: 10 30 10 30; -fx-cursor: hand;");
        btn.setEffect(new DropShadow(5, Color.BLACK));
        btn.setOnMouseEntered(e -> { if(!btn.isDisabled()) { btn.setScaleX(1.1); btn.setScaleY(1.1); } });
        btn.setOnMouseExited(e -> { if(!btn.isDisabled()) { btn.setScaleX(1.0); btn.setScaleY(1.0); } });
    }

    public void setOpponentLeftState() {
        btnRematch.setDisable(true);
        btnRematch.setText("상대방 나감");
        btnRematch.setStyle("-fx-background-color: #616161; -fx-text-fill: #9E9E9E; -fx-background-radius: 30; -fx-padding: 10 30 10 30;");

        if (blinkAnimation != null) blinkAnimation.stop();
        lblRematchNoti.setOpacity(1.0);
        lblRematchNoti.setVisible(true);
        lblRematchNoti.toFront();

        lblRematchNoti.setText("상대방이 나갔습니다.");
        lblRematchNoti.setTextFill(Color.web("#FF5252"));
        lblRematchNoti.setStyle("-fx-effect: dropshadow(gaussian, red, 10, 0.5, 0, 0);");
    }

    public void showRematchNotification() {
        lblRematchNoti.setText("상대방이 재경기를 원합니다!");
        lblRematchNoti.setTextFill(Color.rgb(0, 255, 255));
        lblRematchNoti.setStyle("-fx-effect: dropshadow(gaussian, cyan, 15, 0.5, 0, 0);");
        lblRematchNoti.setOpacity(1.0);
        lblRematchNoti.setVisible(true);
        lblRematchNoti.toFront();
        if (blinkAnimation != null) blinkAnimation.playFromStart();
    }

    private void setupRhythmComboUI() {
        double size = 55.0;
        comboHexagon.getPoints().addAll(0.0, size/2, size*0.866, 0.0, size*1.732, size/2, size*1.732, size*1.5, size*0.866, size*2.0, 0.0, size*1.5);
        comboHexagon.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#311B92")), new Stop(1, Color.web("#6200EA"))));
        comboHexagon.setStroke(Color.web("#B388FF")); comboHexagon.setStrokeWidth(3); comboHexagon.setEffect(new DropShadow(10, Color.web("#651FFF")));
        lblComboCount.setFont(Font.font("Impact", 46)); lblComboCount.setTextFill(Color.WHITE); lblComboCount.setEffect(new DropShadow(2, Color.BLACK));
        lblComboLabel.setFont(Font.font("Arial Black", 12)); lblComboLabel.setTextFill(Color.web("#E0E0E0"));
        comboTextBox.getChildren().addAll(lblComboCount, lblComboLabel); comboTextBox.setAlignment(Pos.CENTER);
        comboBadgePane.getChildren().addAll(comboHexagon, comboTextBox); comboBadgePane.setOpacity(0.5);
    }

    private VBox createLeftInfoPanel() {
        VBox panel = new VBox(15); panel.setPadding(new Insets(20, 15, 20, 20)); panel.setAlignment(Pos.TOP_LEFT);
        panel.setStyle("-fx-background-color: rgba(255,255,255,0.5); -fx-background-radius: 0 20 20 0;");
        Label title = new Label("HOW TO PLAY"); title.setFont(Font.font("Impact", 20)); title.setTextFill(Color.web("#3E2723")); title.setUnderline(true);
        VBox goalBox = new VBox(5);
        Label goalTitle = new Label("🏆 승리 조건 (60초)"); goalTitle.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 14));
        Label goalDesc = new Label("제한 시간 종료 시,\n상대보다 많은 땅을 차지하세요!"); goalDesc.setWrapText(true); goalDesc.setFont(Font.font("Malgun Gothic", 12));
        goalBox.getChildren().addAll(goalTitle, goalDesc);
        VBox tileBox = new VBox(8);
        Label tileTitle = new Label("🎨 타일 규칙"); tileTitle.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 14));
        tileBox.getChildren().addAll(createLegendRow(Color.WHITE, "빈 땅", "입력 시 내 땅(+1점)"), createLegendRow(Color.rgb(255, 107, 129), "상대 땅", "뺏어서 빈 땅으로 만듦(-1점)"), createLegendRow(Color.rgb(84, 199, 236), "내 땅", "이미 점령됨 (효과 없음)"));
        VBox feverBox = new VBox(5);
        Label feverTitle = new Label("🔥 각성 (10 Combo)"); feverTitle.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 14)); feverTitle.setTextFill(Color.RED);
        Label feverDesc = new Label("10콤보 이상 달성 시 각성!\n상대 땅을 '즉시' 내 땅으로 만듭니다."); feverDesc.setWrapText(true); feverDesc.setFont(Font.font("Malgun Gothic", 12));
        feverBox.getChildren().addAll(feverTitle, feverDesc);
        panel.getChildren().addAll(title, goalBox, new Separator(), tileBox, new Separator(), feverBox);
        return panel;
    }

    private VBox createRightInfoPanel() {
        VBox panel = new VBox(15); panel.setPadding(new Insets(20, 20, 20, 15)); panel.setAlignment(Pos.TOP_LEFT);
        panel.setStyle("-fx-background-color: rgba(255,255,255,0.5); -fx-background-radius: 20 0 0 20;");
        Label title = new Label("ITEM GUIDE"); title.setFont(Font.font("Impact", 20)); title.setTextFill(Color.web("#3E2723")); title.setUnderline(true);
        VBox buffBox = new VBox(8);
        Label buffTitle = new Label("💎 버프 (나에게 적용)"); buffTitle.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 14)); buffTitle.setTextFill(Color.web("#0288D1"));
        buffBox.getChildren().addAll(createItemRow("스플래시", "주변 타일 동시 공격", Color.DEEPSKYBLUE, "SPLASH"), createItemRow("보호막", "5초간 내 땅 무적", Color.GOLD, "BARRIER"), createItemRow("콤보가드", "콤보 끊김 1회 방어", Color.LIMEGREEN, "GUARD"));
        VBox trapBox = new VBox(8);
        Label trapTitle = new Label("🐙 트랩 (상대에게 적용)"); trapTitle.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 14)); trapTitle.setTextFill(Color.web("#C62828"));
        trapBox.getChildren().addAll(createItemRow("먹물", "상대 화면 가리기", Color.BLACK, "INK"), createItemRow("EMP", "상대 땅 3개 파괴", Color.BLUE, "EMP"), createItemRow("혼란", "상대 글자 뒤집기", Color.PURPLE, "CONFUSION"));
        panel.getChildren().addAll(title, buffBox, new Separator(), trapBox);
        return panel;
    }

    private HBox createLegendRow(Color color, String name, String desc) {
        HBox row = new HBox(10); row.setAlignment(Pos.CENTER_LEFT);
        Rectangle rect = new Rectangle(20, 20, color); rect.setStroke(Color.GRAY); rect.setArcWidth(5); rect.setArcHeight(5);
        VBox textBox = new VBox(0); Label nameLbl = new Label(name); nameLbl.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 12));
        Label descLbl = new Label(desc); descLbl.setFont(Font.font("Malgun Gothic", 10)); descLbl.setTextFill(Color.DARKGRAY);
        textBox.getChildren().addAll(nameLbl, descLbl); row.getChildren().addAll(rect, textBox); return row;
    }

    private HBox createItemRow(String name, String desc, Color iconColor, String type) {
        HBox row = new HBox(10); row.setAlignment(Pos.CENTER_LEFT);
        Canvas iconCanvas = new Canvas(28, 28); drawIcon(iconCanvas.getGraphicsContext2D(), type, iconColor);
        VBox textBox = new VBox(0); Label nameLbl = new Label(name); nameLbl.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 12));
        Label descLbl = new Label(desc); descLbl.setFont(Font.font("Malgun Gothic", 10)); descLbl.setTextFill(Color.DARKGRAY); descLbl.setWrapText(true);
        textBox.getChildren().addAll(nameLbl, descLbl); row.getChildren().addAll(iconCanvas, textBox); return row;
    }

    private void drawIcon(GraphicsContext gc, String type, Color color) {
        gc.setFill(color);
        switch (type) {
            case "SPLASH" -> { gc.fillOval(4, 6, 20, 20); gc.setFill(Color.WHITE); gc.fillOval(16, 10, 4, 4); }
            case "BARRIER" -> { gc.fillRoundRect(5, 4, 18, 20, 8, 8); gc.setStroke(Color.ORANGE); gc.setLineWidth(2); gc.strokeRoundRect(5, 4, 18, 20, 8, 8); }
            case "GUARD" -> { gc.setStroke(color); gc.setLineWidth(3); gc.strokeOval(4, 4, 20, 20); }
            case "INK" -> { gc.fillOval(4, 4, 10, 10); gc.fillOval(12, 8, 12, 12); gc.fillOval(6, 14, 8, 8); }
            case "EMP" -> { gc.setFill(color); gc.fillPolygon(new double[]{14, 20, 12, 14, 8, 16}, new double[]{4, 10, 10, 20, 14, 14}, 6); }
            case "CONFUSION" -> { gc.setFont(Font.font("System", FontWeight.BOLD, 20)); gc.fillText("?", 8, 22); }
        }
    }

    // --- API ---
    public StackPane getRoot() { return mainStack; }
    public LandGrabPanel getLandGrabPanel() { return landGrabPanel; }
    public TextField getInputField() { return inputField; }
    public Button getRematchButton() { return btnRematch; }
    public Button getQuitButton() { return btnQuit; }
    public HBox getControlBox() { return controlBox; }

    public void showGameOver(boolean isWin, String reason, int myScore, int oppScore) {
        lblResultTitle.setText(isWin ? "VICTORY" : "DEFEAT");
        lblResultTitle.setTextFill(isWin ? Color.GOLD : Color.GRAY);
        lblResultReason.setText(reason);
        lblResultScore.setText("나 " + myScore + " : " + oppScore + " 상대");
        btnRematch.setDisable(false); btnRematch.setText("재경기 신청");
        styleButton(btnRematch, "#4CAF50");
        lblRematchNoti.setVisible(false);
        if(blinkAnimation != null) blinkAnimation.stop();
        gameOverOverlay.setVisible(true); gameOverOverlay.toFront();
    }

    public void hideGameOver() { gameOverOverlay.setVisible(false); }
    public void setRematchRequestedState() { btnRematch.setDisable(true); btnRematch.setText("수락 대기중..."); }

    public void setTimeText(String text) { txtTime.setText(text.replace("남은 시간: ", "")); }
    public void setMyScoreText(String text) { txtMyScore.setText(text.replaceAll("[^0-9]", "")); }
    public void setAiScoreText(String text) { txtAiScore.setText(text.replaceAll("[^0-9]", "")); }
    public void setPlayerNames(String myName, String oppName) { txtMyName.setText(myName); txtOppName.setText(oppName); }

    public void setComboText(String text) {
        String numStr = text.replaceAll("[^0-9]", "");
        int combo = numStr.isEmpty() ? 0 : Integer.parseInt(numStr);
        lblComboCount.setText(String.valueOf(combo));
        ScaleTransition st = new ScaleTransition(Duration.millis(100), lblComboCount);
        st.setFromX(1.0); st.setFromY(1.0); st.setToX(1.4); st.setToY(1.4); st.setAutoReverse(true); st.setCycleCount(2); st.play();
        comboBadgePane.setOpacity(combo > 0 ? 1.0 : 0.5);
        if (combo >= 10) {
            lblComboLabel.setText("FEVER!"); lblComboLabel.setTextFill(Color.YELLOW);
            comboHexagon.setStroke(Color.ORANGE);
            comboHexagon.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.RED), new Stop(1, Color.YELLOW)));
            comboHexagon.setEffect(new Glow(0.8));
        } else {
            lblComboLabel.setText("COMBO"); lblComboLabel.setTextFill(Color.web("#E0E0E0"));
            comboHexagon.setStroke(Color.web("#B388FF"));
            comboHexagon.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#311B92")), new Stop(1, Color.web("#6200EA"))));
            comboHexagon.setEffect(new DropShadow(10, Color.web("#651FFF")));
        }
    }

    public void setComboGuardActive(boolean active) {
        if (active) {
            comboBadgePane.setStyle("-fx-effect: dropshadow(gaussian, gold, 20, 0.7, 0, 0);");
            lblComboLabel.setText("GUARD");
        } else {
            comboBadgePane.setStyle("");
        }
    }

    public void setEffectsText(String text) { lblStatus.setText(text); }
    public void setLastItemText(String text) { if(!text.contains("없음") && !text.contains("-")) lblStatus.setText(text + " 발동!"); }
    public void flashHit() { landGrabPanel.flashHit(); }
    public void flashMiss() { landGrabPanel.flashMiss(); }
    public void flashItem(Color color) { landGrabPanel.flashBuffColor(color); }
}