package typingarena.app;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
import typingarena.core.landgrab.LandGrabViewState;
import typingarena.minigames.landgrab.LandGrabMatchView;
import typingarena.minigames.landgrab.LandGrabPanel;
import typingarena.minigames.landgrab.LandGrabSoundManager;
import typingarena.net.Message;
import typingarena.net.NetClient;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LandGrabOnlineStage extends Stage {

    private final NetClient client;
    private final LandGrabMatchView view = new LandGrabMatchView();
    private final LandGrabPanel landGrabPanel = view.getLandGrabPanel();

    private String sessionId;
    private boolean running = false;
    private String myNickname;
    private String opponentNickname = "상대";

    // [중요] 내가 Player A인지 B인지 식별하는 플래그
    private boolean isPlayerA = true;

    private Timeline displayTimer;
    private double clientTimeMs = 60000.0;

    public LandGrabOnlineStage(NetClient client, String myNickname) {
        this.client = client;
        this.myNickname = myNickname;
        setTitle("온라인 땅따먹기");
        setResizable(false);

        // [1] UI 이벤트 연결
        view.getInputField().setOnAction(e -> submitWord());
        view.getRematchButton().setOnAction(e -> sendRematchRequest());
        view.getQuitButton().setOnAction(e -> {
            sendForfeit();
            close();
        });

        // [2] 자동 종료 시 실행할 동작 (포기 선언 후 창 닫기)
        view.setOnCloseAction(() -> {
            sendForfeit();
            close();
        });

        Scene scene = new Scene(view.getRoot(), 1200, 800);
        setScene(scene);

        // 창 X버튼 눌렀을 때
        setOnCloseRequest(e -> {
            sendForfeit();
            view.hideGameOver();
        });

        // 인게임 타이머 (클라이언트 예측용)
        displayTimer = new Timeline(new KeyFrame(Duration.millis(100), e -> {
            if (running && clientTimeMs > 0) {
                clientTimeMs -= 100;
                if (clientTimeMs < 0) clientTimeMs = 0;
                view.setTimeText(formatTime(clientTimeMs));
            }
        }));
        displayTimer.setCycleCount(Timeline.INDEFINITE);
    }

    public void handleMessage(Message msg) {
        if (msg == null || msg.type == null) return;
        String type = msg.type.toUpperCase(Locale.ROOT);

        Platform.runLater(() -> {
            switch (type) {
                case "GAME_START_BROADCAST" -> handleStart(msg);
                case "GAME_UPDATE_BROADCAST" -> handleUpdate(msg);
                case "GAME_END_BROADCAST" -> handleEnd(msg);
                // [3] 알림 메시지 핸들러 연결
                case "GAME_REMATCH_NOTICE" -> handleRematchNotice(msg);
                case "GAME_OPPONENT_LEFT" -> handleOpponentLeft(msg);
            }
        });
    }

    private void handleRematchNotice(Message msg) {
        view.showRematchNotification();
    }

    private void handleOpponentLeft(Message msg) {
        view.setOpponentLeftState();
    }

    private void handleStart(Message msg) {
        this.sessionId = msg.sessionId;
        running = true;
        clientTimeMs = 60000.0;
        displayTimer.playFromStart();

        // [Sound] 멀티 게임 시작 시 BGM 재생 및 시작 효과음 재생
        LandGrabSoundManager.getInstance().playBgm("bgm_game.wav");
        LandGrabSoundManager.getInstance().play("sfx_start.wav");

        view.hideGameOver();
        view.getInputField().setDisable(false);
        view.getInputField().clear();
        view.getInputField().requestFocus();

        Map<String, Object> data = msg.data;
        if (data != null) {
            List<String> players = (List<String>) data.get("players");
            if (players != null) {
                String p1 = players.size() > 0 ? players.get(0) : "Player1";
                String p2 = players.size() > 1 ? players.get(1) : "Player2";

                if (myNickname.equals(p1)) {
                    this.opponentNickname = p2;
                    this.isPlayerA = true; // 나는 A (파랑)
                    landGrabPanel.setMyIdentity(true);
                } else {
                    this.opponentNickname = p1;
                    this.isPlayerA = false; // 나는 B (빨강 -> 패널에서 파랑으로 변환)
                    landGrabPanel.setMyIdentity(false);
                }
                view.setPlayerNames(myNickname, opponentNickname);
            }
            updateFromData(data);
        }
        if (!isShowing()) show();
    }

    private void handleUpdate(Message msg) {
        if (!running || sessionId == null || !sessionId.equals(msg.sessionId)) return;
        Map<String, Object> data = msg.data;
        if (data == null) return;

        // 서버 시간 동기화 (오차 보정)
        double serverTimeMs = toDouble(data.get("timeMs"));
        if (Math.abs(clientTimeMs - serverTimeMs) > 1000) {
            clientTimeMs = serverTimeMs;
        }
        updateFromData(data);
    }

    private void updateFromData(Map<String, Object> data) {
        int scoreSelf = toInt(data.get("scoreSelf"));
        int scoreOpp = toInt(data.get("scoreOpponent"));
        int comboSelf = toInt(data.get("comboSelf"));

        view.setMyScoreText(""+scoreSelf);
        view.setAiScoreText(""+scoreOpp);
        view.setComboText(""+comboSelf);

        // [핵심] 콤보 가드 상태 실시간 동기화
        boolean myComboGuardActive;
        if (isPlayerA) {
            myComboGuardActive = Boolean.TRUE.equals(data.get("combo_guard_a"));
        } else {
            myComboGuardActive = Boolean.TRUE.equals(data.get("combo_guard_b"));
        }
        view.setComboGuardActive(myComboGuardActive);

        // 뷰 상태 업데이트
        LandGrabViewState state = new LandGrabViewState(data);
        String debuff = (String) data.get("debuff");
        boolean flipWords = "FLIP_WORDS".equals(debuff);
        boolean barrierA = Boolean.TRUE.equals(data.get("barrier_a"));
        boolean barrierB = Boolean.TRUE.equals(data.get("barrier_b"));

        landGrabPanel.setExtraEffects(flipWords, barrierA, barrierB);
        landGrabPanel.updateState(state);

        // 애니메이션 트리거 처리
        Map<String, Object> anim = (Map<String, Object>) data.get("animation_trigger");
        if (anim != null) {
            String type = String.valueOf(anim.get("type"));
            int r = toInt(anim.get("r"));
            int c = toInt(anim.get("c"));

            LandGrabSoundManager sm = LandGrabSoundManager.getInstance();

            // [Sound Update] MISS 메시지 처리
            if (type.contains("MISS")) {
                landGrabPanel.flashMiss();
                sm.play("sfx_miss.wav");
            }
            else {
                // 정답일 때 (HIT 또는 아이템 발동 등)
                // 내 행동인지 확인 (상대방 버프/공격은 제외)
                boolean isMyAction = type.startsWith("ATTACK_") || (type.startsWith("BUFF_") && !type.contains("OPP_")) || type.contains("HIT");

                // 1. [Priority 1] 피버 진입 체크 (아이템 여부 무관하게 10콤보 달성 시 무조건 재생)
                if (isMyAction && comboSelf == 10) {
                    sm.play("sfx_fever_start.wav");
                }

                // 2. [Specifics] 아이템 사운드 처리 (아이템이 있으면 HIT 처리 블록으로 가지 않음)
                if (type.contains("ATTACK_INK")) {
                    landGrabPanel.showFloatingText("먹물 발사!", r, c, "#444", "#000");
                    sm.play("sfx_item_ink.wav");
                }
                else if (type.contains("TRAP_INK")) landGrabPanel.showInkSplashAnimation(r, c);
                else if (type.contains("BUFF_SPLASH")) {
                    landGrabPanel.showSplashAnimation(r, c);
                    sm.play("sfx_item_splash.wav");
                }
                else if (type.contains("OPP_SPLASH")) landGrabPanel.showFloatingText("상대 스플래시!", r, c, "cyan", "blue");
                else if (type.contains("BUFF_BARRIER")) {
                    landGrabPanel.showFloatingText("보호막 가동!", r, c, "gold", "orange");
                    sm.play("sfx_item_barrier.wav");
                }
                else if (type.contains("OPP_BARRIER")) landGrabPanel.showFloatingText("상대 보호막!", r, c, "orange", "red");
                else if (type.contains("BUFF_COMBO_GUARD")) {
                    landGrabPanel.showFloatingText("콤보 가드!", r, c, "lime", "green");
                    sm.play("sfx_item_guard.wav");
                }
                else if (type.contains("OPP_COMBO_GUARD")) landGrabPanel.showFloatingText("상대 콤보가드!", r, c, "red", "darkred");
                else if (type.contains("ATTACK_CONFUSION")) {
                    landGrabPanel.showFloatingText("혼란 공격!", r, c, "purple", "violet");
                    sm.play("sfx_item_confuse.wav");
                }
                else if (type.contains("TRAP_CONFUSION")) landGrabPanel.showFloatingText("혼란 걸림!", r, c, "red", "darkred");
                else if (type.contains("ATTACK_EMP")) {
                    landGrabPanel.showFloatingText("EMP 발동!", r, c, "blue", "cyan");
                    sm.play("sfx_item_emp.wav");
                }
                else if (type.contains("TRAP_EMP")) landGrabPanel.showFloatingText("상대 EMP!", r, c, "red", "orange");

                    // [Sound Update] 3. HIT 처리 (아이템이 없을 때만 여기로 옴)
                else if (type.contains("HIT")) {
                    landGrabPanel.flashHit();

                    // [안전 장치] 좌표가 유효한지 확인
                    if (r >= 0 && r < 10 && c >= 0 && c < 10) {

                        // 각성 상태(10콤보 이상)면 무조건 Steal 재생
                        if (comboSelf >= 10) {
                            sm.play("sfx_steal.wav");
                        } else {
                            // 일반 상태면 타일에 따라 구분
                            var targetTile = state.getTileState(r, c);
                            if (targetTile == typingarena.core.landgrab.LandGrabLogic.TileState.EMPTY) {
                                sm.play("sfx_destroy.wav"); // 파괴
                            } else {
                                sm.play("sfx_hit.wav");     // 점령
                            }
                        }
                    } else {
                        sm.play("sfx_hit.wav");
                    }
                }
            }
        }
    }

    private void handleEnd(Message msg) {
        if (sessionId == null || !sessionId.equals(msg.sessionId)) return;
        running = false;
        displayTimer.stop();
        view.getInputField().setDisable(true);
        Map<String, Object> data = msg.data;
        if (data != null) {
            String result = valueOf(data.get("result"));
            String reason = valueOf(data.get("message"));
            int myScore = toInt(data.get("scoreSelf"));
            int oppScore = toInt(data.get("scoreOpponent"));
            boolean isWin = "승리".equals(result);
            if ("무승부".equals(result)) reason += " (무승부)";
            view.showGameOver(isWin, reason, myScore, oppScore);
        }
    }

    private void submitWord() {
        if (!running || sessionId == null) return;
        String typed = view.getInputField().getText().trim();
        if (typed.isEmpty()) return;
        view.getInputField().clear();
        Message action = Message.of("GAME_ACTION");
        action.sessionId = sessionId;
        action.data = Map.of("word", typed);
        client.send(action);
    }

    private void sendRematchRequest() {
        if (sessionId == null) return;
        Message msg = Message.of("GAME_REMATCH_REQUEST");
        msg.sessionId = sessionId;
        client.send(msg);
        view.setRematchRequestedState();
    }

    private void sendForfeit() {
        if (sessionId == null) return;
        Message msg = Message.of("GAME_FORFEIT");
        msg.sessionId = sessionId;
        client.send(msg);

        // [Sound] 기권 시 BGM 정지
        LandGrabSoundManager.getInstance().stopBgm();

        running = false;
        displayTimer.stop();
    }

    private String valueOf(Object obj) { return obj == null ? "-" : String.valueOf(obj); }
    private String formatTime(double ms) { return String.format("%.1fs", ms / 1000.0); }
    private double toDouble(Object obj) {
        if (obj instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(obj)); } catch (Exception e) { return 0; }
    }
    private int toInt(Object obj) {
        if (obj instanceof Number n) return n.intValue();
        try { return (int) Double.parseDouble(String.valueOf(obj)); } catch (Exception e) { return 0; }
    }
}