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

    public void spawn() {
        HeartItem h = new HeartItem(spawnX, spawnY);
        activeHearts.add(h);
        layer.getChildren().add(h);
    }

    public void update() {
        Iterator<HeartItem> hit = activeHearts.iterator();
        while (hit.hasNext()) {
            HeartItem h = hit.next();
            h.move();
            if (h.getTranslateX() < -50) {
                layer.getChildren().remove(h);
                hit.remove();
            }
        }
    }

    public boolean checkInput(String text, SimpleIntegerProperty castleHp) {
        for (HeartItem h : activeHearts) {
            if (text.equals(h.getKeyword())) { 
                if (castleHp.get() < 5) {
                    castleHp.set(castleHp.get() + 1);
                }
                layer.getChildren().remove(h);
                activeHearts.remove(h);
                return true; 
            }
        }
        return false; 
    }

    public void clear() {
        for (HeartItem h : activeHearts) {
            layer.getChildren().remove(h);
        }
        activeHearts.clear();
    }
}