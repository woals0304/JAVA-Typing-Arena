package typingarena.minigames.castledefense;

import javafx.scene.image.Image;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class TileManager {
    // 타일 타입 상수 (메인 게임에서 가져옴)
    public static final int G=0, G1=1, G2=7, P=2, PD=3, P1=4, PU=5, F=8, F1=9, F2=10, F3=11, F4=12, F1U=13, F4U=14, F1D=15, F4D=16, S=17, S1=18;

    private final Map<String, Image> images = new HashMap<>();
    private static final String BASE_PATH = "/images/castledefense/Tiles/";

    public TileManager() {
        loadResources();
    }

    private void loadResources() {
        String[] tiles = {"G", "G1", "G2", "P", "P1", "PU", "PD", "F", "F1", "F2", "F3", "F4", "S", "S1"};
        for (String t : tiles) {
            try (InputStream is = getClass().getResourceAsStream(BASE_PATH + t + ".png")) {
                if (is != null) images.put(t, new Image(is));
            } catch (Exception e) {
                System.err.println("타일 이미지 로드 실패: " + t);
            }
        }
    }

    public Image getBaseImage(int type) {
        if (type == P || type == F) return images.get("P");
        if (type == PU || type == F1U) return images.get("PU");
        if (type == PD || type == F1D) return images.get("PD");
        if (type == P1) return images.get("P1");
        if (type == G1) return images.get("G1");
        if (type == G2) return images.get("G2");
        return images.get("G");
    }

    public Image getTopImage(int type) {
        if (type == F) return images.get("F");
        if (type == F1 || type == F1U || type == F1D) return images.get("F1");
        if (type == F2) return images.get("F2");
        if (type == F3) return images.get("F3");
        if (type == F4 || type == F4U || type == F4D) return images.get("F4");
        if (type == S) return images.get("S");
        if (type == S1) return images.get("S1");
        return null;
    }
}