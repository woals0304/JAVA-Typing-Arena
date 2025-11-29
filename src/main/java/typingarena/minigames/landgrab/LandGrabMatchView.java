package typingarena.minigames.landgrab;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

import java.io.InputStream;

public class LandGrabMatchView {

    // [테마 색상]
    private static final Color THEME_BG_COLOR = Color.web("#FDF5E6");
    private static final Color THEME_PANEL_BG = Color.web("#FFF3E0");
    private static final Color THEME_STROKE = Color.web("#5D4037");
    private static final Color THEME_TEXT_MAIN = Color.web("#4E342E");

    private static final Color COLOR_P1 = Color.web("#29B6F6");
    private static final Color COLOR_P2 = Color.web("#EF5350");
    private static final Color COLOR_GOLD_START = Color.web("#FFD54F");
    private static final Color COLOR_GOLD_END = Color.web("#FF6F00");
    private static final Color COLOR_TIMER_BG = Color.web("#D7CCC8");

    private static final Color COMBO_BG_PURPLE_START = Color.web("#BA68C8");
    private static final Color COMBO_BG_PURPLE_END = Color.web("#7B1FA2");
    private static final Color COMBO_TEXT_NORMAL = Color.web("#E1BEE7");
    private static final Color COMBO_TEXT_FEVER = Color.WHITE;

    private Font cookieFontMain;
    private Font cookieFontBold;
    private Font cookieFontTitle;

    private final StackPane root = new StackPane();
    private final Group contentGroup = new Group();
    private final StackPane gameContent = new StackPane();

    private static final double BASE_WIDTH = 1200;
    private static final double BASE_HEIGHT = 800;
    private final Scale contentScale = new Scale(1, 1, 0, 0);

    private final BorderPane mainLayout = new BorderPane();
    private final LandGrabPanel landGrabPanel;

    // --- Header ---
    private final Text txtMyName = new Text("YOU");
    private final Text txtMyScore = new Text("0");
    private final Text txtOppName = new Text("COMPUTER");
    private final Text txtAiScore = new Text("0");

    // 메인 타이머
    private final StackPane timerContainer = new StackPane();
    private final Rectangle timerFill = new Rectangle();
    private final Rectangle timerBg = new Rectangle();
    private final Rectangle timerClip = new Rectangle();
    private final StackPane timerFillWrapper = new StackPane();
    private final Rectangle timerBorder = new Rectangle();
    private final Label lblTimeText = new Label("60");
    private final double MAX_TIMER_WIDTH = 250.0;
    private final double TIMER_HEIGHT = 32.0;
    private final double TIMER_STROKE = 3.0;

    // --- Combo ---
    private final HBox comboWrapper = new HBox(8);
    private final StackPane comboBadgePane = new StackPane();
    private final Polygon comboHexagon = new Polygon();
    private final Label lblComboValue = new Label("0");
    private final Label lblComboText = new Label("COMBO");
    private final Label lblComboGuardMsg = new Label("🛡 가드 ON");

    // --- Input ---
    private final TextField inputField = new TextField();
    private final HBox controlBox = new HBox();

    // --- Game Over ---
    private final StackPane gameOverOverlay = new StackPane();
    private final VBox gameOverBox = new VBox(15);
    private final Label lblResultTitle = new Label("RESULT");
    private final Label lblResultScore = new Label("");
    private final Button btnRematch = new Button("재경기 신청");
    private final Button btnQuit = new Button("나가기");
    private final Label lblRematchStatus = new Label("");

    // [수정] 게임 오버용 타이머 구성 요소
    private final StackPane autoCloseContainer = new StackPane();
    private final Rectangle autoCloseBg = new Rectangle();
    private final Rectangle autoCloseFill = new Rectangle();
    private final StackPane autoCloseFillWrapper = new StackPane();
    private final Rectangle autoCloseClip = new Rectangle(); // 클리핑 마스크
    private final Rectangle autoCloseBorder = new Rectangle();
    private final Label lblAutoCloseText = new Label("30");

    private final double AUTO_CLOSE_WIDTH = 280.0;
    private final double AUTO_CLOSE_HEIGHT = 32.0;
    private final double AUTO_CLOSE_STROKE = 3.0; // 테두리 두께

    private Timeline autoCloseTimeline;
    private FadeTransition blinkAnimation;

    private Runnable onCloseAction;

