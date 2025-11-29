package typingarena.server;

import typingarena.net.Message;
import typingarena.server.core.ServerContext;
import typingarena.server.lobby.Room;
import typingarena.server.match.Matchmaker;
import typingarena.server.auth.AuthService;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClientHandler extends Thread {

    private final ServerMain server;
    private final ServerContext context;
    private final Matchmaker matchmaker;
    private final Socket socket;

    private final AuthService authService;
    private String loggedInUserId = null;
    private String loggedInNickname = null;

    private BufferedReader in;
    private BufferedWriter out;
    private Room currentRoom;
    private String pendingMatchGameType;
    private String currentSessionId;

    public ClientHandler(ServerMain server, ServerContext context, Matchmaker matchmaker, Socket socket) {
        this.server = server;
        this.context = context;
        this.matchmaker = matchmaker;
        this.socket = socket;
        this.authService = context.getAuthService();
        setName("Client-" + socket.getRemoteSocketAddress());
        setDaemon(true);
    }

    public String getLoggedInUserId() { return loggedInUserId; }

    @Override
    public void run() {
        try (socket) {
            context.registerClient(this);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            String line;
            while ((line = in.readLine()) != null) {
                Message msg = context.getGson().fromJson(line, Message.class);
                handle(msg);
            }
        } catch (IOException ignored) {
        } finally {
            context.unregisterClient(this);
            server.onClientDisconnected(this);
        }
    }

    private void handle(Message m) {
        String type = (m.type == null) ? "" : m.type.toUpperCase(Locale.ROOT);

        if (loggedInUserId == null && !type.equals("REGISTER_REQUEST") && !type.equals("LOGIN_REQUEST")) {
            sendError("auth", "로그인이 필요합니다.");
            return;
        }

        switch (type) {
            case "REGISTER_REQUEST" -> {
                Map<String, Object> data = m.data;
                String id = (String) data.get("id");
                String pw = (String) data.get("pw");
                String nickname = (String) data.get("nickname");
                boolean success = authService.register(id, pw, nickname);
                Message res = Message.of("REGISTER_RESPONSE");
                res.data = Map.of("success", success);
                send(res);
            }
            case "LOGIN_REQUEST" -> {
                Map<String, Object> data = m.data;
                String id = (String) data.get("id");
                String pw = (String) data.get("pw");
                Map<String, Object> result = authService.login(id, pw);
                if (result != null) {
                    this.loggedInUserId = (String) result.get("id");
                    this.loggedInNickname = (String) result.get("nickname");
                    result.put("success", true);
                    Message res = Message.of("LOGIN_RESPONSE");
                    res.data = result;
                    send(res);
                } else {
                    Message res = Message.of("LOGIN_RESPONSE");
                    res.data = Map.of("success", false);
                    send(res);
                }
            }
            case "LIST_ROOMS_REQUEST", "LIST_ROOMS" -> sendRooms();
            case "CREATE_ROOM_REQUEST", "CREATE_ROOM" -> handleCreateRoom(m);
            case "JOIN_ROOM_REQUEST", "JOIN_ROOM" -> handleJoinRoom(m);
            case "LEAVE_ROOM_REQUEST", "LEAVE_ROOM" -> leaveRoom();

            case "MATCH_REQUEST" -> matchmaker.requestMatch(this, gameTypeOf(m));
            case "MATCH_CANCEL" -> matchmaker.cancelMatch(this, gameTypeOf(m));

            case "GAME_ACTION" -> server.onGameAction(this, m);
            case "GAME_FORFEIT" -> server.onGameForfeit(this);
            case "GAME_REMATCH_REQUEST" -> server.onGameRematchRequest(this, m);
            default -> {}
        }
    }

    private String gameTypeOf(Message m) {
        if (m.data != null && m.data.get("gameType") != null) {
            return String.valueOf(m.data.get("gameType"));
        }
        return "";
    }

    private void handleCreateRoom(Message m) {
        String name = (m.roomName == null || m.roomName.isBlank()) ? "새 방" : m.roomName.trim();
        Room r = new Room(name);
        context.getRooms().put(r.getId(), r);
        sendRoomsToAll();
    }

    private void handleJoinRoom(Message m) {
        Room r = context.getRooms().get(m.roomId);
        if (r != null) {
            joinRoom(r);
            Message joined = Message.of("JOIN_ROOM_RESPONSE");
            joined.roomId = r.getId();
            joined.data = Map.of("success", true, "roomId", r.getId());
            send(joined);
        }
    }

    private void joinRoom(Room r) {
        leaveRoom();
        currentRoom = r;
        r.getClients().add(this);
        sendRoomsToAll();
    }

    public void leaveRoom() {
        if (currentRoom != null) {
            currentRoom.getClients().remove(this);
            if (currentRoom.getClients().isEmpty()) {
                context.getRooms().remove(currentRoom.getId());
            }
            currentRoom = null;
            sendRoomsToAll();
        }
    }

    private void sendRooms() {
        Message res = Message.of("LIST_ROOMS_RESPONSE");
        List<Map<String, Object>> list = new ArrayList<>();
        for (Room r : context.getRooms().values()) list.add(r.toSummary());
        res.data = Map.of("rooms", list);
        send(res);
    }

    private void sendRoomsToAll() {
        sendRooms();
        if (currentRoom != null) {
            for (ClientHandler c : currentRoom.getClients()) {
                if (c != this) c.sendRooms();
            }
        }
    }

    public void send(Message m) {
        try {
            out.write(context.getGson().toJson(m));
            out.write("\n");
            out.flush();
        } catch (IOException ignored) {}
    }

    public void sendError(String type, String message) {
        Message err = Message.of(type + "_ERROR");
        err.data = Map.of("message", message);
        send(err);
    }

    public void notifyMatchWaiting(String gameType) {
        pendingMatchGameType = gameType;
        Message waiting = Message.of("MATCH_WAITING");
        waiting.data = Map.of("gameType", gameType, "message", "상대를 찾는 중입니다...");
        send(waiting);
    }

    public void notifyMatchCancelled() {
        pendingMatchGameType = null;
        Message cancelled = Message.of("MATCH_CANCELLED");
        cancelled.data = Map.of("message", "매칭이 취소되었습니다.");
        send(cancelled);
    }

    public void clearPendingMatch() { pendingMatchGameType = null; }
    public boolean hasPendingMatch() { return pendingMatchGameType != null; }
    public boolean isConnected() { return socket != null && !socket.isClosed(); }
    public String getNickname() { return (loggedInNickname != null) ? loggedInNickname : "Player"; }
    public void setNickname(String nickname) { if (nickname != null && !nickname.isBlank()) this.loggedInNickname = nickname.trim(); }
    public void setCurrentSession(String sessionId) { this.currentSessionId = sessionId; }
    public String getCurrentSession() { return currentSessionId; }
    public String getPendingMatchGameType() { return pendingMatchGameType; }
}
