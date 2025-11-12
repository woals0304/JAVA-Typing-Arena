package typingarena.server.session;

import typingarena.core.tugofwar.ActiveEffects;
import typingarena.core.tugofwar.GameLogic;
import typingarena.core.tugofwar.TugOfWarWordGenerator;
import typingarena.net.Message;
import typingarena.server.ClientHandler;
import typingarena.server.core.ServerContext;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TugOfWarSession {

    private final String id = UUID.randomUUID().toString();
    private final PlayerState left;
    private final PlayerState right;
    private final ServerContext context;
    private final Random rnd = new Random();

    private double pos = 0.0;
    private int timeMs = 60_000;
    private boolean running = true;
    private ScheduledFuture<?> ticker;

    public TugOfWarSession(ServerContext context, ClientHandler a, ClientHandler b) {
        this.context = context;
        this.left = new PlayerState(a);
        this.right = new PlayerState(b);
    }

    public String getId() {
        return id;
    }

    public void start() {
        left.assignWord();
        right.assignWord();
        sendStart(left, right.getNickname());
        sendStart(right, left.getNickname());
        ticker = context.getScheduler().scheduleAtFixedRate(this::tick, 100, 100, TimeUnit.MILLISECONDS);
    }

    public void dispose() {
        if (ticker != null) ticker.cancel(false);
        running = false;
        context.getTugSessions().remove(id);
        left.setCurrentSession(null);
        right.setCurrentSession(null);
    }

    private void tick() {
        if (!running) return;
        timeMs -= 100;
        if (timeMs <= 0) {
            finishByScore("시간 종료");
        } else {
            sendUpdate();
        }
    }

    public void handleWord(ClientHandler client, String typed) {
        if (!running || typed == null || typed.isEmpty()) return;
        PlayerState player = (client == left.getClient()) ? left : right;
        PlayerState opponent = (player == left) ? right : left;

        if (!typed.equalsIgnoreCase(player.getCurrentWord().text())) return;

        double push = 8.0;
        if (player.effects.isPowerGripActive()) push *= 2.0;
        double anchorFactor = opponent.effects.isAnchorActive() ? 0.2 : 1.0;
        pos += (player == left ? push : -push) * anchorFactor;

        player.incrementScore();
        applyModifierReward(player, opponent);
        player.assignWord();
        sendUpdate();

        if (pos >= 100) finish(left, right, left.getNickname() + " 측이 승리!");
        else if (pos <= -100) finish(right, left, right.getNickname() + " 측이 승리!");
    }

    public void forfeit(ClientHandler quitter, String reason) {
        if (!running) return;
        PlayerState winner = (quitter == left.getClient()) ? right : left;
        PlayerState loser = (winner == left) ? right : left;
        finish(winner, loser, reason);
    }

    private void applyModifierReward(PlayerState player, PlayerState opponent) {
        GameLogic.WordModifier modifier = player.getCurrentWord().modifier();
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
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("gameType", "TUG_OF_WAR");
        payload.put("yourWord", player.currentWord.text());
        payload.put("opponentWord", "???");
        payload.put("opponent", opponentName);
        payload.put("timeMs", timeMs);
        payload.put("modifierSelf", player.currentWord.modifier().name());
        payload.put("effectsSelf", player.effects.describeEffects());
        payload.put("lastItemSelf", player.lastItem);
        payload.put("blindSelf", player.effects.isBlindActive());
        start.data = payload;
        player.send(start);
        player.setCurrentSession(id);
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
        self.send(update);
    }

    private void finishByScore(String reason) {
        if (left.score > right.score) finish(left, right, reason);
        else if (right.score > left.score) finish(right, left, reason);
        else finish(null, null, reason + " - 무승부");
    }

    private void finish(PlayerState winner, PlayerState loser, String reason) {
        if (!running) return;
        running = false;
        dispose();

        sendEnd(left, winner == left, reason);
        sendEnd(right, winner == right, reason);
    }

    private void sendEnd(PlayerState player, boolean isWinner, String reason) {
        Message end = Message.of("GAME_END_BROADCAST");
        end.sessionId = id;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("gameType", "TUG_OF_WAR");
        payload.put("result", isWinner ? "승리" : "패배");
        payload.put("message", reason);
        payload.put("scoreSelf", player.score);
        payload.put("scoreOpponent", player == left ? right.score : left.score);
        payload.put("pos", pos);
        end.data = payload;
        player.send(end);
    }

    public void assignToContext() {
        context.getTugSessions().put(id, this);
    }

    private class PlayerState {
        private final ClientHandler client;
        private final ActiveEffects effects = new ActiveEffects();
        private TugOfWarWordGenerator.Word currentWord;
        private int score = 0;
        private String lastItem = "없음";

        private PlayerState(ClientHandler client) {
            this.client = client;
        }

        private void assignWord() {
            this.currentWord = TugOfWarWordGenerator.next(rnd);
        }

        private void incrementScore() {
            score++;
        }

        private void send(Message m) {
            client.send(m);
        }

        private String getNickname() {
            return client.getNickname();
        }

        private ClientHandler getClient() {
            return client;
        }

        private TugOfWarWordGenerator.Word getCurrentWord() {
            return currentWord;
        }

        private void setCurrentSession(String sessionId) {
            client.setCurrentSession(sessionId);
        }
    }
}