    public LandGrabMatchView() {
        this.landGrabPanel = new LandGrabPanel();

        loadFonts();
        initLayoutStructure();
        initStyles();
        buildUI();
        setupEventHandlers();

        Platform.runLater(this::updateScale);
        root.layoutBoundsProperty().addListener((obs, old, bounds) -> updateScale());

        // [Sound] 버튼 효과음 연결
        addSoundToButton(btnRematch);
        addSoundToButton(btnQuit);
    }

    public void setOnCloseAction(Runnable action) {
        this.onCloseAction = action;
    }

    private void performClose() {
        if (autoCloseTimeline != null) autoCloseTimeline.stop();
        if (onCloseAction != null) {
            onCloseAction.run();
        }
    }

    private void loadFonts() {
        try {
            InputStream is = getClass().getResourceAsStream("/fonts/CookieRun Regular.otf");
            if (is != null) {
                Font rawFont = Font.loadFont(is, 20);
                cookieFontMain = Font.font(rawFont.getFamily(), FontWeight.NORMAL, 14);
                cookieFontBold = Font.font(rawFont.getFamily(), FontWeight.BOLD, 24);
                cookieFontTitle = Font.font(rawFont.getFamily(), FontWeight.EXTRA_BOLD, 32);
            } else {
                cookieFontMain = Font.font("Malgun Gothic", FontWeight.BOLD, 14);
                cookieFontBold = Font.font("Malgun Gothic", FontWeight.BOLD, 24);
                cookieFontTitle = Font.font("Impact", 32);
            }
        } catch (Exception e) {
            cookieFontMain = Font.font("System", 14);
            cookieFontBold = Font.font("System", 24);
            cookieFontTitle = Font.font("System", 32);
        }
    }
    private Font getFont(double size) { return Font.font(cookieFontMain.getFamily(), FontWeight.NORMAL, size); }
    private Font getBoldFont(double size) { return Font.font(cookieFontMain.getFamily(), FontWeight.BOLD, size); }

    private void initLayoutStructure() {
        root.setStyle("-fx-background-color: #3E2723;");
        root.setAlignment(Pos.CENTER);
        gameContent.setPrefSize(BASE_WIDTH, BASE_HEIGHT);
        gameContent.setMinSize(BASE_WIDTH, BASE_HEIGHT);
        gameContent.setMaxSize(BASE_WIDTH, BASE_HEIGHT);
        gameContent.setBackground(new Background(new BackgroundFill(THEME_BG_COLOR, CornerRadii.EMPTY, Insets.EMPTY)));
        gameContent.getTransforms().add(contentScale);
        contentGroup.getChildren().add(gameContent);
        root.getChildren().add(contentGroup);
    }

