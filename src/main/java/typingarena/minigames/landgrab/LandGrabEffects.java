package typingarena.minigames.landgrab;

/**
 * '땅따먹기'의 아이템 효과 지속시간을 관리하는 클래스.
 * ActiveEffects.java와 동일한 '결'을 따름.
 */
public class LandGrabEffects {

    long blindUntil = 0L; // 먹물: 단어 부분 가리기

    public boolean isBlindActive() {
        return System.currentTimeMillis() < blindUntil;
    }

    public void clearAll() {
        blindUntil = 0L;
    }

    public void activateBlind(long durationMs) {
        long now = System.currentTimeMillis();
        // 기존 효과 시간과 새 효과 시간을 비교해 더 긴 쪽을 택함
        blindUntil = Math.max(blindUntil, now + durationMs);
    }

    // HUD 표시용 문자열
    public String describeEffects() {
        if (isBlindActive()) {
            return "효과: [먹물]";
        }
        return "효과: 없음";
    }
}