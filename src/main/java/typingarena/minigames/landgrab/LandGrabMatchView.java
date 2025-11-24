package typingarena.minigames.landgrab;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition; // [수정] 이 부분이 빠져서 오류가 났습니다. 추가 완료!
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

public class LandGrabMatchView {

    // [1] 최상위 루트: 윈도우 배경 (여백이 생길 경우 이 색상이 보임)
    private final StackPane mainRoot = new StackPane();

    // [2] 스케일링 그룹: 게임 콘텐츠를 통째로 담아서 확대/축소할 컨테이너
    private final Group contentGroup = new Group();

    // [3] 실제 콘텐츠 패널: 개발 기준 해상도 (1200 x 800)
    private final StackPane contentPane = new StackPane();
    private static final double BASE_WIDTH = 1200;
    private static final double BASE_HEIGHT = 800;

    private final BorderPane gameRoot = new BorderPane();
    private final LandGrabPanel landGrabPanel;

    // --- HUD Components ---
    private final Rectangle timeBarFill = new Rectangle();
    private final Rectangle timeBarBg = new Rectangle();
    private final double TIME_BAR_MAX_WIDTH = 200.0;
    private final Text txtTimeInt = new Text("60");

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
    private final Label lblComboGuardMsg = new Label("🛡 콤보가드 ON");

    // 하단 입력창
    private final TextField inputField = new TextField();
    private final HBox controlBox = new HBox(10);

    // --- Game Over Overlay ---
    private final StackPane gameOverOverlay = new StackPane();
    private final VBox resultBox = new VBox(20);
    private final Label lblResultTitle = new Label("VICTORY");
    private final Label lblResultReason = new Label("");
    private final Label lblResultScore = new Label("");
    private final Button btnRematch = new Button("재경기 신청");
    private final Button btnQuit = new Button("나가기");
    private final Label lblRematchNoti = new Label("상대방이 재경기를 원합니다!");
    private final Label lblCooldown = new Label("잠시 후 버튼이 활성화됩니다...");

    private FadeTransition blinkAnimation;

    public LandGrabMatchView() {
        this.landGrabPanel = new LandGrabPanel();

        // 1. 전체 배경색
        mainRoot.setStyle("-fx-background-color: #FFF3E0;");

        // 2. 콘텐츠 패널 설정 (기준 해상도 고정)
        contentPane.setPrefSize(BASE_WIDTH, BASE_HEIGHT);
        contentPane.setMinSize(BASE_WIDTH, BASE_HEIGHT);
        contentPane.setMaxSize(BASE_WIDTH, BASE_HEIGHT);
        contentPane.setStyle("-fx-background-color: transparent;");

        // 3. UI 조립
        buildGameUI();

        // 4. 구조 연결: Root -> Group -> ContentPane
        contentGroup.getChildren().add(contentPane);
        mainRoot.getChildren().add(contentGroup);

        // 5. 리사이즈 리스너 (정석 레터박스 스케일링)
        mainRoot.widthProperty().addListener((o, oldVal, newVal) -> scaleContent());
        mainRoot.heightProperty().addListener((o, oldVal, newVal) -> scaleContent());

        // 초기화 시 강제 호출
        Platform.runLater(this::scaleContent);
    }

    // =================================================================================
    // [핵심 로직] 레터박스 스케일링 (Fit Inside)
    // =================================================================================
    private void scaleContent() {
        double width = mainRoot.getWidth();
        double height = mainRoot.getHeight();

        if (width == 0 || height == 0) return;

        // 1. 가로/세로 비율 계산
        double scaleX = width / BASE_WIDTH;
        double scaleY = height / BASE_HEIGHT;

        // 2. 더 작은 비율을 선택 (화면 밖으로 나가지 않게 함)
        double scale = Math.min(scaleX, scaleY);

        // 3. Group에 스케일 적용
        contentGroup.setScaleX(scale);
        contentGroup.setScaleY(scale);

        // 4. StackPane 덕분에 Group은 항상 화면 정중앙에 위치함
    }

    private void buildGameUI() {
        gameRoot.setPrefSize(BASE_WIDTH, BASE_HEIGHT);

        StackPane headerContainer = createStyledHeader();
        gameRoot.setTop(headerContainer);

        VBox leftPanel = createLeftInfoPanel();
        leftPanel.setMinWidth(220);
        gameRoot.setLeft(leftPanel);

        VBox rightPanel = createRightInfoPanel();
        rightPanel.setMinWidth(220);
        gameRoot.setRight(rightPanel);

        StackPane centerWrapper = new StackPane(landGrabPanel);
        centerWrapper.setPadding(new Insets(10));
        centerWrapper.setEffect(new DropShadow(25, Color.rgb(0,0,0,0.25)));
        gameRoot.setCenter(centerWrapper);

        setupInputBar();
        gameRoot.setBottom(controlBox);

        setupGameOverOverlay();

        contentPane.getChildren().addAll(gameRoot, gameOverOverlay);
    }

