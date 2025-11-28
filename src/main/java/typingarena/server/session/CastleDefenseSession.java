package typingarena.server.session;

import typingarena.net.Message;
import typingarena.server.ClientHandler;
import typingarena.server.core.ServerContext;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class CastleDefenseSession {
    private final String id = UUID.randomUUID().toString();
    private final ClientHandler p1;
    private final ClientHandler p2;
    private final ServerContext context;
    
    // [협동] 공유하는 데이터
    private int teamScore = 0;
    private int teamHp = 6; // 두 명이니 HP를 6으로 넉넉하게 주거나 3으로 유지
    private Timer gameTimer;
    private boolean isFinished = false;

    public CastleDefenseSession(ServerContext context, ClientHandler p1, ClientHandler p2) {
        this.context = context;
        this.p1 = p1;
        this.p2 = p2;
    }

    public String getId() { return id; }

    public void start() {
        p1.setCurrentSession(id);
        p2.setCurrentSession(id);

        Message startMsg = Message.of("GAME_START_BROADCAST");
        startMsg.sessionId = id;
        startMsg.gameType = "CASTLE_DEFENSE";
        
        // 닉네임 정보 등을 함께 보내줌 (협동 파트너 표시용)
        startMsg.data = Map.of(
            "p1", p1.getNickname(),
            "p2", p2.getNickname(),
            "teamHp", teamHp
        );

        p1.send(startMsg);
        p2.send(startMsg);

        // 60초 후 승리 처리
        gameTimer = new Timer();
        gameTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                finishGame(true, "방어 성공!"); // 시간 종료 = 승리
            }
        }, 60000); 
    }

    // 클라이언트 행동 처리 (점수 획득, 데미지 입음 등)
    public void handleAction(ClientHandler sender, Message msg) {
        if (isFinished || msg.data == null) return;

        // 1. 몬스터 처치 (점수 획득)
        if (msg.data.containsKey("scoreAdd")) {
            int add = toInt(msg.data.get("scoreAdd"));
            teamScore += add;
            broadcastUpdate();
        }
        
        // 2. 데미지 입음 (HP 감소)
        if (msg.data.containsKey("damage")) {
            int dmg = toInt(msg.data.get("damage"));
            teamHp -= dmg;
            if (teamHp < 0) teamHp = 0;
            
            broadcastUpdate(); // HP 변경 알림

            if (teamHp <= 0) {
                finishGame(false, "성이 파괴되었습니다."); // HP 0 = 패배
            }
        }
        
        // 3. HP 회복 (하트 아이템)
        if (msg.data.containsKey("heal")) {
             int heal = toInt(msg.data.get("heal"));
             teamHp += heal;
             broadcastUpdate();
        }
    }

    private void broadcastUpdate() {
        Message update = Message.of("GAME_UPDATE_BROADCAST");
        update.sessionId = id;
        // 팀 점수와 HP를 모두에게 똑같이 보냄
        update.data = Map.of(
            "teamScore", teamScore,
            "teamHp", teamHp
        );
        p1.send(update);
        p2.send(update);
    }
    
    public void forfeit(ClientHandler loser, String reason) {
        finishGame(false, loser.getNickname() + "님이 나갔습니다.");
    }

    private synchronized void finishGame(boolean isWin, String message) {
        if (isFinished) return;
        isFinished = true;
        if (gameTimer != null) gameTimer.cancel();
        
        Message endMsg = Message.of("GAME_END_BROADCAST");
        endMsg.sessionId = id;
        
        // 협동이므로 결과(result)도 동일함
        String result = isWin ? "승리" : "패배";
        
        endMsg.data = Map.of(
            "result", result,
            "message", message,
            "finalScore", teamScore
        );
        
        p1.send(endMsg);
        p2.send(endMsg);
        
        // 세션 종료 후 정리 (ServerContext에서 제거 등)
        // context.getCastleSessions().remove(id); // 필요 시 추가
    }

    private int toInt(Object o) {
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return 0; }
    }
}