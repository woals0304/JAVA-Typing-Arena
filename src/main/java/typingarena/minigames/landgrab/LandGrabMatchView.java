package typingarena.minigames.landgrab;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

public class LandGrabMatchView {

    private final BorderPane root = new BorderPane();
    private final LandGrabPanel landGrabPanel;

    // HUD 라벨
    private final Label lblTime = createHudBadge("60.0s", "#FFF8E1", "#FFB74D", 18);
    private final Label lblMyName = new Label("나");
    private final Label lblMyScore = createHudBadge("0", "#E1F5FE", "#4FC3F7", 22);
    private final Label lblOppName = new Label("상대");
    private final Label lblAiScore = createHudBadge("0", "#FFEBEE", "#E57373", 22);

    // 리듬게임 스타일 콤보 UI
    private final Label lblComboCount = new Label("0");
    private final Label lblComboLabel = new Label("COMBO");
    private final ProgressBar comboBar = new ProgressBar(0);
    private final VBox comboContainer = new VBox(2);
    private final StackPane comboWrapper = new StackPane();

    // 상태 메시지
    private final Label lblStatus = new Label("Ready?");
    private final TextField inputField = new TextField();
    private final HBox controlBox = new HBox(10);

    public LandGrabMatchView() {
        this.landGrabPanel = new LandGrabPanel();
        root.setStyle("-fx-background-color: #FFF3E0;");

        // 1. 상단 HUD
        HBox scoreBox = new HBox(20);
        scoreBox.setAlignment(Pos.CENTER);

        VBox myBox = createPlayerBox(lblMyName, lblMyScore, "#0288D1");
        VBox aiBox = createPlayerBox(lblOppName, lblAiScore, "#D32F2F");

        Label vsLabel = new Label("VS");
        vsLabel.setFont(Font.font("CookieRun Regular", FontWeight.BOLD, 22));
        vsLabel.setTextFill(Color.GRAY);

        setupComboUI();

        scoreBox.getChildren().addAll(lblTime, myBox, vsLabel, aiBox, comboWrapper);
        scoreBox.setPadding(new Insets(10, 20, 10, 20));

        StackPane topContainer = new StackPane(scoreBox);
        topContainer.setPadding(new Insets(10));
        topContainer.setStyle("-fx-background-color: rgba(255,255,255,0.8); -fx-background-radius: 0 0 20 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        lblStatus.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 16));
        lblStatus.setTextFill(Color.web("#F57C00"));

        VBox topLayout = new VBox(10, topContainer, lblStatus);
        topLayout.setAlignment(Pos.CENTER);
        root.setTop(topLayout);

        // 2. 좌우 사이드 패널 (버그 수정: 최소 너비 설정)
        VBox leftPanel = createSidePanel("나의 아이템 (Buff)", true);
        leftPanel.setMinWidth(160); // [버그 수정] 최소 너비 고정

        VBox rightPanel = createSidePanel("상대 아이템 (Trap)", false);
        rightPanel.setMinWidth(160); // [버그 수정] 최소 너비 고정

        root.setLeft(leftPanel);
        root.setRight(rightPanel);

        // 3. 중앙 게임판
        StackPane centerWrapper = new StackPane(landGrabPanel);
        centerWrapper.setPadding(new Insets(10, 10, 10, 10));
        centerWrapper.setEffect(new DropShadow(20, Color.rgb(0,0,0,0.2)));
        centerWrapper.setMinSize(0, 0); // [버그 수정] 부모가 줄어들 때 같이 줄어들도록 설정

        root.setCenter(centerWrapper);

        // 4. 하단 입력창
        setupInputBar();
        root.setBottom(controlBox);
    }

    private void setupComboUI() {
        lblComboCount.setFont(Font.font("CookieRun Regular", FontWeight.BOLD, 28));
        lblComboCount.setTextFill(Color.web("#6A1B9A"));

        lblComboLabel.setFont(Font.font("System", FontWeight.BOLD, 10));
        lblComboLabel.setTextFill(Color.GRAY);

        comboBar.setPrefWidth(100);
        comboBar.setPrefHeight(8);
        comboBar.setStyle("-fx-accent: #BA68C8; -fx-control-inner-background: #F3E5F5; -fx-text-box-border: transparent;");

        comboContainer.getChildren().addAll(lblComboCount, lblComboLabel, comboBar);
        comboContainer.setAlignment(Pos.CENTER);
        comboContainer.setPadding(new Insets(8, 15, 8, 15));
        comboContainer.setStyle("-fx-background-color: white; -fx-background-radius: 15;");

        comboWrapper.getChildren().add(comboContainer);
        comboWrapper.setStyle("-fx-background-radius: 18; -fx-background-color: transparent; -fx-padding: 3;");
    }

    private VBox createSidePanel(String title, boolean isBuff) {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20, 15, 20, 15));
        panel.setPrefWidth(160);
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setStyle("-fx-background-color: rgba(255,255,255,0.4);");

