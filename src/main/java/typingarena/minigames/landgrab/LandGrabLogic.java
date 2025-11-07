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
import java.util.List; // [신규] List 임포트
import java.util.Random;
import java.util.function.Consumer;

/**
 * [대규모 수정됨]
 * 1. ... (이전 수정 사항들) ...
 * 7. [신규] '먹물!' 텍스트 애니메이션 콜백 (onInkSplashCallback) 추가
 * 8. [신규] 먹물 효과가 여러 개 중첩되도록 List<BlindedTile> 로직 반영
 */
public class LandGrabLogic {

    // ===== 1. 단어장 로딩 (동일) =====
    private static final String WORD_RESOURCE = "words/ko.json";
    private static final String[] DEFAULT_WORDS = {"land", "grab", "typing", "arena", "game"};
    private static final List<String> WORD_POOL = loadWordPool();
    private final Random rnd = new Random();

    // ===== 2. 게임 상태 정의 (동일) =====
    public enum TileState { EMPTY, PLAYER, AI }
    public enum WordModifier { NEUTRAL, TRAP, BUFF }

    public static final int GRID_SIZE = 10;
    private final TileState[][] grid = new TileState[GRID_SIZE][GRID_SIZE];
    private final String[][] wordGrid = new String[GRID_SIZE][GRID_SIZE];
    private final WordModifier[][] modifierGrid = new WordModifier[GRID_SIZE][GRID_SIZE];

    private int scorePlayer = 0;
    private int scoreAI = 0;
    private int combo = 0;
    private int timeMs = 60_000;
    private boolean running = false;
    private final LandGrabEffects effects = new LandGrabEffects();

    private Consumer<int[]> onSplashCallback = (coords) -> {};
    private Consumer<int[]> onInkSplashCallback = (coords) -> {};

    // ===== 3. 게임 밸런스 값 (동일) =====
    private int aiTickTimerMs = 0;
    private static final int AI_CAPTURE_INTERVAL_MS = 2_000;
    private static final int BLIND_DURATION_MS = 3_000;

    // ===== 4. 공개 Getter/Setter (동일) =====
    public TileState getTileState(int r, int c) { return grid[r][c]; }
    public String getWord(int r, int c) { return wordGrid[r][c]; }
    public WordModifier getModifier(int r, int c) { return modifierGrid[r][c]; }
    public int getScorePlayer() { return scorePlayer; }
    public int getScoreAI() { return scoreAI; }
    public int getCombo() { return combo; }
    public int getTimeMs() { return timeMs; }
    public boolean isRunning() { return running; }
    public LandGrabEffects getEffects() { return effects; }

    public void setOnSplashCallback(Consumer<int[]> callback) {
        this.onSplashCallback = (callback != null) ? callback : (coords) -> {};
    }

    public void setOnInkSplashCallback(Consumer<int[]> callback) {
        this.onInkSplashCallback = (callback != null) ? callback : (coords) -> {};
    }

    // ===== 5. 게임 흐름 제어 (동일) =====
    public void startGame() {
        scorePlayer = 0;
        scoreAI = 0;
        combo = 0;
        timeMs = 60_000;
        running = true;
        effects.clearAll();
        aiTickTimerMs = AI_CAPTURE_INTERVAL_MS;

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                grid[r][c] = TileState.EMPTY;
                setNewWord(r, c, TileState.EMPTY);
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

        if (scorePlayer + scoreAI == GRID_SIZE * GRID_SIZE) {
            running = false;
            if (scorePlayer > scoreAI) return "승리! 모든 땅을 차지했습니다.";
            if (scoreAI > scorePlayer) return "패배... AI에게 모두 빼앗겼습니다.";
            return "무승부!";
        }
        return null;
    }

