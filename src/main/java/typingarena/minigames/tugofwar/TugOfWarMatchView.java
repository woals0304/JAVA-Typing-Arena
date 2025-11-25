package typingarena.minigames.tugofwar;

import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 싱글/멀티 모두에서 사용하는 공통 UI.
 * LandGrab와 동일한 테마·배치를 적용해 두 게임이 같은 아트 디렉션을 공유한다.
 */
public class TugOfWarMatchView {

    // [테마 색상] LandGrab과 동일한 팔레트
    private static final Color THEME_BG_COLOR = Color.web("#FDF5E6");
    private static final Color THEME_PANEL_BG = Color.web("#FFF3E0");
    private static final Color THEME_STROKE = Color.web("#5D4037");
    private static final Color THEME_TEXT_MAIN = Color.web("#4E342E");
    private static final Color THEME_TEXT_MUTED = Color.web("#6D4C41");

    private static final Color COLOR_P1 = Color.web("#29B6F6");
    private static final Color COLOR_P2 = Color.web("#EF5350");
    private static final Color COLOR_GOLD_START = Color.web("#FFD54F");
    private static final Color COLOR_GOLD_END = Color.web("#FF6F00");
    private static final Color COLOR_TIMER_BG = Color.web("#D7CCC8");
    private static final Color COMBO_BG_PURPLE_START = Color.web("#BA68C8");
    private static final Color COMBO_BG_PURPLE_END = Color.web("#7B1FA2");

    private static final double BASE_WIDTH = 1200;
    private static final double BASE_HEIGHT = 800;

    private Font cookieFontMain;
    private Font cookieFontBold;
    private Font cookieFontTitle;

    private final StackPane root = new StackPane();
    private final Group contentGroup = new Group();
    private final StackPane gameContent = new StackPane();
    private final BorderPane mainLayout = new BorderPane();
    private final Scale contentScale = new Scale(1, 1, 0, 0);

    private final RopePanel ropePanel = new RopePanel();

    // Header
    private final Text txtMyName = new Text("YOU");
    private final Text txtMyScore = new Text("0");
    private final Text txtOppName = new Text("RIVAL");
    private final Text txtOppScore = new Text("0");

    // 타이머
    private final StackPane timerContainer = new StackPane();
    private final Rectangle timerFill = new Rectangle();
    private final Rectangle timerBg = new Rectangle();
    private final Rectangle timerClip = new Rectangle();
    private final StackPane timerFillWrapper = new StackPane();
    private final Rectangle timerBorder = new Rectangle();
    private final Label lblTimeText = new Label("60");
    private final double MAX_TIMER_WIDTH = 250.0;
    private final double TIMER_HEIGHT = 28.0;
    private final double TIMER_STROKE = 3.0;

    // Combo
    private final HBox comboWrapper = new HBox(8);
    private final StackPane comboBadgePane = new StackPane();
    private final Polygon comboHexagon = new Polygon();
    private final Label lblComboValue = new Label("0");
    private final Label lblComboText = new Label("COMBO");
    private int lastComboValue = 0;

    // HUD
    private final Label lblScore = new Label("점수: 0");
    private final Label lblPos = new Label("위치: 0.0");
    private final Label lblEffects = new Label("효과: 없음");
    private final Label lblLastItem = new Label("최근 아이템: 없음");
    private final Label lblRematch = new Label("");
    private static final Color REMATCH_ACCENT = Color.web("#29B6F6");
    private static final Color REMATCH_MUTED = Color.web("#8D6E63");

    // Input
    private final TextField inputField = new TextField();
    private final HBox controlBox = new HBox(12);

    // Game Over Overlay
    private final StackPane gameOverOverlay = new StackPane();
    private final VBox gameOverBox = new VBox(15);
    private final Label lblResultTitle = new Label("RESULT");
    private final Label lblResultDetail = new Label("");
    private final Label lblResultExtra = new Label("");
    private final Button btnRematch = new Button("재경기 신청");
    private final Button btnQuit = new Button("나가기");
    private final Label lblRematchStatus = new Label("");

    // auto-close bar
    private final StackPane autoCloseContainer = new StackPane();
    private final Rectangle autoCloseBg = new Rectangle();
    private final Rectangle autoCloseFill = new Rectangle();
    private final StackPane autoCloseFillWrapper = new StackPane();
    private final Rectangle autoCloseClip = new Rectangle();
    private final Rectangle autoCloseBorder = new Rectangle();
    private final Label lblAutoCloseText = new Label("30");

