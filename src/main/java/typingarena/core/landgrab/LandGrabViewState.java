package typingarena.core.landgrab;

import java.util.ArrayList;
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
        // [수정] getActiveBlindedTiles()는 List<BlindedTile>을 반환하므로 바로 할당
        this.blindedTiles = coreLogic.getEffects().getActiveBlindedTiles();
    }

    /**
     * [신규] (멀티플레이용) Map에서 상태를 복사하는 생성자
     * @param data 서버가 보낸 JSON의 data Map
     */
    @SuppressWarnings("unchecked") // (Map/List 캐스팅 경고 무시)
    public LandGrabViewState(Map<String, Object> data) {
        int size = LandGrabLogic.GRID_SIZE;
        this.grid = new LandGrabLogic.TileState[size][size];
        this.wordGrid = new String[size][size];
        this.modifierGrid = new LandGrabLogic.WordModifier[size][size];

        // 1. 그리드 상태 파싱
        parseGrid(data.get("grid"), this.grid, LandGrabLogic.TileState.class, LandGrabLogic.TileState.EMPTY);
        // 2. 단어 그리드 파싱
        parseGrid(data.get("words"), this.wordGrid, String.class, "");
        // 3. 모디파이어 그리드 파싱
        parseGrid(data.get("modifiers"), this.modifierGrid, LandGrabLogic.WordModifier.class, LandGrabLogic.WordModifier.NEUTRAL);

        // 4. 먹물 타일 리스트 파싱
        this.blindedTiles = parseBlindedTiles(data.get("ink_tiles"));
    }

    // --- 헬퍼 메서드: Map에서 List<List<String>>으로 온 그리드 데이터를 파싱 ---

    @SuppressWarnings("unchecked")
    private <T> void parseGrid(Object gridData, T[][] targetGrid, Class<T> enumClass, T defaultValue) {
        if (!(gridData instanceof List)) {
            fillGrid(targetGrid, defaultValue); // 데이터 없으면 기본값으로 채움
            return;
        }

        try {
            List<List<String>> rows = (List<List<String>>) gridData;
            int size = LandGrabLogic.GRID_SIZE;

            for (int r = 0; r < size; r++) {
                if (r >= rows.size()) break;
                List<String> cols = rows.get(r);
                for (int c = 0; c < size; c++) {
                    if (c >= cols.size()) break;
                    String val = cols.get(c);

                    if (val == null) {
                        targetGrid[r][c] = defaultValue;
                        continue;
                    }

                    if (enumClass == String.class) {
                        targetGrid[r][c] = (T) val;
                    } else { // Enum 타입 파싱 (TileState, WordModifier)
                        targetGrid[r][c] = (T) Enum.valueOf((Class<Enum>) enumClass, val);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Grid 파싱 실패: " + e.getMessage());
            fillGrid(targetGrid, defaultValue);
        }
    }

    private <T> void fillGrid(T[][] targetGrid, T defaultValue) {
        for (int r = 0; r < targetGrid.length; r++) {
            for (int c = 0; c < targetGrid[r].length; c++) {
                targetGrid[r][c] = defaultValue;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<LandGrabEffects.BlindedTile> parseBlindedTiles(Object inkData) {
        if (!(inkData instanceof List)) {
            return Collections.emptyList();
        }

        List<LandGrabEffects.BlindedTile> tiles = new ArrayList<>();
        try {
            List<Map<String, Object>> inkList = (List<Map<String, Object>>) inkData;
            for (Map<String, Object> tileData : inkList) {
                // JSON은 숫자를 Double로 파싱하므로, Number로 받아 int로 변환
                int r = ((Number) tileData.get("r")).intValue();
                int c = ((Number) tileData.get("c")).intValue();
                long until = ((Number) tileData.get("until")).longValue(); // (until은 사실 클라에선 불필요)
                tiles.add(new LandGrabEffects.BlindedTile(r, c, until));
            }
        } catch (Exception e) {
            System.err.println("먹물 타일 파싱 실패: " + e.getMessage());
        }
        return tiles;
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