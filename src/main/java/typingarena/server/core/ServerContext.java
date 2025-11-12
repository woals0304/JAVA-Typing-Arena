package typingarena.server.core;

import com.google.gson.Gson;
import typingarena.server.ClientHandler;
import typingarena.server.lobby.Room;
import typingarena.server.session.TugOfWarSession;

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

    public ServerContext(Gson gson, ScheduledExecutorService scheduler) {
        this.gson = gson;
        this.scheduler = scheduler;
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
