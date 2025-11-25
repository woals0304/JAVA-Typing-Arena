package typingarena.core.landgrab;

import com.google.gson.Gson;
import typingarena.core.landgrab.LandGrabEffects.ItemType;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LandGrabLogic {

    public record SubmitResult(int resultCode, int r, int c, ItemType itemType) {}

    private static final String WORD_RESOURCE = "words/ko.json";
    private static final String[] DEFAULT_WORDS = {"land", "grab", "typing", "arena", "game"};
    private static final List<String> WORD_POOL = loadWordPool();
    private final Random rnd = new Random();

    public enum TileState { EMPTY, PLAYER_A, PLAYER_B }
    public enum WordModifier { NEUTRAL, TRAP, BUFF }

    public static final int GRID_SIZE = 10;

    private final TileState[][] grid = new TileState[GRID_SIZE][GRID_SIZE];
    private final String[][] wordGrid = new String[GRID_SIZE][GRID_SIZE];
    private final WordModifier[][] modifierGrid = new WordModifier[GRID_SIZE][GRID_SIZE];

    private int scoreA = 0;
    private int scoreB = 0;
    private int comboA = 0;
    private int comboB = 0;

    private final LandGrabEffects effects = new LandGrabEffects();

    private static final int AWAKENING_COMBO = 10;
    private static final int BARRIER_DURATION = 5000;
    private static final int COMBO_GUARD_DURATION = 5000;

    public TileState getTileState(int r, int c) { return grid[r][c]; }
    public String getWord(int r, int c) { return wordGrid[r][c]; }
    public WordModifier getModifier(int r, int c) { return modifierGrid[r][c]; }

    public int getScore(TileState player) { return (player == TileState.PLAYER_A) ? scoreA : scoreB; }
    public int getCombo(TileState player) { return (player == TileState.PLAYER_A) ? comboA : comboB; }
    public LandGrabEffects getEffects() { return effects; }

    public TileState[][] getGrid() { return grid; }
    public String[][] getWordGrid() { return wordGrid; }
    public WordModifier[][] getModifierGrid() { return modifierGrid; }

    public void startGame() {
        scoreA = 0; scoreB = 0; comboA = 0; comboB = 0;
        effects.clearAll();
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                grid[r][c] = TileState.EMPTY;
                setNewWord(r, c, TileState.EMPTY);
            }
        }
    }

    public SubmitResult submitAnswer(String typed, TileState who) {
        if (typed == null || typed.isEmpty()) return new SubmitResult(0, -1, -1, ItemType.NONE);

        boolean isPlayerA = (who == TileState.PLAYER_A);
        TileState opponent = isPlayerA ? TileState.PLAYER_B : TileState.PLAYER_A;

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                if (grid[r][c] == who) continue;

                if (effects.isTileBlinded(r, c, isPlayerA)) continue;

                if (typed.equalsIgnoreCase(wordGrid[r][c])) {
                    // 상대방 보호막 체크
                    if (grid[r][c] == opponent && effects.isBarrierActive(!isPlayerA)) {
                        return new SubmitResult(0, r, c, ItemType.NONE);
                    }

                    boolean captured = false;
                    TileState oldState = grid[r][c];
                    TileState newState = who;
                    int currentCombo = isPlayerA ? comboA : comboB;

                    if (oldState == TileState.EMPTY) {
                        grid[r][c] = who;
                        updateScore(who, 1);
                        captured = true;
                    } else if (oldState == opponent) {
                        if (currentCombo >= AWAKENING_COMBO) {
                            grid[r][c] = who;
                            updateScore(opponent, -1);
                            updateScore(who, 1);
                            newState = who;
                            captured = true;
                        } else {
                            grid[r][c] = TileState.EMPTY;
                            updateScore(opponent, -1);
                            newState = TileState.EMPTY;
                            captured = true;
                        }
                    }

                    if (captured) {
                        if (isPlayerA) comboA++; else comboB++;
                        WordModifier modifier = modifierGrid[r][c];
                        setNewWord(r, c, newState);

                        ItemType triggeredItem = ItemType.NONE;
                        if (modifier != WordModifier.NEUTRAL) {
                            triggeredItem = applyItemEffect(r, c, modifier, who, opponent);
                        }
                        return new SubmitResult(1, r, c, triggeredItem);
                    }
                }
            }
        }

        if (!effects.isComboGuardActive(isPlayerA)) {
            if (isPlayerA) comboA = 0; else comboB = 0;
        }
        return new SubmitResult(0, -1, -1, ItemType.NONE);
    }

    private void updateScore(TileState player, int delta) {
        if (player == TileState.PLAYER_A) scoreA += delta; else scoreB += delta;
    }

    private ItemType applyItemEffect(int r, int c, WordModifier modifier, TileState self, TileState opponent) {
        boolean isSelfA = (self == TileState.PLAYER_A);
        ItemType resultItem = ItemType.NONE;

        if (modifier == WordModifier.BUFF) {
            int roll = rnd.nextInt(3);
            if (roll == 0) { applySplashCapture(r, c, self, opponent); resultItem = ItemType.BUFF_SPLASH; }
            else if (roll == 1) { effects.activateBarrier(isSelfA, BARRIER_DURATION); resultItem = ItemType.BUFF_BARRIER; }
            else { effects.activateComboGuard(isSelfA, COMBO_GUARD_DURATION); resultItem = ItemType.BUFF_COMBO_GUARD; }
        } else if (modifier == WordModifier.TRAP) {
            int roll = rnd.nextInt(3);
            if (roll == 0) resultItem = ItemType.TRAP_INK;
            else if (roll == 1) { applyEmpCapture(opponent); resultItem = ItemType.TRAP_EMP; }
            else resultItem = ItemType.TRAP_CONFUSION;
        }

        if (resultItem != ItemType.NONE) effects.recordItemActivation(resultItem);
        return resultItem;
    }

    private void applySplashCapture(int r, int c, TileState self, TileState opponent) {
        int[] dr = {-1, 1, 0, 0}; int[] dc = {0, 0, -1, 1};
        List<int[]> validTargets = new ArrayList<>();
        for(int i = 0; i < 4; i++) {
            int nr = r + dr[i]; int nc = c + dc[i];
            if (nr >= 0 && nr < GRID_SIZE && nc >= 0 && nc < GRID_SIZE) {
                if (grid[nr][nc] != self) validTargets.add(new int[]{nr, nc});
            }
        }
        if (!validTargets.isEmpty()) {
            int[] t = validTargets.get(rnd.nextInt(validTargets.size()));
            int nr = t[0]; int nc = t[1];
            boolean isSelfA = (self == TileState.PLAYER_A);
            if (grid[nr][nc] == opponent && effects.isBarrierActive(!isSelfA)) return;

            TileState oldState = grid[nr][nc];
            TileState newState = self;
            if (oldState == TileState.EMPTY) { grid[nr][nc] = self; updateScore(self, 1); }
            else if (oldState == opponent) { grid[nr][nc] = TileState.EMPTY; updateScore(opponent, -1); newState = TileState.EMPTY; }
            setNewWord(nr, nc, newState);
        }
    }

    private void applyEmpCapture(TileState targetPlayer) {
        boolean isTargetA = (targetPlayer == TileState.PLAYER_A);
        if (effects.isBarrierActive(isTargetA)) return;
        List<int[]> targetTiles = new ArrayList<>();
        for(int r=0; r<GRID_SIZE; r++){
            for(int c=0; c<GRID_SIZE; c++){
                if(grid[r][c] == targetPlayer) targetTiles.add(new int[]{r,c});
            }
        }
        Collections.shuffle(targetTiles);
        int count = Math.min(targetTiles.size(), 3);
        for(int i=0; i<count; i++) {
            int[] t = targetTiles.get(i);
            grid[t[0]][t[1]] = TileState.EMPTY;
            updateScore(targetPlayer, -1);
            setNewWord(t[0], t[1], TileState.EMPTY);
        }
    }

    private void setNewWord(int r, int c, TileState currentState) {
        String newWord; int tryCount = 0;
        do { newWord = randomWord(); tryCount++; if (isWordDuplicateOnGrid(newWord, r, c)) newWord = null; } while (newWord == null && tryCount < 200);
        if (newWord == null) newWord = randomWord();
        wordGrid[r][c] = newWord;
        modifierGrid[r][c] = randomWordModifier(currentState);
    }

    private boolean isWordDuplicateOnGrid(String word, int r, int c) {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                if (i == r && j == c) continue;
                if (word.equalsIgnoreCase(wordGrid[i][j])) return true;
            }
        }
        return false;
    }

    private String randomWord() {
        if (WORD_POOL.isEmpty()) return "ERROR";
        return WORD_POOL.get(rnd.nextInt(WORD_POOL.size()));
    }

    // [수정] 아이템 확률 대폭 상향 (테스트 및 재미를 위해)
    // - 기존: Buff 15%, Trap 15% (총 30%) -> 변경: Buff 20%, Trap 20% (총 40%)
    private WordModifier randomWordModifier(TileState currentState) {
        if (currentState != TileState.EMPTY) return WordModifier.NEUTRAL;
        double roll = rnd.nextDouble();
        if (roll < 0.20) return WordModifier.BUFF; // 20% 확률
        else if (roll < 0.40) return WordModifier.TRAP; // 20% 확률
        return WordModifier.NEUTRAL;
    }

    private static List<String> loadWordPool() {
        try (InputStream in = LandGrabLogic.class.getClassLoader().getResourceAsStream(WORD_RESOURCE)) {
            if (in == null) return new ArrayList<>(Arrays.asList(DEFAULT_WORDS));
            try (Reader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                Gson gson = new Gson();
                WordList data = gson.fromJson(reader, WordList.class);
                if (data == null || data.words == null) return new ArrayList<>(Arrays.asList(DEFAULT_WORDS));
                List<String> words = new ArrayList<>();
                for (String word : data.words) { if (word != null && !word.trim().isEmpty()) words.add(word.trim()); }
                return Collections.unmodifiableList(words);
            }
        } catch (Exception e) { return new ArrayList<>(Arrays.asList(DEFAULT_WORDS)); }
    }
    private static final class WordList { List<String> words; }
}