    public Pane getRoot() {
        return mainRoot;
    }

    // --- UI 구성 요소 ---

    private StackPane createStyledHeader() {
        StackPane root = new StackPane();
        root.setMaxHeight(160);

        StackPane bg = new StackPane();
        bg.setMaxHeight(140);
        bg.setStyle("-fx-background-color: rgba(255,255,255,0.95); -fx-background-radius: 0 0 60 60; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 15, 0, 0, 5);");
        StackPane.setAlignment(bg, Pos.TOP_CENTER);

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(10, 40, 0, 40));

        ColumnConstraints colLeft = new ColumnConstraints(); colLeft.setPercentWidth(33); colLeft.setHalignment(HPos.LEFT);
        ColumnConstraints colCenter = new ColumnConstraints(); colCenter.setPercentWidth(34); colCenter.setHalignment(HPos.CENTER);
        ColumnConstraints colRight = new ColumnConstraints(); colRight.setPercentWidth(33); colRight.setHalignment(HPos.RIGHT);

        grid.getColumnConstraints().addAll(colLeft, colCenter, colRight);

        VBox timeGaugeBox = createGlossyTimeBar(); grid.add(timeGaugeBox, 0, 0);
        HBox scoreBoard = createScoreBoard(); grid.add(scoreBoard, 1, 0);
        VBox comboUI = createComboUI(); grid.add(comboUI, 2, 0);