    private void initStyles() {
        // [메인 타이머 스타일]
        timerBg.setWidth(MAX_TIMER_WIDTH);
        timerBg.setHeight(TIMER_HEIGHT);
        timerBg.setArcWidth(TIMER_HEIGHT); timerBg.setArcHeight(TIMER_HEIGHT);
        timerBg.setFill(COLOR_TIMER_BG);
        timerBg.setStroke(null);

        double innerHeight = TIMER_HEIGHT - (TIMER_STROKE * 2);
        double innerMaxW = MAX_TIMER_WIDTH - (TIMER_STROKE * 2);
        timerFill.setWidth(innerMaxW); timerFill.setHeight(innerHeight);
        timerFill.setArcWidth(0); timerFill.setArcHeight(0);
        timerFill.setFill(COLOR_P1);
        timerFill.setStroke(null);

        timerClip.setWidth(MAX_TIMER_WIDTH); timerClip.setHeight(TIMER_HEIGHT);
        timerClip.setArcWidth(TIMER_HEIGHT); timerClip.setArcHeight(TIMER_HEIGHT);

        timerFillWrapper.setMaxSize(MAX_TIMER_WIDTH, TIMER_HEIGHT);
        timerFillWrapper.setAlignment(Pos.CENTER_LEFT);
        if(timerFillWrapper.getChildren().isEmpty()) timerFillWrapper.getChildren().add(timerFill);
        timerFillWrapper.setClip(timerClip);

        timerBorder.setWidth(MAX_TIMER_WIDTH); timerBorder.setHeight(TIMER_HEIGHT);
        timerBorder.setArcWidth(TIMER_HEIGHT); timerBorder.setArcHeight(TIMER_HEIGHT);
        timerBorder.setFill(Color.TRANSPARENT); timerBorder.setStroke(THEME_STROKE);
        timerBorder.setStrokeWidth(TIMER_STROKE); timerBorder.setStrokeType(StrokeType.INSIDE);

        lblTimeText.setFont(getBoldFont(18));
        lblTimeText.setTextFill(THEME_STROKE);

        StackPane.setAlignment(timerBg, Pos.CENTER_LEFT);
        StackPane.setAlignment(timerFillWrapper, Pos.CENTER_LEFT);
        StackPane.setAlignment(timerBorder, Pos.CENTER_LEFT);
        StackPane.setAlignment(lblTimeText, Pos.CENTER);
        StackPane.setMargin(timerFill, new Insets(0, 0, 0, TIMER_STROKE));

        timerContainer.getChildren().setAll(timerBg, timerFillWrapper, timerBorder, lblTimeText);
        timerContainer.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        // Input
        inputField.setFont(getBoldFont(22)); inputField.setPromptText("단어를 입력하세요!");
        inputField.setAlignment(Pos.CENTER);
        inputField.setStyle("-fx-background-radius: 30; -fx-background-color: white; -fx-border-color: #5D4037; -fx-border-width: 4px; -fx-border-radius: 30; -fx-text-fill: #3E2723; -fx-prompt-text-fill: gray;");
        HBox.setHgrow(inputField, Priority.ALWAYS);
        controlBox.setAlignment(Pos.CENTER); controlBox.setPadding(new Insets(15, 150, 15, 150));
        controlBox.setStyle("-fx-background-color: #FFECB3; -fx-background-radius: 40 40 0 0; -fx-border-color: #5D4037; -fx-border-width: 4px 4px 0 4px; -fx-border-radius: 40 40 0 0;");
        controlBox.getChildren().add(inputField);
        mainLayout.setBottom(controlBox);

        // Combo Guard
        lblComboGuardMsg.setFont(getBoldFont(11)); lblComboGuardMsg.setTextFill(Color.WHITE);
        lblComboGuardMsg.setStyle("-fx-background-color: #43A047; -fx-background-radius: 12; -fx-padding: 2 8; -fx-border-color: #1B5E20; -fx-border-width: 2px; -fx-border-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 3, 0, 0, 1);");
        lblComboGuardMsg.setVisible(false);

        // Buttons
        styleCookieButton(btnRematch, COLOR_P1);
        styleCookieButton(btnQuit, COLOR_P2);

        // [수정] 게임오버 스타일
        gameOverOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6);");
        gameOverOverlay.setVisible(false);

        gameOverBox.setAlignment(Pos.CENTER);
        gameOverBox.setMinWidth(480);
        gameOverBox.setMaxWidth(550);
        gameOverBox.setMaxHeight(Region.USE_PREF_SIZE);
        gameOverBox.setPadding(new Insets(30));
        gameOverBox.setStyle("-fx-background-color: #FFF8E1; -fx-background-radius: 40; -fx-border-color: #5D4037; -fx-border-width: 6px; -fx-border-radius: 40; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 20, 0, 0, 10);");

        // [핵심 수정] 게임 오버용 타이머 스타일 (메인과 동일한 핏 적용)
        autoCloseBg.setWidth(AUTO_CLOSE_WIDTH); autoCloseBg.setHeight(AUTO_CLOSE_HEIGHT);
        autoCloseBg.setArcWidth(AUTO_CLOSE_HEIGHT); autoCloseBg.setArcHeight(AUTO_CLOSE_HEIGHT);
        autoCloseBg.setFill(COLOR_TIMER_BG);
        autoCloseBg.setStroke(null);

        // 내부 크기 계산: 전체 - (테두리 * 2)
        double acInnerH = AUTO_CLOSE_HEIGHT - (AUTO_CLOSE_STROKE * 2);
        double acInnerW = AUTO_CLOSE_WIDTH - (AUTO_CLOSE_STROKE * 2);

        autoCloseFill.setWidth(acInnerW); autoCloseFill.setHeight(acInnerH);
        autoCloseFill.setArcWidth(0); autoCloseFill.setArcHeight(0); // 직각 (클리핑)
        autoCloseFill.setFill(COLOR_P1);
        autoCloseFill.setStroke(null);

        autoCloseClip.setWidth(AUTO_CLOSE_WIDTH); autoCloseClip.setHeight(AUTO_CLOSE_HEIGHT);
        autoCloseClip.setArcWidth(AUTO_CLOSE_HEIGHT); autoCloseClip.setArcHeight(AUTO_CLOSE_HEIGHT);

        autoCloseFillWrapper.setMaxSize(AUTO_CLOSE_WIDTH, AUTO_CLOSE_HEIGHT);
        autoCloseFillWrapper.setAlignment(Pos.CENTER_LEFT);
        if(autoCloseFillWrapper.getChildren().isEmpty()) autoCloseFillWrapper.getChildren().add(autoCloseFill);
        autoCloseFillWrapper.setClip(autoCloseClip);

