package typingarena.server.match;

import typingarena.server.ClientHandler;
import typingarena.net.Message; // Import 유지

import java.util.LinkedList;
import java.util.List;

public class Matchmaker {

    public interface Listener {
        void onMatchReady(String gameType, ClientHandler p1, ClientHandler p2);
    }

    private final Listener listener;
    private final List<ClientHandler> tugQueue = new LinkedList<>();
    private final List<ClientHandler> landQueue = new LinkedList<>();
    private final List<ClientHandler> castleQueue = new LinkedList<>();

    public Matchmaker(Listener listener) {
        this.listener = listener;
    }

    public synchronized void requestMatch(ClientHandler client, String gameType) {
        System.out.println("[Matchmaker] 요청 수신: " + gameType + " / 유저: " + client.getNickname());

        if ("TUG_OF_WAR".equalsIgnoreCase(gameType)) {
            addToQueue(tugQueue, client, "TUG_OF_WAR");
        }
        else if ("LAND_GRAB".equalsIgnoreCase(gameType)) {
            addToQueue(landQueue, client, "LAND_GRAB");
        }
        else if ("CASTLE_DEFENSE".equalsIgnoreCase(gameType)) {
            addToQueue(castleQueue, client, "CASTLE_DEFENSE");
        }
        else {
            client.sendError("MATCH_REQUEST", "지원하지 않는 게임 타입입니다: " + gameType);
        }
    }

    private void addToQueue(List<ClientHandler> queue, ClientHandler client, String gameType) {
        if (queue.contains(client)) return;

        queue.add(client);
        client.notifyMatchWaiting(gameType);
        System.out.println("[Matchmaker] 대기열 진입 (" + gameType + "): 현재 " + queue.size() + "명");

        if (queue.size() >= 2) {
            ClientHandler p1 = queue.remove(0);
            ClientHandler p2 = queue.remove(0);
            p1.clearPendingMatch();
            p2.clearPendingMatch();

            // [Fix] 딜레이 없이 즉시 게임 시작 (로비 멈춤 현상 해결)
            System.out.println("[Matchmaker] 매칭 성사! -> 즉시 ServerMain 호출");
            listener.onMatchReady(gameType, p1, p2);
        }
    }

    public synchronized void cancelMatch(ClientHandler client, String gameType) {
        if ("TUG_OF_WAR".equalsIgnoreCase(gameType)) tugQueue.remove(client);
        else if ("LAND_GRAB".equalsIgnoreCase(gameType)) landQueue.remove(client);
        else if ("CASTLE_DEFENSE".equalsIgnoreCase(gameType)) castleQueue.remove(client);
        client.notifyMatchCancelled();
    }
}