        root.getChildren().addAll(bg, grid);
        return root;
    }

    private VBox createGlossyTimeBar() {
        VBox container = new VBox(5); container.setAlignment(Pos.CENTER_LEFT); container.setMinWidth(220);
        Label lblTitle = new Label("남은 시간"); lblTitle.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 16)); lblTitle.setTextFill(Color.GRAY);
        double barHeight = 28;
        timeBarBg.setWidth(TIME_BAR_MAX_WIDTH); timeBarBg.setHeight(barHeight); timeBarBg.setArcWidth(20); timeBarBg.setArcHeight(20);
        timeBarBg.setFill(Color.web("#37474F")); timeBarBg.setEffect(new InnerShadow(5, Color.BLACK));
        timeBarFill.setWidth(TIME_BAR_MAX_WIDTH); timeBarFill.setHeight(barHeight); timeBarFill.setArcWidth(20); timeBarFill.setArcHeight(20);
        updateTimeBarColor(false);
        StackPane barStack = new StackPane(timeBarBg, timeBarFill); barStack.setAlignment(Pos.CENTER_LEFT);
        txtTimeInt.setFont(Font.font("Impact", 32)); txtTimeInt.setFill(Color.web("#37474F"));
        HBox barWithText = new HBox(15, barStack, txtTimeInt); barWithText.setAlignment(Pos.CENTER_LEFT);
        container.getChildren().addAll(lblTitle, barWithText);
        return container;
    }

    private HBox createScoreBoard() {
        HBox scoreBoard = new HBox(20); scoreBoard.setAlignment(Pos.CENTER);
        StackPane myScorePanel = createScorePanel(txtMyName, txtMyScore, Color.web("#0288D1"));
        StackPane oppScorePanel = createScorePanel(txtOppName, txtAiScore, Color.web("#D32F2F"));
        Text txtVS = new Text("VS"); txtVS.setFont(Font.font("Impact", 60));
        txtVS.setFill(new LinearGradient(0,0,0,1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.LIGHTGRAY), new Stop(0.5, Color.GRAY), new Stop(1, Color.DARKGRAY)));
        txtVS.setEffect(new DropShadow(5, Color.WHITE));
        scoreBoard.getChildren().addAll(myScorePanel, txtVS, oppScorePanel);
        return scoreBoard;
    }

    private VBox createComboUI() {
        setupRhythmComboUI();
        VBox comboContainer = new VBox(5); comboContainer.setAlignment(Pos.TOP_CENTER);
        lblComboGuardMsg.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 14)); lblComboGuardMsg.setTextFill(Color.LIMEGREEN);
        lblComboGuardMsg.setEffect(new DropShadow(2, Color.WHITE)); lblComboGuardMsg.setVisible(false);
        comboContainer.getChildren().addAll(comboBadgePane, lblComboGuardMsg);
        return comboContainer;
    }

    private StackPane createScorePanel(Text nameTxt, Text scoreTxt, Color themeColor) {
        StackPane panel = new StackPane(); panel.setMinWidth(200); panel.setMaxWidth(200);
        Rectangle bg = new Rectangle(200, 80); bg.setArcWidth(20); bg.setArcHeight(20);
        bg.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.WHITE), new Stop(1, Color.web("#F5F5F5"))));
        bg.setStroke(themeColor); bg.setStrokeWidth(4); bg.setEffect(new DropShadow(5, Color.rgb(0,0,0,0.1)));
        Rectangle nameTag = new Rectangle(180, 28); nameTag.setArcWidth(14); nameTag.setArcHeight(14); nameTag.setFill(themeColor);
        StackPane namePane = new StackPane(nameTag, nameTxt); namePane.setAlignment(Pos.CENTER); namePane.setTranslateY(-35);
        nameTxt.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 16)); nameTxt.setFill(Color.WHITE);
        scoreTxt.setFont(Font.font("Impact", 50)); scoreTxt.setFill(themeColor); scoreTxt.setTranslateY(5);
        panel.getChildren().addAll(bg, scoreTxt, namePane);
        return panel;
    }

    public LandGrabPanel getLandGrabPanel() { return landGrabPanel; }
    public TextField getInputField() { return inputField; }
    public Button getRematchButton() { return btnRematch; }
    public Button getQuitButton() { return btnQuit; }
    public HBox getControlBox() { return controlBox; }

    public void setTimeText(String text) {
        try {
            String numStr = text.replaceAll("[^0-9.]", "");
            if (numStr.isEmpty()) return;
            double ms = Double.parseDouble(numStr) * 1000;
            double ratio = Math.max(0, ms / 60000.0);
            timeBarFill.setWidth(TIME_BAR_MAX_WIDTH * ratio);
            boolean isUrgent = ms <= 10000;
            updateTimeBarColor(isUrgent);
            int seconds = (int) Math.ceil(ms / 1000.0);
            txtTimeInt.setText(String.valueOf(seconds));
            txtTimeInt.setFill(isUrgent ? Color.RED : Color.web("#37474F"));
        } catch (Exception e) { txtTimeInt.setText("0"); }
    }

    private void updateTimeBarColor(boolean isUrgent) {
        if (isUrgent) {
            timeBarFill.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#FFCDD2")), new Stop(0.5, Color.web("#E53935")), new Stop(1, Color.web("#B71C1C"))));
        } else {
            timeBarFill.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#80DEEA")), new Stop(0.5, Color.web("#00ACC1")), new Stop(1, Color.web("#006064"))));
        }
    }

    public void setComboText(String text) {
        String numStr = text.replaceAll("[^0-9]", "");
        int combo = numStr.isEmpty() ? 0 : Integer.parseInt(numStr);
        String currentText = lblComboCount.getText();
        lblComboCount.setText(String.valueOf(combo));
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
        if (!numStr.equals(currentText)) {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), lblComboCount);
            st.setFromX(1.0); st.setFromY(1.0); st.setToX(1.4); st.setToY(1.4); st.setAutoReverse(true); st.setCycleCount(2); st.play();
        }
    }

    public void setMyScoreText(String text) { txtMyScore.setText(text.replaceAll("[^0-9]", "")); }
    public void setAiScoreText(String text) { txtAiScore.setText(text.replaceAll("[^0-9]", "")); }
    public void setPlayerNames(String myName, String oppName) { txtMyName.setText(myName); txtOppName.setText(oppName); }
    public void setLastItemText(String text) { }
    public void setEffectsText(String text) { }

    public void setComboGuardActive(boolean active) {
        if (active) {
            comboBadgePane.setStyle("-fx-effect: dropshadow(gaussian, gold, 20, 0.7, 0, 0);");
            lblComboGuardMsg.setVisible(true);
        } else {
            comboBadgePane.setStyle("");
            lblComboGuardMsg.setVisible(false);
        }
    }

    public void flashHit() { landGrabPanel.flashHit(); }
    public void flashMiss() { landGrabPanel.flashMiss(); }
    public void flashItem(Color color) { landGrabPanel.flashBuffColor(color); }

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

    private void setupGameOverOverlay() {
        gameOverOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85);");
        gameOverOverlay.setVisible(false);
        resultBox.setAlignment(Pos.CENTER);

        lblResultTitle.setFont(Font.font("Impact", 70)); lblResultTitle.setEffect(new Glow(0.8));
        lblResultReason.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 20)); lblResultReason.setTextFill(Color.LIGHTGRAY);
        lblResultScore.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 28)); lblResultScore.setTextFill(Color.WHITE);

        styleButton(btnRematch, "#4CAF50"); styleButton(btnQuit, "#F44336");

        lblRematchNoti.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 22));
        lblRematchNoti.setTextFill(Color.rgb(0, 255, 255));
        lblRematchNoti.setStyle("-fx-effect: dropshadow(gaussian, cyan, 15, 0.5, 0, 0);");
        lblRematchNoti.setVisible(false);

        lblCooldown.setFont(Font.font("Malgun Gothic", 14)); lblCooldown.setTextFill(Color.GRAY); lblCooldown.setVisible(false);

        blinkAnimation = new FadeTransition(Duration.seconds(0.4), lblRematchNoti);
        blinkAnimation.setFromValue(1.0); blinkAnimation.setToValue(0.2); blinkAnimation.setAutoReverse(true);
        blinkAnimation.setCycleCount(FadeTransition.INDEFINITE);

        HBox btnBox = new HBox(30, btnQuit, btnRematch); btnBox.setAlignment(Pos.CENTER);
        resultBox.getChildren().addAll(lblResultTitle, lblResultReason, lblResultScore, lblRematchNoti, btnBox, lblCooldown);
        gameOverOverlay.getChildren().add(resultBox);
    }

    private void styleButton(Button btn, String colorHex) {
        btn.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 18));
        btn.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: white; -fx-background-radius: 30; -fx-padding: 10 30 10 30; -fx-cursor: hand;");
        btn.setEffect(new DropShadow(5, Color.BLACK));
        btn.setOnMouseEntered(e -> { if(!btn.isDisabled()) { btn.setScaleX(1.1); btn.setScaleY(1.1); } });
        btn.setOnMouseExited(e -> { if(!btn.isDisabled()) { btn.setScaleX(1.0); btn.setScaleY(1.0); } });
    }

    public void showGameOver(boolean isWin, String reason, int myScore, int oppScore) {
        lblResultTitle.setText(isWin ? "VICTORY" : "DEFEAT"); lblResultTitle.setTextFill(isWin ? Color.GOLD : Color.GRAY);
        lblResultReason.setText(reason); lblResultScore.setText("나 " + myScore + " : " + oppScore + " 상대");
        btnRematch.setDisable(false); btnRematch.setText("재경기 신청"); styleButton(btnRematch, "#4CAF50");
        lblRematchNoti.setVisible(false); if(blinkAnimation != null) blinkAnimation.stop();
        btnRematch.setDisable(true); btnQuit.setDisable(true); lblCooldown.setVisible(true);
        PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
        delay.setOnFinished(e -> { btnRematch.setDisable(false); btnQuit.setDisable(false); lblCooldown.setVisible(false); });
        delay.play();
        gameOverOverlay.setVisible(true); gameOverOverlay.toFront(); inputField.setDisable(true); mainRoot.requestFocus();
    }

    public void hideGameOver() { gameOverOverlay.setVisible(false); }
    public void setRematchRequestedState() { btnRematch.setDisable(true); btnRematch.setText("수락 대기중..."); }
    public void setOpponentLeftState() {
        btnRematch.setDisable(true); btnRematch.setText("상대방 나감");
        btnRematch.setStyle("-fx-background-color: #616161; -fx-text-fill: #9E9E9E; -fx-background-radius: 30; -fx-padding: 10 30 10 30;");
        if (blinkAnimation != null) blinkAnimation.stop();
        lblRematchNoti.setOpacity(1.0); lblRematchNoti.setVisible(true); lblRematchNoti.toFront();
        lblRematchNoti.setText("상대방이 나갔습니다."); lblRematchNoti.setTextFill(Color.web("#FF5252"));
        lblRematchNoti.setStyle("-fx-effect: dropshadow(gaussian, red, 10, 0.5, 0, 0);");
    }
    public void showRematchNotification() {
        lblRematchNoti.setText("상대방이 재경기를 원합니다!"); lblRematchNoti.setTextFill(Color.rgb(0, 255, 255));
        lblRematchNoti.setStyle("-fx-effect: dropshadow(gaussian, cyan, 15, 0.5, 0, 0);");
        lblRematchNoti.setOpacity(1.0); lblRematchNoti.setVisible(true); lblRematchNoti.toFront();
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
        VBox panel = new VBox(15); panel.setPadding(new Insets(20, 15, 20, 15)); panel.setAlignment(Pos.TOP_LEFT);
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
}