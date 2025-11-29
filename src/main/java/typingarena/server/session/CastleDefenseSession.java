package typingarena.server.session;

import typingarena.net.Message;
import typingarena.server.ClientHandler;
import typingarena.server.core.ServerContext;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CastleDefenseSession {
    private final String id = UUID.randomUUID().toString();
    private final ClientHandler p1;
    private final ClientHandler p2;
    private final ServerContext context;
    
    private int teamScore = 0;
    private int teamHp = 20; 
    
    private ScheduledFuture<?> spawnTask;
    private long gameTime = 0; 
    private int spawnCounter = 0;
    private boolean cleanedUp = false;

    private List<String> wordPool = new ArrayList<>();

    private static class WordData {
        List<String> words;
    }

    public CastleDefenseSession(ServerContext context, ClientHandler p1, ClientHandler p2) {
        this.context = context;
        this.p1 = p1;
        this.p2 = p2;
        loadWords();
    }

    private void loadWords() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("words/ko.json")) {
            if (in == null) {
                System.err.println("Error: words/ko.json not found.");
                wordPool.add("JAVA"); wordPool.add("SERVER"); 
                return;
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                WordData data = context.getGson().fromJson(reader, WordData.class);
                if (data != null && data.words != null) {
                    wordPool.addAll(data.words);
                }
            }
        } catch (Exception e) {
            wordPool.add("ERROR");
        }
    }

    public String getId() { return id; }

    public void start() {
        p1.setCurrentSession(id);
        p2.setCurrentSession(id);
        startGameLoop();
    }

    private void startGameLoop() {
        teamScore = 0;
        teamHp = 20;
        gameTime = 0;
        spawnCounter = 0;

        if (spawnTask != null) spawnTask.cancel(true);

        sendStartMessage(p1, true);
        sendStartMessage(p2, false);
        
        System.out.println("TAJADEFENSE Session Started: " + id);

        spawnTask = context.getScheduler().scheduleAtFixedRate(this::gameLoop, 0, 1000, TimeUnit.MILLISECONDS);
    }

    private void sendStartMessage(ClientHandler client, boolean isPlayer1) {
        Message msg = Message.of("GAME_START_BROADCAST");
        msg.sessionId = id;
        msg.data = Map.of(
            "gameType", "CASTLE_DEFENSE",
            "p1", p1.getNickname(),
            "p2", p2.getNickname(),
            "teamHp", teamHp,
            "isPlayer1", isPlayer1
        );
        client.send(msg);
    }

    private void gameLoop() {
        try {
            gameTime += 1000;

            if (gameTime >= 60000) {
                finishGame(true, "VICTORY!");
                return;
            }

            // 난이도 조절 (로그 제거됨)
            if (gameTime == 1000 || gameTime % 3000 == 0) {
                broadcastSpawn("NORMAL");
            }
            if (gameTime % 5000 == 0) {
                broadcastSpawn("FAST");
            }
            if (gameTime % 20000 == 0) {
                broadcastSpawn("HEART");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cleanupSession() {
        if (cleanedUp) return;
        cleanedUp = true;
        if (spawnTask != null) spawnTask.cancel(false);
        context.getCastleSessions().remove(id);
        p1.setCurrentSession(null);
        p2.setCurrentSession(null);
    }

    private void broadcastSpawn(String type) {
        if (wordPool.isEmpty()) return;

        String word = wordPool.get(new Random().nextInt(wordPool.size()));
        double yRatio = 0.2 + new Random().nextDouble() * 0.6;
        int spawnId = ++spawnCounter; 

        Message msg = Message.of("GAME_SPAWN_BROADCAST");
        msg.sessionId = id;
        msg.data = Map.of(
            "spawnId", spawnId, 
            "spawnType", type, 
            "word", word, 
            "yRatio", yRatio
        );
        p1.send(msg);
        p2.send(msg);
    }

    public void handleAction(ClientHandler sender, Message msg) {
        if (msg.data == null) return;
        boolean changed = false;

        if (msg.data.containsKey("killId")) {
            int mid = toInt(msg.data.get("killId"));
            int scoreAdd = toInt(msg.data.get("scoreAdd"));
            teamScore += scoreAdd;
            changed = true;

            Message killMsg = Message.of("MONSTER_KILLED");
            killMsg.sessionId = id;
            killMsg.data = Map.of("killId", mid);
            p1.send(killMsg);
            p2.send(killMsg);
        }
        
        if (msg.data.containsKey("hitId")) {
            int mid = toInt(msg.data.get("hitId"));
            String newWord = (String) msg.data.get("newWord");
            
            Message updateMsg = Message.of("MONSTER_UPDATE");
            updateMsg.sessionId = id;
            updateMsg.data = Map.of("hitId", mid, "newWord", newWord);
            p1.send(updateMsg);
            p2.send(updateMsg);
        }

        if (msg.data.containsKey("attack")) {
            ClientHandler opponent = (sender == p1) ? p2 : p1;
            Message atkMsg = Message.of("OPPONENT_ATTACK");
            atkMsg.sessionId = id;
            if (msg.data.containsKey("targetId")) {
                atkMsg.data = Map.of("targetId", msg.data.get("targetId"));
            }
            opponent.send(atkMsg);
        }
        
        if (msg.data.containsKey("damage")) {
            int dmg = toInt(msg.data.get("damage"));
            teamHp -= dmg;
            if (teamHp < 0) teamHp = 0;
            changed = true;
            
            if (teamHp <= 0) {
                finishGame(false, "HP가 0이 되었습니다.");
                return;
            }
        }
        
        if (msg.data.containsKey("heal")) {
             int heal = toInt(msg.data.get("heal"));
             teamHp += heal;
             changed = true;
        }

        if (changed) broadcastUpdate();
    }

    private void broadcastUpdate() {
        Message update = Message.of("GAME_UPDATE_BROADCAST");
        update.sessionId = id;
        update.data = Map.of("teamScore", teamScore, "teamHp", teamHp);
        p1.send(update);
        p2.send(update);
    }
    
    public void forfeit(ClientHandler loser, String reason) {
        finishGame(false, loser.getNickname() + " left.");
    }

    private synchronized void finishGame(boolean isWin, String message) {
        if (spawnTask != null) spawnTask.cancel(true);
        Message endMsg = Message.of("GAME_END_BROADCAST");
        endMsg.sessionId = id;
        endMsg.data = Map.of("result", isWin ? "승리" : "패배", "message", message, "finalScore", teamScore);
        p1.send(endMsg);
        p2.send(endMsg);
        cleanupSession();
    }

    private int toInt(Object o) {
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return 0; }
    }
}
