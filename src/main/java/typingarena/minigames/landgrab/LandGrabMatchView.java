package typingarena.minigames.landgrab;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class LandGrabMatchView {

    private final BorderPane root = new BorderPane();
    private final LandGrabPanel landGrabPanel;

    // HUD 라벨
    private final Label lblTime = createHudBadge("60.0s", "#FFF8E1", "#FFB74D", 18);
    private final Label lblMyScore = createHudBadge("0", "#E1F5FE", "#4FC3F7", 22);
    private final Label lblAiScore = createHudBadge("0", "#FFEBEE", "#E57373", 22);
    private final Label lblCombo = createHudBadge("0 Combo", "#F3E5F5", "#BA68C8", 16);

    // 상태 메시지 (상단 중앙)
    private final Label lblStatus = new Label("Ready?");

    private final TextField inputField = new TextField();
    private final HBox controlBox = new HBox(10); // 하단 바

    public LandGrabMatchView() {
        this.landGrabPanel = new LandGrabPanel();
        root.setStyle("-fx-background-color: #FFF3E0;");

        // 1. 상단 HUD
        HBox scoreBox = new HBox(15);
        scoreBox.setAlignment(Pos.CENTER);

        VBox myBox = createPlayerBox("나 (Player)", lblMyScore, "#0288D1");
        VBox aiBox = createPlayerBox("상대 (AI)", lblAiScore, "#D32F2F");
        Label vsLabel = new Label("VS");
        vsLabel.setFont(Font.font("CookieRun Regular", FontWeight.BOLD, 20));
        vsLabel.setTextFill(Color.GRAY);

        scoreBox.getChildren().addAll(lblTime, myBox, vsLabel, aiBox, lblCombo);
        scoreBox.setPadding(new Insets(10, 20, 10, 20));

        StackPane topContainer = new StackPane(scoreBox);
        topContainer.setPadding(new Insets(10));
        topContainer.setStyle("-fx-background-color: rgba(255,255,255,0.8); -fx-background-radius: 0 0 20 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        lblStatus.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 16));
        lblStatus.setTextFill(Color.web("#F57C00"));

        VBox topLayout = new VBox(10, topContainer, lblStatus);
        topLayout.setAlignment(Pos.CENTER);
        root.setTop(topLayout);

        // 2. 중앙 게임판
        StackPane centerWrapper = new StackPane(landGrabPanel);
        centerWrapper.setPadding(new Insets(10, 30, 10, 30));
        centerWrapper.setEffect(new DropShadow(20, Color.rgb(0,0,0,0.2)));
        root.setCenter(centerWrapper);

        // 3. 하단 입력창 (버튼 없이 꽉 차게)
        inputField.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 18));
        inputField.setPromptText("단어를 입력하세요...");
        inputField.setStyle(
                "-fx-background-radius: 30;" +
                        "-fx-background-color: white;" +
                        "-fx-border-color: #BA68C8;" +
                        "-fx-border-width: 2px;" +
                        "-fx-border-radius: 30;" +
                        "-fx-padding: 8 20 8 20;" +
                        "-fx-text-fill: #333;"
        );
        HBox.setHgrow(inputField, Priority.ALWAYS);

        controlBox.setAlignment(Pos.CENTER);
        controlBox.setPadding(new Insets(15, 20, 15, 20));
        controlBox.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-background-radius: 20 20 0 0;");

        // [수정] 버튼 없이 입력창만 추가
        controlBox.getChildren().add(inputField);

        root.setBottom(controlBox);
    }

    private VBox createPlayerBox(String title, Label scoreLabel, String colorCode) {
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", 12));
        titleLabel.setTextFill(Color.web("#757575"));
        scoreLabel.setTextFill(Color.web(colorCode));
        VBox box = new VBox(2, titleLabel, scoreLabel);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private Label createHudBadge(String text, String bgColor, String borderColor, int fontSize) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("CookieRun Regular", fontSize));
        if (lbl.getFont().getName().equals("System")) {
            lbl.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, fontSize));
        }
        lbl.setPadding(new Insets(5, 12, 5, 12));
        lbl.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 15; -fx-border-color: " + borderColor + "; -fx-border-radius: 15; -fx-border-width: 2px;");
        return lbl;
    }

    // [핵심] 오류 해결을 위한 Getter 추가
    public HBox getControlBox() { return controlBox; }

    public BorderPane getRoot() { return root; }
    public LandGrabPanel getLandGrabPanel() { return landGrabPanel; }
    public TextField getInputField() { return inputField; }

    public void setTimeText(String text) { lblTime.setText(text.replace("남은 시간: ", "")); }
    public void setMyScoreText(String text) { lblMyScore.setText(extractScore(text)); }
    public void setAiScoreText(String text) { lblAiScore.setText(extractScore(text)); }
    public void setComboText(String text) { lblCombo.setText(text.replace("콤보: ", "") + " Combo"); }

    public void setEffectsText(String text) { lblStatus.setText(text); }
    public void setLastItemText(String text) {
        if(!text.contains("없음") && !text.contains("-")) {
            lblStatus.setText(text + " 발동!");
        }
    }

    private String extractScore(String text) { return text.replaceAll("[^0-9]", ""); }
    public void flashHit() { landGrabPanel.flashHit(); }
    public void flashMiss() { landGrabPanel.flashMiss(); }
    public void flashItem(Color color) { landGrabPanel.flashBuffColor(color); }
}