        autoCloseBorder.setWidth(AUTO_CLOSE_WIDTH); autoCloseBorder.setHeight(AUTO_CLOSE_HEIGHT);
        autoCloseBorder.setArcWidth(AUTO_CLOSE_HEIGHT); autoCloseBorder.setArcHeight(AUTO_CLOSE_HEIGHT);
        autoCloseBorder.setFill(Color.TRANSPARENT);
        autoCloseBorder.setStroke(THEME_STROKE);
        autoCloseBorder.setStrokeWidth(AUTO_CLOSE_STROKE);
        autoCloseBorder.setStrokeType(StrokeType.INSIDE);

        lblAutoCloseText.setFont(getBoldFont(16));
        lblAutoCloseText.setTextFill(THEME_STROKE);

        StackPane.setAlignment(autoCloseBg, Pos.CENTER);
        StackPane.setAlignment(autoCloseFillWrapper, Pos.CENTER);
        StackPane.setAlignment(autoCloseBorder, Pos.CENTER);
        StackPane.setAlignment(lblAutoCloseText, Pos.CENTER);

        // [중요] 왼쪽 테두리만큼 여백 추가하여 침범 방지
        StackPane.setMargin(autoCloseFill, new Insets(0, 0, 0, AUTO_CLOSE_STROKE));

