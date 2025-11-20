package typingarena.core.tugofwar;

/**
 * 줄다리기에서 사용하는 버프/트랩 효과 지속시간 관리.
 */
public class ActiveEffects {
    private long powerGripUntil = 0L;
    private long anchorUntil = 0L;
    private long blindUntil = 0L;
    private long jamoSplitUntil = 0L;

    public boolean isPowerGripActive() {
        return System.currentTimeMillis() < powerGripUntil;
    }

    public boolean isAnchorActive() {
        return System.currentTimeMillis() < anchorUntil;
    }

    public boolean isBlindActive() {
        return System.currentTimeMillis() < blindUntil;
    }

    public boolean isJamoSplitActive() {
        return System.currentTimeMillis() < jamoSplitUntil;
    }

    public void activatePowerGrip(long durationMs) {
        long now = System.currentTimeMillis();
        powerGripUntil = Math.max(powerGripUntil, now + durationMs);
    }

    public void activateAnchor(long durationMs) {
        long now = System.currentTimeMillis();
        anchorUntil = Math.max(anchorUntil, now + durationMs);
    }

    public void activateBlind(long durationMs) {
        long now = System.currentTimeMillis();
        blindUntil = Math.max(blindUntil, now + durationMs);
    }

    public void activateJamoSplit(long durationMs) {
        long now = System.currentTimeMillis();
        jamoSplitUntil = Math.max(jamoSplitUntil, now + durationMs);
    }

    public void clearAll() {
        powerGripUntil = 0L;
        anchorUntil = 0L;
        blindUntil = 0L;
        jamoSplitUntil = 0L;
    }

    public String describeEffects() {
        StringBuilder sb = new StringBuilder();
        if (isPowerGripActive()) sb.append("[파워그립] ");
        if (isAnchorActive()) sb.append("[앵커] ");
        if (isBlindActive()) sb.append("[먹물] ");
        if (isJamoSplitActive()) sb.append("[자소분리] ");
        if (sb.length() == 0) return "효과: 없음";
        return "효과: " + sb.toString().trim();
    }
}
