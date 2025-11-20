package typingarena.minigames.tugofwar;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import typingarena.core.tugofwar.GameLogic;

/**
 * 기존 싱글 플레이 줄다리기 Stage.
 * UI는 TugOfWarMatchView를 재활용해 멀티 버전과 공통으로 유지한다.
 */
public class TugOfWarGame extends Stage {

    private final GameLogic logic = new GameLogic();
    private final TugOfWarMatchView view = new TugOfWarMatchView();
    private final RopePanel ropePanel = view.getRopePanel();
    private final TextField inputField = view.getInputField();
    private final Button startButton = new Button("게임 시작");

    private final Timeline gameLoop;
    private long lastItemNotifiedAt = 0L;

    public TugOfWarGame() {
        setTitle("Typing Arena - 줄다리기");

        inputField.setDisable(true);
        startButton.setOnAction(e -> startGame());
        startButton.setFont(inputField.getFont());
        view.getControlBox().getChildren().add(startButton);

        Scene scene = new Scene(view.getRoot(), 1000, 600);
        setScene(scene);

        inputField.setOnAction(e -> handleSubmit());
        scene.setOnMouseClicked(e -> {
            if (!inputField.isDisabled()) {
                inputField.requestFocus();
            }
        });

        setOnShown(e -> {
            ropePanel.activate();
            updateView();
        });

        gameLoop = new Timeline(new KeyFrame(Duration.millis(100), e -> onTick()));
        gameLoop.setCycleCount(Timeline.INDEFINITE);
        setOnCloseRequest(e -> gameLoop.stop());
        setOnHidden(e -> {
            gameLoop.stop();
            ropePanel.dispose();
        });

        updateHUD();
        updateView();
    }

    private void startGame() {
        logic.startGame();
        startButton.setDisable(true);
        inputField.setDisable(false);
        inputField.clear();
        inputField.requestFocus();
        lastItemNotifiedAt = 0L;

        updateHUD();
        updateView();
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
            view.flashCorrect();
        } else {
            view.flashWrong();
        }

        inputField.clear();
        inputField.requestFocus();

        updateHUD();
        updateView();
    }

    private void onTick() {
        String result = logic.tick();
        updateHUD();
        updateView();

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
        view.setTimeText(String.format("남은 시간: %.1fs", logic.getTimeMs() / 1000.0));
        view.setScoreText("점수: " + logic.getScore());
        view.setComboText("콤보: " + logic.getCombo());
        view.setPosText(String.format("위치: %.1f", logic.getPos()));
        view.setEffectsText(logic.getEffects().describeEffects());

        GameLogic.ItemType itemType = logic.getLastActivatedItem();
        view.setLastItemText("최근 아이템: " + formatItemLabel(itemType));

        long activatedAt = logic.getLastItemActivatedAt();
        if (itemType != GameLogic.ItemType.NONE && activatedAt > lastItemNotifiedAt) {
            lastItemNotifiedAt = activatedAt;
            view.flashItem(colorForItem(itemType));
        }
    }

    private void updateView() {
        TugOfWarViewState state = new TugOfWarViewState(
                logic.getPos(),
                logic.getCurrentWord(),
                logic.getCurrentWordModifier(),
                logic.getEffects().isBlindActive(),
                logic.getEffects().isJamoSplitActive()
        );
        ropePanel.updateState(state);
    }

    private String formatItemLabel(GameLogic.ItemType itemType) {
        return switch (itemType) {
            case POWER_GRIP -> "파워 그립";
            case ANCHOR -> "앵커";
            case BLIND -> "먹물";
            case JAMO_SPLIT -> "자소 분리";
            default -> "없음";
        };
    }

    private Color colorForItem(GameLogic.ItemType itemType) {
        return switch (itemType) {
            case POWER_GRIP -> Color.rgb(80, 160, 255);
            case ANCHOR -> Color.rgb(80, 200, 120);
            case BLIND -> Color.rgb(30, 30, 30);
            case JAMO_SPLIT -> Color.rgb(120, 90, 200);
            default -> Color.TRANSPARENT;
        };
    }
}