        autoCloseContainer.getChildren().setAll(autoCloseBg, autoCloseFillWrapper, autoCloseBorder, lblAutoCloseText);
        autoCloseContainer.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        lblRematchStatus.setFont(getBoldFont(14));
        lblRematchStatus.setMinHeight(25);
    }

    private void buildUI() {
        mainLayout.setPadding(new Insets(15, 20, 0, 20));
        mainLayout.setTop(buildUnifiedHeader());

        StackPane centerWrapper = new StackPane(landGrabPanel);
        centerWrapper.setPadding(new Insets(10));
        centerWrapper.setMinSize(300, 300);
        landGrabPanel.setEffect(new DropShadow(15, Color.rgb(0,0,0,0.15)));
        mainLayout.setCenter(centerWrapper);

        mainLayout.setLeft(buildSidePanel("게임 규칙", buildRulesContent()));
        mainLayout.setRight(buildSidePanel("아이템 도감", buildItemsContent()));

        HBox btnBox = new HBox(15, btnQuit, btnRematch);
        btnBox.setAlignment(Pos.CENTER);

        gameOverBox.getChildren().clear();
        gameOverBox.getChildren().addAll(
                lblResultTitle,
                lblResultScore,
                new Region() {{ setMinHeight(10); }},
                btnBox,
                lblRematchStatus,
                autoCloseContainer
        );
        gameOverOverlay.getChildren().add(gameOverBox);

        root.getChildren().addAll(mainLayout, gameOverOverlay);
    }

    // ... (Header 등 기존 코드) ...
    private StackPane buildUnifiedHeader() {
        StackPane headerContainer = new StackPane(); headerContainer.setPadding(new Insets(0, 0, 10, 0)); headerContainer.setAlignment(Pos.CENTER);
        Rectangle bg = new Rectangle(1100, 110); bg.setArcWidth(40); bg.setArcHeight(40); bg.setFill(Color.rgb(255, 248, 225, 0.7)); bg.setStroke(Color.rgb(93, 64, 55, 0.2)); bg.setStrokeWidth(2);
        GridPane grid = new GridPane(); grid.setAlignment(Pos.CENTER); grid.setMaxWidth(1050); grid.setHgap(20);
        ColumnConstraints col1 = new ColumnConstraints(); col1.setPercentWidth(33); col1.setHalignment(HPos.CENTER);
        ColumnConstraints col2 = new ColumnConstraints(); col2.setPercentWidth(34); col2.setHalignment(HPos.CENTER);
        ColumnConstraints col3 = new ColumnConstraints(); col3.setPercentWidth(33); col3.setHalignment(HPos.CENTER);
        grid.getColumnConstraints().addAll(col1, col2, col3);
        VBox timeBox = new VBox(15); timeBox.setAlignment(Pos.CENTER);
        Label lblTimeTitle = new Label("남은 시간"); lblTimeTitle.setFont(getBoldFont(14)); lblTimeTitle.setTextFill(Color.GRAY);
        timeBox.getChildren().addAll(lblTimeTitle, timerContainer);
        HBox scoreBox = new HBox(15); scoreBox.setAlignment(Pos.CENTER);
        StackPane p1 = createScoreBadge(txtMyName, txtMyScore, COLOR_P1); StackPane p2 = createScoreBadge(txtOppName, txtAiScore, COLOR_P2);
        Text txtVs = new Text("VS"); txtVs.setFont(Font.font("Impact", 45)); txtVs.setFill(Color.LIGHTGRAY); txtVs.setEffect(new DropShadow(2, Color.WHITE));
        scoreBox.getChildren().addAll(p1, txtVs, p2);
        buildComboHexagon(); comboWrapper.setAlignment(Pos.CENTER); comboWrapper.getChildren().addAll(comboBadgePane, lblComboGuardMsg);
        grid.add(timeBox, 0, 0); grid.add(scoreBox, 1, 0); grid.add(comboWrapper, 2, 0);
        headerContainer.getChildren().addAll(bg, grid); return headerContainer;
    }
    private StackPane createScoreBadge(Text name, Text score, Color color) {
        StackPane p = new StackPane(); p.setPrefSize(140, 70);
        Rectangle bg = new Rectangle(140, 70); bg.setArcWidth(25); bg.setArcHeight(25); bg.setFill(Color.WHITE); bg.setStroke(color); bg.setStrokeWidth(4); bg.setEffect(new DropShadow(3, Color.rgb(0,0,0,0.1)));
        Rectangle tag = new Rectangle(100, 22); tag.setArcWidth(11); tag.setArcHeight(11); tag.setFill(color);
        StackPane namePane = new StackPane(tag, name); namePane.setTranslateY(-38);
        name.setFont(getBoldFont(12)); name.setFill(Color.WHITE);
        score.setFont(getBoldFont(38)); score.setFill(color); score.setTranslateY(5);
        p.getChildren().addAll(bg, score, namePane); return p;
    }
    private void buildComboHexagon() {
        double size = 45.0; comboHexagon.getPoints().addAll(0.0, size/2, size*0.866, 0.0, size*1.732, size/2, size*1.732, size*1.5, size*0.866, size*2.0, 0.0, size*1.5);
        updateComboVisuals(false, false);
        VBox box = new VBox(-3); box.setAlignment(Pos.CENTER);
        lblComboValue.setFont(getBoldFont(32)); lblComboValue.setTextFill(Color.WHITE);
        lblComboText.setFont(getBoldFont(10)); lblComboText.setTextFill(COMBO_TEXT_NORMAL);
        box.getChildren().addAll(lblComboValue, lblComboText);
        comboBadgePane.getChildren().addAll(comboHexagon, box);
    }
    private void updateComboVisuals(boolean isFever, boolean isGuardActive) {
        if (isFever) { comboHexagon.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, new Stop(0, COLOR_GOLD_START), new Stop(1, COLOR_GOLD_END))); lblComboText.setTextFill(COMBO_TEXT_FEVER); }
        else { comboHexagon.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, new Stop(0, COMBO_BG_PURPLE_START), new Stop(1, COMBO_BG_PURPLE_END))); lblComboText.setTextFill(COMBO_TEXT_NORMAL); }
        if (isGuardActive) { comboHexagon.setStroke(Color.LIMEGREEN); comboHexagon.setStrokeWidth(5); comboHexagon.setEffect(new Glow(0.6)); }
        else { comboHexagon.setStroke(Color.WHITE); comboHexagon.setStrokeWidth(3); if (isFever) comboHexagon.setEffect(new Glow(0.7)); else comboHexagon.setEffect(new DropShadow(5, Color.rgb(0,0,0,0.2))); }
    }
    private VBox buildSidePanel(String titleText, VBox content) {
        VBox panel = new VBox(12); panel.setPadding(new Insets(20)); panel.setMinWidth(220); panel.setMaxWidth(220);
        panel.setStyle("-fx-background-color: #FFF3E0; -fx-background-radius: 25; -fx-border-color: #5D4037; -fx-border-width: 3px; -fx-border-radius: 25; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");
        Label title = new Label(titleText); title.setFont(getBoldFont(18)); title.setTextFill(THEME_TEXT_MAIN);
        Separator sep = new Separator(); sep.setStyle("-fx-background-color: #8D6E63;");
        panel.getChildren().addAll(title, sep, content); VBox.setVgrow(content, Priority.ALWAYS); return panel;
    }
    private VBox buildRulesContent() {
        VBox box = new VBox(15);
        box.getChildren().addAll(createIconGuideItem("승리 조건", "60초 종료 시\n더 많은 땅 차지!", "TROPHY"), createIconGuideItem("빈 땅", "입력 시 내 땅 (+1점)", "EMPTY_TILE"), createIconGuideItem("상대 땅", "뺏으면 빈 땅 됨 (-1점)", "ENEMY_TILE"), createIconGuideItem("내 땅", "이미 점령 완료", "MY_TILE"), new Separator(), createIconGuideItem("피버 모드", "10콤보 달성 시\n상대 땅 즉시 점령!", "FEVER"));
        return box;
    }
    private VBox buildItemsContent() {
        VBox box = new VBox(12);
        box.getChildren().addAll(new Label("💎 버프 (나)"), createIconGuideItem("스플래시", "인접 타일 동시 공격", "SPLASH"), createIconGuideItem("보호막", "5초간 내 땅 무적", "BARRIER"), createIconGuideItem("콤보가드", "5초간 콤보 끊김 방어", "GUARD"), new Separator(), new Label("🐙 방해 (상대)"), createIconGuideItem("먹물", "상대 타일 2개 가리기", "INK"), createIconGuideItem("EMP", "상대 땅 3개를 빈 땅으로", "EMP"), createIconGuideItem("혼란", "상대 단어 5초간 뒤집기", "CONFUSION"));
        box.getChildren().forEach(n -> { if(n instanceof Label l) { l.setFont(getBoldFont(13)); l.setTextFill(THEME_TEXT_MAIN); } });
        return box;
    }
    private HBox createIconGuideItem(String title, String desc, String iconType) {
        HBox row = new HBox(10); row.setAlignment(Pos.CENTER_LEFT); Canvas iconCanvas = new Canvas(24, 24); drawGuideIcon(iconCanvas.getGraphicsContext2D(), iconType);
        VBox t = new VBox(2); Label l1 = new Label(title); l1.setFont(getBoldFont(13)); l1.setTextFill(THEME_TEXT_MAIN); Label l2 = new Label(desc); l2.setFont(getFont(11)); l2.setTextFill(Color.web("#6D4C41")); l2.setWrapText(true);
        t.getChildren().addAll(l1, l2); row.getChildren().addAll(iconCanvas, t); return row;
    }
    private void drawGuideIcon(GraphicsContext gc, String type) {
        switch (type) {
            case "TROPHY" -> { gc.setFill(COLOR_GOLD_START); gc.fillPolygon(new double[]{2, 22, 18, 6}, new double[]{4, 4, 14, 14}, 4); gc.fillRect(10, 14, 4, 6); gc.fillRect(6, 20, 12, 2); }
            case "EMPTY_TILE" -> { gc.setFill(Color.WHITE); gc.setStroke(THEME_STROKE); gc.setLineWidth(1.5); gc.fillRoundRect(2, 2, 20, 20, 6, 6); gc.strokeRoundRect(2, 2, 20, 20, 6, 6); }
            case "ENEMY_TILE" -> { gc.setFill(COLOR_P2); gc.fillRoundRect(2, 2, 20, 20, 6, 6); }
            case "MY_TILE" -> { gc.setFill(COLOR_P1); gc.fillRoundRect(2, 2, 20, 20, 6, 6); }
            case "FEVER" -> { gc.setFill(Color.ORANGERED); gc.fillPolygon(new double[]{12, 18, 14, 12, 10, 6}, new double[]{2, 8, 14, 22, 14, 8}, 6); }
            case "SPLASH" -> { gc.setFill(COLOR_P1); gc.fillPolygon(new double[]{12,15,22,16,20,13,12,9,2,8,2,11}, new double[]{2,8,6,13,20,16,22,16,20,13,6,8}, 12); }
            case "BARRIER" -> { gc.setFill(COLOR_GOLD_START); gc.setStroke(THEME_STROKE); gc.setLineWidth(1.5); gc.fillRoundRect(4, 2, 16, 20, 8, 8); gc.strokeRoundRect(4, 2, 16, 20, 8, 8); }
            case "GUARD" -> { gc.setStroke(Color.LIMEGREEN); gc.setLineWidth(3); gc.strokeOval(4, 4, 16, 16); }
            case "INK" -> { gc.setFill(Color.BLACK); gc.fillOval(4, 4, 10, 10); gc.fillOval(12, 10, 8, 8); gc.fillOval(6, 14, 6, 6); }
            case "EMP" -> { gc.setFill(Color.web("#2979FF")); gc.fillPolygon(new double[]{12, 18, 10, 12, 6, 14}, new double[]{2, 10, 10, 22, 14, 14}, 6); }
            case "CONFUSION" -> { gc.setFill(Color.MAGENTA); gc.setFont(Font.font("Arial", FontWeight.BOLD, 20)); gc.fillText("?", 7, 20); }
        }
    }
    private void updateScale() { }
    public StackPane getRoot() { return root; }
    public LandGrabPanel getLandGrabPanel() { return landGrabPanel; }
    public TextField getInputField() { return inputField; }
    public Button getRematchButton() { return btnRematch; }
    public Button getQuitButton() { return btnQuit; }

    public void setTimeText(String text) {
        try {
            String numStr = text.replaceAll("[^0-9.]", "");
            if (numStr.isEmpty()) return;
            double ms = Double.parseDouble(numStr) * 1000;
            double ratio = Math.max(0, Math.min(1.0, ms / 60000.0));

            double innerMaxWidth = MAX_TIMER_WIDTH - (TIMER_STROKE * 2);
            timerFill.setWidth(innerMaxWidth * ratio);

            int seconds = (int)Math.ceil(ms/1000.0);
            lblTimeText.setText(String.valueOf(seconds));

            if (ratio < 0.2) {
                timerFill.setFill(COLOR_P2);
                lblTimeText.setTextFill(Color.RED);
            } else {
                timerFill.setFill(COLOR_P1);
                lblTimeText.setTextFill(THEME_STROKE);
            }
        } catch (Exception e) { lblTimeText.setText("0"); }
    }

    public void setComboText(String text) {
        String numStr = text.replaceAll("[^0-9]", ""); int combo = numStr.isEmpty() ? 0 : Integer.parseInt(numStr); String current = lblComboValue.getText();
        lblComboValue.setText(String.valueOf(combo)); comboBadgePane.setOpacity(combo > 0 ? 1.0 : 0.5);
        boolean isFever = (combo >= 10); if (isFever) { lblComboText.setText("FEVER!"); } else { lblComboText.setText("COMBO"); }
        updateComboVisuals(isFever, lblComboGuardMsg.isVisible());
        if (!numStr.equals(current) && combo > 0) { ScaleTransition st = new ScaleTransition(Duration.millis(100), comboBadgePane); st.setFromX(1.0); st.setFromY(1.0); st.setToX(1.2); st.setToY(1.2); st.setAutoReverse(true); st.setCycleCount(2); st.play(); }
    }
    public void setComboGuardActive(boolean active) { lblComboGuardMsg.setVisible(active); int currentCombo = 0; try { currentCombo = Integer.parseInt(lblComboValue.getText()); } catch(Exception e){} updateComboVisuals(currentCombo >= 10, active); }
    public void setMyScoreText(String text) { txtMyScore.setText(text.replaceAll("[^0-9]", "")); }
    public void setAiScoreText(String text) { txtAiScore.setText(text.replaceAll("[^0-9]", "")); }
    public void setPlayerNames(String myName, String oppName) { txtMyName.setText(myName); txtOppName.setText(oppName); }
    public void flashHit() { landGrabPanel.flashHit(); }
    public void flashMiss() { landGrabPanel.flashMiss(); }
    public void flashItem(Color color) { landGrabPanel.flashBuffColor(color); }

    public void showGameOver(boolean isWin, String reason, int myScore, int oppScore) {
        // [Sound] 게임 종료 시 BGM 끄고 결과음 재생
        LandGrabSoundManager sm = LandGrabSoundManager.getInstance();
        sm.stopBgm();

        if (reason.contains("무승부") || myScore == oppScore) {
            sm.play("sfx_draw.wav");
            lblResultTitle.setText("DRAW"); lblResultTitle.setTextFill(Color.web("#9575CD"));
        } else {
            if (isWin) sm.play("sfx_win.wav");
            else sm.play("sfx_lose.wav");

            lblResultTitle.setText(isWin ? "VICTORY!" : "DEFEAT...");
            lblResultTitle.setTextFill(isWin ? COLOR_GOLD_START : Color.GRAY);
        }
        lblResultTitle.setFont(getBoldFont(48)); lblResultTitle.setEffect(new DropShadow(3, THEME_STROKE));
        lblResultScore.setText("나 " + myScore + "  vs  " + oppScore + " 상대");
        lblResultScore.setFont(getBoldFont(24)); lblResultScore.setTextFill(THEME_TEXT_MAIN);
        btnRematch.setDisable(false); btnRematch.setText("재경기 신청"); lblRematchStatus.setText("");
        if (blinkAnimation != null) blinkAnimation.stop();

        gameOverOverlay.setVisible(true); gameOverOverlay.toFront();
        startAutoCloseTimer();
    }

    // [수정] 게임 오버 타이머 로직 (핏 보정 적용)
    private void startAutoCloseTimer() {
        if (autoCloseTimeline != null) autoCloseTimeline.stop();

        final double TOTAL_SECONDS = 30.0;
        final double UPDATE_INTERVAL = 0.1;

        double innerMaxWidth = AUTO_CLOSE_WIDTH - (AUTO_CLOSE_STROKE * 2);
        autoCloseFill.setWidth(innerMaxWidth); // 꽉 찬 상태로 시작
        autoCloseFill.setFill(COLOR_P1);

        autoCloseTimeline = new Timeline(new KeyFrame(Duration.seconds(UPDATE_INTERVAL), e -> {
            double currentWidth = autoCloseFill.getWidth();
            double decreaseAmount = (innerMaxWidth * UPDATE_INTERVAL) / TOTAL_SECONDS;
            double newWidth = currentWidth - decreaseAmount;

            if (newWidth <= 0) {
                autoCloseFill.setWidth(0);
                autoCloseTimeline.stop();
                performClose();
            } else {
                autoCloseFill.setWidth(newWidth);
                // 전체 비율 기준 시간 표시
                double ratio = newWidth / innerMaxWidth;
                lblAutoCloseText.setText(String.valueOf((int)Math.ceil(ratio * TOTAL_SECONDS)));

                if (ratio < 0.2) {
                    autoCloseFill.setFill(COLOR_P2);
                    lblAutoCloseText.setTextFill(Color.RED);
                } else {
                    autoCloseFill.setFill(COLOR_P1);
                    lblAutoCloseText.setTextFill(THEME_STROKE);
                }
            }
        }));
        autoCloseTimeline.setCycleCount(Timeline.INDEFINITE);
        autoCloseTimeline.play();
    }

    public void hideGameOver() { gameOverOverlay.setVisible(false); if (autoCloseTimeline != null) autoCloseTimeline.stop(); }
    public void setRematchRequestedState() { btnRematch.setDisable(true); btnRematch.setText("수락 대기중..."); }

    public void setOpponentLeftState() {
        btnRematch.setDisable(true);

        // [중요 수정] 애니메이션 시작 전 투명도와 가시성 확실하게 초기화
        lblRematchStatus.setOpacity(1.0);
        lblRematchStatus.setVisible(true);

        lblRematchStatus.setText("상대방이 나갔습니다.");
        lblRematchStatus.setTextFill(COLOR_P2);

        // 깜빡임 애니메이션 초기화 (없으면 생성)
        if (blinkAnimation == null) {
            blinkAnimation = new FadeTransition(Duration.seconds(0.5), lblRematchStatus);
            blinkAnimation.setFromValue(1.0);
            blinkAnimation.setToValue(0.2);
            blinkAnimation.setCycleCount(FadeTransition.INDEFINITE);
            blinkAnimation.setAutoReverse(true);
        }
        // 애니메이션 시작!
        blinkAnimation.playFromStart();
    }

    public void showRematchNotification() { lblRematchStatus.setText("상대방이 재경기를 원합니다!"); lblRematchStatus.setTextFill(COLOR_P1); if (blinkAnimation == null) { blinkAnimation = new FadeTransition(Duration.seconds(0.5), lblRematchStatus); blinkAnimation.setFromValue(1.0); blinkAnimation.setToValue(0.2); blinkAnimation.setCycleCount(FadeTransition.INDEFINITE); blinkAnimation.setAutoReverse(true); } blinkAnimation.playFromStart(); }

    private void styleCookieButton(Button btn, Color color) {
        btn.setFont(getBoldFont(16));
        String hex = toHex(color);
        btn.setStyle("-fx-background-color: " + hex + "; -fx-text-fill: white; -fx-background-radius: 25; -fx-border-color: #5D4037; -fx-border-width: 2px; -fx-border-radius: 25; -fx-padding: 10 30; -fx-cursor: hand;");
        btn.setEffect(new DropShadow(3, color.darker()));
    }
    private String toHex(Color c) { return String.format("#%02X%02X%02X", (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255)); }

    private void setupEventHandlers() {
        btnQuit.setOnAction(e -> performClose());
    }

    // [Sound] 버튼 효과음 연결 헬퍼 메서드
    private void addSoundToButton(Button btn) {
        btn.setOnMouseEntered(e -> LandGrabSoundManager.getInstance().play("sfx_ui_hover.wav"));
        btn.setOnMouseClicked(e -> LandGrabSoundManager.getInstance().play("sfx_ui_click.wav"));
    }
}