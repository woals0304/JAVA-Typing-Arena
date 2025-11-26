package typingarena.minigames.landgrab;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import typingarena.core.landgrab.LandGrabLogic;
import typingarena.core.landgrab.LandGrabLogic.TileState;
import typingarena.core.landgrab.LandGrabEffects.ItemType;
import typingarena.core.landgrab.LandGrabViewState;

import java.io.InputStream;
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

    private final StackPane startOverlay = new StackPane();
    private final Button startButton = new Button("게임 시작");

    private final Timeline gameLoop;
    private double timeMs = 60000.0;
    private boolean running = false;

    private int aiTickTimerMs = 0;
    private static final int AI_CAPTURE_INTERVAL_MS = 2000;

    private long confusionUntilPlayer = 0L;

    public LandGrabGame() {
        setTitle("싱글 땅따먹기 ");
        initModality(Modality.NONE);

        setResizable(false);

        // [Sound] 사운드 매니저 로딩 및 배경음악 준비
        LandGrabSoundManager sm = LandGrabSoundManager.getInstance();
        sm.loadSound("sfx_start.wav");
        sm.loadSound("sfx_hit.wav");
        sm.loadSound("sfx_miss.wav");
        sm.loadSound("sfx_destroy.wav"); // [Sound] 파괴음 추가 로딩
        sm.loadSound("sfx_steal.wav");   // [Sound] 뺏기음 추가 로딩
        sm.loadSound("sfx_fever_start.wav"); // [Sound] 피버 진입음 추가 로딩
        sm.loadSound("sfx_item_splash.wav");
        sm.loadSound("sfx_item_barrier.wav");
        sm.loadSound("sfx_item_guard.wav");
        sm.loadSound("sfx_item_ink.wav");
        sm.loadSound("sfx_item_confuse.wav");
        sm.loadSound("sfx_item_emp.wav");
        // [Sound] 배경음악 시작
        sm.playBgm("bgm_game.wav");

        inputField.setDisable(true);

        styleStartOverlay();
        view.getRoot().getChildren().add(startOverlay);

        view.setOnCloseAction(this::close);

        startButton.setOnAction(e -> startGame());
        view.getRematchButton().setOnAction(e -> startGame());
        view.getQuitButton().setOnAction(e -> close());

        Scene scene = new Scene(view.getRoot(), 1200, 800);
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
            // [Sound] 창 닫을 때 음악 정지
            LandGrabSoundManager.getInstance().stopBgm();
        });

        view.setPlayerNames("나", "컴퓨터");
        updateHUD();
        updateView();
    }

    private void styleStartOverlay() {
        startOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85);");

        Label title = new Label("싱글 플레이");
        try {
            InputStream is = getClass().getResourceAsStream("/fonts/CookieRun Regular.otf");
            if (is != null) { title.setFont(Font.loadFont(is, 60)); }
            else { title.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 60)); }
        } catch (Exception e) { title.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 60)); }

        title.setTextFill(Color.WHITE);
        title.setEffect(new Glow(0.8));

        startButton.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 24));
        startButton.setStyle("-fx-background-color: #29B6F6; -fx-text-fill: white; -fx-background-radius: 30; -fx-padding: 15 50; -fx-cursor: hand; -fx-border-color: #5D4037; -fx-border-width: 3px; -fx-border-radius: 30;");
        startButton.setEffect(new DropShadow(10, Color.web("#0288D1")));
        startButton.setOnMouseEntered(e -> startButton.setScaleX(1.1));
        startButton.setOnMouseExited(e -> startButton.setScaleX(1.0));

        VBox box = new VBox(30, title, startButton);
        box.setAlignment(Pos.CENTER);
        startOverlay.getChildren().add(box);
    }

    private void startGame() {
        timeMs = 60000.0; running = true; aiTickTimerMs = AI_CAPTURE_INTERVAL_MS; confusionUntilPlayer = 0L;
        coreLogic.startGame(); view.hideGameOver(); startOverlay.setVisible(false); inputField.setDisable(false); inputField.clear(); inputField.requestFocus();

        // 게임 시작 시 1P(나)임을 패널에 알림 (파란색 표시)
        landGrabPanel.setMyIdentity(true);

        // [Sound Update] 게임 시작 효과음 재생
        LandGrabSoundManager.getInstance().play("sfx_start.wav");

        updateHUD(); updateView(); gameLoop.playFromStart();
    }

    private void handleSubmit() {
        if (!running) { inputField.clear(); return; }
        String typed = inputField.getText().trim();
        LandGrabLogic.SubmitResult result = coreLogic.submitAnswer(typed, TileState.PLAYER_A);
        handleResultEffect(result, true);
        inputField.clear(); inputField.requestFocus(); updateHUD(); updateView();
    }

    private void onTick() {
        if (!isShowing() || !running) { gameLoop.stop(); return; }
        timeMs -= 100; aiTickTimerMs -= 100;
        if (aiTickTimerMs <= 0) { simulateAiTurn(); aiTickTimerMs = AI_CAPTURE_INTERVAL_MS; }

        // [핵심 수정] 매 틱마다 콤보 가드 상태 확인 (싱글 플레이용)
        boolean isGuardActive = coreLogic.getEffects().isComboGuardActive(true); // Player A 기준
        view.setComboGuardActive(isGuardActive);

        updateHUD(); updateView(); checkGameEnd();
    }

    private void simulateAiTurn() {
        List<int[]> targets = new ArrayList<>(); TileState[][] grid = coreLogic.getGrid();
        for (int r = 0; r < LandGrabLogic.GRID_SIZE; r++) { for (int c = 0; c < LandGrabLogic.GRID_SIZE; c++) { if (grid[r][c] != TileState.PLAYER_B) targets.add(new int[]{r, c}); } }
        if (!targets.isEmpty()) { int[] t = targets.get(rnd.nextInt(targets.size())); String targetWord = coreLogic.getWord(t[0], t[1]); LandGrabLogic.SubmitResult result = coreLogic.submitAnswer(targetWord, TileState.PLAYER_B); handleResultEffect(result, false); }
    }

    private void handleResultEffect(LandGrabLogic.SubmitResult result, boolean isMe) {
        LandGrabSoundManager sm = LandGrabSoundManager.getInstance();

        if (result.resultCode() > 0) {
            int r = result.r(); int c = result.c(); ItemType type = result.itemType();

            // === [Sound Update] 상황별 사운드 분기 적용 ===
            if (isMe) {
                landGrabPanel.flashHit();

                // [중요] 아이템이 없을 때만 기본 사운드 재생 (아이템 획득 시에는 중복 재생 방지)
                if (type == ItemType.NONE) {
                    // 1. 현재 로직 상의 타일 상태와 콤보 확인
                    TileState currentTile = coreLogic.getTileState(r, c);
                    int currentCombo = coreLogic.getCombo(TileState.PLAYER_A);

                    // 2. 상황별 사운드 재생
                    // [Case A & B] 각성 상태 (10콤보 이상) -> 무조건 강력한 뺏기 사운드
                    if (currentCombo == 10) {
                        sm.play("sfx_fever_start.wav");
                        sm.play("sfx_steal.wav");
                    } else if (currentCombo > 10) {
                        sm.play("sfx_steal.wav");
                    }
                    // [Case C] 일반 상태 -> 타일 종류에 따라 구분
                    else {
                        if (currentTile == TileState.EMPTY) {
                            // 빈 땅이 됨 -> 상대 땅 파괴(중립화)
                            sm.play("sfx_destroy.wav");
                        } else {
                            // 내 땅이 됨 -> 일반 점령
                            sm.play("sfx_hit.wav");
                        }
                    }
                }
                // 아이템이 있는 경우(type != NONE)는 아래 switch문에서 해당 아이템 소리만 재생됨
            }
            // ====================================================

            switch (type) {
                case BUFF_SPLASH -> {
                    if (isMe) {
                        landGrabPanel.showSplashAnimation(r, c);
                        sm.play("sfx_item_splash.wav"); // [Sound] 스플래시
                    } else landGrabPanel.showFloatingText("상대 스플래시!", r, c, "cyan", "blue");
                }
                case BUFF_BARRIER -> {
                    if (isMe) {
                        landGrabPanel.showFloatingText("보호막 가동!", r, c, "gold", "orange");
                        sm.play("sfx_item_barrier.wav"); // [Sound] 보호막
                    } else landGrabPanel.showFloatingText("상대 보호막!", r, c, "orange", "red");
                }
                case BUFF_COMBO_GUARD -> {
                    if (isMe) {
                        landGrabPanel.showFloatingText("콤보 가드!", r, c, "lime", "green");
                        sm.play("sfx_item_guard.wav"); // [Sound] 콤보가드
                        // setComboGuardActive는 onTick에서 자동 처리됨
                    } else {
                        landGrabPanel.showFloatingText("상대 콤보가드!", r, c, "red", "darkred");
                    }
                }
                case TRAP_INK -> {
                    if (isMe) {
                        landGrabPanel.showFloatingText("먹물 발사!", r, c, "#444", "#000");
                        sm.play("sfx_item_ink.wav"); // [Sound] 먹물 발사 (내가 쏨)
                    } else { applyInkToPlayer(2); landGrabPanel.showInkSplashAnimation(r, c); }
                }
                case TRAP_CONFUSION -> {
                    if (isMe) {
                        landGrabPanel.showFloatingText("혼란 공격!", r, c, "purple", "violet");
                        sm.play("sfx_item_confuse.wav"); // [Sound] 혼란 공격 (내가 걺)
                    } else { confusionUntilPlayer = System.currentTimeMillis() + 5000; landGrabPanel.showFloatingText("혼란 걸림!", r, c, "red", "darkred"); }
                }
                case TRAP_EMP -> {
                    if (isMe) {
                        landGrabPanel.showFloatingText("EMP 발동!", r, c, "blue", "cyan");
                        sm.play("sfx_item_emp.wav"); // [Sound] EMP 발동
                    } else { landGrabPanel.showFloatingText("상대 EMP!", r, c, "red", "orange"); }
                }
                default -> {}
            }
        } else {
            if (isMe) {
                view.flashMiss();
                sm.play("sfx_miss.wav"); // [Sound] 오타 소리
            }
        }
    }

    private void applyInkToPlayer(int count) { List<int[]> candidates = new ArrayList<>(); for (int r = 0; r < LandGrabLogic.GRID_SIZE; r++) { for (int c = 0; c < LandGrabLogic.GRID_SIZE; c++) { if (!coreLogic.getEffects().isTileBlinded(r, c, true)) candidates.add(new int[]{r, c}); } } Collections.shuffle(candidates); int applied = 0; for (int[] coord : candidates) { if (applied >= count) break; coreLogic.getEffects().activateBlindTile(coord[0], coord[1], 3000, true); applied++; } }

    private void checkGameEnd() {
        String resultReason = null; int scoreA = coreLogic.getScore(TileState.PLAYER_A); int scoreB = coreLogic.getScore(TileState.PLAYER_B);
        if (timeMs <= 0) resultReason = "Time Over"; boolean isFull = (scoreA + scoreB == LandGrabLogic.GRID_SIZE * LandGrabLogic.GRID_SIZE); if (!running && isFull) resultReason = "All Captured";
        if (resultReason != null) { running = false; gameLoop.stop(); inputField.setDisable(true); boolean isWin = (scoreA > scoreB); view.showGameOver(isWin, resultReason, scoreA, scoreB); }
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
        view.setTimeText(String.format("%.1fs", timeMs / 1000.0));
        view.setMyScoreText("" + coreLogic.getScore(TileState.PLAYER_A));
        view.setAiScoreText("" + coreLogic.getScore(TileState.PLAYER_B));
        view.setComboText("" + coreLogic.getCombo(TileState.PLAYER_A));
    }
}