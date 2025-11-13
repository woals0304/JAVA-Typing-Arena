package typingarena.minigames.landgrab;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

// [신규] 핵심 엔진(Model)과 UI View를 import 합니다.
import typingarena.core.landgrab.LandGrabLogic;
import typingarena.core.landgrab.LandGrabEffects.ItemType;
import typingarena.core.landgrab.LandGrabViewState; // [신규] ViewState import
import typingarena.minigames.landgrab.LandGrabMatchView; // UI 뼈대

/**
 * [대규모 리팩토링됨]
 * 1. 'TugOfWarGame'처럼 이 클래스가 '싱글 플레이 제어기(Controller)' 역할을 합니다.
 * 2. 'core.landgrab.LandGrabLogic' (엔진)을 생성합니다.
 * 3. 'LandGrabMatchView' (UI 뼈대)를 생성합니다. (View는 이제 '바보'입니다.)
 * 4. [수정] AI 타이머(onTick)가 'coreLogic'의 상태를 'viewState'로 복사하여 'panel'에 주입합니다.
 */
public class LandGrabGame extends Stage {

    // --- Model ---
    private final LandGrabLogic coreLogic = new LandGrabLogic();

    // --- View ---
    // [수정] View 생성자 변경 (이제 logic을 주입받지 않음)
    private final LandGrabMatchView view = new LandGrabMatchView();
    private final LandGrabPanel landGrabPanel = view.getLandGrabPanel();
    private final TextField inputField = view.getInputField();
    private final Button startButton = new Button("게임 시작");

    // --- Controller ---
    private final Timeline gameLoop;
    private int timeMs = 60_000;
    private boolean running = false;
    private int aiTickTimerMs = 0;
    private static final int AI_CAPTURE_INTERVAL_MS = 2_000;
    private long lastItemNotifiedAt = 0L;

    public LandGrabGame() {
        setTitle("Typing Arena - 땅따먹기");
        initModality(Modality.NONE);

        // [수정] 'startButton'을 view의 controlBox에 추가합니다.
        inputField.setDisable(true);
        startButton.setFont(inputField.getFont());
        startButton.setOnAction(e -> startGame());
        view.getControlBox().getChildren().add(startButton);

        // [수정] view.getRoot()를 Scene으로 사용합니다.
        Scene scene = new Scene(view.getRoot(), 700, 800);
        setScene(scene);

        // ===== 이벤트 바인딩 (동일) =====
        inputField.setOnAction(e -> handleSubmit());
        scene.setOnMouseClicked(e -> {
            if (!inputField.isDisabled()) inputField.requestFocus();
        });
        setOnShown(e -> {
            landGrabPanel.activate();
            updateView(); // [신규] 켜질 때 View 갱신
        });
        gameLoop = new Timeline(new KeyFrame(Duration.millis(100), e -> onTick()));
        gameLoop.setCycleCount(Timeline.INDEFINITE);
        setOnCloseRequest(e -> gameLoop.stop());
        setOnHidden(e -> {
            gameLoop.stop();
            landGrabPanel.dispose();
        });

        updateHUD();
        updateView(); // [신규] 초기 View 상태 갱신
    }

    /**
     * [수정] startGame: coreLogic 초기화 후 view도 갱신
     */
    private void startGame() {
        // 1. Controller 상태 초기화
        timeMs = 60_000;
        running = true;
        aiTickTimerMs = AI_CAPTURE_INTERVAL_MS;
        lastItemNotifiedAt = 0L;

        // 2. Model(엔진) 상태 초기화
        coreLogic.startGame();

        // 3. UI 초기화
        startButton.setDisable(true);
        inputField.setDisable(false);
        inputField.clear();
        inputField.requestFocus();

        // 4. View 갱신
        updateHUD();
        updateView(); // [신규]

        gameLoop.playFromStart();
    }

    /**
     * [수정] handleSubmit: coreLogic 호출 후 view 갱신
     */
    private void handleSubmit() {
        if (!running) {
            inputField.clear();
            return;
        }
        String typed = inputField.getText().trim();

        LandGrabLogic.SubmitResult result = coreLogic.submitAnswer(typed);

        if (result.resultCode() > 0) { // 1, 2, 3 (성공)
            view.flashHit();

            if (result.resultCode() == 2) { // 2 = 버프
                landGrabPanel.showSplashAnimation(result.r(), result.c());
            } else if (result.resultCode() == 3) { // 3 = 트랩
                landGrabPanel.showInkSplashAnimation(result.r(), result.c());
            }

        } else { // 0 (실패)
            view.flashMiss();
        }

        inputField.clear();
        inputField.requestFocus();
        updateHUD();
        updateView(); // [신규]
    }

