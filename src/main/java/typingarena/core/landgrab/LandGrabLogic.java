package typingarena.core.landgrab;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import typingarena.core.landgrab.LandGrabEffects.ItemType; // [수정] Effects 경로 변경

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
 * [신규] 땅따먹기 게임의 핵심 로직 (싱글/멀티 공용 엔진)
 * UI, 타이머, 네트워크에 의존하지 않습니다.
 * (기존 LandGrabLogic.java에서 AI, 시간, UI 콜백을 제거)
 */
public class LandGrabLogic {

    // ===== [신규] submitAnswer의 결과를 담을 전용 데이터 객체 =====
    /**
     * @param resultCode 0=실패, 1=일반성공, 2=버프성공, 3=트랩성공
     * @param r 성공한 타일의 row
     * @param c 성공한 타일의 col
     */
    public record SubmitResult(int resultCode, int r, int c) {}
    // =========================================================

    // ===== 1. 단어장 로딩 (동일) =====
    private static final String WORD_RESOURCE = "words/ko.json";
    private static final String[] DEFAULT_WORDS = {"land", "grab", "typing", "arena", "game"};
    private static final List<String> WORD_POOL = loadWordPool();
    private final Random rnd = new Random();

    // ===== 2. 게임 상태 정의 (Model) =====
    public enum TileState { EMPTY, PLAYER, AI }
    public enum WordModifier { NEUTRAL, TRAP, BUFF }

    public static final int GRID_SIZE = 10;
    private final TileState[][] grid = new TileState[GRID_SIZE][GRID_SIZE];
    private final String[][] wordGrid = new String[GRID_SIZE][GRID_SIZE];
    private final WordModifier[][] modifierGrid = new WordModifier[GRID_SIZE][GRID_SIZE];

    private int scorePlayer = 0;
    private int scoreAI = 0;
    private int combo = 0;

    // [제거] timeMs, running, aiTickTimerMs (Controller의 역할이므로 제거)

    private final LandGrabEffects effects = new LandGrabEffects();

    // [제거] onSplashCallback, onInkSplashCallback (Controller의 역할이므로 제거)

    // ===== 3. 게임 밸런스 값 (동일) =====
    // [제거] AI_CAPTURE_INTERVAL_MS (Controller의 역할이므로 제거)
    private static final int BLIND_DURATION_MS = 3_000;

    // ===== 4. 공개 Getter (Model 상태) =====
    public TileState getTileState(int r, int c) { return grid[r][c]; }
    public String getWord(int r, int c) { return wordGrid[r][c]; }
    public WordModifier getModifier(int r, int c) { return modifierGrid[r][c]; }
    public int getScorePlayer() { return scorePlayer; }
    public int getScoreAI() { return scoreAI; }
    public int getCombo() { return combo; }
    public LandGrabEffects getEffects() { return effects; }

    // [제거] getTimeMs, isRunning (Controller가 관리)
    // [제거] 콜백 Setter (Controller가 관리)

    // ===== 5. 게임 흐름 제어 (Model) =====
    public void startGame() {
        scorePlayer = 0;
        scoreAI = 0;
        combo = 0;
        effects.clearAll();

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                grid[r][c] = TileState.EMPTY;
                setNewWord(r, c, TileState.EMPTY);
            }
        }
    }

    // [제거] tick() 메서드 (AI 타이머/시간/승패 판정은 Controller가 담당)

    /**
     * [신규] AI가 타일을 캡처하는 '엔진' 로직
     * (누가 호출할지는 Controller가 결정)
     */
    public void aiCaptureTile() {
        List<int[]> emptyTiles = new ArrayList<>();
        List<int[]> playerTiles = new ArrayList<>();

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
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
     * [수정] 플레이어 입력 처리 (Model)
     * 입력(typed)을 받아 상태(grid, score)만 변경하고,
     * 어떤 효과가 발생했는지 '결과(SubmitResult)'를 반환합니다.
     * @return SubmitResult (결과 코드 및 좌표 r, c)
     */
    public SubmitResult submitAnswer(String typed) { // [수정] 반환 타입 int -> SubmitResult
        if (typed == null || typed.isEmpty()) return new SubmitResult(0, -1, -1); // 0 = 실패

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                if (grid[r][c] == TileState.PLAYER) continue;
                if (effects.isTileBlinded(r, c)) continue;

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
                            applyBlindTileEffect();
                            effects.recordItemActivation(ItemType.TRAP_BLIND);
                            return new SubmitResult(3, r, c); // [수정] 3 = 트랩 성공 + 좌표
                        } else if (modifier == WordModifier.BUFF) {
                            applySplashCapture(r, c);
                            effects.recordItemActivation(ItemType.BUFF_SPLASH);
                            return new SubmitResult(2, r, c); // [수정] 2 = 버프 성공 + 좌표
                        }
                        return new SubmitResult(1, r, c); // [수정] 1 = 일반 성공 + 좌표
                    }
                }
            }
        }
        combo = 0;
        return new SubmitResult(0, -1, -1); // [수정] 0 = 실패
    }

    // (applyBlindTileEffect, applySplashCapture, setNewWord, isWordDuplicateOnGrid,
    //  randomWord, randomWordModifier, loadWordPool, WordList...
    //  ...이하 모든 헬퍼 메서드는 원본과 동일하게 복사)

    // (복사 시작)
    private void applyBlindTileEffect() {
        List<int[]> validTargets = new ArrayList<>();
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
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