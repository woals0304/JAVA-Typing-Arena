package typingarena.server.session;

// core (엔진) 및 net(메시지) import
import typingarena.core.landgrab.LandGrabEffects;
import typingarena.core.landgrab.LandGrabLogic;
import typingarena.net.Message;
// 서버 공용 모듈 import
import typingarena.server.ClientHandler;
import typingarena.server.core.ServerContext;
import typingarena.server.db.DatabaseManager;

// Java 유틸리티 import
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * [신규] 서버 측 '땅따먹기' 멀티플레이 제어기(Controller)
 * - 'TugOfWarSession'과 달리 'core.landgrab.LandGrabLogic' (엔진)을 재사용합니다.
 */
public class LandGrabSession {

    private final String id = UUID.randomUUID().toString();
    private final ServerContext context;
    private final String gameType = "LAND_GRAB"; // [수정] DB에 기록될 게임 타입

    // --- Model ---
    private final LandGrabLogic coreLogic = new LandGrabLogic(); // [중요] 핵심 엔진 사용

    // --- Players ---
    private final ClientHandler playerA;
    private final ClientHandler playerB;
    // (TODO: 2P(playerB)의 입력을 coreLogic.submitAnswer(word, playerB)처럼
    //        처리하려면 coreLogic의 대대적인 수정이 필요합니다.
    //        지금은 1P(playerA) vs AI(coreLogic.aiCaptureTile)로 진행합니다.)

    // --- Controller ---
    private int timeMs = 60_000;
    private boolean running = true;
    private ScheduledFuture<?> ticker; // 게임 루프 타이머
    private int aiTickTimerMs = 0; // (TODO: 2P 로직으로 대체 필요)
    private static final int AI_CAPTURE_INTERVAL_MS = 2_000;

    public LandGrabSession(ServerContext context, ClientHandler a, ClientHandler b) {
        this.context = context;
        this.playerA = a;
        this.playerB = b;
    }

    public String getId() {
        return id;
    }

    /**
     * Matchmaker가 호출: 게임 시작!
     */
    public void start() {
        // 1. Model(엔진) 상태 초기화
        coreLogic.startGame();

        // 2. Controller 상태 초기화
        timeMs = 60_000;
        running = true;
        aiTickTimerMs = AI_CAPTURE_INTERVAL_MS;

        // 3. 게임 루프 시작 (100ms마다 onTick 실행)
        ticker = context.getScheduler().scheduleAtFixedRate(this::onTick, 100, 100, TimeUnit.MILLISECONDS);

        // 4. 모든 클라이언트에게 "게임 시작" 메시지 전파
        Message startMsg = Message.of("GAME_START_BROADCAST");
        startMsg.sessionId = this.id;

        // [수정] 게임 시작 시 "현재 그리드 상태"를 함께 보냅니다.
        startMsg.data = Map.of(
                "gameType", this.gameType,
                "timeMs", (double) timeMs,
                "players", List.of(playerA.getNickname(), playerB.getNickname()),
                // [신규] 클라이언트가 맵을 그릴 수 있도록 초기 상태 전송
                "grid", buildGridData(coreLogic.getGrid(), LandGrabLogic.TileState.class),
                "words", buildGridData(coreLogic.getWordGrid(), String.class),
                "modifiers", buildGridData(coreLogic.getModifierGrid(), LandGrabLogic.WordModifier.class),
                "ink_tiles", buildBlindedTilesData()
        );
        broadcast(startMsg); // 방의 모두에게 전송

        // 5. 각 클라이언트의 현재 세션 ID 설정
        playerA.setCurrentSession(id);
        playerB.setCurrentSession(id);
    }