    private final double AUTO_CLOSE_WIDTH = 280.0;
    private final double AUTO_CLOSE_HEIGHT = 32.0;
    private final double AUTO_CLOSE_STROKE = 3.0;

    private Timeline autoCloseTimeline;
    private Runnable onCloseAction;

    private double lastTimeMs = 60_000;

    public TugOfWarMatchView() {
        loadFonts();
        initLayoutStructure();
        initStyles();
        buildUI();
        setupButtonStyler();

        Platform.runLater(this::updateScale);
        root.layoutBoundsProperty().addListener((obs, oldV, newV) -> updateScale());
    }

    private void loadFonts() {
        try (InputStream is = getClass().getResourceAsStream("/fonts/CookieRun Regular.otf")) {
            if (is != null) {
                Font base = Font.loadFont(is, 20);
                cookieFontMain = Font.font(base.getFamily(), FontWeight.NORMAL, 14);
                cookieFontBold = Font.font(base.getFamily(), FontWeight.BOLD, 22);
                cookieFontTitle = Font.font(base.getFamily(), FontWeight.EXTRA_BOLD, 32);
            } else {
                cookieFontMain = Font.font("Malgun Gothic", FontWeight.BOLD, 14);
                cookieFontBold = Font.font("Malgun Gothic", FontWeight.BOLD, 22);
                cookieFontTitle = Font.font("Impact", 32);
            }
        } catch (Exception e) {
            cookieFontMain = Font.font("System", 14);
            cookieFontBold = Font.font("System", 22);
            cookieFontTitle = Font.font("System", 32);
        }
    }

    private Font getFont(double size) {
        return Font.font(cookieFontMain.getFamily(), FontWeight.NORMAL, size);
    }

    private Font getBoldFont(double size) {
        return Font.font(cookieFontMain.getFamily(), FontWeight.BOLD, size);
    }

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
        buildTimerBar();

        // Combo
        buildComboHexagon();
        comboWrapper.setAlignment(Pos.CENTER);

        // Input
        inputField.setFont(getBoldFont(22));
        inputField.setPromptText("단어를 입력하세요!");
        inputField.setAlignment(Pos.CENTER_LEFT);
        inputField.setStyle("-fx-background-radius: 28; -fx-background-color: white; -fx-border-color: #5D4037; -fx-border-width: 3px; -fx-border-radius: 28; -fx-text-fill: #3E2723; -fx-prompt-text-fill: gray; -fx-padding: 12 16;");
        HBox.setHgrow(inputField, Priority.ALWAYS);

        controlBox.setAlignment(Pos.CENTER_LEFT);
        controlBox.setSpacing(12);
        controlBox.setPadding(new Insets(12, 20, 12, 20));
        controlBox.setStyle("-fx-background-color: #FFECB3; -fx-background-radius: 40 40 0 0; -fx-border-color: #5D4037; -fx-border-width: 3px 3px 0 3px; -fx-border-radius: 40 40 0 0;");

        lblRematch.setFont(getBoldFont(14));
        lblRematch.setTextFill(REMATCH_MUTED);
        lblRematch.setStyle("-fx-background-color: #FFF8E1; -fx-background-radius: 14; -fx-padding: 6 10; -fx-border-color: #D7CCC8; -fx-border-radius: 14;");

        lblScore.setFont(getBoldFont(14));
        lblPos.setFont(getBoldFont(14));
        lblEffects.setFont(getBoldFont(13));
        lblLastItem.setFont(getBoldFont(13));
        lblScore.setTextFill(THEME_TEXT_MAIN);
        lblPos.setTextFill(THEME_TEXT_MAIN);
        lblEffects.setTextFill(THEME_TEXT_MAIN);
        lblLastItem.setTextFill(THEME_TEXT_MAIN);
        lblEffects.setWrapText(true);
        lblLastItem.setWrapText(true);

        // Game over labels
        lblResultTitle.setFont(getBoldFont(46));
        lblResultDetail.setFont(getBoldFont(20));
        lblResultExtra.setFont(getBoldFont(16));

