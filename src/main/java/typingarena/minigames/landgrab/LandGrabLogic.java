package typingarena.minigames.landgrab;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import typingarena.minigames.landgrab.LandGrabEffects.ItemType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer; // [신규] 콜백을 위해 임포트

/**
 * [대규모 수정됨]
 * 1. [룰 1] TileState에서 'NEUTRAL' (회색) 제거
 * 2. [룰 1] submitAnswer/aiCapture: 땅 뺏기 룰을 'EMPTY'(흰색)로 변경
 * 3. [룰 2] applySplashCapture: 스플래시 효과도 'EMPTY'(흰색)로 변경
 * 4. [수정] '스플래시!' 텍스트 피드백을 'effects.activateSplashText()' 대신 'onSplashCallback' 콜백 호출로 변경
 * 5. '중립' 점수 관련 로직 모두 제거
 */
public class LandGrabLogic {

    // ===== 1. 단어장 로딩 (동일) =====
    private static final String WORD_RESOURCE = "words/ko.json";
    private static final String[] DEFAULT_WORDS = {"land", "grab", "typing", "arena", "game"};
    private static final List<String> WORD_POOL = loadWordPool();
    private final Random rnd = new Random();

    // ===== 2. 게임 상태 정의 (수정) =====
    // [수정] NEUTRAL (회색 중립 지대) 제거
    public enum TileState { EMPTY, PLAYER, AI }
    public enum WordModifier { NEUTRAL, TRAP, BUFF }

    public static final int GRID_SIZE = 10;
    private final TileState[][] grid = new TileState[GRID_SIZE][GRID_SIZE];
    private final String[][] wordGrid = new String[GRID_SIZE][GRID_SIZE];
    private final WordModifier[][] modifierGrid = new WordModifier[GRID_SIZE][GRID_SIZE];

    private int scorePlayer = 0;
    private int scoreAI = 0;
    // private int scoreNeutral = 0; // [수정] 중립 타일 카운트 제거
    private int combo = 0;
    private int timeMs = 60_000;
    private boolean running = false;
    private final LandGrabEffects effects = new LandGrabEffects();

    // [신규] 애니메이션 콜백 (좌표 [r, c]를 전달)
    private Consumer<int[]> onSplashCallback = (coords) -> {}; // Null 방지

    // ===== 3. 게임 밸런스 값 (동일) =====
    private int aiTickTimerMs = 0;
    private static final int AI_CAPTURE_INTERVAL_MS = 2_000;

    // ===== 4. 공개 Getter/Setter (수정) =====
    public TileState getTileState(int r, int c) { return grid[r][c]; }
    public String getWord(int r, int c) { return wordGrid[r][c]; }
    public WordModifier getModifier(int r, int c) { return modifierGrid[r][c]; }
    public int getScorePlayer() { return scorePlayer; }
    public int getScoreAI() { return scoreAI; }
    // public int getScoreNeutral() { return scoreNeutral; } // [수정] 제거
    public int getCombo() { return combo; }
    public int getTimeMs() { return timeMs; }
    public boolean isRunning() { return running; }
    public LandGrabEffects getEffects() { return effects; }

    /**
     * [신규] LandGrabGame에서 애니메이션 콜백을 주입하기 위한 메서드
     */
    public void setOnSplashCallback(Consumer<int[]> callback) {
        this.onSplashCallback = (callback != null) ? callback : (coords) -> {};
    }

