package typingarena.app;

import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
import typingarena.core.landgrab.LandGrabViewState;
import typingarena.minigames.landgrab.LandGrabMatchView;
import typingarena.minigames.landgrab.LandGrabPanel;
import typingarena.net.Message;
import typingarena.net.NetClient;

import java.util.Locale;
import java.util.Map;
import java.util.List;

public class LandGrabOnlineStage extends Stage {

    private final NetClient client;
    private final LandGrabMatchView view = new LandGrabMatchView();
    private final LandGrabPanel landGrabPanel = view.getLandGrabPanel();

    private String sessionId;
    private boolean running = false;
    private String myNickname;
    private String opponentNickname = "상대";

    public LandGrabOnlineStage(NetClient client, String myNickname) {
        this.client = client;
        this.myNickname = myNickname;
        setTitle("온라인 땅따먹기");

        view.getInputField().setOnAction(e -> submitWord());

        // [수정] 사이드바 때문에 가로폭을 좀 더 넓힘 (720 -> 900)
        Scene scene = new Scene(view.getRoot(), 950, 800);
        setScene(scene);

        setOnCloseRequest(e -> {
            if (running) sendForfeit();
        });
    }

    public void handleMessage(Message msg) {
        if (msg == null || msg.type == null) return;
        String type = msg.type.toUpperCase(Locale.ROOT);
        Platform.runLater(() -> {
            switch (type) {
                case "GAME_START_BROADCAST" -> handleStart(msg);
                case "GAME_UPDATE_BROADCAST" -> handleUpdate(msg);
                case "GAME_END_BROADCAST" -> handleEnd(msg);
                default -> {}
            }
        });
    }

    private void handleStart(Message msg) {
        this.sessionId = msg.sessionId;
        running = true;
        view.getInputField().setDisable(false);
        view.getInputField().clear();
        view.getInputField().requestFocus();

        Map<String, Object> data = msg.data;
        if (data != null) {
            List<String> players = (List<String>) data.get("players");
            if (players != null) {
                // [신규] 닉네임 설정 로직
                String p1 = players.size() > 0 ? players.get(0) : "Player1";
                String p2 = players.size() > 1 ? players.get(1) : "Player2";

                // 내가 누군지 판단하여 UI 설정
                if (myNickname.equals(p1)) {
                    this.opponentNickname = p2;
                } else {
                    this.opponentNickname = p1;
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
        updateFromData(data);
    }

    private void updateFromData(Map<String, Object> data) {
        view.setTimeText(formatTime(toDouble(data.get("timeMs"))));

        int scoreSelf = toInt(data.get("scoreSelf"));
        int scoreOpp = toInt(data.get("scoreOpponent"));
        int comboSelf = toInt(data.get("comboSelf"));

        view.setMyScoreText("나: " + scoreSelf + "칸");
        view.setAiScoreText("상대: " + scoreOpp + "칸");

        // [수정] 콤보 텍스트 업데이트 (게이지바 연동)
        view.setComboText("콤보: " + comboSelf);

        LandGrabViewState state = new LandGrabViewState(data);
        String debuff = (String) data.get("debuff");
        boolean flipWords = "FLIP_WORDS".equals(debuff);
        boolean barrierA = Boolean.TRUE.equals(data.get("barrier_a"));
        boolean barrierB = Boolean.TRUE.equals(data.get("barrier_b"));

        landGrabPanel.setExtraEffects(flipWords, barrierA, barrierB);
        landGrabPanel.updateState(state);

        Map<String, Object> anim = (Map<String, Object>) data.get("animation_trigger");
        if (anim != null) {
            String type = String.valueOf(anim.get("type"));
            int r = toInt(anim.get("r"));
            int c = toInt(anim.get("c"));

            if (type.contains("ATTACK_INK")) landGrabPanel.showFloatingText("먹물 발사!", r, c, "#444", "#000");
            else if (type.contains("TRAP_INK")) landGrabPanel.showInkSplashAnimation(r, c);
            else if (type.contains("BUFF_SPLASH")) landGrabPanel.showSplashAnimation(r, c);
            else if (type.contains("OPP_SPLASH")) landGrabPanel.showFloatingText("상대 스플래시!", r, c, "cyan", "blue");
            else if (type.contains("BUFF_BARRIER")) landGrabPanel.showFloatingText("보호막 가동!", r, c, "gold", "orange");
            else if (type.contains("OPP_BARRIER")) landGrabPanel.showFloatingText("상대 보호막!", r, c, "orange", "red");

            else if (type.contains("BUFF_COMBO_GUARD")) {
                landGrabPanel.showFloatingText("콤보 가드!", r, c, "lime", "green");
                // [신규] 콤보 가드 UI 활성화 (5초간 유지)
                activateComboGuardUI();
            }

            else if (type.contains("OPP_COMBO_GUARD")) landGrabPanel.showFloatingText("상대 콤보가드!", r, c, "red", "darkred");
            else if (type.contains("ATTACK_CONFUSION")) landGrabPanel.showFloatingText("혼란 공격!", r, c, "purple", "violet");
            else if (type.contains("TRAP_CONFUSION")) landGrabPanel.showFloatingText("혼란 걸림!", r, c, "red", "darkred");
            else if (type.contains("ATTACK_EMP")) landGrabPanel.showFloatingText("EMP 발동!", r, c, "blue", "cyan");
            else if (type.contains("TRAP_EMP")) landGrabPanel.showFloatingText("상대 EMP!", r, c, "red", "orange");
            else if (type.contains("HIT")) landGrabPanel.flashHit();
        }
    }

    // [신규] 콤보 가드 시각 효과 타이머
    private void activateComboGuardUI() {
        view.setComboGuardActive(true);
        PauseTransition delay = new PauseTransition(Duration.seconds(5)); // 5초 지속
        delay.setOnFinished(e -> view.setComboGuardActive(false));
        delay.play();
    }

    private void handleEnd(Message msg) {
        if (sessionId == null || !sessionId.equals(msg.sessionId)) return;
        running = false;
        view.getInputField().setDisable(true);
        Map<String, Object> data = msg.data;
        if (data != null) {
            String result = valueOf(data.get("result"));
            String reason = valueOf(data.get("message"));
            view.setEffectsText(result + " - " + reason);
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

    private void sendForfeit() {
        if (!running || sessionId == null) return;
        Message msg = Message.of("GAME_FORFEIT");
        msg.sessionId = sessionId;
        client.send(msg);
        running = false;
        view.getInputField().setDisable(true);
    }

    private String valueOf(Object obj) { return obj == null ? "-" : String.valueOf(obj); }
    private String formatTime(double ms) { return String.format("남은 시간: %.1fs", ms / 1000.0); }
    private double toDouble(Object obj) {
        if (obj instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(obj)); } catch (Exception e) { return 0; }
    }
    private int toInt(Object obj) {
        if (obj instanceof Number n) return n.intValue();
        try { return (int) Double.parseDouble(String.valueOf(obj)); } catch (Exception e) { return 0; }
    }
}