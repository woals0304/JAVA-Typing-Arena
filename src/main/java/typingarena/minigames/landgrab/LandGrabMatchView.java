package typingarena.minigames.landgrab;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
// [제거] core.landgrab.LandGrabLogic import 제거

/**
 * [신규] 땅따먹기 게임의 공통 UI (View)
 * (TugOfWarMatchView.java와 동일한 역할)
 * [수정] '바보' View가 되었으므로, 생성자에서 coreLogic을 받지 않습니다.
 */
public class LandGrabMatchView {

    private final BorderPane root = new BorderPane();
    private final LandGrabPanel landGrabPanel; // Panel

    // HUD labels (LandGrabGame.java에서 가져옴)
    private final Label lblTime = createHudLabel("남은 시간: 60.0s");
    private final Label lblMyScore = createHudLabel("나: 0칸");
    private final Label lblAiScore = createHudLabel("AI: 0칸");
    private final Label lblCombo = createHudLabel("콤보: 0");
    private final Label lblEffects = createHudLabel("효과: 없음");
    private final Label lblLastItem = createHudLabel("최근 아이템: 없음");

    private final TextField inputField = new TextField();
    private final HBox controlBox = new HBox(12); // 하단 컨트롤 (입력창, 버튼)

    /**
     * [수정] LandGrabMatchView는 '바보' View이므로 엔진이 필요 없습니다.
     * LandGrabPanel() 생성자가 비었으므로, 그냥 생성합니다.
     */
    public LandGrabMatchView() {
        this.landGrabPanel = new LandGrabPanel();

        // ===== 상단 HUD =====
        HBox top = new HBox(18, lblTime, lblMyScore, lblAiScore, lblCombo, lblEffects, lblLastItem);
        top.setAlignment(Pos.CENTER);
        top.setPadding(new Insets(12, 24, 12, 24));
        root.setTop(top);

        // ===== 중앙(경기장) =====
        StackPane centerWrapper = new StackPane(landGrabPanel);
        centerWrapper.setPadding(new Insets(0, 24, 0, 24));
        centerWrapper.setMinSize(300, 300);
        root.setCenter(centerWrapper);

        // ===== 하단(입력창) =====
        inputField.setFont(Font.font("System", FontWeight.NORMAL, 22));
        inputField.setPromptText("단어를 입력하고 Enter 키를 누르세요");
        HBox.setHgrow(inputField, Priority.ALWAYS);

        controlBox.setAlignment(Pos.CENTER);
        controlBox.getChildren().add(inputField); // 기본으로 입력창만 둠

        BorderPane bottom = new BorderPane();
        bottom.setPadding(new Insets(16, 24, 16, 24));
        bottom.setCenter(controlBox);
        root.setBottom(bottom);
    }

    private Label createHudLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("System", FontWeight.BOLD, 16));
        return lbl;
    }

    // --- Controller가 사용할 Getter ---

    public BorderPane getRoot() {
        return root;
    }

    public LandGrabPanel getLandGrabPanel() {
        return landGrabPanel;
    }

    public TextField getInputField() {
        return inputField;
    }

    public HBox getControlBox() {
        return controlBox;
    }

    // --- Controller가 사용할 HUD Setter ---

    public void setTimeText(String text) {
        lblTime.setText(text);
    }

    public void setMyScoreText(String text) {
        lblMyScore.setText(text);
    }

    public void setAiScoreText(String text) {
        lblAiScore.setText(text);
    }

    public void setComboText(String text) {
        lblCombo.setText(text);
    }

    public void setEffectsText(String text) {
        lblEffects.setText(text);
    }

    public void setLastItemText(String text) {
        lblLastItem.setText(text);
    }

    public void flashHit() {
        landGrabPanel.flashHit();
    }

    public void flashMiss() {
        landGrabPanel.flashMiss();
    }

    public void flashItem(Color color) {
        landGrabPanel.flashBuffColor(color);
    }
}