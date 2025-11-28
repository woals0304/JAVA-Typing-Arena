package typingarena.minigames.castledefense;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import typingarena.net.Message;
import typingarena.net.NetClient;
import java.util.Map;

public class CastleDefenseOnlineStage extends CastleDefenseGame {

    private final NetClient client;
    private String sessionId;

    public CastleDefenseOnlineStage(NetClient client) {
        // 부모 생성자 호출 (isMultiplayer = true)
        super(true);
        this.client = client;
        
        // [중요] 싱글용 'START' 버튼이 있는 오버레이를 숨기고 대기 상태 메시지 표시
        this.gameStartOverlay.setVisible(true);
        this.gameStartOverlay.getChildren().clear(); // 기존 버튼 제거
        javafx.scene.control.Label waiting = new javafx.scene.control.Label("매칭 성공! 서버 신호 대기 중...");
        waiting.setStyle("-fx-font-size: 20px; -fx-text-fill: white;");
        this.gameStartOverlay.getChildren().add(waiting);

        // 창 닫을 때 기권 처리
        this.setOnCloseRequest(e -> {
            sendForfeit();
            stopGame();
        });
    }

    // 서버 메시지 핸들러
    public void handleMessage(Message msg) {
        if (msg == null || msg.type == null) return;
        
        Platform.runLater(() -> {
            switch (msg.type) {
                case "GAME_START_BROADCAST" -> handleGameStart(msg);
                case "GAME_UPDATE_BROADCAST" -> handleGameUpdate(msg);
                case "GAME_END_BROADCAST" -> handleGameEnd(msg);
                // 필요 시 상대방 공격 이펙트 처리 등을 여기에 추가
            }
        });
    }

    private void handleGameStart(Message msg) {
        this.sessionId = msg.sessionId;
        this.gameStartOverlay.setVisible(false); // 대기 화면 끄기
        
        // 부모의 startGame()을 호출하되, 로컬 타이머/스폰 로직은 
        // 서버 UPDATE에 의존하거나 여기서 재정의해야 함.
        // 일단 편의상 부모 로직을 그대로 쓰되, 몬스터 스폰은 서버와 싱크를 맞추는 것이 정석입니다.
        // 여기서는 "각자 플레이하고 점수만 경쟁"하는 방식으로 구현합니다.
        super.startGame(); 
    }

    private void handleGameUpdate(Message msg) {
    if (!isRunning || msg.data == null) return;

    // [협동] 서버에서 온 팀 점수와 HP로 UI 업데이트
    if (msg.data.containsKey("teamScore")) {
        int tScore = ((Number) msg.data.get("teamScore")).intValue();
        // 부모 클래스의 점수 UI 갱신 (직접 접근 protected 필요)
        this.txtScore.setText(String.valueOf(tScore));
    }
    
    if (msg.data.containsKey("teamHp")) {
        int tHp = ((Number) msg.data.get("teamHp")).intValue();
        // 부모 클래스의 HP UI 갱신
        this.castleHp.set(tHp);
    }
}
    private void handleGameEnd(Message msg) {
        stopGame();
        boolean isWin = "승리".equals(msg.data.get("result"));
        showGameOver(isWin);
    }

    // [중요] 몬스터 처치 시 서버로 점수 전송
    @Override
    protected void killMonster(Monster m) {
        super.killMonster(m); // 로컬 처리(이펙트 등)
    
    if (sessionId != null) {
        Message action = new Message();
        action.type = "GAME_ACTION";
        action.sessionId = sessionId;
        // "나 10점 벌었어"라고 보냄 -> 서버가 더해줌
        action.data = Map.of("scoreAdd", 10); 
        client.send(action);
    }
}

    // 기권 처리
    private void sendForfeit() {
        if (sessionId != null) {
            Message msg = new Message();
            msg.type = "GAME_FORFEIT";
            msg.sessionId = sessionId;
            client.send(msg);
        }
    }
}