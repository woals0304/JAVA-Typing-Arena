package typingarena.server;

import com.google.gson.Gson;
import typingarena.net.Message;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** 매우 단순한 멀티 로비 서버: 방 목록/생성/입장/퇴장만 */
public class ServerMain {
    private final int port;
    private final Gson gson = new Gson();
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    public ServerMain(int port) { this.port = port; }

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

    // --- Room 관리 ---
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

    // --- 클라이언트 처리 ---
    static class Client extends Thread {
        private final ServerMain server;
        private final Socket socket;
        private BufferedReader in;
        private BufferedWriter out;
        private Room current;
        private String nickname = "Player";

        Client(ServerMain server, Socket socket) {
            this.server = server; this.socket = socket;
            setName("Client-" + socket.getRemoteSocketAddress());
            setDaemon(true);
        }

        @Override public void run() {
            try (socket) {
                in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

                String line;
                while ((line = in.readLine()) != null) {
                    Message m = server.gson.fromJson(line, Message.class);
                    handle(m);
                }
            } catch (IOException ignored) {
            } finally {
                leaveRoom();
            }
        }

        private void handle(Message m) {
            switch (m.type) {
                case "list_rooms" -> sendRooms();
                case "create_room" -> {
                    String name = (m.roomName == null || m.roomName.isBlank()) ? "새 방" : m.roomName.trim();
                    Room r = new Room(name);
                    server.rooms.put(r.id, r);
                    sendRoomsToAll(); // 전체에 갱신
                }
                case "join_room" -> {
                    Room r = server.rooms.get(m.roomId);
                    if (r != null) {
                        nickname = (m.nickname != null && !m.nickname.isBlank()) ? m.nickname : nickname;
                        joinRoom(r);
                        Message joined = Message.of("joined");
                        joined.roomId = r.id;
                        send(joined);
                    }
                }
                case "leave_room" -> leaveRoom();
                default -> {} // 무시
            }
        }

        private void joinRoom(Room r) {
            leaveRoom();
            current = r;
            r.clients.add(this);
            sendRoomsToAll();
        }

        private void leaveRoom() {
            if (current != null) {
                current.clients.remove(this);
                if (current.clients.isEmpty()) {
                    server.rooms.remove(current.id);
                }
                current = null;
                sendRoomsToAll();
            }
        }

        private void sendRooms() {
            Message res = Message.of("rooms");
            List<Map<String, Object>> list = new ArrayList<>();
            for (Room r : server.rooms.values()) list.add(r.toSummary());
            res.data = Map.of("list", list);
            send(res);
        }

        private void sendRoomsToAll() {
            List<Client> everyone = new ArrayList<>();
            // 모든 연결된 클라에게(모든 방의 모든 클라 + 로비상태 클라)
            for (Room r : server.rooms.values()) everyone.addAll(r.clients);
            // 로비만 있는 클라(방에 없는)는 여기선 추적 안 하지만
            // 실제론 서버가 전체 세션 목록을 들고 있으면 거기에도 전송하는 구조가 이상적
            // 간단히: 현재 스레드 자신에게만 + 같은 방 사람들에겐 확실히 전송
            sendRooms();
            if (current != null) {
                for (Client c : current.clients) if (c != this) c.sendRooms();
            }
        }

        private void send(Message m) {
            try {
                out.write(server.gson.toJson(m));
                out.write("\n"); out.flush();
            } catch (IOException ignored) {}
        }
    }
}