    /**
     * [수정됨] AI가 먹물 타일을 피해서 점령하도록 (List 기반)
     */
    private void aiCaptureTile() {
        if (!running) return;

        List<int[]> emptyTiles = new ArrayList<>();
        List<int[]> playerTiles = new ArrayList<>();

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                // [수정] effects.isTileBlinded(r, c)로 해당 타일이 가려졌는지 확인
                if (effects.isTileBlinded(r, c)) {
                    continue;
                }

                switch(grid[r][c]) {
                    case EMPTY: emptyTiles.add(new int[]{r,c}); break;
                    case PLAYER: playerTiles.add(new int[]{r,c}); break;
                    default: break;
                }
            }
        }

        int[] target = null;
        if (!emptyTiles.isEmpty()) {
            target = emptyTiles.get(rnd.nextInt(emptyTiles.size()));
        } else if (!playerTiles.isEmpty()) {
            target = playerTiles.get(rnd.nextInt(playerTiles.size()));
        }

        if (target != null) {
            int r = target[0];
            int c = target[1];
            TileState oldState = grid[r][c];
            TileState newState = TileState.AI;

            switch(oldState) {
                case EMPTY:
                    grid[r][c] = TileState.AI;
                    scoreAI++;
                    break;
                case PLAYER:
                    grid[r][c] = TileState.EMPTY;
                    scorePlayer--;
                    newState = TileState.EMPTY;
                    break;
                default: break;
            }
            setNewWord(r, c, newState);
        }
    }

    /**
     * [대규모 수정] 플레이어 입력 처리 (List 기반 먹물 타일 확인)
     */
    public boolean submitAnswer(String typed) {
        if (!running || typed == null || typed.isEmpty()) return false;

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                // 이미 내 땅인 곳은 무시
                if (grid[r][c] == TileState.PLAYER) continue;

                // [수정] effects.isTileBlinded(r, c)로 해당 타일이 가려졌는지 확인
                if (effects.isTileBlinded(r, c)) {
                    continue;
                }

                if (typed.equalsIgnoreCase(wordGrid[r][c])) {
                    TileState oldState = grid[r][c];
                    TileState newState = TileState.PLAYER;
                    boolean captured = false;

                    switch(oldState) {
                        case EMPTY:
                            grid[r][c] = TileState.PLAYER;
                            scorePlayer++;
                            captured = true;
                            break;
                        case AI:
                            grid[r][c] = TileState.EMPTY;
                            scoreAI--;
                            newState = TileState.EMPTY;
                            captured = true;
                            break;
                        default: break;
                    }

                    if (captured) {
                        combo++;
                        WordModifier modifier = modifierGrid[r][c];

                        setNewWord(r, c, newState);

                        if (modifier == WordModifier.TRAP) {
                            // [수정] applyBlindTileEffect가 이제 List를 보고 빈 곳에 추가함
                            applyBlindTileEffect();
                            effects.recordItemActivation(ItemType.TRAP_BLIND);
                            onInkSplashCallback.accept(new int[]{r, c});

                        } else if (modifier == WordModifier.BUFF) {
                            applySplashCapture(r, c);
                            effects.recordItemActivation(ItemType.BUFF_SPLASH);
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
     * [수정됨] 먹물 트랩 발동 시, '내 땅이 아니고' + '이미 가려지지 않은' 타일 중 하나를 랜덤하게 가림
     */
    private void applyBlindTileEffect() {
        List<int[]> validTargets = new ArrayList<>();
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                // [수정] 내 땅(PLAYER)이 아니고, 현재 이미 가려진 타일도 아닌 경우
                if (grid[r][c] != TileState.PLAYER && !effects.isTileBlinded(r, c)) {
                    validTargets.add(new int[]{r, c});
                }
            }
        }

        if (!validTargets.isEmpty()) {
            int[] target = validTargets.get(rnd.nextInt(validTargets.size()));
            effects.activateBlindTile(target[0], target[1], BLIND_DURATION_MS);
        }
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
                case AI:
                    grid[nr][nc] = TileState.EMPTY;
                    scoreAI--;
                    newState = TileState.EMPTY;
                    break;
                default: break;
            }
            setNewWord(nr, nc, newState);
        }
    }


    // ===== 6. 단어 생성 로직 (수정) =====

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

    /**
     * [수정됨] 단어 중복 검사 (List 기반 먹물 타일 확인)
     */
    private boolean isWordDuplicateOnGrid(String word, int r, int c) {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                if (i == r && j == c) continue;

                // [수정] effects.isTileBlinded(i, j)로 해당 타일이 가려졌는지 확인
                if (effects.isTileBlinded(i, j)) {
                    continue;
                }

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