        Label titleLbl = new Label(title);
        titleLbl.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 14));
        titleLbl.setTextFill(Color.web("#5D4037"));
        panel.getChildren().add(titleLbl);

        if (isBuff) {
            panel.getChildren().add(createItemRow("스플래시", "주변 타일 동시 공격", Color.DEEPSKYBLUE, "SPLASH"));
            panel.getChildren().add(createItemRow("보호막", "내 땅 무적 (5초)", Color.GOLD, "BARRIER"));
            panel.getChildren().add(createItemRow("콤보가드", "콤보 끊김 1회 방어", Color.LIMEGREEN, "GUARD"));
        } else {
            panel.getChildren().add(createItemRow("먹물", "상대 화면 가리기", Color.BLACK, "INK"));
            panel.getChildren().add(createItemRow("EMP", "상대 땅 파괴", Color.BLUE, "EMP"));
            panel.getChildren().add(createItemRow("혼란", "글자 뒤집기", Color.PURPLE, "CONFUSION"));
        }

        return panel;
    }

    private HBox createItemRow(String name, String desc, Color iconColor, String type) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Canvas iconCanvas = new Canvas(32, 32);
        drawIcon(iconCanvas.getGraphicsContext2D(), type, iconColor);

        VBox textBox = new VBox(2);
        Label nameLbl = new Label(name);
        nameLbl.setFont(Font.font("System", FontWeight.BOLD, 12));
        Label descLbl = new Label(desc);
        descLbl.setFont(Font.font("System", 10));
        descLbl.setTextFill(Color.GRAY);
        descLbl.setWrapText(true);

        textBox.getChildren().addAll(nameLbl, descLbl);
        row.getChildren().addAll(iconCanvas, textBox);

        return row;
    }

    private void drawIcon(GraphicsContext gc, String type, Color color) {
        gc.setFill(color);
        switch (type) {
            case "SPLASH" -> {
                gc.fillOval(4, 8, 24, 24);
                gc.setFill(Color.WHITE);
                gc.fillOval(18, 12, 6, 6);
            }
            case "BARRIER" -> {
                gc.fillRoundRect(6, 4, 20, 24, 10, 10);
                gc.setStroke(Color.ORANGE);
                gc.setLineWidth(2);
                gc.strokeRoundRect(6, 4, 20, 24, 10, 10);
            }
            case "GUARD" -> {
                gc.setStroke(color);
                gc.setLineWidth(4);
                gc.strokeOval(6, 6, 20, 20);
            }
            case "INK" -> {
                gc.fillOval(4, 4, 12, 12);
                gc.fillOval(14, 8, 14, 14);
                gc.fillOval(8, 14, 10, 10);
            }
            case "EMP" -> {
                gc.setFill(color);
                gc.fillPolygon(new double[]{16, 24, 14, 16, 8, 18}, new double[]{4, 12, 12, 24, 16, 16}, 6);
            }
            case "CONFUSION" -> {
                gc.setFont(Font.font("System", FontWeight.BOLD, 24));
                gc.fillText("?", 10, 24);
            }
        }
    }

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

    private VBox createPlayerBox(Label nameLabel, Label scoreLabel, String colorCode) {
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        nameLabel.setTextFill(Color.web("#757575"));
        scoreLabel.setTextFill(Color.web(colorCode));
        VBox box = new VBox(2, nameLabel, scoreLabel);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private Label createHudBadge(String text, String bgColor, String borderColor, int fontSize) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("CookieRun Regular", fontSize));
        if (lbl.getFont().getName().equals("System")) lbl.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, fontSize));
        lbl.setPadding(new Insets(5, 12, 5, 12));
        lbl.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 15; -fx-border-color: " + borderColor + "; -fx-border-radius: 15; -fx-border-width: 2px;");
        return lbl;
    }

    public BorderPane getRoot() { return root; }
    public LandGrabPanel getLandGrabPanel() { return landGrabPanel; }
    public TextField getInputField() { return inputField; }
    public HBox getControlBox() { return controlBox; }

    public void setTimeText(String text) { lblTime.setText(text.replace("남은 시간: ", "")); }
    public void setMyScoreText(String text) { lblMyScore.setText(extractScore(text)); }
    public void setAiScoreText(String text) { lblAiScore.setText(extractScore(text)); }

    public void setPlayerNames(String myName, String oppName) {
        lblMyName.setText(myName);
        lblOppName.setText(oppName);
    }

    public void setComboText(String text) {
        String numStr = text.replaceAll("[^0-9]", "");
        int combo = numStr.isEmpty() ? 0 : Integer.parseInt(numStr);
        lblComboCount.setText(String.valueOf(combo));
        double progress = Math.min(combo / 10.0, 1.0);
        comboBar.setProgress(progress);
        if (combo >= 10) {
            lblComboLabel.setText("FEVER!");
            lblComboLabel.setTextFill(Color.RED);
            comboContainer.setEffect(new Glow(0.8));
        } else {
            lblComboLabel.setText("COMBO");
            lblComboLabel.setTextFill(Color.GRAY);
            comboContainer.setEffect(null);
        }
    }

    public void setComboGuardActive(boolean active) {
        if (active) {
            comboWrapper.setStyle("-fx-background-color: #FFD700; -fx-background-radius: 18; -fx-padding: 3; -fx-effect: dropshadow(gaussian, gold, 10, 0.5, 0, 0);");
            lblComboLabel.setText("GUARD ON");
            lblComboLabel.setTextFill(Color.GOLD.darker());
        } else {
            comboWrapper.setStyle("-fx-background-color: transparent; -fx-padding: 3;");
        }
    }

    public void setEffectsText(String text) { lblStatus.setText(text); }
    public void setLastItemText(String text) {
        if(!text.contains("없음") && !text.contains("-")) lblStatus.setText(text + " 발동!");
    }
    private String extractScore(String text) { return text.replaceAll("[^0-9]", ""); }

    public void flashHit() { landGrabPanel.flashHit(); }
    public void flashMiss() { landGrabPanel.flashMiss(); }
    public void flashItem(Color color) { landGrabPanel.flashBuffColor(color); }
}