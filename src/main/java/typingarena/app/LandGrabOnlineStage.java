package typingarena.app;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
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

    // [수정] 기권 버튼 필드 제거됨

    private String sessionId;
    private boolean running = false;

    private String myNickname = "PlayerA";
    private String opponentNickname = "PlayerB";

    public LandGrabOnlineStage(NetClient client, String myNickname) {
        this.client = client;
        this.myNickname = myNickname;
        setTitle("온라인 땅따먹기");

        // [수정] 기권 버튼 추가 및 이벤트 리스너 코드 삭제됨

        view.getInputField().setOnAction(e -> submitWord());

        Scene scene = new Scene(view.getRoot(), 720, 800);
        setScene(scene);

        // [유지] 창을 닫으면 자동으로 기권 처리
        setOnCloseRequest(e -> {
            if (running) {
                sendForfeit();
            }
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
                this.opponentNickname = players.stream()
                        .filter(name -> !name.equals(myNickname))
                        .findFirst()
                        .orElse("Opponent");
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
        view.setComboText("콤보: " + comboSelf + (comboSelf >= 10 ? " (각성!)" : ""));

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
            else if (type.contains("BUFF_COMBO_GUARD")) landGrabPanel.showFloatingText("콤보 가드!", r, c, "lime", "green");
            else if (type.contains("OPP_COMBO_GUARD")) landGrabPanel.showFloatingText("상대 콤보가드!", r, c, "red", "darkred");
            else if (type.contains("ATTACK_CONFUSION")) landGrabPanel.showFloatingText("혼란 공격!", r, c, "purple", "violet");
            else if (type.contains("TRAP_CONFUSION")) landGrabPanel.showFloatingText("혼란 걸림!", r, c, "red", "darkred");
            else if (type.contains("ATTACK_EMP")) landGrabPanel.showFloatingText("EMP 발동!", r, c, "blue", "cyan");
            else if (type.contains("TRAP_EMP")) landGrabPanel.showFloatingText("상대 EMP!", r, c, "red", "orange");
            else if (type.contains("HIT")) landGrabPanel.flashHit();
        }
    }

    private void handleEnd(Message msg) {
        if (sessionId == null || !sessionId.equals(msg.sessionId)) return;
        running = false;
        view.getInputField().setDisable(true);

        Map<String, Object> data = msg.data;
        if (data != null) {
            String result = valueOf(data.get("result"));
            String reason = valueOf(data.get("message"));
            view.setEffectsText(result + " - " + reason); // 상태 메시지창에 결과 표시
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