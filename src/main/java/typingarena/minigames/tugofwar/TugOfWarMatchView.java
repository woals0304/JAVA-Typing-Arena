package typingarena.minigames.tugofwar;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
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

/**
 * 싱글/멀티 모두에서 사용하는 공통 UI.
 */
public class TugOfWarMatchView {

    private final BorderPane root = new BorderPane();
    private final RopePanel ropePanel = new RopePanel();

    // HUD labels
    private final Label lblTime = createHudLabel("남은 시간: 60.0s");
    private final Label lblScore = createHudLabel("점수: -");
    private final Label lblCombo = createHudLabel("콤보: -");
    private final Label lblPos = createHudLabel("위치: 0.0");
    private final Label lblEffects = createHudLabel("효과: 없음");
    private final Label lblLastItem = createHudLabel("최근 아이템: 없음");

    private final TextField inputField = new TextField();
    private final HBox controlBox = new HBox(12);

    public TugOfWarMatchView() {
        HBox top = new HBox(18, lblTime, lblScore, lblCombo, lblPos, lblEffects, lblLastItem);
        top.setAlignment(Pos.CENTER);
        top.setPadding(new Insets(12, 24, 12, 24));
        root.setTop(top);

        ropePanel.setWidth(900);
        ropePanel.setHeight(380);
        StackPane centerWrapper = new StackPane(ropePanel);
        centerWrapper.setPadding(new Insets(0, 24, 0, 24));
        centerWrapper.setMinSize(300, 200);
        centerWrapper.widthProperty().addListener((obs, oldV, newV) -> {
            ropePanel.setWidth(Math.max(1, newV.doubleValue()));
            ropePanel.redraw();
        });
        centerWrapper.heightProperty().addListener((obs, oldV, newV) -> {
            ropePanel.setHeight(Math.max(1, newV.doubleValue()));
            ropePanel.redraw();
        });
        root.setCenter(centerWrapper);

        inputField.setFont(Font.font("System", FontWeight.NORMAL, 22));
        inputField.setPromptText("단어를 입력하고 Enter 키를 누르세요");
        HBox.setHgrow(inputField, Priority.ALWAYS);

        controlBox.setAlignment(Pos.CENTER_LEFT);
        controlBox.getChildren().add(inputField);

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

    public BorderPane getRoot() {
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

    public void setTimeText(String text) {
        lblTime.setText(text);
    }

    public void setScoreText(String text) {
        lblScore.setText(text);
    }

    public void setComboText(String text) {
        lblCombo.setText(text);
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
}