    /**
     * ClientHandler가 호출: 플레이어의 단어 입력
     */
    public void handleWord(ClientHandler client, String word) {
        if (!running) return;

        // TODO: 지금은 1P(playerA)만 입력받음
        if (client != playerA) {
            return; // 2P의 입력은 일단 무시
        }

        LandGrabLogic.SubmitResult result = coreLogic.submitAnswer(word);

        Map<String, Object> animationTrigger = null;
        if (result.resultCode() == 2) { // 2 = 버프
            animationTrigger = Map.of("type", "SPLASH", "r", result.r(), "c", result.c());
        } else if (result.resultCode() == 3) { // 3 = 트랩
            animationTrigger = Map.of("type", "INK_SPLASH", "r", result.r(), "c", result.c());
        }

        // [수정] 변경된 타일만 보낼 수도 있지만, 일단은 '전체 갱신'으로 처리
        broadcastUpdate(animationTrigger);
    }

    /**
     * 100ms마다 게임 루프가 호출
     */
    private void onTick() {
        if (!running) return;
        try {
            // 1. 시간 감소
            timeMs -= 100;

            // 2. AI 타이머 작동 (TODO: 이 로직은 Player B의 입력으로 대체되어야 함)
            aiTickTimerMs -= 100;

            boolean stateChanged = false; // [신규] 상태가 변경됐을 때만 전송

            if (aiTickTimerMs <= 0) {
                coreLogic.aiCaptureTile(); // 엔진의 AI 로직 호출
                aiTickTimerMs = AI_CAPTURE_INTERVAL_MS;
                stateChanged = true; // AI가 타일을 먹어서 상태 변경
            }

            // 3. 게임 종료 조건 확인
            String resultMessage = null;
            ClientHandler winner = null, loser = null;

            if (timeMs <= 0) {
                running = false;
                resultMessage = "시간 종료!";
                if (coreLogic.getScorePlayer() > coreLogic.getScoreAI()) { winner = playerA; loser = playerB; }
                else if (coreLogic.getScoreAI() > coreLogic.getScorePlayer()) { winner = playerB; loser = playerA; }
            }
            // [수정] !running 체크 추가 (시간 종료와 동시에 타일 점령 시 중복 방지)
            if (!running && (coreLogic.getScorePlayer() + coreLogic.getScoreAI() == LandGrabLogic.GRID_SIZE * LandGrabLogic.GRID_SIZE)) {
                running = false; // (timeMs > 0 이어도 종료)
                resultMessage = "모든 타일 점령!";
                if (coreLogic.getScorePlayer() > coreLogic.getScoreAI()) { winner = playerA; loser = playerB; }
                else { winner = playerB; loser = playerA; }
            }

            // 4. 게임 종료 처리
            if (!running) {
                finish(winner, loser, resultMessage);
            } else if (stateChanged) {
                // (AI가 타일을 먹었으므로) 갱신된 상태 전파
                broadcastUpdate(null); // 애니메이션 없음
            }
            // (아무 일도 없으면 갱신 안 함 -> 네트워크 트래픽 절약)

        } catch (Exception e) {
            e.printStackTrace();
            if (ticker != null) ticker.cancel(true); // 오류 시 루프 중지
        }
    }

    /**
     * [신규] 현재 '엔진'의 상태를 JSON(Map)으로 만들어 전파 (TODO 제거 완료)
     */
    private void broadcastUpdate(Map<String, Object> animationTrigger) {
        Map<String, Object> data = new LinkedHashMap<>(); // 순서 보장
        data.put("game", gameType);
        data.put("timeMs", (double) timeMs);

        // [신규] LandGrabViewState(Map)이 기대하는 데이터 구조
        data.put("grid", buildGridData(coreLogic.getGrid(), LandGrabLogic.TileState.class));
        data.put("words", buildGridData(coreLogic.getWordGrid(), String.class));
        data.put("modifiers", buildGridData(coreLogic.getModifierGrid(), LandGrabLogic.WordModifier.class));
        data.put("ink_tiles", buildBlindedTilesData());

        data.put("scores", Map.of(
                playerA.getNickname(), coreLogic.getScorePlayer(),
                playerB.getNickname(), coreLogic.getScoreAI()
        ));
        data.put("animation_trigger", animationTrigger);

        Message updateMsg = Message.of("GAME_UPDATE_BROADCAST");
        updateMsg.sessionId = this.id;
        updateMsg.data = data;
        broadcast(updateMsg);
    }

