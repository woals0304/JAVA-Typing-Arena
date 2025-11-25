package typingarena.minigames.castledefense;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.layout.Pane;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HeartManager {
    private final List<HeartItem> activeHearts = new ArrayList<>();
    private final Pane layer;
    private final double spawnX;
    private final double spawnY;

    public HeartManager(Pane layer, double spawnX, double spawnY) {
        this.layer = layer;
        this.spawnX = spawnX;
        this.spawnY = spawnY;
    }

    // 하트 생성
    public void spawn() {
        HeartItem h = new HeartItem(spawnX, spawnY);
        activeHearts.add(h);
        layer.getChildren().add(h);
    }

    // 이동 및 화면 밖 제거
    public void update() {
        Iterator<HeartItem> hit = activeHearts.iterator();
        while (hit.hasNext()) {
            HeartItem h = hit.next();
            h.move();
            if (h.getTranslateX() < -50) { // 화면 왼쪽으로 나가면 삭제
                layer.getChildren().remove(h);
                hit.remove();
            }
        }
    }

    // 입력 확인 ("하트" 입력 시 획득)
    public boolean checkInput(String text, SimpleIntegerProperty castleHp) {
        for (HeartItem h : activeHearts) {
            if (text.equals(h.getKeyword())) { // "하트"
                // 체력 회복 (최대 5)
                if (castleHp.get() < 5) {
                    castleHp.set(castleHp.get() + 1);
                }
                // 아이템 제거
                layer.getChildren().remove(h);
                activeHearts.remove(h);
                return true; // 입력 처리됨
            }
        }
        return false; // 일치하는 하트 없음
    }

    // 게임 초기화 시 모든 하트 삭제
    public void clear() {
        for (HeartItem h : activeHearts) {
            layer.getChildren().remove(h);
        }
        activeHearts.clear();
    }
}