    // ===== 5. 게임 흐름 제어 (수정) =====
    public void startGame() {
        scorePlayer = 0;
        scoreAI = 0;
        // scoreNeutral = 0; // [수정] 제거
        combo = 0;
        timeMs = 60_000;
        running = true;
        effects.clearAll();
        aiTickTimerMs = AI_CAPTURE_INTERVAL_MS;

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                grid[r][c] = TileState.EMPTY;
                setNewWord(r, c, TileState.EMPTY); // 새 단어로 채우기
            }
        }
    }

    public String tick() {
        if (!running) return null;
        timeMs -= 100;

        aiTickTimerMs -= 100;
        if (aiTickTimerMs <= 0) {
            aiCaptureTile();
            aiTickTimerMs = AI_CAPTURE_INTERVAL_MS;
        }

        if (timeMs <= 0) {
            running = false;
            if (scorePlayer > scoreAI) return "승리! 더 많은 땅을 차지했습니다.";
            if (scoreAI > scorePlayer) return "패배... AI가 더 많습니다.";
            return "무승부!";
        }

        // [수정] 중립 점수 제거
        if (scorePlayer + scoreAI == GRID_SIZE * GRID_SIZE) {
            running = false;
            if (scorePlayer > scoreAI) return "승리! 모든 땅을 차지했습니다.";
            if (scoreAI > scorePlayer) return "패배... AI에게 모두 빼앗겼습니다.";
            return "무승부!";
        }
        return null;
    }

    /**
     * [수정됨] AI가 땅 뺏기 룰에 따라 타일을 점령
     */
    private void aiCaptureTile() {
        if (!running) return;

        // AI는 자신에게 유리한 타일(EMPTY, PLAYER 순)을 우선 점령
        List<int[]> emptyTiles = new ArrayList<>();
        List<int[]> playerTiles = new ArrayList<>();
        // List<int[]> neutralTiles = new ArrayList<>(); // [수정] 제거

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                switch(grid[r][c]) {
                    case EMPTY: emptyTiles.add(new int[]{r,c}); break;
                    case PLAYER: playerTiles.add(new int[]{r,c}); break;
                    // case NEUTRAL: neutralTiles.add(new int[]{r,c}); break; // [수정] 제거
                    default: break;
                }
            }
        }

        int[] target = null;
        if (!emptyTiles.isEmpty()) {
            target = emptyTiles.get(rnd.nextInt(emptyTiles.size()));
        } else if (!playerTiles.isEmpty()) { // 뺏을 플레이어 땅이 있다면
            target = playerTiles.get(rnd.nextInt(playerTiles.size()));
        }
        // [수정] 중립 타일 점령 로직 제거

        if (target != null) {
            int r = target[0];
            int c = target[1];
            TileState oldState = grid[r][c];
            TileState newState = TileState.AI; // 기본은 AI 땅으로

            switch(oldState) {
                case EMPTY:
                    grid[r][c] = TileState.AI;
                    scoreAI++;
                    break;
                case PLAYER: // [룰 1 수정] 플레이어 땅 -> '흰색'(EMPTY)으로
                    grid[r][c] = TileState.EMPTY;
                    scorePlayer--;
                    newState = TileState.EMPTY;
                    break;
                // [수정] 중립 점령 로직 제거
                default: break;
            }
            setNewWord(r, c, newState); // 새 단어 생성
        }
    }

    /**
     * [대규모 수정] 플레이어 입력 처리 (새 룰 + 콜백 적용)
     */
    public boolean submitAnswer(String typed) {
        if (!running || typed == null || typed.isEmpty()) return false;

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                // 이미 내 땅인 곳은 무시 (단어는 보이지만 칠 필요 없음)
                if (grid[r][c] == TileState.PLAYER) continue;

                if (typed.equalsIgnoreCase(wordGrid[r][c])) {
                    TileState oldState = grid[r][c];
                    TileState newState = TileState.PLAYER; // 기본은 내 땅으로
                    boolean captured = false;

                    switch(oldState) {
                        case EMPTY:
                            grid[r][c] = TileState.PLAYER;
                            scorePlayer++;
                            captured = true;
                            break;
                        case AI: // [룰 1 수정] AI 땅 -> '흰색'(EMPTY)으로
                            grid[r][c] = TileState.EMPTY;
                            scoreAI--;
                            newState = TileState.EMPTY;
                            captured = true; // (흰색으로 뺏은 것도 성공)
                            break;
                        // [수정] 중립 점령 로직 제거
                        default: break;
                    }

                    if (captured) {
                        combo++;
                        WordModifier modifier = modifierGrid[r][c]; // 캡처 전 모디파이어 기억

                        setNewWord(r, c, newState);

                        if (modifier == WordModifier.TRAP) {
                            effects.activateBlind(3_000);
                            effects.recordItemActivation(ItemType.TRAP_BLIND);
                        } else if (modifier == WordModifier.BUFF) {
                            applySplashCapture(r, c);
                            effects.recordItemActivation(ItemType.BUFF_SPLASH);

                            // [수정] 텍스트 타이머 대신, 애니메이션 콜백 호출 (좌표 전달)
                            onSplashCallback.accept(new int[]{r, c});
                        }

                        return true; // 성공
                    }
                }
            }
        }

        combo = 0;
        return false; // 오답
    }

    /**
     * [수정됨] 룰 2: 인접 타일 획득 (버프 아이템)
     */
    private void applySplashCapture(int r, int c) {
        int[] dr = {-1, 1, 0, 0};
        int[] dc = {0, 0, -1, 1};
        List<int[]> validTargets = new ArrayList<>();

        for(int i = 0; i < 4; i++) {
            int nr = r + dr[i];
            int nc = c + dc[i];
            if (nr >= 0 && nr < GRID_SIZE && nc >= 0 && nc < GRID_SIZE) {
                if (grid[nr][nc] != TileState.PLAYER) {
                    validTargets.add(new int[]{nr, nc});
                }
            }
        }

        if (!validTargets.isEmpty()) {
            int[] target = validTargets.get(rnd.nextInt(validTargets.size()));
            int nr = target[0];
            int nc = target[1];
            TileState oldState = grid[nr][nc];
            TileState newState = TileState.PLAYER;

            switch(oldState) {
                case EMPTY:
                    grid[nr][nc] = TileState.PLAYER;
                    scorePlayer++;
                    break;
                case AI: // [룰 1 수정] 스플래시로 AI 땅 칠 때도 '흰색'(EMPTY)으로
                    grid[nr][nc] = TileState.EMPTY;
                    scoreAI--;
                    newState = TileState.EMPTY;
                    break;
                // [수정] 중립 로직 제거
                default: break;
            }
            setNewWord(nr, nc, newState); // 스플래시 타격된 곳도 새 단어
        }
    }


    // ===== 6. 단어 생성 로직 (동일) =====

    private void setNewWord(int r, int c, TileState currentState) {
        String newWord;
        int tryCount = 0;
        do {
            newWord = randomWord();
            tryCount++;
            if (isWordDuplicateOnGrid(newWord, r, c)) {
                newWord = null;
            }
        } while (newWord == null && tryCount < 200);

        if (newWord == null) newWord = randomWord();

        wordGrid[r][c] = newWord;
        modifierGrid[r][c] = randomWordModifier(currentState);
    }

    private boolean isWordDuplicateOnGrid(String word, int r, int c) {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                if (i == r && j == c) continue;
                if (grid[i][j] != TileState.PLAYER) {
                    if (word.equalsIgnoreCase(wordGrid[i][j])) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private String randomWord() {
        if (WORD_POOL.isEmpty()) return "ERROR";
        return WORD_POOL.get(rnd.nextInt(WORD_POOL.size()));
    }

    private WordModifier randomWordModifier(TileState currentState) {
        if (currentState == TileState.PLAYER) {
            return WordModifier.NEUTRAL;
        }
        double roll = rnd.nextDouble();
        if (roll < 0.15) {
            return WordModifier.BUFF;
        } else if (roll < 0.30) {
            return WordModifier.TRAP;
        }
        return WordModifier.NEUTRAL;
    }

    // ===== 7. 단어장 로더 (동일) =====
    private static List<String> loadWordPool() {
        try (InputStream in = LandGrabLogic.class.getClassLoader().getResourceAsStream(WORD_RESOURCE)) {
            if (in == null) {
                System.err.println(WORD_RESOURCE + " 리소스를 찾을 수 없습니다. 기본 단어를 사용합니다.");
                return new ArrayList<>(Arrays.asList(DEFAULT_WORDS));
            }
            try (Reader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                Gson gson = new Gson();
                WordList data = gson.fromJson(reader, WordList.class);
                if (data == null || data.words == null || data.words.isEmpty()) {
                    return new ArrayList<>(Arrays.asList(DEFAULT_WORDS));
                }

                List<String> words = new ArrayList<>();
                for (String word : data.words) {
                    if (word != null && !word.trim().isEmpty()) {
                        words.add(word.trim());
                    }
                }
                return Collections.unmodifiableList(words);
            }
        } catch (IOException | JsonSyntaxException | JsonIOException e) {
            e.printStackTrace();
            return new ArrayList<>(Arrays.asList(DEFAULT_WORDS));
        }
    }
    private static final class WordList {
        List<String> words;
    }
}