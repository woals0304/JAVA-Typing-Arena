package typingarena.minigames.castledefense;

import javafx.beans.property.SimpleIntegerProperty;

public class Castle {
    // JavaFX 화면 업데이트를 위한 HP 속성
    private SimpleIntegerProperty hpProperty = new SimpleIntegerProperty(3);
    
    // [추가] 최대 HP 제한 (예: 5개)
    private final int MAX_HP = 5; 
    
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
    
    // [추가] 님의 요청: 하트 아이템을 먹었을 때 HP 증가
    public void addHp() {
        if (getHp() < MAX_HP) { // 최대 HP를 넘지 않을 때만
           hpProperty.set(getHp() + 1);
        }
    }
    
    public boolean isDestroyed() {
        return getHp() <= 0;
    }
}