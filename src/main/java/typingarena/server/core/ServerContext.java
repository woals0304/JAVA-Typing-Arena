package typingarena.server.core;

import com.google.gson.Gson;
import typingarena.server.ClientHandler;
import typingarena.server.lobby.Room;
import typingarena.server.session.TugOfWarSession;

// [추가] 3단계에서 추가된 임포트
import typingarena.server.auth.AuthService;
import typingarena.server.db.DatabaseManager;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 서버 전역에서 공유하는 상태/리소스 모음.
 */
public class ServerContext {

    private final Gson gson;
    private final ScheduledExecutorService scheduler;
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, TugOfWarSession> tugSessions = new ConcurrentHashMap<>();
    private final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();

    private final AuthService authService; // [추가] 인증 서비스

    public ServerContext(Gson gson, ScheduledExecutorService scheduler) {
        this.gson = gson;
        this.scheduler = scheduler;
        
        // [추가] 서버 시작 시 DB와 인증 서비스 초기화
        DatabaseManager.getInstance(); // DB 파일/테이블 생성 보장
        this.authService = new AuthService();
    }

    // [추가] 인증 서비스 getter
    public AuthService getAuthService() {
        return authService;
    }

    public Gson getGson() {
        return gson;
    }

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    public Map<String, Room> getRooms() {
        return rooms;
    }

    public Map<String, TugOfWarSession> getTugSessions() {
        return tugSessions;
    }

    public Set<ClientHandler> getClients() {
        return clients;
    }

    public void registerClient(ClientHandler handler) {
        clients.add(handler);
    }

    public void unregisterClient(ClientHandler handler) {
        clients.remove(handler);
    }
}