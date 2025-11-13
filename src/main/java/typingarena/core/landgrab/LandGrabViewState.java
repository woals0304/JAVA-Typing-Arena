package typingarena.core.landgrab;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * [신규] LandGrabPanel(View)에 전달되는 최소 상태 스냅샷.
 * (TugOfWarViewState와 동일한 역할)
 * 이 객체는 '핵심 엔진(Model)'에 대해 아무것도 모릅니다.
 */
public class LandGrabViewState {

    // Panel.draw()가 그리기에 필요한 모든 데이터를 저장합니다.
    private final LandGrabLogic.TileState[][] grid;
    private final String[][] wordGrid;
    private final LandGrabLogic.WordModifier[][] modifierGrid;
    private final List<LandGrabEffects.BlindedTile> blindedTiles;

    /**
     * 기본 생성자 (빈 화면)
     */
    public LandGrabViewState() {
        int size = LandGrabLogic.GRID_SIZE;
        this.grid = new LandGrabLogic.TileState[size][size];
        this.wordGrid = new String[size][size];
        this.modifierGrid = new LandGrabLogic.WordModifier[size][size];
        this.blindedTiles = Collections.emptyList();

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                this.grid[r][c] = LandGrabLogic.TileState.EMPTY;
                this.wordGrid[r][c] = "";
                this.modifierGrid[r][c] = LandGrabLogic.WordModifier.NEUTRAL;
            }
        }
    }

    /**
     * '핵심 엔진(Model)'으로부터 상태를 복사하는 생성자 (싱글플레이용)
     */
    public LandGrabViewState(LandGrabLogic coreLogic) {
        int size = LandGrabLogic.GRID_SIZE;
        this.grid = new LandGrabLogic.TileState[size][size];
        this.wordGrid = new String[size][size];
        this.modifierGrid = new LandGrabLogic.WordModifier[size][size];

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                this.grid[r][c] = coreLogic.getTileState(r, c);
                this.wordGrid[r][c] = coreLogic.getWord(r, c);
                this.modifierGrid[r][c] = coreLogic.getModifier(r, c);
            }
        }
        this.blindedTiles = coreLogic.getEffects().getActiveBlindedTiles();
    }

    /**
     * (멀티플레이용) Map에서 상태를 복사하는 생성자 (나중에 5단계에서 사용)
     * @param data 서버가 보낸 JSON의 data Map
     */
    public LandGrabViewState(Map<String, Object> data) {
        // TODO: 멀티플레이 연동 시, 'TugOfWarOnlineStage'처럼
        //       서버가 보낸 Map에서 'tiles_changed', 'ink_tiles_added' 등을 파싱하여
        //       grid, wordGrid, modifierGrid, blindedTiles를 채우는 로직 구현 필요
        //
        // (일단 지금은 싱글플레이용 빈 생성자만 둠)
        this(); // 임시로 기본 생성자 호출
    }


    // --- Panel(View)이 사용할 Getter ---

    public LandGrabLogic.TileState getTileState(int r, int c) {
        return grid[r][c];
    }

    public String getWord(int r, int c) {
        return wordGrid[r][c];
    }

    public LandGrabLogic.WordModifier getModifier(int r, int c) {
        return modifierGrid[r][c];
    }

    public List<LandGrabEffects.BlindedTile> getActiveBlindedTiles() {
        return blindedTiles;
    }
}