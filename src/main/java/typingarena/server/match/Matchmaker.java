package typingarena.server.match;

import typingarena.server.ClientHandler;
import java.util.LinkedList;
import java.util.List;

public class Matchmaker {

    public interface Listener {
        void onMatchReady(String gameType, ClientHandler p1, ClientHandler p2);
    }

    private final Listener listener;
    
    // 기존 대기열
    private final List<ClientHandler> tugQueue = new LinkedList<>();
    private final List<ClientHandler> landQueue = new LinkedList<>();
    
    // ▼ [추가] 성 지키기 대기열 (이게 없어서 막힌 겁니다!)
    private final List<ClientHandler> castleQueue = new LinkedList<>();

    public Matchmaker(Listener listener) {
        this.listener = listener;
    }

    public synchronized void requestMatch(ClientHandler client, String gameType) {
        // [디버그] 매치메이커가 요청을 받았는지 확인
        System.out.println("[Matchmaker] 요청 수신: " + gameType + " / 유저: " + client.getNickname());

        if ("TUG_OF_WAR".equalsIgnoreCase(gameType)) {
            addToQueue(tugQueue, client, "TUG_OF_WAR");
        } 
        else if ("LAND_GRAB".equalsIgnoreCase(gameType)) {
            addToQueue(landQueue, client, "LAND_GRAB");
        }
        // ▼ [추가] 성 지키기 매칭 로직 연결
        else if ("CASTLE_DEFENSE".equalsIgnoreCase(gameType)) {
            addToQueue(castleQueue, client, "CASTLE_DEFENSE");
        }
        else {
            System.out.println("[Matchmaker] 알 수 없는 게임 타입: " + gameType);
            client.sendError("MATCH_REQUEST", "지원하지 않는 게임 타입입니다: " + gameType);
        }
    }

    private void addToQueue(List<ClientHandler> queue, ClientHandler client, String gameType) {
        if (queue.contains(client)) return;
        
        queue.add(client);
        client.notifyMatchWaiting(gameType);
        System.out.println("[Matchmaker] 대기열 진입 (" + gameType + "): 현재 " + queue.size() + "명");

        // 2명이 모이면 리스너(ServerMain)에게 "준비됐다"고 알림
        if (queue.size() >= 2) {
            ClientHandler p1 = queue.remove(0);
            ClientHandler p2 = queue.remove(0);
            p1.clearPendingMatch();
            p2.clearPendingMatch();
            System.out.println("[Matchmaker] 매칭 성사! -> ServerMain 호출");
            listener.onMatchReady(gameType, p1, p2);
        }
    }

    public synchronized void cancelMatch(ClientHandler client, String gameType) {
        if ("TUG_OF_WAR".equalsIgnoreCase(gameType)) {
            tugQueue.remove(client);
        } else if ("LAND_GRAB".equalsIgnoreCase(gameType)) {
            landQueue.remove(client);
        } 
        // ▼ [추가] 취소 로직도 추가
        else if ("CASTLE_DEFENSE".equalsIgnoreCase(gameType)) {
            castleQueue.remove(client);
        }
        
        client.notifyMatchCancelled();
    }
}