        lblRematchStatus.setFont(getBoldFont(14));
        lblRematchStatus.setMinHeight(25);
    }

    private void buildUI() {
        mainLayout.setPadding(new Insets(15, 20, 10, 20));
        mainLayout.setTop(buildUnifiedHeader());
        mainLayout.setCenter(buildCenterArea());
        mainLayout.setLeft(buildSidePanel("게임 규칙", buildRulesContent()));
        mainLayout.setRight(buildSidePanel("아이템 도감", buildItemsContent()));

        controlBox.getChildren().addAll(inputField, lblRematch);
        BorderPane bottom = new BorderPane();
        bottom.setCenter(controlBox);
        mainLayout.setBottom(bottom);

        gameContent.getChildren().add(mainLayout);
        buildGameOverOverlay();
        gameContent.getChildren().add(gameOverOverlay);
    }

    private StackPane buildUnifiedHeader() {
        StackPane headerContainer = new StackPane();
        headerContainer.setPadding(new Insets(0, 0, 10, 0));
        headerContainer.setAlignment(Pos.CENTER);

        Rectangle bg = new Rectangle(1100, 110);
        bg.setArcWidth(40);
        bg.setArcHeight(40);
        bg.setFill(Color.rgb(255, 248, 225, 0.8));
        bg.setStroke(Color.rgb(93, 64, 55, 0.25));
        bg.setStrokeWidth(2);

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setMaxWidth(1050);
        grid.setHgap(20);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(33);
        col1.setHalignment(HPos.CENTER);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(34);
        col2.setHalignment(HPos.CENTER);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(33);
        col3.setHalignment(HPos.CENTER);
        grid.getColumnConstraints().addAll(col1, col2, col3);

        VBox timeBox = new VBox(6);
        timeBox.setAlignment(Pos.CENTER);
        Label lblTimeTitle = new Label("남은 시간");
        lblTimeTitle.setFont(getBoldFont(14));
        lblTimeTitle.setTextFill(Color.GRAY);
        timeBox.getChildren().addAll(lblTimeTitle, timerContainer);

        HBox scoreBox = new HBox(15);
        scoreBox.setAlignment(Pos.CENTER);
        StackPane p1 = createScoreBadge(txtMyName, txtMyScore, COLOR_P1);
        StackPane p2 = createScoreBadge(txtOppName, txtOppScore, COLOR_P2);
        Text txtVs = new Text("VS");
        txtVs.setFont(Font.font("Impact", 45));
        txtVs.setFill(Color.LIGHTGRAY);
        txtVs.setEffect(new DropShadow(2, Color.WHITE));
        scoreBox.getChildren().addAll(p1, txtVs, p2);

        comboWrapper.getChildren().setAll(comboBadgePane);

        grid.add(timeBox, 0, 0);
        grid.add(scoreBox, 1, 0);
        grid.add(comboWrapper, 2, 0);

        headerContainer.getChildren().addAll(bg, grid);
        return headerContainer;
    }

    private StackPane createScoreBadge(Text name, Text score, Color color) {
        StackPane p = new StackPane();
        p.setPrefSize(150, 70);

        Rectangle bg = new Rectangle(150, 70);
        bg.setArcWidth(25);
        bg.setArcHeight(25);
        bg.setFill(Color.WHITE);
        bg.setStroke(color);
        bg.setStrokeWidth(4);
        bg.setEffect(new DropShadow(3, Color.rgb(0, 0, 0, 0.12)));

        Rectangle tag = new Rectangle(110, 22);
        tag.setArcWidth(11);
        tag.setArcHeight(11);
        tag.setFill(color);

        StackPane namePane = new StackPane(tag, name);
        namePane.setTranslateY(-38);
        name.setFont(getBoldFont(12));
        name.setFill(Color.WHITE);

        score.setFont(getBoldFont(38));
        score.setFill(color);
        score.setTranslateY(5);

        p.getChildren().addAll(bg, score, namePane);
        return p;
    }

    private void buildComboHexagon() {
        double size = 45.0;
        comboHexagon.getPoints().addAll(
                0.0, size / 2,
                size * 0.866, 0.0,
                size * 1.732, size / 2,
                size * 1.732, size * 1.5,
                size * 0.866, size * 2.0,
                0.0, size * 1.5
        );
        updateComboVisuals(false);

        VBox box = new VBox(-3);
        box.setAlignment(Pos.CENTER);
        lblComboValue.setFont(getBoldFont(32));
        lblComboValue.setTextFill(Color.WHITE);
        lblComboText.setFont(getBoldFont(10));
        lblComboText.setTextFill(Color.web("#E1BEE7"));
        box.getChildren().addAll(lblComboValue, lblComboText);
        comboBadgePane.getChildren().addAll(comboHexagon, box);
    }

    private void updateComboVisuals(boolean isFever) {
        if (isFever) {
            comboHexagon.setFill(new LinearGradient(
                    0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, COLOR_GOLD_START), new Stop(1, COLOR_GOLD_END)
            ));
            comboHexagon.setEffect(new DropShadow(6, Color.rgb(0, 0, 0, 0.2)));
            lblComboText.setTextFill(Color.WHITE);
        } else {
            comboHexagon.setFill(new LinearGradient(
                    0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, COMBO_BG_PURPLE_START), new Stop(1, COMBO_BG_PURPLE_END)
            ));
            comboHexagon.setEffect(new DropShadow(5, Color.rgb(0, 0, 0, 0.15)));
            lblComboText.setTextFill(Color.web("#E1BEE7"));
        }
        comboHexagon.setStroke(Color.WHITE);
        comboHexagon.setStrokeWidth(3);
    }

    private StackPane buildCenterArea() {
        VBox centerColumn = new VBox(14);
        centerColumn.setAlignment(Pos.TOP_CENTER);
        centerColumn.getChildren().add(buildStatusRow());
        centerColumn.getChildren().add(buildRopeCard());
        centerColumn.setPadding(new Insets(0, 10, 0, 10));

        StackPane wrapper = new StackPane(centerColumn);
        wrapper.setPadding(new Insets(6, 4, 6, 4));
        return wrapper;
    }

    private HBox buildStatusRow() {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER);
        row.getChildren().addAll(
                createInfoChip("점수", lblScore),
                createInfoChip("밧줄 위치", lblPos),
                createInfoChip("효과", lblEffects),
                createInfoChip("최근 아이템", lblLastItem)
        );
        return row;
    }

    private StackPane createInfoChip(String title, Label value) {
        Label titleLbl = new Label(title);
        titleLbl.setFont(getBoldFont(12));
        titleLbl.setTextFill(THEME_TEXT_MUTED);

        value.setWrapText(true);
        value.setMaxWidth(240);

        VBox box = new VBox(4, titleLbl, value);
        box.setAlignment(Pos.TOP_LEFT);

        StackPane chip = new StackPane(box);
        chip.setPadding(new Insets(10, 14, 10, 14));
        chip.setMinHeight(64);
        chip.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16; -fx-border-color: #D7CCC8; -fx-border-width: 2px; -fx-border-radius: 16;");
        chip.setEffect(new DropShadow(8, Color.rgb(0, 0, 0, 0.08)));
        return chip;
    }

    private StackPane buildRopeCard() {
        ropePanel.setWidth(900);
        ropePanel.setHeight(400);

        StackPane ropeWrapper = new StackPane(ropePanel);
        ropeWrapper.setPadding(new Insets(24));
        ropeWrapper.setStyle("-fx-background-color: white; -fx-background-radius: 32; -fx-border-color: #5D4037; -fx-border-width: 4px; -fx-border-radius: 32;");
        ropeWrapper.setEffect(new DropShadow(15, Color.rgb(0, 0, 0, 0.15)));
        ropeWrapper.setMinSize(520, 320);

        ropeWrapper.widthProperty().addListener((obs, oldV, newV) -> {
            ropePanel.setWidth(Math.max(1, newV.doubleValue() - 60));
            ropePanel.redraw();
        });
        ropeWrapper.heightProperty().addListener((obs, oldV, newV) -> {
            ropePanel.setHeight(Math.max(1, newV.doubleValue() - 60));
            ropePanel.redraw();
        });

        return ropeWrapper;
    }

    private void buildTimerBar() {
        timerBg.setWidth(MAX_TIMER_WIDTH);
        timerBg.setHeight(TIMER_HEIGHT);
        timerBg.setArcWidth(TIMER_HEIGHT);
        timerBg.setArcHeight(TIMER_HEIGHT);
        timerBg.setFill(COLOR_TIMER_BG);
        timerBg.setStroke(null);

        double innerHeight = TIMER_HEIGHT - (TIMER_STROKE * 2);
        double innerMaxW = MAX_TIMER_WIDTH - (TIMER_STROKE * 2);
        timerFill.setWidth(innerMaxW);
        timerFill.setHeight(innerHeight);
        timerFill.setArcWidth(0);
        timerFill.setArcHeight(0);
        timerFill.setFill(COLOR_P1);
        timerFill.setStroke(null);

        timerClip.setWidth(MAX_TIMER_WIDTH);
        timerClip.setHeight(TIMER_HEIGHT);
        timerClip.setArcWidth(TIMER_HEIGHT);
        timerClip.setArcHeight(TIMER_HEIGHT);

        timerFillWrapper.setMaxSize(MAX_TIMER_WIDTH, TIMER_HEIGHT);
        timerFillWrapper.setAlignment(Pos.CENTER_LEFT);
        if (timerFillWrapper.getChildren().isEmpty()) {
            timerFillWrapper.getChildren().add(timerFill);
        }
        timerFillWrapper.setClip(timerClip);

        timerBorder.setWidth(MAX_TIMER_WIDTH);
        timerBorder.setHeight(TIMER_HEIGHT);
        timerBorder.setArcWidth(TIMER_HEIGHT);
        timerBorder.setArcHeight(TIMER_HEIGHT);
        timerBorder.setFill(Color.TRANSPARENT);
        timerBorder.setStroke(THEME_STROKE);
        timerBorder.setStrokeWidth(TIMER_STROKE);
        timerBorder.setStrokeType(StrokeType.INSIDE);

        lblTimeText.setFont(getBoldFont(18));
        lblTimeText.setTextFill(THEME_STROKE);

        StackPane.setAlignment(timerBg, Pos.CENTER_LEFT);
        StackPane.setAlignment(timerFillWrapper, Pos.CENTER_LEFT);
        StackPane.setAlignment(timerBorder, Pos.CENTER_LEFT);
        StackPane.setAlignment(lblTimeText, Pos.CENTER);
        StackPane.setMargin(timerFill, new Insets(0, 0, 0, TIMER_STROKE));

        timerContainer.getChildren().setAll(timerBg, timerFillWrapper, timerBorder, lblTimeText);
        timerContainer.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    }

    private VBox buildSidePanel(String titleText, VBox content) {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(20));
        panel.setMinWidth(220);
        panel.setMaxWidth(220);
        panel.setStyle("-fx-background-color: #FFF3E0; -fx-background-radius: 25; -fx-border-color: #5D4037; -fx-border-width: 3px; -fx-border-radius: 25; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");
        Label title = new Label(titleText);
        title.setFont(getBoldFont(18));
        title.setTextFill(THEME_TEXT_MAIN);
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #8D6E63;");
        panel.getChildren().addAll(title, sep, content);
        VBox.setVgrow(content, Priority.ALWAYS);
        return panel;
    }

    private VBox buildRulesContent() {
        VBox box = new VBox(15);
        box.getChildren().addAll(
                createIconGuideItem("승리 조건", "60초 안에 밧줄을 오른쪽으로 끌어당기면 승리!", "TROPHY"),
                createIconGuideItem("정답 입력", "단어를 맞추면 밧줄이 내 쪽으로 이동", "HIT"),
                createIconGuideItem("오답", "콤보가 끊기고 밧줄이 상대 쪽으로", "MISS"),
                createIconGuideItem("버프/트랩 단어", "단어 색에 따라 버프(초록)/트랩(빨강) 발동", "MODIFIER")
        );
        return box;
    }

    private VBox buildItemsContent() {
        VBox box = new VBox(12);
        box.getChildren().addAll(
                new Label("💪 버프"),
                createIconGuideItem("파워 그립", "정답 시 끌어당기는 힘 2배 (5초)", "POWER"),
                createIconGuideItem("앵커", "상대 밀어내기 속도 90% 감소 (3초)", "ANCHOR"),
                new Separator(),
                new Label("⚠️ 트랩"),
                createIconGuideItem("먹물", "현재 단어가 3초간 가려짐", "BLIND"),
                createIconGuideItem("자모 분리", "단어가 자모로 분리되어 표시", "JAMO")
        );
        box.getChildren().forEach(n -> {
            if (n instanceof Label l) {
                l.setFont(getBoldFont(13));
                l.setTextFill(THEME_TEXT_MAIN);
            }
        });
        return box;
    }

    private HBox createIconGuideItem(String title, String desc, String iconType) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Canvas iconCanvas = new Canvas(26, 26);
        drawGuideIcon(iconCanvas.getGraphicsContext2D(), iconType);
        VBox t = new VBox(2);
        Label l1 = new Label(title);
        l1.setFont(getBoldFont(13));
        l1.setTextFill(THEME_TEXT_MAIN);
        Label l2 = new Label(desc);
        l2.setFont(getFont(11));
        l2.setTextFill(Color.web("#6D4C41"));
        l2.setWrapText(true);
        t.getChildren().addAll(l1, l2);
        row.getChildren().addAll(iconCanvas, t);
        return row;
    }

    private void drawGuideIcon(GraphicsContext gc, String type) {
        switch (type) {
            case "TROPHY" -> {
                gc.setFill(COLOR_GOLD_START);
                gc.fillPolygon(new double[]{2, 24, 20, 6}, new double[]{4, 4, 18, 18}, 4);
                gc.fillRect(11, 18, 6, 6);
                gc.fillRect(7, 24, 14, 2);
            }
            case "HIT" -> {
                gc.setFill(COLOR_P1);
                gc.fillRoundRect(4, 6, 18, 14, 6, 6);
                gc.setFill(Color.WHITE);
                gc.fillPolygon(new double[]{8, 16, 8}, new double[]{8, 13, 18}, 3);
            }
            case "MISS" -> {
                gc.setFill(COLOR_P2);
                gc.fillRoundRect(4, 6, 18, 14, 6, 6);
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(3);
                gc.strokeLine(8, 9, 18, 19);
                gc.strokeLine(18, 9, 8, 19);
            }
            case "MODIFIER" -> {
                gc.setFill(Color.web("#2E7D32"));
                gc.fillOval(4, 4, 8, 8);
                gc.setFill(Color.web("#C62828"));
                gc.fillOval(14, 14, 8, 8);
            }
            case "POWER" -> {
                gc.setFill(Color.web("#64B5F6"));
                gc.fillPolygon(new double[]{13, 20, 12, 13, 6, 14}, new double[]{2, 12, 12, 24, 14, 14}, 6);
            }
            case "ANCHOR" -> {
                gc.setStroke(Color.web("#5D4037"));
                gc.setLineWidth(2.5);
                gc.strokeOval(6, 2, 14, 14);
                gc.strokeLine(13, 9, 13, 22);
                gc.strokeArc(4, 14, 10, 10, 0, 180, javafx.scene.shape.ArcType.OPEN);
                gc.strokeArc(12, 14, 10, 10, 0, -180, javafx.scene.shape.ArcType.OPEN);
            }
            case "BLIND" -> {
                gc.setFill(Color.BLACK);
                gc.fillOval(5, 9, 16, 8);
                gc.setFill(Color.WHITE);
                gc.fillOval(11, 11, 4, 4);
            }
            case "JAMO" -> {
                gc.setFill(Color.MEDIUMPURPLE);
                gc.fillRoundRect(3, 4, 20, 18, 6, 6);
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Arial", FontWeight.BOLD, 11));
                gc.fillText("ㅈㅏ", 6, 16);
            }
            default -> { }
        }
    }

    private void setupButtonStyler() {
        controlBox.getChildren().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Node n : change.getAddedSubList()) {
                        if (n instanceof Button btn) {
                            styleCookieButton(btn, COLOR_P1);
                        }
                    }
                }
            }
        });

        // 게임오버 버튼 기본 스타일
        styleCookieButton(btnRematch, COLOR_P1);
        styleCookieButton(btnQuit, COLOR_P2);
    }

    private void styleCookieButton(Button btn, Color color) {
        btn.setFont(getBoldFont(15));
        String hex = toHex(color);
        btn.setStyle("-fx-background-color: " + hex + "; -fx-text-fill: white; -fx-background-radius: 20; -fx-border-color: #5D4037; -fx-border-width: 2px; -fx-border-radius: 20; -fx-padding: 8 18; -fx-cursor: hand;");
        btn.setEffect(new DropShadow(3, color.darker()));
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }

    private void updateScale() {
        double scale = Math.min(root.getWidth() / BASE_WIDTH, root.getHeight() / BASE_HEIGHT);
        if (Double.isNaN(scale) || Double.isInfinite(scale) || scale <= 0) return;
        contentScale.setX(scale);
        contentScale.setY(scale);
    }

    private void buildGameOverOverlay() {
        gameOverOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6);");
        gameOverOverlay.setVisible(false);

        gameOverBox.setAlignment(Pos.CENTER);
        gameOverBox.setMinWidth(480);
        gameOverBox.setMaxWidth(550);
        gameOverBox.setMaxHeight(Region.USE_PREF_SIZE);
        gameOverBox.setPadding(new Insets(30));
        gameOverBox.setStyle("-fx-background-color: #FFF8E1; -fx-background-radius: 40; -fx-border-color: #5D4037; -fx-border-width: 6px; -fx-border-radius: 40; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 20, 0, 0, 10);");

        // Auto close bar styled to match main timer
        autoCloseBg.setWidth(AUTO_CLOSE_WIDTH); autoCloseBg.setHeight(AUTO_CLOSE_HEIGHT);
        autoCloseBg.setArcWidth(AUTO_CLOSE_HEIGHT); autoCloseBg.setArcHeight(AUTO_CLOSE_HEIGHT);
        autoCloseBg.setFill(COLOR_TIMER_BG);
        autoCloseBg.setStroke(null);

        double acInnerH = AUTO_CLOSE_HEIGHT - (AUTO_CLOSE_STROKE * 2);
        double acInnerW = AUTO_CLOSE_WIDTH - (AUTO_CLOSE_STROKE * 2);

        autoCloseFill.setWidth(acInnerW); autoCloseFill.setHeight(acInnerH);
        autoCloseFill.setArcWidth(0); autoCloseFill.setArcHeight(0);
        autoCloseFill.setFill(COLOR_P1);
        autoCloseFill.setStroke(null);

        autoCloseClip.setWidth(AUTO_CLOSE_WIDTH); autoCloseClip.setHeight(AUTO_CLOSE_HEIGHT);
        autoCloseClip.setArcWidth(AUTO_CLOSE_HEIGHT); autoCloseClip.setArcHeight(AUTO_CLOSE_HEIGHT);

        autoCloseFillWrapper.setMaxSize(AUTO_CLOSE_WIDTH, AUTO_CLOSE_HEIGHT);
        autoCloseFillWrapper.setAlignment(Pos.CENTER_LEFT);
        if (autoCloseFillWrapper.getChildren().isEmpty()) autoCloseFillWrapper.getChildren().add(autoCloseFill);
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
        StackPane.setMargin(autoCloseFill, new Insets(0, 0, 0, AUTO_CLOSE_STROKE));

        autoCloseContainer.getChildren().setAll(autoCloseBg, autoCloseFillWrapper, autoCloseBorder, lblAutoCloseText);
        autoCloseContainer.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        HBox buttons = new HBox(12, btnQuit, btnRematch);
        buttons.setAlignment(Pos.CENTER);
        buttons.setPadding(new Insets(10, 0, 0, 0));

        gameOverBox.getChildren().setAll(
                lblResultTitle,
                lblResultDetail,
                lblResultExtra,
                buttons,
                lblRematchStatus,
                autoCloseContainer
        );
        gameOverOverlay.getChildren().add(gameOverBox);
    }

    // ===== 외부 노출 =====
    public StackPane getRoot() {
        return root;
    }

    public RopePanel getRopePanel() {
        return ropePanel;
    }

    public TextField getInputField() {
        return inputField;
    }

    public HBox getControlBox() {
        return controlBox;
    }

    public Button getRematchButton() { return btnRematch; }
    public Button getQuitButton() { return btnQuit; }
    public Label getRematchStatusLabel() { return lblRematch; }
    public void setOnCloseAction(Runnable action) { this.onCloseAction = action; }

    public void showGameOver(String title, String detail, String extra, Color accent, String rematchText, String quitText) {
        if (autoCloseTimeline != null) autoCloseTimeline.stop();
        lblResultTitle.setText(title);
        lblResultDetail.setText(detail);
        lblResultExtra.setText(extra);
        lblResultTitle.setTextFill(accent);
        lblResultDetail.setTextFill(THEME_TEXT_MAIN);
        lblResultExtra.setTextFill(THEME_TEXT_MUTED);
        lblRematchStatus.setText("");
        btnRematch.setDisable(false);
        btnQuit.setDisable(false);
        btnRematch.setText(rematchText);
        btnQuit.setText(quitText);
        styleCookieButton(btnRematch, accent);
        styleCookieButton(btnQuit, COLOR_P2);

        gameOverOverlay.setVisible(true);
        gameOverOverlay.toFront();
        startAutoCloseTimer();
    }

    public void hideGameOver() {
        gameOverOverlay.setVisible(false);
        if (autoCloseTimeline != null) autoCloseTimeline.stop();
    }

    // ===== HUD 업데이트 =====
    public void setTimeText(String text) {
        double seconds = extractNumber(text, lastTimeMs / 1000.0);
        updateTimer(seconds * 1000.0);
    }

    public void setTimeMs(double ms) {
        updateTimer(ms);
    }

    private void updateTimer(double ms) {
        lastTimeMs = ms;
        double ratio = Math.max(0.0, Math.min(1.0, ms / 60000.0));
        double innerMaxWidth = MAX_TIMER_WIDTH - (TIMER_STROKE * 2);
        timerFill.setWidth(innerMaxWidth * ratio);
        lblTimeText.setText(String.valueOf((int) Math.ceil(ms / 1000.0)));

        if (ratio < 0.2) {
            timerFill.setFill(COLOR_P2);
            lblTimeText.setTextFill(Color.RED);
        } else {
            timerFill.setFill(COLOR_P1);
            lblTimeText.setTextFill(THEME_STROKE);
        }
    }

    public void setScoreText(String text) {
        lblScore.setText(text);
        int[] scores = parseScores(text);
        txtMyScore.setText(String.valueOf(scores[0]));
        txtOppScore.setText(String.valueOf(scores[1]));
    }

    public void setComboText(String text) {
        lblComboValue.setText(text.replaceAll("[^0-9]", "").isEmpty() ? "0" : text.replaceAll("[^0-9]", ""));
        int comboVal = 0;
        try {
            comboVal = Integer.parseInt(lblComboValue.getText());
        } catch (Exception ignored) {
        }
        boolean fever = comboVal >= 10;
        lblComboText.setText(fever ? "FEVER!" : "COMBO");
        updateComboVisuals(fever);

        boolean changed = comboVal != lastComboValue;
        lastComboValue = comboVal;

        if (comboVal > 0 && changed) {
            ScaleTransition st = new ScaleTransition(Duration.millis(120), comboBadgePane);
            st.setFromX(1.0);
            st.setFromY(1.0);
            st.setToX(1.12);
            st.setToY(1.12);
            st.setAutoReverse(true);
            st.setCycleCount(2);
            st.play();
        }
    }

    public void setPosText(String text) {
        lblPos.setText(text);
    }

    public void setEffectsText(String text) {
        lblEffects.setText(text);
    }

    public void setLastItemText(String text) {
        lblLastItem.setText(text);
    }

    public void flashCorrect() {
        ropePanel.flashRight();
    }

    public void flashWrong() {
        ropePanel.flashLeft();
    }

    public void flashItem(Color color) {
        ropePanel.flashBuffColor(color);
    }

    public void setRematchStatus(String text, boolean accent) {
        lblRematch.setText(text);
        lblRematch.setTextFill(accent ? REMATCH_ACCENT : REMATCH_MUTED);
    }

    // ===== Game over timer =====
    private void startAutoCloseTimer() {
        if (autoCloseTimeline != null) autoCloseTimeline.stop();

        final double TOTAL_SECONDS = 30.0;
        final double UPDATE_INTERVAL = 0.1;

        double innerMaxWidth = AUTO_CLOSE_WIDTH - (AUTO_CLOSE_STROKE * 2);
        autoCloseFill.setWidth(innerMaxWidth);
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
                double ratio = newWidth / innerMaxWidth;
                lblAutoCloseText.setText(String.valueOf((int) Math.ceil(ratio * TOTAL_SECONDS)));

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

    private void performClose() {
        if (autoCloseTimeline != null) autoCloseTimeline.stop();
        if (onCloseAction != null) onCloseAction.run();
    }

    // ===== 유틸 =====
    private int[] parseScores(String text) {
        List<Integer> nums = new ArrayList<>();
        Matcher m = Pattern.compile("(-?\\d+)").matcher(text);
        while (m.find()) {
            nums.add(Integer.parseInt(m.group(1)));
        }
        int my = nums.size() >= 1 ? nums.get(0) : 0;
        int opp = nums.size() >= 2 ? nums.get(1) : 0;
        return new int[]{my, opp};
    }

    private double extractNumber(String text, double fallback) {
        Matcher m = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)").matcher(text);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }
}