    // --- [신규] 헬퍼: Grid 데이터를 List<List<String>>으로 변환 ---
    private <T> List<List<String>> buildGridData(T[][] grid, Class<T> enumClass) {
        List<List<String>> rowList = new ArrayList<>();
        int size = LandGrabLogic.GRID_SIZE;
        for (int r = 0; r < size; r++) {
            List<String> colList = new ArrayList<>();
            for (int c = 0; c < size; c++) {
                if (grid[r][c] == null) {
                    colList.add(""); // (NPE 방어)
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

    // --- [신규] 헬퍼: 먹물 타일 데이터를 List<Map>으로 변환 ---
    private List<Map<String, Object>> buildBlindedTilesData() {
        List<Map<String, Object>> inkList = new ArrayList<>();
        // [수정] coreLogic에서 effects를 가져옴
        List<LandGrabEffects.BlindedTile> tiles = coreLogic.getEffects().getActiveBlindedTiles();
        for (LandGrabEffects.BlindedTile tile : tiles) {
            inkList.add(Map.of("r", tile.r(), "c", tile.c(), "until", tile.until()));
        }
        return inkList;
    }

    /**
     * 'TugOfWarSession'의 'forfeit' 메서드와 유사
     */
    public void forfeit(ClientHandler quitter, String reason) {
        if (!running) return;
        ClientHandler winner = (quitter == playerA) ? playerB : playerA;
        ClientHandler loser = (quitter == playerA) ? playerA : playerB;
        finish(winner, loser, reason);
    }

    /**
     * 'TugOfWarSession'의 'finish' 메서드와 유사
     */
    private void finish(ClientHandler winner, ClientHandler loser, String reason) {
        if (!running) return; // 이미 종료됨
        running = false;
        if (ticker != null) ticker.cancel(false); // 게임 루프 중지

        // 1. DB에 전적 기록
        recordGameResults(winner, loser);

        // 2. 각 클라이언트에게 종료 메시지 전송
        boolean isDraw = (winner == null && loser == null);
        sendEnd(playerA, (playerA == winner), isDraw, reason);
        sendEnd(playerB, (playerB == winner), isDraw, reason);

        // 3. 세션 정리
        context.getLandGrabSessions().remove(id); // [수정] LandGrab Map에서 제거
        playerA.setCurrentSession(null);
        playerB.setCurrentSession(null);
    }

    private void sendEnd(ClientHandler player, boolean isWinner, boolean isDraw, String reason) {
        Message end = Message.of("GAME_END_BROADCAST");
        end.sessionId = id;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("gameType", gameType);
        payload.put("result", isDraw ? "무승부" : (isWinner ? "승리" : "패배"));
        payload.put("message", reason);
        payload.put("scoreSelf", (player == playerA) ? coreLogic.getScorePlayer() : coreLogic.getScoreAI());
        payload.put("scoreOpponent", (player == playerA) ? coreLogic.getScoreAI() : coreLogic.getScorePlayer());
        end.data = payload;
        broadcast(end);
    }

    /**
     * 'TugOfWarSession'의 'recordGameResults' 메서드와 동일
     */
    private void recordGameResults(ClientHandler winner, ClientHandler loser) {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            String winnerId = (winner != null) ? winner.getLoggedInUserId() : null;
            String loserId = (loser != null) ? loser.getLoggedInUserId() : null;

            if (winnerId != null && loserId != null) {
                dbManager.updateGameRecord(winnerId, gameType, true); // 승리
                dbManager.updateGameRecord(loserId, gameType, false); // 패배
                System.out.println("[전적 기록] " + winnerId + " (승) vs " + loserId + " (패) - " + gameType);
            } else if (winner == null && loser == null) {
                System.out.println("[전적 기록] 무승부. - " + gameType);
            }
            // (기권 등 한 명만 있는 경우도 처리...)

        } catch (Exception e) {
            System.err.println("전적 기록 중 오류 발생 (LandGrab): " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 방 안의 모든 유저에게 메시지 전송
     */
    public void broadcast(Message msg) {
        if (playerA != null && playerA.isConnected()) playerA.send(msg);
        if (playerB != null && playerB.isConnected()) playerB.send(msg);
    }
}