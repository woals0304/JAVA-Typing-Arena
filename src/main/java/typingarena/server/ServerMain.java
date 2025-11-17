package typingarena.server;

import com.google.gson.Gson;
import typingarena.net.Message;
import typingarena.server.core.ServerContext;
import typingarena.server.match.Matchmaker;
import typingarena.server.session.LandGrabSession; // [신규] LandGrabSession import
import typingarena.server.session.TugOfWarSession;
import typingarena.server.db.DatabaseManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map; // [신규] Map import
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 서버 부트스트랩 및 상위 조정자.
 * [수정] LandGrabSession을 지원하도록 수정됨.
 */
public class ServerMain implements Matchmaker.Listener {

    private final int port;
    private final Gson gson = new Gson();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final ServerContext context = new ServerContext(gson, scheduler);
    private final Matchmaker matchmaker = new Matchmaker(this);

    public ServerMain(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws Exception {

        DatabaseManager.getInstance();

        int port = 7777;
        System.out.println("[Server] Listening on " + port);
        new ServerMain(port).run();
    }

    public void run() throws IOException {
        try (ServerSocket ss = new ServerSocket(port)) {
            while (true) {
                Socket socket = ss.accept();
                ClientHandler handler = new ClientHandler(this, context, matchmaker, socket);
                handler.start();
            }
        }
    }

    // ===== Matchmaker.Listener (수정) =====
    @Override
    public void onMatchReady(String gameType, ClientHandler a, ClientHandler b) {
        if ("TUG_OF_WAR".equalsIgnoreCase(gameType)) {
            startTugOfWarSession(a, b);
        }
        // [신규] LAND_GRAB 매칭 성공 시
        else if ("LAND_GRAB".equalsIgnoreCase(gameType)) {
            startLandGrabSession(a, b);
        }
        else {
            // (이제 Matchmaker가 거르므로 이 코드는 실행되지 않아야 함)
            Message err = Message.of("MATCH_REQUEST_ERROR");
            err.data = java.util.Map.of("message", "지원하지 않는 게임 타입입니다.");
            a.send(err);
            b.send(err);
        }
    }

    private void startTugOfWarSession(ClientHandler a, ClientHandler b) {
        TugOfWarSession session = new TugOfWarSession(context, a, b);
        context.getTugSessions().put(session.getId(), session);
        session.start();
    }

    /**
     * [신규] LandGrab 세션을 시작합니다.
     */
    private void startLandGrabSession(ClientHandler a, ClientHandler b) {
        LandGrabSession session = new LandGrabSession(context, a, b);
        context.getLandGrabSessions().put(session.getId(), session); // [수정] LandGrab Map에 저장
        session.start();
    }


    // ===== 호출 지점 (수정) =====
    public void onClientDisconnected(ClientHandler client) {
        matchmaker.cancelMatch(client, client.getPendingMatchGameType());
        endSessionFor(client, "상대가 연결을 종료했습니다.");
        client.leaveRoom(); // (Room 기능은 현재 사용되지 않음)
    }

    public void onGameAction(ClientHandler client, Message msg) {
        String sessionId = msg.sessionId != null ? msg.sessionId : client.getCurrentSession();
        if (sessionId == null) return;

        // [수정] TugOfWar 세션 확인
        TugOfWarSession tugSession = context.getTugSessions().get(sessionId);
        if (tugSession != null) {
            String word = getWordFromMessage(msg);
            if (word != null) {
                tugSession.handleWord(client, word.trim());
            }
            return; // 처리 완료
        }

        // [신규] LandGrab 세션 확인
        LandGrabSession landGrabSession = context.getLandGrabSessions().get(sessionId);
        if (landGrabSession != null) {
            String word = getWordFromMessage(msg);
            if (word != null) {
                landGrabSession.handleWord(client, word.trim());
            }
            return; // 처리 완료
        }
    }

    // [신규] 중복 코드 분리 (단어 추출)
    private String getWordFromMessage(Message msg) {
        if (msg.data != null && msg.data.get("word") != null) {
            return String.valueOf(msg.data.get("word"));
        } else if (msg.text != null) { // (혹시 모를 구버전 호환)
            return msg.text;
        }
        return null;
    }

    public void onGameForfeit(ClientHandler client) {
        endSessionFor(client, "상대가 포기했습니다.");
    }

    private void endSessionFor(ClientHandler client, String reason) {
        String sessionId = client.getCurrentSession();
        if (sessionId == null) return;

        // [수정] TugOfWar 세션 확인
        TugOfWarSession tugSession = context.getTugSessions().get(sessionId);
        if (tugSession != null) {
            tugSession.forfeit(client, reason);
            return; // 처리 완료
        }

        // [신규] LandGrab 세션 확인
        LandGrabSession landGrabSession = context.getLandGrabSessions().get(sessionId);
        if (landGrabSession != null) {
            landGrabSession.forfeit(client, reason);
            return; // 처리 완료
        }
    }
}