package typingarena.server.session;

import typingarena.core.landgrab.LandGrabEffects;
import typingarena.core.landgrab.LandGrabLogic;
import typingarena.core.landgrab.LandGrabLogic.TileState;
import typingarena.core.landgrab.LandGrabEffects.ItemType;
import typingarena.net.Message;
import typingarena.server.ClientHandler;
import typingarena.server.core.ServerContext;
import typingarena.server.db.DatabaseManager;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class LandGrabSession {

    private final String id = UUID.randomUUID().toString();
    private final ServerContext context;
    private final String gameType = "LAND_GRAB";

    private final LandGrabLogic coreLogic = new LandGrabLogic();
    private final Random rnd = new Random();

    private final ClientHandler playerA;
    private final ClientHandler playerB;

    private int timeMs = 60_000;
    private boolean running = true;
    private ScheduledFuture<?> ticker;

    private long confusionUntilA = 0L;
    private long confusionUntilB = 0L;

    private boolean rematchRequestA = false;
    private boolean rematchRequestB = false;
    private boolean cleanedUp = false;

    public LandGrabSession(ServerContext context, ClientHandler a, ClientHandler b) {
        this.context = context;
        this.playerA = a;
        this.playerB = b;
    }

    public String getId() {
        return id;
    }

    public void start() {
        resetGameData();
        startLoop();

        playerA.setCurrentSession(id);
        playerB.setCurrentSession(id);
        sendStartBroadcast();
    }

    private void startLoop() {
        running = true;
        if (ticker != null && !ticker.isCancelled()) {
            ticker.cancel(true);
        }
        ticker = context.getScheduler().scheduleAtFixedRate(this::onTick, 100, 100, TimeUnit.MILLISECONDS);
    }

    private void resetGameData() {
        coreLogic.startGame();
        timeMs = 60_000;
        confusionUntilA = 0L;
        confusionUntilB = 0L;
        rematchRequestA = false;
        rematchRequestB = false;
    }

    private void restartGame() {
        resetGameData();
        startLoop();
        sendStartBroadcast();
    }

    private void cleanupSession() {
        if (cleanedUp) return;
        cleanedUp = true;
        if (ticker != null) ticker.cancel(false);
        context.getLandGrabSessions().remove(id);
        if (playerA != null) playerA.setCurrentSession(null);
        if (playerB != null) playerB.setCurrentSession(null);
    }

    public void handleRematchRequest(ClientHandler client) {
        // 게임 중에는 재경기 요청 무시
        if (running) return;

        if (client == playerA) rematchRequestA = true;
        else if (client == playerB) rematchRequestB = true;

        if (rematchRequestA && rematchRequestB) {
            restartGame();
        } else {
            ClientHandler opponent = (client == playerA) ? playerB : playerA;
            if (opponent != null && opponent.isConnected()) {
                Message notice = Message.of("GAME_REMATCH_NOTICE");
                notice.sessionId = this.id;
                opponent.send(notice);
            }
        }
    }

    private void sendStartBroadcast() {
        List<String> players = List.of(playerA.getNickname(), playerB.getNickname());

        if (playerA != null && playerA.isConnected()) {
            Message msg = Message.of("GAME_START_BROADCAST");
            msg.sessionId = this.id;
            Map<String, Object> data = buildStateFor(TileState.PLAYER_A, null, -1, -1);
            data.put("players", players);
            msg.data = data;
            playerA.send(msg);
        }
        if (playerB != null && playerB.isConnected()) {
            Message msg = Message.of("GAME_START_BROADCAST");
            msg.sessionId = this.id;
            Map<String, Object> data = buildStateFor(TileState.PLAYER_B, null, -1, -1);
            data.put("players", players);
            msg.data = data;
            playerB.send(msg);
        }
    }

    public void handleWord(ClientHandler client, String word) {
        if (!running) return;

        TileState who;
        boolean isPlayerA;

        if (client == playerA) {
            who = TileState.PLAYER_A;
            isPlayerA = true;
        } else if (client == playerB) {
            who = TileState.PLAYER_B;
            isPlayerA = false;
        } else {
            return;
        }

        LandGrabLogic.SubmitResult result = coreLogic.submitAnswer(word, who);

        String animForActor = null;
        String animForOpponent = null;

        if (result.resultCode() > 0) {
            if (result.itemType() != ItemType.NONE) {
                switch (result.itemType()) {
                    case BUFF_SPLASH -> { animForActor = "BUFF_SPLASH"; animForOpponent = "OPP_SPLASH"; }
                    case BUFF_BARRIER -> { animForActor = "BUFF_BARRIER"; animForOpponent = "OPP_BARRIER"; }
                    case BUFF_COMBO_GUARD -> { animForActor = "BUFF_COMBO_GUARD"; animForOpponent = "OPP_COMBO_GUARD"; }
                    case TRAP_INK -> { animForActor = "ATTACK_INK"; animForOpponent = "TRAP_INK"; applyInkTo(!isPlayerA, 2); }
                    case TRAP_EMP -> { animForActor = "ATTACK_EMP"; animForOpponent = "TRAP_EMP"; }
                    case TRAP_CONFUSION -> { animForActor = "ATTACK_CONFUSION"; animForOpponent = "TRAP_CONFUSION"; applyConfusionTo(!isPlayerA, 5000); }
                }
            } else {
                animForActor = "HIT";
            }
        }
        else {
            animForActor = "MISS";
        }

        int r = result.r();
        int c = result.c();

        if (isPlayerA) {
            sendUpdate(animForActor, animForOpponent, r, c);
        } else {
            sendUpdate(animForOpponent, animForActor, r, c);
        }
    }

    private void applyInkTo(boolean targetIsA, int count) {
        List<int[]> candidates = new ArrayList<>();
        for (int r = 0; r < LandGrabLogic.GRID_SIZE; r++) {
            for (int c = 0; c < LandGrabLogic.GRID_SIZE; c++) {
                if (!coreLogic.getEffects().isTileBlinded(r, c, targetIsA)) {
                    candidates.add(new int[]{r, c});
                }
            }
        }
        Collections.shuffle(candidates);
        int applied = 0;
        for (int[] coord : candidates) {
            if (applied >= count) break;
            coreLogic.getEffects().activateBlindTile(coord[0], coord[1], 3000, targetIsA);
            applied++;
        }
    }

    private void applyConfusionTo(boolean targetIsA, long durationMs) {
        long until = System.currentTimeMillis() + durationMs;
        if (targetIsA) confusionUntilA = Math.max(confusionUntilA, until);
        else confusionUntilB = Math.max(confusionUntilB, until);
    }

    private void onTick() {
        if (!running) return;
        try {
            timeMs -= 100;
            if (timeMs <= 0) {
                finishByScore("시간 종료!");
                return;
            }

            int totalScore = coreLogic.getScore(TileState.PLAYER_A) + coreLogic.getScore(TileState.PLAYER_B);
            if (totalScore == LandGrabLogic.GRID_SIZE * LandGrabLogic.GRID_SIZE) {
                finishByScore("모든 타일 점령!");
                return;
            }

            if (timeMs % 1000 == 0) {
                sendUpdate(null, null, -1, -1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (ticker != null) ticker.cancel(true);
        }
    }

    private void finishByScore(String reason) {
        int scoreA = coreLogic.getScore(TileState.PLAYER_A);
        int scoreB = coreLogic.getScore(TileState.PLAYER_B);

        if (scoreA > scoreB) {
            finish(playerA, playerB, reason);
        } else if (scoreB > scoreA) {
            finish(playerB, playerA, reason);
        } else {
            finish(null, null, reason + " (무승부)");
        }
    }

    private void sendUpdate(String animTriggerA, String animTriggerB, int r, int c) {
        if (playerA != null && playerA.isConnected()) {
            Message msgA = Message.of("GAME_UPDATE_BROADCAST");
            msgA.sessionId = this.id;
            msgA.data = buildStateFor(TileState.PLAYER_A, animTriggerA, r, c);
            playerA.send(msgA);
        }
        if (playerB != null && playerB.isConnected()) {
            Message msgB = Message.of("GAME_UPDATE_BROADCAST");
            msgB.sessionId = this.id;
            msgB.data = buildStateFor(TileState.PLAYER_B, animTriggerB, r, c);
            playerB.send(msgB);
        }
    }

    private Map<String, Object> buildStateFor(TileState me, String myAnimation, int r, int c) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("gameType", gameType);
        data.put("timeMs", (double) timeMs);

        data.put("grid", buildGridData(coreLogic.getGrid(), TileState.class));
        data.put("words", buildGridData(coreLogic.getWordGrid(), String.class));
        data.put("modifiers", buildGridData(coreLogic.getModifierGrid(), LandGrabLogic.WordModifier.class));

        boolean isMeA = (me == TileState.PLAYER_A);
        data.put("ink_tiles", buildBlindedTilesData(isMeA));

        TileState opp = (me == TileState.PLAYER_A) ? TileState.PLAYER_B : TileState.PLAYER_A;
        data.put("scoreSelf", coreLogic.getScore(me));
        data.put("scoreOpponent", coreLogic.getScore(opp));
        data.put("comboSelf", coreLogic.getCombo(me));
        data.put("comboOpponent", coreLogic.getCombo(opp));

        long now = System.currentTimeMillis();
        long myConfusionUntil = isMeA ? confusionUntilA : confusionUntilB;
        if (myConfusionUntil > now) {
            data.put("debuff", "FLIP_WORDS");
        }

        data.put("barrier_a", coreLogic.getEffects().isBarrierActive(true));
        data.put("barrier_b", coreLogic.getEffects().isBarrierActive(false));
        data.put("combo_guard_a", coreLogic.getEffects().isComboGuardActive(true));
        data.put("combo_guard_b", coreLogic.getEffects().isComboGuardActive(false));

        if (myAnimation != null) {
            data.put("animation_trigger", Map.of("type", myAnimation, "r", r, "c", c));
        }

        return data;
    }

    private <T> List<List<String>> buildGridData(T[][] grid, Class<T> enumClass) {
        List<List<String>> rowList = new ArrayList<>();
        for (int r = 0; r < LandGrabLogic.GRID_SIZE; r++) {
            List<String> colList = new ArrayList<>();
            for (int c = 0; c < LandGrabLogic.GRID_SIZE; c++) {
                if (grid[r][c] == null) {
                    colList.add("");
                } else if (enumClass == String.class) {
                    colList.add((String) grid[r][c]);
                } else {
                    colList.add(((Enum) grid[r][c]).name());
                }
            }
            rowList.add(colList);
        }
        return rowList;
    }

    private List<Map<String, Object>> buildBlindedTilesData(boolean isPlayerA) {
        List<Map<String, Object>> inkList = new ArrayList<>();
        List<LandGrabEffects.BlindedTile> tiles = coreLogic.getEffects().getActiveBlindedTiles(isPlayerA);
        for (LandGrabEffects.BlindedTile tile : tiles) {
            inkList.add(Map.of("r", tile.r(), "c", tile.c(), "until", tile.until()));
        }
        return inkList;
    }

    // [중요 수정] 중도 포기(나가기) 시에는 확실하게 세션을 정리하도록 수정
    public void forfeit(ClientHandler quitter, String reason) {
        ClientHandler opponent = (quitter == playerA) ? playerB : playerA;

        // [수정] 게임이 끝난 상태(재경기 대기 중)에서 나갈 경우
        if (!running) {
            // [Fix] isConnected() 체크를 제거하여, 상대방이 남아있다면 무조건 전송 시도
            if (opponent != null) {
                Message leftMsg = Message.of("GAME_OPPONENT_LEFT");
                leftMsg.sessionId = this.id;
                opponent.send(leftMsg);
            }
            cleanupSession();
            return;
        }

        ClientHandler winner = (quitter == playerA) ? playerB : playerA;
        ClientHandler loser = (quitter == playerA) ? playerA : playerB;

        finish(winner, loser, reason);
<<<<<<< HEAD
=======

        // [문제 해결] 게임 도중 나갈 때도 남은 사람에게 "상대방이 나갔습니다" 메시지 전송
        if (opponent != null) {
            Message leftMsg = Message.of("GAME_OPPONENT_LEFT");
            leftMsg.sessionId = this.id;
            opponent.send(leftMsg);
        }

        // [중요] 게임 도중 기권한 경우는 재경기 불가능 -> 즉시 세션 정리
>>>>>>> d6d2377ba104a4e4043e4110b552f49cbb2c54ad
        cleanupSession();
    }

    private void finish(ClientHandler winner, ClientHandler loser, String reason) {
        if (!running) return;
        running = false;
        if (ticker != null) ticker.cancel(false);

        recordGameResults(winner, loser);

        int scoreA = coreLogic.getScore(TileState.PLAYER_A);
        int scoreB = coreLogic.getScore(TileState.PLAYER_B);
        boolean isDraw = (winner == null && loser == null);

        sendEnd(playerA, (playerA == winner), isDraw, reason, scoreA, scoreB);
        sendEnd(playerB, (playerB == winner), isDraw, reason, scoreB, scoreA);
<<<<<<< HEAD
=======

        // [핵심 수정 사항 유지]
        // 여기서 cleanupSession()을 호출하던 코드를 삭제했습니다.
        // 이제 게임이 끝나도 세션은 살아있어서 재경기 요청을 받을 수 있습니다.
>>>>>>> d6d2377ba104a4e4043e4110b552f49cbb2c54ad
    }

    private void sendEnd(ClientHandler player, boolean isWinner, boolean isDraw, String reason, int myScore, int oppScore) {
        if (player == null || !player.isConnected()) return;

        Message end = Message.of("GAME_END_BROADCAST");
        end.sessionId = id;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("gameType", gameType);

        String resultStr;
        if (isDraw) resultStr = "무승부";
        else if (isWinner) resultStr = "승리";
        else resultStr = "패배";

        payload.put("result", resultStr);
        payload.put("message", reason);
        payload.put("scoreSelf", myScore);
        payload.put("scoreOpponent", oppScore);

        end.data = payload;
        player.send(end);
    }

    private void recordGameResults(ClientHandler winner, ClientHandler loser) {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            String winnerId = (winner != null) ? winner.getLoggedInUserId() : null;
            String loserId = (loser != null) ? loser.getLoggedInUserId() : null;

            if (winnerId != null && loserId != null) {
                dbManager.updateGameRecord(winnerId, gameType, true);
                dbManager.updateGameRecord(loserId, gameType, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}