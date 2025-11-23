package typingarena.server;

import com.google.gson.Gson;
import typingarena.net.Message;
import typingarena.server.core.ServerContext;
import typingarena.server.match.Matchmaker;
import typingarena.server.session.LandGrabSession;
import typingarena.server.session.TugOfWarSession;
import typingarena.server.db.DatabaseManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

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

    @Override
    public void onMatchReady(String gameType, ClientHandler a, ClientHandler b) {
        if ("TUG_OF_WAR".equalsIgnoreCase(gameType)) {
            startTugOfWarSession(a, b);
        } else if ("LAND_GRAB".equalsIgnoreCase(gameType)) {
            startLandGrabSession(a, b);
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

    private void startLandGrabSession(ClientHandler a, ClientHandler b) {
        LandGrabSession session = new LandGrabSession(context, a, b);
        context.getLandGrabSessions().put(session.getId(), session);
        session.start();
    }

    public void onClientDisconnected(ClientHandler client) {
        matchmaker.cancelMatch(client, client.getPendingMatchGameType());
        endSessionFor(client, "상대가 연결을 종료했습니다.");
        client.leaveRoom();
    }

    public void onGameAction(ClientHandler client, Message msg) {
        String sessionId = msg.sessionId != null ? msg.sessionId : client.getCurrentSession();
        if (sessionId == null) return;

        TugOfWarSession tugSession = context.getTugSessions().get(sessionId);
        if (tugSession != null) {
            String word = getWordFromMessage(msg);
            if (word != null) tugSession.handleWord(client, word.trim());
            return;
        }

        LandGrabSession landGrabSession = context.getLandGrabSessions().get(sessionId);
        if (landGrabSession != null) {
            String word = getWordFromMessage(msg);
            if (word != null) landGrabSession.handleWord(client, word.trim());
            return;
        }
    }

    // [핵심 수정] 재경기 요청 처리 메서드 추가!
    public void onGameRematchRequest(ClientHandler client) {
        String sessionId = client.getCurrentSession();
        if (sessionId == null) return;

        // LandGrab 세션만 재경기 지원
        LandGrabSession landGrabSession = context.getLandGrabSessions().get(sessionId);
        if (landGrabSession != null) {
            landGrabSession.handleRematchRequest(client);
        }
    }

    private String getWordFromMessage(Message msg) {
        if (msg.data != null && msg.data.get("word") != null) {
            return String.valueOf(msg.data.get("word"));
        } else if (msg.text != null) {
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

        TugOfWarSession tugSession = context.getTugSessions().get(sessionId);
        if (tugSession != null) {
            tugSession.forfeit(client, reason);
            return;
        }

        LandGrabSession landGrabSession = context.getLandGrabSessions().get(sessionId);
        if (landGrabSession != null) {
            landGrabSession.forfeit(client, reason);
            return;
        }
    }
}