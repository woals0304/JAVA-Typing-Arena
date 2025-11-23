package typingarena.server.match;

import typingarena.net.Message;
import typingarena.server.ClientHandler;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Matchmaker {

    private static final List<String> SUPPORTED_GAMES = List.of("TUG_OF_WAR", "LAND_GRAB");

    private final Listener listener;
    private final Map<String, Queue<ClientHandler>> queues = new ConcurrentHashMap<>();

    public Matchmaker(Listener listener) {
        this.listener = listener;
        SUPPORTED_GAMES.forEach(type -> queues.put(type, new ConcurrentLinkedQueue<>()));
    }

    public synchronized void requestMatch(ClientHandler client, String gameType) {
        if (gameType == null || gameType.isBlank()) {
            client.sendError("MATCH_REQUEST", "게임 타입이 지정되지 않았습니다.");
            return;
        }
        gameType = gameType.toUpperCase(Locale.ROOT);

        if (!SUPPORTED_GAMES.contains(gameType)) {
            client.sendError("MATCH_REQUEST", "지원하지 않는 게임 타입입니다.");
            return;
        }

        if (client.hasPendingMatch()) {
            client.sendError("MATCH_REQUEST", "이미 매칭 중입니다.");
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
                if (b != null && b.isConnected()) queue.add(b);
                continue;
            }
            if (b == null || !b.isConnected()) {
                if (a.isConnected()) queue.add(a);
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