package typingarena.minigames.tugofwar;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

public class TugOfWarGame extends Stage {

    private final GameLogic logic = new GameLogic();
    private final RopePanel ropePanel = new RopePanel(logic);

    // HUD 라벨들
    private final Label lblTime = new Label("남은 시간: 60.0s");
    private final Label lblScore = new Label("점수: 0");
    private final Label lblCombo = new Label("콤보: 0");
    private final Label lblPos = new Label("위치: 0.0");
    private final Label lblEffects = new Label("효과: 없음");
    private final Label lblLastItem = new Label("최근 아이템: 없음");

    // 입력창 / 버튼들
    private final TextField inputField = new TextField();
    private final Button startButton = new Button("게임 시작");

    private final Timeline gameLoop;
    private long lastItemNotifiedAt = 0L;

    public TugOfWarGame() {
        setTitle("Typing Arena - 줄다리기");
        initModality(Modality.NONE);

        BorderPane root = new BorderPane();

        // ===== 상단 HUD =====
        HBox top = new HBox(18, lblTime, lblScore, lblCombo, lblPos, lblEffects, lblLastItem);
        top.setAlignment(Pos.CENTER);
        top.setPadding(new Insets(12, 24, 12, 24));
        Font hudFont = Font.font("System", FontWeight.BOLD, 16);
        lblTime.setFont(hudFont);
        lblScore.setFont(hudFont);
        lblCombo.setFont(hudFont);
        lblPos.setFont(hudFont);
        lblEffects.setFont(hudFont);
        lblLastItem.setFont(hudFont);

        // ===== 중앙(경기장) =====
        ropePanel.setWidth(900);
        ropePanel.setHeight(380);
        StackPane centerWrapper = new StackPane(ropePanel);
        centerWrapper.setPadding(new Insets(0, 24, 0, 24));
        centerWrapper.setMinSize(300, 200);
        centerWrapper.widthProperty().addListener((obs, oldV, newV) -> {
            double width = Math.max(1, newV.doubleValue());
            ropePanel.setWidth(width);
            ropePanel.redraw();
        });
        centerWrapper.heightProperty().addListener((obs, oldV, newV) -> {
            double height = Math.max(1, newV.doubleValue());
            ropePanel.setHeight(height);
            ropePanel.redraw();
        });

        // ===== 하단(입력창 + 시작 버튼) =====
        inputField.setFont(Font.font("System", FontWeight.NORMAL, 22));
        inputField.setPromptText("단어를 입력하고 Enter 키를 누르세요");
        inputField.setDisable(true);

        startButton.setFont(Font.font("System", FontWeight.BOLD, 18));
        startButton.setOnAction(e -> startGame());

        HBox bottomContent = new HBox(12, inputField, startButton);
        bottomContent.setAlignment(Pos.CENTER);
        HBox.setHgrow(inputField, Priority.ALWAYS);

        BorderPane bottom = new BorderPane();
        bottom.setPadding(new Insets(16, 24, 16, 24));
        bottom.setCenter(bottomContent);

        root.setTop(top);
        root.setCenter(centerWrapper);
        root.setBottom(bottom);

        Scene scene = new Scene(root, 1000, 600);
        setScene(scene);

        // ===== 이벤트 바인딩 =====
        inputField.setOnAction(e -> handleSubmit());
        scene.setOnMouseClicked(e -> {
            if (!inputField.isDisabled()) {
                inputField.requestFocus();
            }
        });

        setOnShown(e -> {
            ropePanel.activate();
            ropePanel.redraw();
        });

        // 게임 루프: 100ms마다 tick
        gameLoop = new Timeline(new KeyFrame(Duration.millis(100), e -> onTick()));
        gameLoop.setCycleCount(Timeline.INDEFINITE);
        setOnCloseRequest(e -> gameLoop.stop());
        setOnHidden(e -> {
            gameLoop.stop();
            ropePanel.dispose();
        });

        updateHUD();
    }

    private void startGame() {
        logic.startGame();
        startButton.setDisable(true);
        inputField.setDisable(false);
        inputField.clear();
        inputField.requestFocus();
        lastItemNotifiedAt = 0L;

        updateHUD();
        ropePanel.redraw();
        gameLoop.playFromStart();
    }

    private void handleSubmit() {
        if (!logic.isRunning()) {
            inputField.clear();
            return;
        }

        String typed = inputField.getText().trim();
        boolean correct = logic.submitAnswer(typed);

        if (correct) {
            ropePanel.flashRight();
        } else {
            ropePanel.flashLeft();
        }

        inputField.clear();
        inputField.requestFocus();

        updateHUD();
        ropePanel.redraw();
    }

    private void onTick() {
        if (!isShowing()) {
            gameLoop.stop();
            return;
        }

        String result = logic.tick();
        updateHUD();
        ropePanel.redraw();

        if (result != null) {
            gameLoop.stop();
            startButton.setDisable(false);
            inputField.setDisable(true);
            showResultDialog(result);
        }
    }

    private void showResultDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("결과");
        alert.setHeaderText("게임 종료");
        alert.setContentText(message + "\n점수: " + logic.getScore() + " / 콤보: " + logic.getCombo());
        alert.initOwner(getOwner() != null ? getOwner() : this);
        alert.show();
    }

    private void updateHUD() {
        if (!isShowing()) {
            return;
        }

        lblTime.setText(String.format("남은 시간: %.1fs", logic.getTimeMs() / 1000.0));
        lblScore.setText("점수: " + logic.getScore());
        lblCombo.setText("콤보: " + logic.getCombo());
        lblPos.setText(String.format("위치: %.1f", logic.getPos()));
        lblEffects.setText(logic.getEffects().describeEffects());

        GameLogic.ItemType itemType = logic.getLastActivatedItem();
        lblLastItem.setText("최근 아이템: " + formatItemLabel(itemType));

        long activatedAt = logic.getLastItemActivatedAt();
        if (itemType != GameLogic.ItemType.NONE && activatedAt > lastItemNotifiedAt) {
            lastItemNotifiedAt = activatedAt;
            ropePanel.flashBuffColor(colorForItem(itemType));
        }
    }

    private String formatItemLabel(GameLogic.ItemType itemType) {
        return switch (itemType) {
            case POWER_GRIP -> "파워 그립";
            case ANCHOR -> "앵커";
            case BLIND -> "먹물";
            default -> "없음";
        };
    }

    private Color colorForItem(GameLogic.ItemType itemType) {
        return switch (itemType) {
            case POWER_GRIP -> Color.rgb(80, 160, 255);
            case ANCHOR -> Color.rgb(80, 200, 120);
            case BLIND -> Color.rgb(30, 30, 30);
            default -> Color.TRANSPARENT;
        };
    }
}
