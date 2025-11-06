package typingarena.minigames.castledefense;

import javafx.beans.property.SimpleIntegerProperty;

public class Castle {
    // JavaFX 화면 업데이트를 위한 HP 속성
    private SimpleIntegerProperty hpProperty = new SimpleIntegerProperty(3);
    
    public SimpleIntegerProperty hpProperty() {
        return hpProperty;
    }

    public int getHp() {
        return hpProperty.get();
    }

    public void takeDamage() {
        if (getHp() > 0) {
            hpProperty.set(getHp() - 1);
        }
    }
    
    public boolean isDestroyed() {
        return getHp() <= 0;
    }
}