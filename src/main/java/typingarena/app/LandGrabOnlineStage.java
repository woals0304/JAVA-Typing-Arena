package typingarena.app;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import typingarena.core.landgrab.LandGrabLogic;
import typingarena.core.landgrab.LandGrabViewState;
import typingarena.minigames.landgrab.LandGrabMatchView;
import typingarena.minigames.landgrab.LandGrabPanel;
import typingarena.net.Message;
import typingarena.net.NetClient;

import java.util.Locale;
import java.util.Map;

/**
 * [신규] 서버와 통신해 온라인 땅따먹기 경기를 표시하는 Stage.
 * (TugOfWarOnlineStage와 동일한 역할)
 */
public class LandGrabOnlineStage extends Stage {

    private final NetClient client;
    private final LandGrabMatchView view = new LandGrabMatchView(); // [수정] LandGrab 뷰 사용
    private final LandGrabPanel landGrabPanel = view.getLandGrabPanel();
    private final Button surrenderBtn = new Button("기권");

    private String sessionId; // 서버가 알려주는 현재 게임 세션 ID
    private boolean running = false;

    public LandGrabOnlineStage(NetClient client) {
        this.client = client;
        setTitle("온라인 땅따먹기");

        // UI 설정
        view.getControlBox().getChildren().add(surrenderBtn);
        surrenderBtn.setDisable(true);
        surrenderBtn.setOnAction(e -> sendForfeit());

        view.getInputField().setOnAction(e -> submitWord());

        Scene scene = new Scene(view.getRoot(), 720, 800); // 땅따먹기 크기에 맞게 조절
        setScene(scene);

        setOnCloseRequest(e -> {
            if (running) {
                sendForfeit();
            }
        });
    }

    /**
     * MultiLobbyStage가 메시지를 받으면 이쪽으로 전달해줍니다.
     */
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
        this.sessionId = msg.sessionId; // (sessionId가 있다고 가정)
        running = true;
        view.getInputField().setDisable(false);
        surrenderBtn.setDisable(false);
        view.getInputField().clear();
        view.getInputField().requestFocus();

        Map<String, Object> data = msg.data;
        if (data != null) {
            // TODO: 서버가 보낸 '시작' 데이터로 HUD 초기화
            view.setTimeText(formatTime(toDouble(data.get("timeMs"))));
            view.setMyScoreText("나: 0칸");
            view.setAiScoreText("상대: 0칸"); // [수정] "AI" -> "상대"
            view.setComboText("매칭 모드");
            view.setEffectsText("효과: 없음");
            view.setLastItemText("최근 아이템: 없음");

            // TODO: 서버가 보낸 '시작' 데이터로 ViewState 생성
            // 지금은 임시로 빈 ViewState 사용
            LandGrabViewState state = new LandGrabViewState();
            updatePanelState(state);
        }
        if (!isShowing()) show();
    }

    private void handleUpdate(Message msg) {
        if (!running || sessionId == null || !sessionId.equals(msg.sessionId)) return;
        Map<String, Object> data = msg.data;
        if (data == null) return;

        // TODO: 서버가 보낸 '업데이트' 데이터로 HUD 갱신
        view.setTimeText(formatTime(toDouble(data.get("timeMs"))));

        // (protocol.md에 정의한 'scores' Map을 사용한다고 가정)
        // view.setMyScoreText("나: " + ...);
        // view.setAiScoreText("상대: " + ...);
        // view.setComboText("...");
        // view.setEffectsText(...);
        // view.setLastItemText(...);

        // TODO: 서버가 보낸 '업데이트' 데이터로 ViewState 생성
        // (protocol.md에 정의한 'tiles_changed' 등을 사용)
        LandGrabViewState state = new LandGrabViewState(data); // (ViewState에 Map 생성자 구현 필요)
        updatePanelState(state);

        // TODO: 서버가 보낸 'animation_trigger'로 애니메이션 호출
        // Map<String, Object> anim = (Map<String, Object>) data.get("animation_trigger");
        // if (anim != null) {
        //    String type = String.valueOf(anim.get("type"));
        //    int r = toInt(anim.get("r"));
        //    int c = toInt(anim.get("c"));
        //    if ("SPLASH".equals(type)) landGrabPanel.showSplashAnimation(r, c);
        //    if ("INK_SPLASH".equals(type)) landGrabPanel.showInkSplashAnimation(r, c);
        // }
    }

    private void handleEnd(Message msg) {
        if (sessionId == null || !sessionId.equals(msg.sessionId)) return;
        running = false;
        view.getInputField().setDisable(true);
        surrenderBtn.setDisable(true);
        Map<String, Object> data = msg.data;
        if (data != null) {
            view.setEffectsText(valueOf(data.get("result")));
            view.setLastItemText(valueOf(data.get("message")));
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
        surrenderBtn.setDisable(true);
    }

    /**
     * '바보' View(Panel)에 ViewState를 주입합니다.
     */
    private void updatePanelState(LandGrabViewState state) {
        landGrabPanel.updateState(state);
    }

    // --- 헬퍼 메서드 (TugOfWarOnlineStage에서 복사) ---

    private String valueOf(Object obj) {
        return obj == null ? "-" : String.valueOf(obj);
    }

    private String formatTime(double ms) {
        return String.format("남은 시간: %.1fs", ms / 1000.0);
    }

    private double toDouble(Object obj) {
        if (obj instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(obj));
        } catch (Exception e) {
            return 0;
        }
    }

    private int toInt(Object obj) {
        if (obj instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(obj));
        } catch (Exception e) {
            return 0;
        }
    }

    // (parseModifier는 LandGrab에 맞게 수정 필요 - 우선 제거)
}