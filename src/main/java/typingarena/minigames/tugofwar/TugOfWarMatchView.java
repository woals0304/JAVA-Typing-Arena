package typingarena.minigames.tugofwar;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * 싱글/멀티 모두에서 사용하는 공통 UI.
 */
public class TugOfWarMatchView {

    // LandGrab 테마에 맞춘 색/여백 토큰
    private static final Color THEME_BG = Color.web("#FDF5E6");
    private static final Color PANEL_BG = Color.web("#FFF3E0");
    private static final Color PANEL_BORDER = Color.web("#D7CCC8");
    private static final Color TEXT_MAIN = Color.web("#4E342E");
    private static final Color TEXT_MUTED = Color.web("#6D4C41");
    private static final Color ACCENT = Color.web("#29B6F6");

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
        root.setBackground(new Background(new BackgroundFill(THEME_BG, CornerRadii.EMPTY, Insets.EMPTY)));
        root.setPadding(new Insets(18, 22, 18, 22));

        HBox top = new HBox(18, lblTime, lblScore, lblCombo, lblPos, lblEffects, lblLastItem);
        top.setAlignment(Pos.CENTER);
        top.setPadding(new Insets(12, 20, 12, 20));
        top.setBackground(new Background(new BackgroundFill(PANEL_BG, new CornerRadii(10), Insets.EMPTY)));
        top.setBorder(new Border(new BorderStroke(PANEL_BORDER, BorderStrokeStyle.SOLID, new CornerRadii(10), BorderWidths.DEFAULT)));
        root.setTop(top);

        ropePanel.setWidth(900);
        ropePanel.setHeight(380);
        StackPane centerWrapper = new StackPane(ropePanel);
        centerWrapper.setPadding(new Insets(16, 12, 4, 12));
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

        inputField.setFont(Font.font("Malgun Gothic", FontWeight.NORMAL, 22));
        inputField.setPromptText("단어를 입력하고 Enter 키를 누르세요");
        inputField.setStyle("""
                -fx-background-color: #fff7e6;
                -fx-border-color: #d7ccc8;
                -fx-border-radius: 8;
                -fx-background-radius: 8;
                -fx-text-fill: #4e342e;
                -fx-padding: 10 12;
                """);
        HBox.setHgrow(inputField, Priority.ALWAYS);

        controlBox.setAlignment(Pos.CENTER_LEFT);
        controlBox.getChildren().add(inputField);
        controlBox.setSpacing(12);

        BorderPane bottom = new BorderPane();
        bottom.setPadding(new Insets(14, 20, 14, 20));
        bottom.setCenter(controlBox);
        bottom.setBackground(new Background(new BackgroundFill(PANEL_BG, new CornerRadii(10), Insets.EMPTY)));
        bottom.setBorder(new Border(new BorderStroke(PANEL_BORDER, BorderStrokeStyle.SOLID, new CornerRadii(10), BorderWidths.DEFAULT)));
        root.setBottom(bottom);
    }

    private Label createHudLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 15));
        lbl.setTextFill(TEXT_MAIN);
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
