package typingarena.minigames.landgrab;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import typingarena.core.landgrab.LandGrabLogic;
import typingarena.core.landgrab.LandGrabLogic.TileState;
import typingarena.core.landgrab.LandGrabEffects.ItemType;
import typingarena.core.landgrab.LandGrabViewState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class LandGrabGame extends Stage {

    private final LandGrabLogic coreLogic = new LandGrabLogic();
    private final Random rnd = new Random();

    private final LandGrabMatchView view = new LandGrabMatchView();
    private final LandGrabPanel landGrabPanel = view.getLandGrabPanel();
    private final TextField inputField = view.getInputField();

    private final Button startButton = new Button("게임 시작");

    private final Timeline gameLoop;
    private double timeMs = 60000.0; // [수정] double로 변경하여 0.1초 단위 표현
    private boolean running = false;

    private int aiTickTimerMs = 0;
    private static final int AI_CAPTURE_INTERVAL_MS = 2000;

    private long confusionUntilPlayer = 0L;

    public LandGrabGame() {
        setTitle("Typing Arena - 땅따먹기 (Single)");
        initModality(Modality.NONE);

        inputField.setDisable(true);
        startButton.setStyle("-fx-background-color: #8E24AA; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20;");
        startButton.setOnAction(e -> startGame());

        view.getControlBox().getChildren().add(startButton);
        view.getRematchButton().setOnAction(e -> startGame());
        view.getQuitButton().setOnAction(e -> close());

        Scene scene = new Scene(view.getRoot(), 950, 800);
        setScene(scene);

        inputField.setOnAction(e -> handleSubmit());
        scene.setOnMouseClicked(e -> {
            if (!inputField.isDisabled()) inputField.requestFocus();
        });
        setOnShown(e -> {
            landGrabPanel.activate();
            updateView();
        });

        // [수정] 0.1초 단위 부드러운 타이머 및 루프
        gameLoop = new Timeline(new KeyFrame(Duration.millis(100), e -> onTick()));
        gameLoop.setCycleCount(Timeline.INDEFINITE);
        setOnCloseRequest(e -> gameLoop.stop());
        setOnHidden(e -> {
            gameLoop.stop();
            landGrabPanel.dispose();
        });

        view.setPlayerNames("나 (Player)", "AI (Computer)");
        updateHUD();
        updateView();
    }

    private void startGame() {
        timeMs = 60000.0;
        running = true;
        aiTickTimerMs = AI_CAPTURE_INTERVAL_MS;
        confusionUntilPlayer = 0L;

        coreLogic.startGame();
        view.hideGameOver();
        startButton.setDisable(true);
        inputField.setDisable(false);
        inputField.clear();
        inputField.requestFocus();

        updateHUD();
        updateView();
        gameLoop.playFromStart();
    }

    private void handleSubmit() {
        if (!running) {
            inputField.clear();
            return;
        }
        String typed = inputField.getText().trim();
        LandGrabLogic.SubmitResult result = coreLogic.submitAnswer(typed, TileState.PLAYER_A);
        handleResultEffect(result, true);
        inputField.clear();
        inputField.requestFocus();
        updateHUD();
        updateView();
    }

    private void onTick() {
        if (!isShowing() || !running) {
            gameLoop.stop();
            return;
        }
        timeMs -= 100;
        aiTickTimerMs -= 100;

        if (aiTickTimerMs <= 0) {
            simulateAiTurn();
            aiTickTimerMs = AI_CAPTURE_INTERVAL_MS;
        }
        updateHUD();
        updateView();
        checkGameEnd();
    }

    private void simulateAiTurn() {
        List<int[]> targets = new ArrayList<>();
        TileState[][] grid = coreLogic.getGrid();
        for (int r = 0; r < LandGrabLogic.GRID_SIZE; r++) {
            for (int c = 0; c < LandGrabLogic.GRID_SIZE; c++) {
                if (grid[r][c] != TileState.PLAYER_B) {
                    targets.add(new int[]{r, c});
                }
            }
        }
        if (!targets.isEmpty()) {
            int[] t = targets.get(rnd.nextInt(targets.size()));
            String targetWord = coreLogic.getWord(t[0], t[1]);
            LandGrabLogic.SubmitResult result = coreLogic.submitAnswer(targetWord, TileState.PLAYER_B);
            handleResultEffect(result, false);
        }
    }

    // [핵심 수정] 멀티플레이와 완전히 동일한 비주얼 이펙트 호출
    private void handleResultEffect(LandGrabLogic.SubmitResult result, boolean isMe) {
        if (result.resultCode() > 0) {
            int r = result.r();
            int c = result.c();
            ItemType type = result.itemType();

            // 기본 히트 이펙트
            if (isMe) landGrabPanel.flashHit();

            // 아이템 이펙트 (멀티와 문구/색상 통일)
            switch (type) {
                case BUFF_SPLASH:
                    if (isMe) landGrabPanel.showSplashAnimation(r, c);
                    else landGrabPanel.showFloatingText("상대 스플래시!", r, c, "cyan", "blue");
                    break;
                case BUFF_BARRIER:
                    if (isMe) landGrabPanel.showFloatingText("보호막 가동!", r, c, "gold", "orange");
                    else landGrabPanel.showFloatingText("상대 보호막!", r, c, "orange", "red");
                    break;
                case BUFF_COMBO_GUARD:
                    if (isMe) {
                        landGrabPanel.showFloatingText("콤보 가드!", r, c, "lime", "green");
                        activateComboGuardUI();
                    } else {
                        landGrabPanel.showFloatingText("상대 콤보가드!", r, c, "red", "darkred");
                    }
                    break;
                case TRAP_INK:
                    if (isMe) {
                        landGrabPanel.showFloatingText("먹물 발사!", r, c, "#444", "#000");
                    } else {
                        // AI가 나를 공격함 -> 실제로 먹물 뿌리기
                        applyInkToPlayer(2);
                        landGrabPanel.showInkSplashAnimation(r, c);
                    }
                    break;
                case TRAP_CONFUSION:
                    if (isMe) {
                        landGrabPanel.showFloatingText("혼란 공격!", r, c, "purple", "violet");
                    } else {
                        confusionUntilPlayer = System.currentTimeMillis() + 5000;
                        landGrabPanel.showFloatingText("혼란 걸림!", r, c, "red", "darkred");
                    }
                    break;
                case TRAP_EMP:
                    if (isMe) {
                        landGrabPanel.showFloatingText("EMP 발동!", r, c, "blue", "cyan");
                    } else {
                        landGrabPanel.showFloatingText("상대 EMP!", r, c, "red", "orange");
                    }
                    break;
                default:
                    break;
            }
        } else {
            // 오답 시 미스 이펙트
            if (isMe) view.flashMiss();
        }
    }

    private void activateComboGuardUI() {
        view.setComboGuardActive(true);
        PauseTransition delay = new PauseTransition(Duration.seconds(5));
        delay.setOnFinished(e -> view.setComboGuardActive(false));
        delay.play();
    }

    private void applyInkToPlayer(int count) {
        List<int[]> candidates = new ArrayList<>();
        for (int r = 0; r < LandGrabLogic.GRID_SIZE; r++) {
            for (int c = 0; c < LandGrabLogic.GRID_SIZE; c++) {
                if (!coreLogic.getEffects().isTileBlinded(r, c, true)) {
                    candidates.add(new int[]{r, c});
                }
            }
        }
        Collections.shuffle(candidates);
        int applied = 0;
        for (int[] coord : candidates) {
            if (applied >= count) break;
            coreLogic.getEffects().activateBlindTile(coord[0], coord[1], 3000, true);
            applied++;
        }
    }

    private void checkGameEnd() {
        String resultReason = null;
        int scoreA = coreLogic.getScore(TileState.PLAYER_A);
        int scoreB = coreLogic.getScore(TileState.PLAYER_B);

        if (timeMs <= 0) {
            resultReason = "Time Over";
        }
        boolean isFull = (scoreA + scoreB == LandGrabLogic.GRID_SIZE * LandGrabLogic.GRID_SIZE);
        if (!running && isFull) {
            resultReason = "All Captured";
        }

        if (resultReason != null) {
            running = false;
            gameLoop.stop();
            startButton.setDisable(false);
            inputField.setDisable(true);

            boolean isWin = (scoreA > scoreB);
            view.showGameOver(isWin, resultReason, scoreA, scoreB);
        }
    }

    private void updateView() {
        LandGrabViewState currentState = new LandGrabViewState(coreLogic);
        boolean isConfused = (System.currentTimeMillis() < confusionUntilPlayer);
        boolean barrierA = coreLogic.getEffects().isBarrierActive(true);
        boolean barrierB = coreLogic.getEffects().isBarrierActive(false);
        landGrabPanel.setExtraEffects(isConfused, barrierA, barrierB);
        landGrabPanel.updateState(currentState);
    }

    private void updateHUD() {
        if (!isShowing()) return;
        view.setTimeText(String.format("남은 시간: %.1fs", timeMs / 1000.0));
        view.setMyScoreText("나: " + coreLogic.getScore(TileState.PLAYER_A));
        view.setAiScoreText("AI: " + coreLogic.getScore(TileState.PLAYER_B));

        // 콤보 업데이트
        int combo = coreLogic.getCombo(TileState.PLAYER_A);
        view.setComboText(""+combo);

        // 마지막 아이템은 텍스트로만 표시 (중복 연출 방지)
        view.setEffectsText(coreLogic.getEffects().describeEffects(true));
    }
}