    /**
     * [수정] onTick: coreLogic 호출 후 view 갱신
     */
    private void onTick() {
        if (!isShowing() || !running) {
            gameLoop.stop();
            return;
        }

        // 1. 시간 감소
        timeMs -= 100;

        // 2. AI 타이머 작동
        aiTickTimerMs -= 100;
        if (aiTickTimerMs <= 0) {
            coreLogic.aiCaptureTile(); // 엔진의 AI 로직 호출
            aiTickTimerMs = AI_CAPTURE_INTERVAL_MS;
        }

        // 3. HUD 및 View 갱신
        updateHUD();
        updateView(); // [신규]

        // 4. 게임 종료 조건 확인
        String result = null;
        if (timeMs <= 0) {
            running = false;
            // (승패 판정 로직 동일)
            if (coreLogic.getScorePlayer() > coreLogic.getScoreAI()) result = "승리! 더 많은 땅을 차지했습니다.";
            else if (coreLogic.getScoreAI() > coreLogic.getScorePlayer()) result = "패배... AI가 더 많습니다.";
            else result = "무승부!";
        }
        if (coreLogic.getScorePlayer() + coreLogic.getScoreAI() == LandGrabLogic.GRID_SIZE * LandGrabLogic.GRID_SIZE) {
            running = false;
            // (승패 판정 로직 동일)
            if (coreLogic.getScorePlayer() > coreLogic.getScoreAI()) result = "승리! 모든 땅을 차지했습니다.";
            else if (coreLogic.getScoreAI() > coreLogic.getScorePlayer()) result = "패배... AI에게 모두 빼앗겼습니다.";
            else result = "무승부!";
        }

        // 5. 게임 종료 처리
        if (result != null) {
            gameLoop.stop();
            startButton.setDisable(false);
            inputField.setDisable(true);
            showResultDialog(result);
        }
    }

    /**
     * [신규] 'TugOfWarGame.updateView'와 동일한 역할
     * Model(coreLogic)의 현재 상태를 ViewState로 복사하여 Panel(View)에 주입
     */
    private void updateView() {
        // 1. Model의 현재 상태를 기반으로 ViewState 객체를 생성
        LandGrabViewState currentState = new LandGrabViewState(coreLogic);
        // 2. View(Panel)에 ViewState 주입
        landGrabPanel.updateState(currentState);
    }

    private void showResultDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("결과");
        alert.setHeaderText("게임 종료");
        alert.setContentText(message + "\n나: " + coreLogic.getScorePlayer() + "칸 / AI: " + coreLogic.getScoreAI() + "칸");
        alert.initOwner(getOwner() != null ? getOwner() : this);
        alert.show();
    }

    /**
     * [수정] HUD 갱신 (view의 Setter 사용)
     */
    private void updateHUD() {
        if (!isShowing()) {
            return;
        }

        view.setTimeText(String.format("남은 시간: %.1fs", timeMs / 1000.0));
        view.setMyScoreText("나: " + coreLogic.getScorePlayer() + "칸");
        view.setAiScoreText("AI: " + coreLogic.getScoreAI() + "칸");
        view.setComboText("콤보: " + coreLogic.getCombo());
        view.setEffectsText(coreLogic.getEffects().describeEffects());

        ItemType itemType = coreLogic.getEffects().getLastItemActivatedItem();
        view.setLastItemText("최근 아이템: " + formatItemLabel(itemType));

        long activatedAt = coreLogic.getEffects().getLastItemActivatedAt();
        if (itemType != ItemType.NONE && activatedAt > lastItemNotifiedAt) {
            lastItemNotifiedAt = activatedAt;
            view.flashItem(colorForItem(itemType));
        }
    }

    // (formatItemLabel, colorForItem 헬퍼 메서드는 동일)
    private String formatItemLabel(ItemType itemType) {
        return switch (itemType) {
            case BUFF_SPLASH -> "스플래시";
            case TRAP_BLIND -> "먹물";
            default -> "없음";
        };
    }

    private Color colorForItem(ItemType itemType) {
        return switch (itemType) {
            case BUFF_SPLASH -> Color.rgb(80, 160, 255);
            case TRAP_BLIND -> Color.rgb(30, 30, 30);
            default -> Color.TRANSPARENT;
        };
    }
}