package typingarena.core.landgrab;

import java.util.ArrayList;
import java.util.List;

public class LandGrabEffects {

    public enum ItemType {
        NONE,
        BUFF_SPLASH, BUFF_BARRIER, BUFF_COMBO_GUARD,
        TRAP_INK, TRAP_EMP, TRAP_CONFUSION
    }

    public record BlindedTile(int r, int c, long until) {}

    // [수정] 먹물 리스트 분리 (A가 안 보이는 타일들, B가 안 보이는 타일들)
    private final List<BlindedTile> blindedTilesA = new ArrayList<>();
    private final List<BlindedTile> blindedTilesB = new ArrayList<>();

    private long barrierUntilA = 0L;
    private long barrierUntilB = 0L;
    private long comboGuardUntilA = 0L;
    private long comboGuardUntilB = 0L;

    private ItemType lastActivatedItem = ItemType.NONE;
    private long lastItemActivatedAt = 0L;

    // ===== 메서드 =====

    // [수정] '누가' 안 보이는지 확인
    public boolean isTileBlinded(int r, int c, boolean isPlayerA) {
        cleanupExpiredTiles();
        List<BlindedTile> targetList = isPlayerA ? blindedTilesA : blindedTilesB;
        for (BlindedTile tile : targetList) {
            if (tile.r() == r && tile.c() == c) return true;
        }
        return false;
    }

    private void cleanupExpiredTiles() {
        long now = System.currentTimeMillis();
        blindedTilesA.removeIf(tile -> tile.until() < now);
        blindedTilesB.removeIf(tile -> tile.until() < now);
    }

    // [수정] 타겟 지정해서 먹물 뿌리기
    public void activateBlindTile(int r, int c, long durationMs, boolean targetIsPlayerA) {
        long now = System.currentTimeMillis();
        // 이미 가려져 있지 않다면 추가
        if (!isTileBlinded(r, c, targetIsPlayerA)) {
            List<BlindedTile> targetList = targetIsPlayerA ? blindedTilesA : blindedTilesB;
            targetList.add(new BlindedTile(r, c, now + durationMs));
        }
    }

    // [수정] 특정 플레이어에게 적용된 먹물 리스트만 반환
    public List<BlindedTile> getActiveBlindedTiles(boolean isPlayerA) {
        cleanupExpiredTiles();
        return isPlayerA ? blindedTilesA : blindedTilesB;
    }

    public boolean isBarrierActive(boolean isPlayerA) {
        long now = System.currentTimeMillis();
        return isPlayerA ? (now < barrierUntilA) : (now < barrierUntilB);
    }

    public void activateBarrier(boolean isPlayerA, long durationMs) {
        long now = System.currentTimeMillis();
        if (isPlayerA) barrierUntilA = Math.max(barrierUntilA, now + durationMs);
        else barrierUntilB = Math.max(barrierUntilB, now + durationMs);
    }

    public boolean isComboGuardActive(boolean isPlayerA) {
        long now = System.currentTimeMillis();
        return isPlayerA ? (now < comboGuardUntilA) : (now < comboGuardUntilB);
    }

    public void activateComboGuard(boolean isPlayerA, long durationMs) {
        long now = System.currentTimeMillis();
        if (isPlayerA) comboGuardUntilA = Math.max(comboGuardUntilA, now + durationMs);
        else comboGuardUntilB = Math.max(comboGuardUntilB, now + durationMs);
    }

    public void recordItemActivation(ItemType itemType) {
        this.lastActivatedItem = itemType;
        this.lastItemActivatedAt = System.currentTimeMillis();
    }

    public ItemType getLastActivatedItem() { return lastActivatedItem; }
    public long getLastItemActivatedAt() { return lastItemActivatedAt; }

    public void clearAll() {
        blindedTilesA.clear();
        blindedTilesB.clear();
        barrierUntilA = 0L;
        barrierUntilB = 0L;
        comboGuardUntilA = 0L;
        comboGuardUntilB = 0L;
        lastActivatedItem = ItemType.NONE;
        lastItemActivatedAt = 0L;
    }

    public String describeEffects(boolean isPlayerA) {
        cleanupExpiredTiles();
        StringBuilder sb = new StringBuilder();
        List<BlindedTile> myList = isPlayerA ? blindedTilesA : blindedTilesB;

        if (!myList.isEmpty()) sb.append("[먹물 ").append(myList.size()).append("] ");
        if (isBarrierActive(isPlayerA)) sb.append("[보호막] ");
        if (isComboGuardActive(isPlayerA)) sb.append("[콤보가드] ");
        return sb.length() == 0 ? "효과: 없음" : "효과: " + sb.toString().trim();
    }
}