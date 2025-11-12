package typingarena.core.tugofwar;

import java.util.Random;

/**
 * 줄다리기 게임의 핵심 로직(싱글/멀티 공용).
 * UI 나 네트워크에 의존하지 않으며, 단어/아이템 생성은 TugOfWarWordGenerator를 통해 수행한다.
 */
public class GameLogic {

    public enum WordModifier {
        NEUTRAL,
        BUFF,
        TRAP
    }

    public enum ItemType {
        NONE,
        POWER_GRIP,
        ANCHOR,
        BLIND
    }

    private final Random rnd = new Random();

    // --- 상태 ---
    private String currentWord = "";
    private WordModifier currentWordModifier = WordModifier.NEUTRAL;
    private ItemType lastActivatedItem = ItemType.NONE;
    private long lastItemActivatedAt = 0L;
    private double pos = 0.0;
    private int score = 0;
    private int combo = 0;
    private int timeMs = 60_000;
    private boolean running = false;

    // --- 파라미터(밸런스 값) ---
    private final double STEP_HIT = 12.0;
    private final double STEP_MISS = 8.0;
    private final double ENEMY_BASE = 0.08;
    private final double ENEMY_GROW = 0.00015;

    private final ActiveEffects effects = new ActiveEffects();

    // ===== Getter =====
    public double getPos() { return pos; }
    public int getScore() { return score; }
    public int getCombo() { return combo; }
    public int getTimeMs() { return timeMs; }
    public boolean isRunning() { return running; }
    public ActiveEffects getEffects() { return effects; }
    public String getCurrentWord() { return currentWord; }
    public WordModifier getCurrentWordModifier() { return currentWordModifier; }
    public ItemType getLastActivatedItem() { return lastActivatedItem; }
    public long getLastItemActivatedAt() { return lastItemActivatedAt; }

    // ===== 게임 시작 =====
    public void startGame() {
        pos = 0.0;
        score = 0;
        combo = 0;
        timeMs = 60_000;
        running = true;

        effects.clearAll();
        lastActivatedItem = ItemType.NONE;
        lastItemActivatedAt = 0L;
        nextWord();
    }

    // ===== 매 틱 =====
    public String tick() {
        if (!running) return null;

        timeMs -= 100;
        if (timeMs < 0) timeMs = 0;

        double elapsedSec = (60_000 - timeMs) / 1000.0;
        double enemyPushPerTick = ENEMY_BASE + ENEMY_GROW * elapsedSec * 100;
        if (effects.isAnchorActive()) {
            enemyPushPerTick *= 0.1;
        }

        pos -= enemyPushPerTick;
        if (pos > 100) pos = 100;
        if (pos < -100) pos = -100;

        if (pos >= 100) {
            running = false;
            return "승리! 오른쪽 끝 도달";
        }
        if (pos <= -100) {
            running = false;
            return "패배… 왼쪽 끝 도달";
        }
        if (timeMs == 0) {
            running = false;
            if (pos > 0) return "시간 종료: 근소한 승리";
            if (pos < 0) return "시간 종료: 근소한 패배";
            return "무승부";
        }

        return null;
    }

    // ===== 입력 처리 =====
    public boolean submitAnswer(String typed) {
        if (!running || typed == null) return false;

        if (typed.equalsIgnoreCase(currentWord)) {
            combo++;
            score += 10 + (combo * 2);

            double push = STEP_HIT;
            if (effects.isPowerGripActive()) {
                push *= 2.0;
            }
            pos += push;

            applyWordModifierReward();
            nextWord();
            return true;
        } else {
            combo = 0;
            pos -= STEP_MISS;
            return false;
        }
    }

    private void nextWord() {
        TugOfWarWordGenerator.Word word = TugOfWarWordGenerator.next(rnd);
        currentWord = word.text();
        currentWordModifier = word.modifier();
    }

    private void applyWordModifierReward() {
        if (!running) return;
        if (currentWordModifier == WordModifier.BUFF) {
            activateRandomBuff();
        } else if (currentWordModifier == WordModifier.TRAP) {
            activateRandomTrap();
        }
    }

    private void activateRandomBuff() {
        if (rnd.nextBoolean()) {
            usePowerGrip();
        } else {
            useAnchor();
        }
    }

    private void activateRandomTrap() {
        useBlind();
    }

    private void recordItemActivation(ItemType itemType) {
        lastActivatedItem = itemType;
        lastItemActivatedAt = System.currentTimeMillis();
    }

    // ===== 아이템 발동 =====
    public void usePowerGrip() {
        if (!running) return;
        effects.activatePowerGrip(5_000);
        recordItemActivation(ItemType.POWER_GRIP);
    }

    public void useAnchor() {
        if (!running) return;
        effects.activateAnchor(3_000);
        recordItemActivation(ItemType.ANCHOR);
    }

    public void useBlind() {
        if (!running) return;
        effects.activateBlind(3_000);
        recordItemActivation(ItemType.BLIND);
    }
}
