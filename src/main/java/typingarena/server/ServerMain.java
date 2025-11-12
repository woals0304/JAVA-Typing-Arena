package typingarena.server;

import com.google.gson.Gson;
import typingarena.core.tugofwar.ActiveEffects;
import typingarena.core.tugofwar.GameLogic;
import typingarena.core.tugofwar.TugOfWarWordGenerator;
import typingarena.net.Message;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/**
 * 멀티 로비 + 자동 매칭 + 온라인 줄다리기 세션 서버.
 */
public class ServerMain {

    private final int port;
    private final Gson gson = new Gson();
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Deque<Client>> matchQueues = new ConcurrentHashMap<>();
    private final Map<String, TugSession> tugSessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final Random rnd = new Random();

    private final Set<Client> clients = Collections.newSetFromMap(new ConcurrentHashMap<>());

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
                Socket s = ss.accept();
                new Client(this, s).start();
            }
        }
    }

    // ---- Room (테스트용) ----
    static class Room {
        final String id = UUID.randomUUID().toString();
        final String name;
        final Set<Client> clients = Collections.synchronizedSet(new HashSet<>());

        Room(String name) { this.name = name; }

        Map<String, Object> toSummary() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("roomId", id);
            m.put("name", name);
            m.put("players", clients.size());
            return m;
        }
    }

    // ---- Client Thread ----
    static class Client extends Thread {
        private final ServerMain server;
        private final Socket socket;
        private BufferedReader in;
        private BufferedWriter out;
        private Room currentRoom;
        private String nickname = "Player";
        private String pendingMatchGameType;
        private String currentSessionId;

        Client(ServerMain server, Socket socket) {
            this.server = server;
            this.socket = socket;
            setName("Client-" + socket.getRemoteSocketAddress());
            setDaemon(true);
        }

        @Override
        public void run() {
            try (socket) {
                server.registerClient(this);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

                String line;
                while ((line = in.readLine()) != null) {
                    Message msg = server.gson.fromJson(line, Message.class);
                    handle(msg);
                }
            } catch (IOException ignored) {
            } finally {
                server.unregisterClient(this);
                server.handleDisconnect(this);
            }
        }

        private void handle(Message m) {
            String type = (m.type == null) ? "" : m.type.toUpperCase(Locale.ROOT);
            switch (type) {
                case "LIST_ROOMS_REQUEST", "LIST_ROOMS" -> sendRooms();
                case "CREATE_ROOM_REQUEST", "CREATE_ROOM" -> handleCreateRoom(m);
                case "JOIN_ROOM_REQUEST", "JOIN_ROOM" -> handleJoinRoom(m);
                case "LEAVE_ROOM_REQUEST", "LEAVE_ROOM" -> leaveRoom();
                case "MATCH_REQUEST" -> server.handleMatchRequest(this, m);
                case "MATCH_CANCEL" -> server.handleMatchCancel(this);
                case "GAME_ACTION" -> server.handleGameAction(this, m);
                case "GAME_FORFEIT" -> server.handleGameForfeit(this);
                default -> {}
            }
        }

        private void handleCreateRoom(Message m) {
            String name = (m.roomName == null || m.roomName.isBlank()) ? "새 방" : m.roomName.trim();
            Room r = new Room(name);
            server.rooms.put(r.id, r);
            sendRoomsToAll();
        }

        private void handleJoinRoom(Message m) {
            Room r = server.rooms.get(m.roomId);
            if (r != null) {
                updateNickname(m);
                joinRoom(r);
                Message joined = Message.of("JOIN_ROOM_RESPONSE");
                joined.roomId = r.id;
                joined.data = Map.of("success", true, "roomId", r.id);
                send(joined);
            }
        }

        private void joinRoom(Room r) {
            leaveRoom();
            currentRoom = r;
            r.clients.add(this);
            sendRoomsToAll();
        }

        private void leaveRoom() {
            if (currentRoom != null) {
                currentRoom.clients.remove(this);
                if (currentRoom.clients.isEmpty()) {
                    server.rooms.remove(currentRoom.id);
                }
                currentRoom = null;
                sendRoomsToAll();
            }
        }

        private void sendRooms() {
            Message res = Message.of("LIST_ROOMS_RESPONSE");
            List<Map<String, Object>> list = new ArrayList<>();
            for (Room r : server.rooms.values()) list.add(r.toSummary());
            res.data = Map.of("rooms", list);
            send(res);
        }

        private void sendRoomsToAll() {
            sendRooms();
            if (currentRoom != null) {
                for (Client c : currentRoom.clients) {
                    if (c != this) c.sendRooms();
                }
            }
        }

        private void updateNickname(Message m) {
            if (m.nickname != null && !m.nickname.isBlank()) {
                nickname = m.nickname.trim();
            } else if (m.data != null && m.data.get("nickname") != null) {
                String nick = String.valueOf(m.data.get("nickname")).trim();
                if (!nick.isEmpty()) nickname = nick;
            }
        }

        private void send(Message m) {
            try {
                out.write(server.gson.toJson(m));
                out.write("\n");
                out.flush();
            } catch (IOException ignored) {}
        }

        private void setPendingMatch(String gameType) {
            this.pendingMatchGameType = gameType;
        }

        private void clearPendingMatch() { this.pendingMatchGameType = null; }

        private void setCurrentSession(String sessionId) { this.currentSessionId = sessionId; }

        private String getCurrentSession() { return currentSessionId; }

        private boolean isConnected() {
            return socket != null && !socket.isClosed();
        }
    }

    private void registerClient(Client c) { clients.add(c); }
    private void unregisterClient(Client c) { clients.remove(c); }

    private void handleDisconnect(Client c) {
        handleMatchCancel(c);
        endSessionFor(c, "상대가 연결을 종료했습니다.");
        c.leaveRoom();
    }

    // ----- 매칭 -----
    void handleMatchRequest(Client client, Message msg) {
        handleMatchCancel(client);
        String gameType = (msg.data != null && msg.data.get("gameType") != null)
                ? String.valueOf(msg.data.get("gameType")).trim()
                : "";
        if (gameType.isEmpty()) {
            client.send(error("MATCH_REQUEST", "gameType is required"));
            return;
        }
        client.updateNickname(msg);
        client.setPendingMatch(gameType);

        Deque<Client> queue = matchQueues.computeIfAbsent(gameType, k -> new ConcurrentLinkedDeque<>());
        synchronized (queue) {
            Client opponent = null;
            while (!queue.isEmpty() && opponent == null) {
                Client candidate = queue.poll();
                if (candidate != null && candidate.isConnected()) opponent = candidate;
            }
            if (opponent == null) {
                queue.offer(client);
                Message waiting = Message.of("MATCH_WAITING");
                waiting.data = Map.of("gameType", gameType, "message", "상대를 찾는 중입니다...");
                client.send(waiting);
            } else {
                opponent.clearPendingMatch();
                client.clearPendingMatch();
                startMatch(opponent, client, gameType);
            }
        }
    }

    void handleMatchCancel(Client client) {
        String gameType = client.pendingMatchGameType;
        if (gameType == null) return;
        Deque<Client> queue = matchQueues.get(gameType);
        if (queue != null) queue.remove(client);
        client.clearPendingMatch();
        Message cancelled = Message.of("MATCH_CANCELLED");
        cancelled.data = Map.of("message", "매칭이 취소되었습니다.");
        client.send(cancelled);
    }

    private void startMatch(Client a, Client b, String gameType) {
        Message success = Message.of("MATCH_SUCCESS");
        success.data = Map.of("gameType", gameType, "players", List.of(a.nickname, b.nickname));
        a.send(success);
        b.send(success);

        if ("TUG_OF_WAR".equalsIgnoreCase(gameType)) {
            TugSession session = new TugSession(a, b);
            tugSessions.put(session.id, session);
            session.start();
        }
    }

    // ----- 게임 액션 -----
    void handleGameAction(Client client, Message msg) {
        String sessionId = msg.sessionId != null ? msg.sessionId : client.getCurrentSession();
        if (sessionId == null) return;
        TugSession session = tugSessions.get(sessionId);
        if (session != null) {
            String word = null;
            if (msg.data != null && msg.data.get("word") != null) word = String.valueOf(msg.data.get("word"));
            else if (msg.text != null) word = msg.text;
            if (word != null) session.handleWord(client, word.trim());
        }
    }

    void handleGameForfeit(Client client) {
        endSessionFor(client, "상대가 포기했습니다.");
    }

    private void endSessionFor(Client client, String reason) {
        String sessionId = client.getCurrentSession();
        if (sessionId == null) return;
        TugSession session = tugSessions.get(sessionId);
        if (session != null) session.forfeit(client, reason);
    }

    private Message error(String type, String message) {
        Message err = Message.of(type + "_ERROR");
        err.data = Map.of("message", message);
        return err;
    }

    // ----- Tug of War Session -----
    class TugSession {
        final String id = UUID.randomUUID().toString();

        final PlayerState left;
        final PlayerState right;

        double pos = 0.0;
        int timeMs = 60_000;
        boolean running = true;
        ScheduledFuture<?> ticker;

        TugSession(Client a, Client b) {
            this.left = new PlayerState(a);
            this.right = new PlayerState(b);
        }

        void start() {
            left.assignWord();
            right.assignWord();
            sendStart(left, right.client.nickname);
            sendStart(right, left.client.nickname);
            ticker = scheduler.scheduleAtFixedRate(this::tick, 100, 100, TimeUnit.MILLISECONDS);
        }

        void tick() {
            if (!running) return;
            timeMs -= 100;
            if (timeMs <= 0) {
                finishByScore("시간 종료");
            } else {
                sendUpdate();
            }
        }

        void handleWord(Client client, String typed) {
            if (!running || typed == null || typed.isEmpty()) return;
            PlayerState player = (client == left.client) ? left : right;
            PlayerState opponent = (player == left) ? right : left;

            if (!typed.equalsIgnoreCase(player.currentWord.text())) return;

            double push = 8.0;
            if (player.effects.isPowerGripActive()) push *= 2.0;
            double anchorFactor = opponent.effects.isAnchorActive() ? 0.2 : 1.0;
            pos += (player == left ? push : -push) * anchorFactor;

            player.score++;
            applyModifierReward(player, opponent);
            player.assignWord();
            sendUpdate();

            if (pos >= 100) finish(left, right, left.client.nickname + " 측이 승리!");
            else if (pos <= -100) finish(right, left, right.client.nickname + " 측이 승리!");
        }

        void forfeit(Client quitter, String reason) {
            if (!running) return;
            PlayerState winner = (quitter == left.client) ? right : left;
            PlayerState loser = (winner == left) ? right : left;
            finish(winner, loser, reason);
        }

        private void applyModifierReward(PlayerState player, PlayerState opponent) {
            GameLogic.WordModifier modifier = player.currentWord.modifier();
            if (modifier == GameLogic.WordModifier.BUFF) {
                if (rnd.nextBoolean()) {
                    player.effects.activatePowerGrip(5_000);
                    player.lastItem = "파워 그립";
                } else {
                    player.effects.activateAnchor(3_000);
                    player.lastItem = "앵커";
                }
            } else if (modifier == GameLogic.WordModifier.TRAP) {
                opponent.effects.activateBlind(3_000);
                opponent.lastItem = "먹물";
            }
        }

        private void sendStart(PlayerState player, String opponentName) {
            Message start = Message.of("GAME_START_BROADCAST");
            start.sessionId = id;
            start.data = Map.of(
                    "gameType", "TUG_OF_WAR",
                    "yourWord", player.currentWord.text(),
                    "opponentWord", "???",
                    "opponent", opponentName,
                    "timeMs", timeMs,
                    "modifierSelf", player.currentWord.modifier().name(),
                    "effectsSelf", player.effects.describeEffects(),
                    "lastItemSelf", player.lastItem,
                    "blindSelf", player.effects.isBlindActive()
            );
            player.client.send(start);
        }

        private void sendUpdate() {
            sendUpdateFor(left, right);
            sendUpdateFor(right, left);
        }

        private void sendUpdateFor(PlayerState self, PlayerState opponent) {
            Message update = Message.of("GAME_UPDATE_BROADCAST");
            update.sessionId = id;
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("gameType", "TUG_OF_WAR");
            payload.put("pos", pos);
            payload.put("timeMs", timeMs);
            payload.put("yourWord", self.currentWord.text());
            payload.put("opponentWord", "???");
            payload.put("modifierSelf", self.currentWord.modifier().name());
            payload.put("scoreSelf", self.score);
            payload.put("scoreOpponent", opponent.score);
            payload.put("effectsSelf", self.effects.describeEffects());
            payload.put("lastItemSelf", self.lastItem);
            payload.put("blindSelf", self.effects.isBlindActive());
            update.data = payload;
            self.client.send(update);
        }

        private void finishByScore(String reason) {
            if (left.score > right.score) finish(left, right, reason);
            else if (right.score > left.score) finish(right, left, reason);
            else finish(null, null, reason + " - 무승부");
        }

        private void finish(PlayerState winner, PlayerState loser, String reason) {
            if (!running) return;
            running = false;
            if (ticker != null) ticker.cancel(false);
            tugSessions.remove(id);
            left.client.setCurrentSession(null);
            right.client.setCurrentSession(null);

            sendEnd(left, winner == left, reason);
            sendEnd(right, winner == right, reason);
        }

        private void sendEnd(PlayerState player, boolean isWinner, String reason) {
            Message end = Message.of("GAME_END_BROADCAST");
            end.sessionId = id;
            end.data = Map.of(
                    "gameType", "TUG_OF_WAR",
                    "result", isWinner ? "승리" : "패배",
                    "message", reason,
                    "scoreSelf", player.score,
                    "scoreOpponent", player == left ? right.score : left.score,
                    "pos", pos
            );
            player.client.send(end);
        }

        class PlayerState {
            final Client client;
            final ActiveEffects effects = new ActiveEffects();
            TugOfWarWordGenerator.Word currentWord;
            int score = 0;
            String lastItem = "없음";

            PlayerState(Client client) {
                this.client = client;
            }

            void assignWord() {
                currentWord = TugOfWarWordGenerator.next(rnd);
            }
        }

    }

}
