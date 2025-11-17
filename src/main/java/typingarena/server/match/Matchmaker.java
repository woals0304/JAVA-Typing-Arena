package typingarena.server.match;

import typingarena.net.Message;
import typingarena.server.ClientHandler;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 자동 매칭 시스템.
 */
public class Matchmaker {

    // [신규] 땅따먹기를 지원하는 게임 목록에 추가
    private static final List<String> SUPPORTED_GAMES = List.of("TUG_OF_WAR", "LAND_GRAB");

    private final Listener listener;
    private final Map<String, Queue<ClientHandler>> queues = new ConcurrentHashMap<>();

    public Matchmaker(Listener listener) {
        this.listener = listener;
        SUPPORTED_GAMES.forEach(type -> queues.put(type, new ConcurrentLinkedQueue<>()));
    }

    public synchronized void requestMatch(ClientHandler client, String gameType) {
        if (gameType == null || gameType.isBlank()) {
            client.send(client.createError("MATCH_REQUEST", "게임 타입이 지정되지 않았습니다."));
            return;
        }
        gameType = gameType.toUpperCase(Locale.ROOT);

        // [수정] 이제 이 if문이 "LAND_GRAB"을 통과시킵니다.
        if (!SUPPORTED_GAMES.contains(gameType)) {
            Message err = client.createError("MATCH_REQUEST", "지원하지 않는 게임 타입입니다.");
            client.send(err);
            return;
        }

        if (client.hasPendingMatch()) {
            client.send(client.createError("MATCH_REQUEST", "이미 매칭 중입니다."));
            return;
        }

        Queue<ClientHandler> queue = queues.get(gameType);
        client.notifyMatchWaiting(gameType);
        queue.add(client);
        findMatch(gameType);
    }

    public synchronized void cancelMatch(ClientHandler client, String gameType) {
        if (gameType == null || gameType.isBlank()) return;
        gameType = gameType.toUpperCase(Locale.ROOT);

        if (!SUPPORTED_GAMES.contains(gameType)) return;

        Queue<ClientHandler> queue = queues.get(gameType);
        if (queue.remove(client)) {
            client.notifyMatchCancelled();
        }
    }

    private synchronized void findMatch(String gameType) {
        Queue<ClientHandler> queue = queues.get(gameType);
        while (queue.size() >= 2) {
            ClientHandler a = queue.poll();
            ClientHandler b = queue.poll();

            if (a == null || !a.isConnected()) {
                if (b != null && b.isConnected()) queue.add(b); // b는 큐에 복귀
                continue;
            }
            if (b == null || !b.isConnected()) {
                if (a.isConnected()) queue.add(a); // a는 큐에 복귀
                continue;
            }

            a.clearPendingMatch();
            b.clearPendingMatch();
            Message success = Message.of("MATCH_SUCCESS");
            success.data = Map.of("gameType", gameType, "message", "매칭 성공!");
            a.send(success);
            b.send(success);

            listener.onMatchReady(gameType, a, b);
        }
    }

    public interface Listener {
        void onMatchReady(String gameType, ClientHandler a, ClientHandler b);
    }
}