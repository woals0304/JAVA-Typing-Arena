package typingarena.app;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import typingarena.core.landgrab.LandGrabViewState; // [수정]
import typingarena.minigames.landgrab.LandGrabMatchView; // [수정]
import typingarena.minigames.landgrab.LandGrabPanel; // [수정]
import typingarena.net.Message;
import typingarena.net.NetClient;

import java.util.Locale;
import java.util.Map;
import java.util.List; // [신규]

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

    private String myNickname = "PlayerA"; // [신규] 서버가 알려준 내 닉네임
    private String opponentNickname = "PlayerB"; // [신규] 서버가 알려준 상대 닉네임

    /**
     * [수정] MultiLobbyStage가 닉네임을 주입해줍니다.
     */
    public LandGrabOnlineStage(NetClient client, String myNickname) {
        this.client = client;
        this.myNickname = myNickname;
        setTitle("온라인 땅따먹기");

        // UI 설정
        view.getControlBox().getChildren().add(surrenderBtn);
        surrenderBtn.setDisable(true);
        surrenderBtn.setOnAction(e -> sendForfeit());

        view.getInputField().setOnAction(e -> submitWord());

        // [수정] 땅따먹기 창 크기에 맞게 조절
        Scene scene = new Scene(view.getRoot(), 720, 800);
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

    /**
     * [수정] 서버가 보낸 '시작' 메시지 처리
     */
    private void handleStart(Message msg) {
        this.sessionId = msg.sessionId;
        running = true;
        view.getInputField().setDisable(false);
        surrenderBtn.setDisable(false);
        view.getInputField().clear();
        view.getInputField().requestFocus();

        Map<String, Object> data = msg.data;
        if (data != null) {
            // [신규] 상대방 닉네임 찾기
            List<String> players = (List<String>) data.get("players");
            if (players != null) {
                this.opponentNickname = players.stream()
                        .filter(name -> !name.equals(myNickname))
                        .findFirst()
                        .orElse("Opponent");
            }

            // [수정] 서버가 보낸 '시작' 데이터로 HUD 초기화
            view.setTimeText(formatTime(toDouble(data.get("timeMs"))));
            view.setMyScoreText(myNickname + ": 0칸");
            view.setAiScoreText(opponentNickname + ": 0칸"); // [수정] "AI" -> "상대"
            view.setComboText("매칭 모드");
            view.setEffectsText("효과: 없음");
            view.setLastItemText("최근 아이템: 없음");

            // [수정] 서버가 보낸 '시작' 데이터(Map)로 ViewState 생성
            LandGrabViewState state = new LandGrabViewState(data);
            updatePanelState(state);
        }
        if (!isShowing()) show();
    }

    /**
     * [수정] 서버가 보낸 '갱신' 메시지 처리 (TODO 완료)
     */
    private void handleUpdate(Message msg) {
        if (!running || sessionId == null || !sessionId.equals(msg.sessionId)) return;
        Map<String, Object> data = msg.data;
        if (data == null) return;

        // 1. HUD 갱신
        view.setTimeText(formatTime(toDouble(data.get("timeMs"))));

        Map<String, Object> scores = (Map<String, Object>) data.get("scores");
        if (scores != null) {
            view.setMyScoreText(myNickname + ": " + toInt(scores.get(myNickname)) + "칸");
            view.setAiScoreText(opponentNickname + ": " + toInt(scores.get(opponentNickname)) + "칸");
        }

        // (protocol.md에 effects, lastItem 필드 추가 시 여기에 갱신 코드 추가)
        // view.setEffectsText(...);
        // view.setLastItemText(...);

        // 2. Panel 갱신 (핵심)
        // [수정] 서버가 보낸 Map 데이터로 'ViewState'를 새로 생성
        LandGrabViewState state = new LandGrabViewState(data);
        updatePanelState(state); // '바보' Panel에 데이터 주입

        // 3. 애니메이션 갱신
        Map<String, Object> anim = (Map<String, Object>) data.get("animation_trigger");
        if (anim != null) {
            String type = String.valueOf(anim.get("type"));
            int r = toInt(anim.get("r"));
            int c = toInt(anim.get("c"));
            if ("SPLASH".equals(type)) landGrabPanel.showSplashAnimation(r, c);
            if ("INK_SPLASH".equals(type)) landGrabPanel.showInkSplashAnimation(r, c);
        }
    }

    /**
     * [수정] 서버가 보낸 '종료' 메시지 처리
     */
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

    /**
     * [수정] 단어 입력 시 서버로 'GAME_ACTION' 전송
     */
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
            // [수정] Double로 먼저 받고 int로 변환 (JSON은 숫자를 double로 파싱)
            return (int) Double.parseDouble(String.valueOf(obj));
        } catch (Exception e) {
            return 0;
        }
    }
}