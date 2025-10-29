package com.typingarena.minigames.tugofwar;

import java.util.Random;

/**
 * 게임 규칙/상태 전부 여기서 관리.
 * 점수, 콤보, 남은 시간, 로프 위치, 단어 생성/판정,
 * 아이템(파워그립/앵커/먹물) 지속시간 갱신까지 전부 담당.
 *
 * TugOfWarGame(프레임)은 화면/버튼/타이머만 담당하고
 * RopePanel은 이 GameLogic을 읽어서 그리기만 함.
 */
public class GameLogic {

    // 랜덤 단어 뽑기용
    private final Random rnd = new Random();

    // --- 상태 ---
    private String currentWord = "apple"; // 지금 쳐야 하는 단어
    private double pos = 0.0;             // 로프 위치 (-100 ~ 100)
    private int score = 0;
    private int combo = 0;
    private int timeMs = 60_000;          // 남은 시간(ms)
    private boolean running = false;

    // --- 파라미터(밸런스 값) ---
    private final double STEP_HIT  = 12.0;    // 정답 시 오른쪽으로 당기는 양
    private final double STEP_MISS = 8.0;     // 오답 시 왼쪽으로 밀리는 양
    private final double ENEMY_BASE = 0.08;   // 기본적으로 왼쪽으로 끌리는 힘
    private final double ENEMY_GROW = 0.00015;// 시간이 지날수록 압박 증가

    // 아이템 효과
    private final ActiveEffects effects = new ActiveEffects();

    // ===== 공개 Getter =====
    public double getPos()          { return pos; }
    public int    getScore()        { return score; }
    public int    getCombo()        { return combo; }
    public int    getTimeMs()       { return timeMs; }
    public boolean isRunning()      { return running; }
    public ActiveEffects getEffects(){ return effects; }
    public String getCurrentWord()  { return currentWord; }

    // ===== 게임 시작 =====
    public void startGame() {
        pos = 0.0;
        score = 0;
        combo = 0;
        timeMs = 60_000;
        running = true;

        effects.clearAll();
        nextWord(); // 첫 단어 세팅
    }

    // ===== 매 틱(100ms마다 호출) =====
    // return 값:
    //  - null이면 아직 진행 중
    //  - "승리! ..." , "패배..." 등 문자열이면 게임 끝 이유
    public String tick() {
        if (!running) return null;

        // 시간 감소
        timeMs -= 100;
        if (timeMs < 0) timeMs = 0;

        // 상대가 나를 왼쪽으로 끄는 힘 계산
        double elapsedSec = (60_000 - timeMs) / 1000.0;
        double enemyPushPerTick = ENEMY_BASE + ENEMY_GROW * elapsedSec * 100;

        // 앵커가 켜져 있으면 거의 안 밀림
        if (effects.isAnchorActive()) {
            enemyPushPerTick *= 0.1;
        }

        // 왼쪽으로 당김
        pos -= enemyPushPerTick;

        // 범위 제한
        if (pos > 100) pos = 100;
        if (pos < -100) pos = -100;

        // 종료 조건 판정
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
            if (pos > 0)  return "시간 종료: 근소한 승리";
            if (pos < 0)  return "시간 종료: 근소한 패배";
            return "무승부";
        }

        return null; // 계속 진행
    }

    // ===== 플레이어 입력 처리 =====
    // typed가 정답이면 true, 아니면 false를 반환.
    public boolean submitAnswer(String typed) {
        if (!running) return false;
        if (typed == null) return false;

        if (typed.equalsIgnoreCase(currentWord)) {
            // 정답
            combo++;
            score += 10 + (combo * 2);

            double push = STEP_HIT;
            if (effects.isPowerGripActive()) {
                push *= 2.0; // 파워그립이면 2배로 민다
            }
            pos += push;

            nextWord(); // 다음 단어 세팅
            return true;
        } else {
            // 오답
            combo = 0;
            pos -= STEP_MISS;
            return false;
        }
    }

    // ===== 단어 생성 =====
    private void nextWord() {
        // 경과 시간에 따라 단어 길이를 늘려 난이도 조절
        int elapsed = 60_000 - timeMs;
        int minLen = 4 + Math.min(elapsed / 15_000, 3); // 0~3 → 4~7
        int maxLen = Math.min(minLen + 1, 8);

        currentWord = randomWord(minLen, maxLen);
    }

    private String randomWord(int minLen, int maxLen) {
        String[] pool = {
            "apple","note","river","korea","typing","banana","window","socket","orange","system",
            "thread","packet","object","combo","vector","method","class","random","matrix","buffer",
            "friend","music","guitar","soccer","player","winner","castle","dragon","danger","shield",
            "future","simple","mobile","attack","defense","victory","balance","energy","memory","rocket",
            "coffee","school","winter","summer","spring","autumn","family","forest","desert","thunder"
        };

        for (int i = 0; i < 100; i++) {
            String w = pool[rnd.nextInt(pool.length)];
            if (w.length() >= minLen && w.length() <= maxLen) {
                return w;
            }
        }
        return pool[rnd.nextInt(pool.length)];
    }

    // ===== 아이템 발동 =====
    public void usePowerGrip() {
        if (!running) return;
        long now = System.currentTimeMillis();
        long dur = 5_000; // 5초
        effects.powerGripUntil = Math.max(effects.powerGripUntil, now + dur);
    }

    public void useAnchor() {
        if (!running) return;
        long now = System.currentTimeMillis();
        long dur = 3_000; // 3초
        effects.anchorUntil = Math.max(effects.anchorUntil, now + dur);
    }

    public void useBlind() {
        if (!running) return;
        long now = System.currentTimeMillis();
        long dur = 3_000; // 3초
        effects.blindUntil = Math.max(effects.blindUntil, now + dur);
    }
}
