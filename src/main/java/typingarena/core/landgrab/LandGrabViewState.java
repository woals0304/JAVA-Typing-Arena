package typingarena.core.landgrab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LandGrabViewState {

    private final LandGrabLogic.TileState[][] grid;
    private final String[][] wordGrid;
    private final LandGrabLogic.WordModifier[][] modifierGrid;
    private final List<LandGrabEffects.BlindedTile> blindedTiles;

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

    // [수정] 싱글 플레이(로직 기반 생성)에서는 항상 '나(Player A)' 시점으로 먹물을 가져옵니다.
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
        // 여기가 에러가 났던 부분입니다! true (Player A)를 인자로 넘겨줍니다.
        this.blindedTiles = coreLogic.getEffects().getActiveBlindedTiles(true);
    }

    @SuppressWarnings("unchecked")
    public LandGrabViewState(Map<String, Object> data) {
        int size = LandGrabLogic.GRID_SIZE;
        this.grid = new LandGrabLogic.TileState[size][size];
        this.wordGrid = new String[size][size];
        this.modifierGrid = new LandGrabLogic.WordModifier[size][size];

        parseGrid(data.get("grid"), this.grid, LandGrabLogic.TileState.class, LandGrabLogic.TileState.EMPTY);
        parseGrid(data.get("words"), this.wordGrid, String.class, "");
        parseGrid(data.get("modifiers"), this.modifierGrid, LandGrabLogic.WordModifier.class, LandGrabLogic.WordModifier.NEUTRAL);

        this.blindedTiles = parseBlindedTiles(data.get("ink_tiles"));
    }

    @SuppressWarnings("unchecked")
    private <T> void parseGrid(Object gridData, T[][] targetGrid, Class<T> enumClass, T defaultValue) {
        if (!(gridData instanceof List)) {
            fillGrid(targetGrid, defaultValue);
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
                    } else {
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
                int r = ((Number) tileData.get("r")).intValue();
                int c = ((Number) tileData.get("c")).intValue();
                long until = ((Number) tileData.get("until")).longValue();
                tiles.add(new LandGrabEffects.BlindedTile(r, c, until));
            }
        } catch (Exception e) {
            System.err.println("먹물 타일 파싱 실패: " + e.getMessage());
        }
        return tiles;
    }

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

    // [추가된 메서드] 특정 좌표(r, c)가 현재 먹물 상태인지 확인
    public boolean isTileBlinded(int r, int c) {
        if (blindedTiles == null) return false;
        for (LandGrabEffects.BlindedTile tile : blindedTiles) {
            if (tile.r() == r && tile.c() == c) {
                return true;
            }
        }
        return false;
    }
}