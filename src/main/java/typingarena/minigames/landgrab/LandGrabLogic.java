package typingarena.minigames.landgrab;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

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

/**
 * [수정됨]
 * - setNewWord: 중복 단어가 나오지 않도록 수정
 * - submitAnswer, aiCaptureTile: 획득한 타일의 단어를 비우도록 수정
 */
public class LandGrabLogic {

    // ===== 1. 단어장 로딩 (GameLogic.java와 100% 동일) =====
    private static final String WORD_RESOURCE = "words/ko.json";
    private static final String[] DEFAULT_WORDS = {"land", "grab", "typing", "arena", "game"};
    private static final List<String> WORD_POOL = loadWordPool();

    private final Random rnd = new Random();

    // ===== 2. 게임 상태 정의 =====
    public enum TileState { EMPTY, PLAYER, AI }
    public enum WordModifier { NEUTRAL, TRAP }

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

    // ===== 3. 게임 밸런스 값 =====
    private int aiTickTimerMs = 0;
    private static final int AI_CAPTURE_INTERVAL_MS = 2_000;

    // ===== 4. 공개 Getter =====
    public TileState getTileState(int r, int c) { return grid[r][c]; }
    public String getWord(int r, int c) { return wordGrid[r][c]; }
    public WordModifier getModifier(int r, int c) { return modifierGrid[r][c]; }
    public int getScorePlayer() { return scorePlayer; }
    public int getScoreAI() { return scoreAI; }
    public int getCombo() { return combo; }
    public int getTimeMs() { return timeMs; }
    public boolean isRunning() { return running; }
    public LandGrabEffects getEffects() { return effects; }

    // ===== 5. 게임 흐름 제어 =====
    public void startGame() {
        scorePlayer = 0;
        scoreAI = 0;
        combo = 0;
        timeMs = 60_000;
        running = true;
        effects.clearAll();
        aiTickTimerMs = AI_CAPTURE_INTERVAL_MS;

        // 그리드 초기화
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                grid[r][c] = TileState.EMPTY;
                setNewWord(r, c); // 새 단어로 채우기 (중복 방지 로직 적용됨)
            }
        }
    }

    public String tick() {
        if (!running) return null;

        timeMs -= 100;

        // AI 로직 처리
        aiTickTimerMs -= 100;
        if (aiTickTimerMs <= 0) {
            aiCaptureTile();
            aiTickTimerMs = AI_CAPTURE_INTERVAL_MS;
        }

        // 종료 조건 판정
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
     * [수정됨] AI가 타일을 획득하고, 그 칸을 비움
     */
    private void aiCaptureTile() {
        if (!running) return;

        List<int[]> emptyTiles = new ArrayList<>();
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                if (grid[r][c] == TileState.EMPTY) {
                    emptyTiles.add(new int[]{r, c});
                }
            }
        }

        if (!emptyTiles.isEmpty()) {
            int[] target = emptyTiles.get(rnd.nextInt(emptyTiles.size()));
            int r = target[0];
            int c = target[1];

            grid[r][c] = TileState.AI;
            scoreAI++;

            // [버그 2 수정] 획득한 칸의 단어를 비워서 다시는 못 치게 함
            wordGrid[r][c] = "";
            modifierGrid[r][c] = WordModifier.NEUTRAL;
        }
    }

    /**
     * [수정됨] 플레이어가 타일을 획득하고, 그 칸을 비움
     */
    public boolean submitAnswer(String typed) {
        if (!running || typed == null || typed.isEmpty()) return false;

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                // 비어있고, 단어가 일치하는 칸을 찾으면
                if (grid[r][c] == TileState.EMPTY && typed.equalsIgnoreCase(wordGrid[r][c])) {
                    // 1. 점령
                    grid[r][c] = TileState.PLAYER;
                    scorePlayer++;
                    combo++;

                    // 2. 아이템 효과 적용
                    if (modifierGrid[r][c] == WordModifier.TRAP) {
                        effects.activateBlind(3_000); // 3초 먹물
                    }

                    // [버그 2 수정] 획득한 칸의 단어를 비워서 다시는 못 치게 함
                    wordGrid[r][c] = "";
                    modifierGrid[r][c] = WordModifier.NEUTRAL;

                    return true;
                }
            }
        }

        combo = 0;
        return false;
    }


    // ===== 6. 단어 생성 로직 =====

    /**
     * [버그 1 수정] 중복되지 않는 새 단어를 (r, c)에 할당
     */
    private void setNewWord(int r, int c) {
        String newWord;
        boolean isDuplicate;
        int tryCount = 0; // 무한 루프 방지 (단어 풀이 100개 미만일 경우)

        do {
            newWord = randomWord();
            isDuplicate = false;
            tryCount++;

            // 다른 *비어있는* 타일 중에 이 단어가 이미 있는지 검사
            for (int i = 0; i < GRID_SIZE; i++) {
                for (int j = 0; j < GRID_SIZE; j++) {
                    // (r, c) 자기 자신 칸이거나, 비어있는 칸이 아니면 검사 스킵
                    if ((i == r && j == c) || grid[i][j] != TileState.EMPTY) {
                        continue;
                    }
                    // 다른 비어있는 칸의 단어와 중복되는지 확인
                    if (newWord.equalsIgnoreCase(wordGrid[i][j])) {
                        isDuplicate = true;
                        break;
                    }
                }
                if (isDuplicate) break;
            }
        } while (isDuplicate && tryCount < 200); // 200번 시도 후에도 중복이면 그냥 사용

        wordGrid[r][c] = newWord;
        modifierGrid[r][c] = randomWordModifier();
    }

    private String randomWord() {
        if (WORD_POOL.isEmpty()) return "ERROR";
        return WORD_POOL.get(rnd.nextInt(WORD_POOL.size()));
    }

    private WordModifier randomWordModifier() {
        if (rnd.nextDouble() < 0.2) {
            return WordModifier.TRAP;
        }
        return WordModifier.NEUTRAL;
    }

    // ===== 7. 단어장 로더 (수정됨) =====
    private static List<String> loadWordPool() {
        // [오류 수정] GameLogic.class -> LandGrabLogic.class
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