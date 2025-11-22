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

import typingarena.core.landgrab.LandGrabLogic;
import typingarena.core.landgrab.LandGrabLogic.TileState;
import typingarena.core.landgrab.LandGrabEffects.ItemType;
import typingarena.core.landgrab.LandGrabViewState;

import java.util.ArrayList;
import java.util.Collections; // [수정] 임포트 추가 완료
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
    private int timeMs = 60_000;
    private boolean running = false;

    private int aiTickTimerMs = 0;
    private static final int AI_CAPTURE_INTERVAL_MS = 2_000;
    private long lastItemNotifiedAt = 0L;

    private long confusionUntilPlayer = 0L;

    public LandGrabGame() {
        setTitle("Typing Arena - 땅따먹기 (Single)");
        initModality(Modality.NONE);

        inputField.setDisable(true);
        startButton.setFont(inputField.getFont());
        startButton.setOnAction(e -> startGame());
        view.getControlBox().getChildren().add(startButton);

        Scene scene = new Scene(view.getRoot(), 700, 800);
        setScene(scene);

        inputField.setOnAction(e -> handleSubmit());
        scene.setOnMouseClicked(e -> {
            if (!inputField.isDisabled()) inputField.requestFocus();
        });
        setOnShown(e -> {
            landGrabPanel.activate();
            updateView();
        });
        gameLoop = new Timeline(new KeyFrame(Duration.millis(100), e -> onTick()));
        gameLoop.setCycleCount(Timeline.INDEFINITE);
        setOnCloseRequest(e -> gameLoop.stop());
        setOnHidden(e -> {
            gameLoop.stop();
            landGrabPanel.dispose();
        });

        updateHUD();
        updateView();
    }

    private void startGame() {
        timeMs = 60_000;
        running = true;
        aiTickTimerMs = AI_CAPTURE_INTERVAL_MS;
        lastItemNotifiedAt = 0L;
        confusionUntilPlayer = 0L;

        coreLogic.startGame();

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

        // 나는 무조건 PLAYER_A
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

        // AI 턴
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

    private void handleResultEffect(LandGrabLogic.SubmitResult result, boolean isMe) {
        if (result.resultCode() > 0) {
            // 일반 타격 시 번쩍임 제거됨 (효과음으로 대체 예정)

            int r = result.r();
            int c = result.c();
            ItemType type = result.itemType();

            if (type != ItemType.NONE) {
                if (type == ItemType.BUFF_SPLASH) {
                    landGrabPanel.showSplashAnimation(r, c);
                } else if (type == ItemType.BUFF_BARRIER) {
                    String text = isMe ? "보호막 가동!" : "상대 보호막!";
                    landGrabPanel.showFloatingText(text, r, c, "gold", "orange");
                } else if (type == ItemType.BUFF_COMBO_GUARD) {
                    String text = isMe ? "콤보 가드!" : "상대 콤보가드!";
                    landGrabPanel.showFloatingText(text, r, c, "lime", "green");
                } else if (type == ItemType.TRAP_INK) {
                    if (isMe) {
                        landGrabPanel.showFloatingText("먹물 공격!", r, c, "#444", "#000");
                    } else {
                        applyInkToPlayer(2);
                        landGrabPanel.showFloatingText("먹물 당함!", r, c, "#444", "#000");
                    }
                } else if (type == ItemType.TRAP_CONFUSION) {
                    if (isMe) {
                        landGrabPanel.showFloatingText("혼란 공격!", r, c, "purple", "violet");
                    } else {
                        confusionUntilPlayer = System.currentTimeMillis() + 5000;
                        landGrabPanel.showFloatingText("혼란 걸림!", r, c, "red", "darkred");
                    }
                } else if (type == ItemType.TRAP_EMP) {
                    landGrabPanel.showFloatingText("EMP!", r, c, "cyan", "blue");
                }
            }
        } else {
            // 틀렸을 때 빨간 번쩍임은 유지
            if (isMe) view.flashMiss();
        }
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
        String result = null;
        int scoreA = coreLogic.getScore(TileState.PLAYER_A);
        int scoreB = coreLogic.getScore(TileState.PLAYER_B);

        if (timeMs <= 0) {
            running = false;
            if (scoreA > scoreB) result = "승리! (Time Over)";
            else if (scoreB > scoreA) result = "패배... (Time Over)";
            else result = "무승부!";
        }
        boolean isFull = (scoreA + scoreB == LandGrabLogic.GRID_SIZE * LandGrabLogic.GRID_SIZE);
        if (!running && isFull) {
            running = false;
            if (scoreA > scoreB) result = "승리! (All Captured)";
            else if (scoreB > scoreA) result = "패배... (All Captured)";
            else result = "무승부!";
        }
        if (result != null) {
            gameLoop.stop();
            startButton.setDisable(false);
            inputField.setDisable(true);
            showResultDialog(result, scoreA, scoreB);
        }
    }

    private void updateView() {
        LandGrabViewState currentState = new LandGrabViewState(coreLogic);

        // [수정 완료] 3개의 인자를 모두 전달하도록 수정
        // 1. 혼란 여부 (싱글에서는 AI가 혼란을 걸면 confusionUntilPlayer로 체크)
        boolean isConfused = (System.currentTimeMillis() < confusionUntilPlayer);
        // 2. 내 보호막 (Player A)
        boolean barrierA = coreLogic.getEffects().isBarrierActive(true);
        // 3. AI 보호막 (Player B)
        boolean barrierB = coreLogic.getEffects().isBarrierActive(false);

        landGrabPanel.setExtraEffects(isConfused, barrierA, barrierB);
        landGrabPanel.updateState(currentState);
    }

    private void showResultDialog(String message, int sA, int sB) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("결과");
        alert.setHeaderText("게임 종료");
        alert.setContentText(message + "\n나: " + sA + "칸 / AI: " + sB + "칸");
        alert.initOwner(getOwner() != null ? getOwner() : this);
        alert.show();
    }

    private void updateHUD() {
        if (!isShowing()) return;
        view.setTimeText(String.format("남은 시간: %.1fs", timeMs / 1000.0));
        view.setMyScoreText("나: " + coreLogic.getScore(TileState.PLAYER_A) + "칸");
        view.setAiScoreText("AI: " + coreLogic.getScore(TileState.PLAYER_B) + "칸");
        int combo = coreLogic.getCombo(TileState.PLAYER_A);
        view.setComboText("콤보: " + combo + (combo >= 10 ? " (각성!)" : ""));
        view.setEffectsText(coreLogic.getEffects().describeEffects(true));
        ItemType itemType = coreLogic.getEffects().getLastActivatedItem();
        view.setLastItemText("최근 아이템: " + formatItemLabel(itemType));
        long activatedAt = coreLogic.getEffects().getLastItemActivatedAt();
        if (itemType != ItemType.NONE && activatedAt > lastItemNotifiedAt) {
            lastItemNotifiedAt = activatedAt;
            view.flashItem(colorForItem(itemType));
        }
    }

    private String formatItemLabel(ItemType itemType) {
        return switch (itemType) {
            case BUFF_SPLASH -> "스플래시";
            case BUFF_BARRIER -> "보호막";
            case BUFF_COMBO_GUARD -> "콤보 가드";
            case TRAP_INK -> "먹물";
            case TRAP_EMP -> "EMP";
            case TRAP_CONFUSION -> "혼란";
            default -> "없음";
        };
    }

    private Color colorForItem(ItemType itemType) {
        String name = itemType.name();
        if (name.startsWith("BUFF")) return Color.rgb(80, 160, 255);
        if (name.startsWith("TRAP")) return Color.rgb(255, 80, 80);
        return Color.TRANSPARENT;
    }
}