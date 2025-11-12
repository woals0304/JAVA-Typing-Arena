package typingarena.server.match;

import typingarena.server.ClientHandler;

import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 게임 타입별 매칭 큐를 관리하는 클래스.
 * 매칭이 성사되면 Listener에게 알린다.
 */
public class Matchmaker {

    public interface Listener {
        void onMatchReady(String gameType, ClientHandler playerA, ClientHandler playerB);
    }

    private final Map<String, Deque<ClientHandler>> queues = new ConcurrentHashMap<>();
    private final Listener listener;

    public Matchmaker(Listener listener) {
        this.listener = listener;
    }

    public void requestMatch(ClientHandler client, String gameType) {
        String normalized = normalize(gameType);
        if (normalized.isEmpty()) {
            client.sendError("MATCH_REQUEST", "gameType is required");
            return;
        }

        Deque<ClientHandler> queue = queues.computeIfAbsent(normalized, k -> new ConcurrentLinkedDeque<>());
        synchronized (queue) {
            ClientHandler opponent = null;
            while (!queue.isEmpty() && opponent == null) {
                ClientHandler candidate = queue.poll();
                if (candidate != null && candidate.isConnected()) {
                    opponent = candidate;
                }
            }
            if (opponent == null) {
                queue.offer(client);
                client.notifyMatchWaiting(normalized);
            } else {
                listener.onMatchReady(normalized, opponent, client);
            }
        }
    }

    public void cancelMatch(ClientHandler client, String gameType) {
        if (gameType == null) return;
        String normalized = normalize(gameType);
        if (normalized.isEmpty()) return;
        Deque<ClientHandler> queue = queues.get(normalized);
        if (queue != null) {
            queue.remove(client);
        }
        client.notifyMatchCancelled();
    }

    private String normalize(String type) {
        return type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
    }
}
