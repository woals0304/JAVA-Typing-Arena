package typingarena.server;

import com.google.gson.Gson;
import typingarena.net.Message;
import typingarena.server.core.ServerContext;
import typingarena.server.match.Matchmaker;
import typingarena.server.session.TugOfWarSession;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 서버 부트스트랩 및 상위 조정자.
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

    // ===== Matchmaker.Listener =====
    @Override
    public void onMatchReady(String gameType, ClientHandler a, ClientHandler b) {
        if ("TUG_OF_WAR".equalsIgnoreCase(gameType)) {
            startTugOfWarSession(a, b);
        } else {
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

    // ===== 호출 지점 =====
    public void onClientDisconnected(ClientHandler client) {
        matchmaker.cancelMatch(client, client.getPendingMatchGameType());
        endSessionFor(client, "상대가 연결을 종료했습니다.");
        client.leaveRoom();
    }

    public void onGameAction(ClientHandler client, Message msg) {
        String sessionId = msg.sessionId != null ? msg.sessionId : client.getCurrentSession();
        if (sessionId == null) return;
        TugOfWarSession session = context.getTugSessions().get(sessionId);
        if (session != null) {
            String word = null;
            if (msg.data != null && msg.data.get("word") != null) {
                word = String.valueOf(msg.data.get("word"));
            } else if (msg.text != null) {
                word = msg.text;
            }
            if (word != null) {
                session.handleWord(client, word.trim());
            }
        }
    }

    public void onGameForfeit(ClientHandler client) {
        endSessionFor(client, "상대가 포기했습니다.");
    }

    private void endSessionFor(ClientHandler client, String reason) {
        String sessionId = client.getCurrentSession();
        if (sessionId == null) return;
        TugOfWarSession session = context.getTugSessions().get(sessionId);
        if (session != null) {
            session.forfeit(client, reason);
        }
    }
}
