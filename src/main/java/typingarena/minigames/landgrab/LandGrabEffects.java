package typingarena.minigames.landgrab;

/**
 * [수정됨] 애니메이션으로 변경함에 따라 '스플래시' 텍스트 타이머 관련 코드 제거
 */
public class LandGrabEffects {

    // --- 1. 시간제 효과 ---
    private long blindUntil = 0L; // 먹물: 단어 부분 가리기

    public boolean isBlindActive() {
        return System.currentTimeMillis() < blindUntil;
    }

    public void activateBlind(long durationMs) {
        long now = System.currentTimeMillis();
        blindUntil = Math.max(blindUntil, now + durationMs);
    }

    // [수정] '스플래시' 텍스트 타이머 (splashTextUntil) 관련 메서드 모두 제거
    // (isSplashTextActive, activateSplashText)

    // --- 2. 순간 발동 효과 추적 (TugOfWar '결' 맞춤) ---
    public enum ItemType { NONE, BUFF_SPLASH, TRAP_BLIND }
    private ItemType lastActivatedItem = ItemType.NONE;
    private long lastItemActivatedAt = 0L;

    public void recordItemActivation(ItemType itemType) {
        lastActivatedItem = itemType;
        lastItemActivatedAt = System.currentTimeMillis();
    }

    public ItemType getLastActivatedItem() { return lastActivatedItem; }
    public long getLastItemActivatedAt() { return lastItemActivatedAt; }

    // --- 3. 공용 ---
    public void clearAll() {
        blindUntil = 0L;
        // splashTextUntil = 0L; // [수정] 제거
        lastActivatedItem = ItemType.NONE;
        lastItemActivatedAt = 0L;
    }

    // HUD 표시용 문자열
    public String describeEffects() {
        if (isBlindActive()) {
            return "효과: [먹물]";
        }
        return "효과: 없음";
    }
}