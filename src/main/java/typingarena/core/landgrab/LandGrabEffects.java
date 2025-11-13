package typingarena.core.landgrab; // [수정] 패키지 경로

import java.util.ArrayList;
import java.util.List;

/**
 * [수정] 땅따먹기 핵심 로직의 '효과' 부분 (core로 이동됨)
 * 1. '스플래시' 텍스트 타이머 제거
 * 2. [신규] '먹물' 효과를 단일 타일 -> '여러 타일'을 동시에 가릴 수 있도록 List<BlindedTile>로 변경
 */
public class LandGrabEffects {

    // --- 1. 시간제 효과 (수정) ---

    /**
     * [신규] 먹물 타일 정보를 저장하는 전용 데이터 객체 (Java 16+ Record)
     * public이어야 LandGrabPanel에서 이 타입을 List로 받을 수 있습니다.
     */
    public record BlindedTile(int r, int c, long until) {}

    // [수정] 단일 변수에서 List로 변경
    private final List<BlindedTile> blindedTiles = new ArrayList<>();


    /**
     * [신규] 특정 타일(r, c)이 현재 먹물로 가려져 있는지 확인
     * (Logic에서 중복 방지, 입력 방지 등에 사용)
     */
    public boolean isTileBlinded(int r, int c) {
        // 먼저 만료된 타일들을 정리
        cleanupExpiredTiles();
        for (BlindedTile tile : blindedTiles) {
            if (tile.r() == r && tile.c() == c) {
                return true;
            }
        }
        return false;
    }

    /**
     * [신규] 만료된 먹물 타일을 리스트에서 제거하는 헬퍼 메서드
     */
    private void cleanupExpiredTiles() {
        long now = System.currentTimeMillis();
        // 리스트에서 'until' 시간이 'now'보다 이전인(만료된) 모든 항목을 제거
        blindedTiles.removeIf(tile -> tile.until() < now);
    }


    /**
     * [수정] activateBlindTile: 단일 타일 덮어쓰기 -> 리스트에 '추가'
     */
    public void activateBlindTile(int r, int c, long durationMs) {
        long now = System.currentTimeMillis();
        // 중복 추가 방지 (이미 해당 타일이 가려져 있다면 추가하지 않음)
        if (isTileBlinded(r, c)) {
            return;
        }
        blindedTiles.add(new BlindedTile(r, c, now + durationMs));
    }

    /**
     * [수정] getBlindedTileCoords -> getActiveBlindedTiles
     * @return 현재 활성화된 (만료되지 않은) 모든 먹물 타일의 List (Panel에서 사용)
     */
    public List<BlindedTile> getActiveBlindedTiles() {
        cleanupExpiredTiles();
        return blindedTiles; // 정리된 리스트 반환
    }


    // --- 2. 순간 발동 효과 추적 (TugOfWar '결' 맞춤) ---
    public enum ItemType { NONE, BUFF_SPLASH, TRAP_BLIND }
    private ItemType lastActivatedItem = ItemType.NONE;
    private long lastItemActivatedAt = 0L;

    public void recordItemActivation(ItemType itemType) {
        lastActivatedItem = itemType;
        lastItemActivatedAt = System.currentTimeMillis();
    }

    // ===== [중요] 컴파일 오류가 나는 바로 그 메서드 =====
    public ItemType getLastItemActivatedItem() {
        return lastActivatedItem;
    }

    public long getLastItemActivatedAt() {
        return lastItemActivatedAt;
    }
    // ==============================================

    // --- 3. 공용 ---
    public void clearAll() {
        // [수정] 리스트 비우기
        blindedTiles.clear();

        lastActivatedItem = ItemType.NONE;
        lastItemActivatedAt = 0L;
    }

    // HUD 표시용 문자열
    public String describeEffects() {
        // [수정] 만료된 타일 정리 후 개수 확인
        cleanupExpiredTiles();
        if (!blindedTiles.isEmpty()) {
            return "효과: [먹물 " + blindedTiles.size() + "칸]";
        }
        return "효과: 없음";
    }
}