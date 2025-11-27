package typingarena.minigames.castledefense;

import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class ComboManager {
    private int combo = 0;
    private final Label comboLabel;
    private static final int MAX_COMBO_THRESHOLD = 10;

    public ComboManager() {
        comboLabel = new Label("Combo: 0");
        updateUI();
    }

    public void increase() {
        combo++;
        updateUI();
    }

    public void reset() {
        combo = 0;
        updateUI();
    }

    public boolean isMaxEffect() {
        return combo >= MAX_COMBO_THRESHOLD;
    }

    public int getScoreMultiplier() {
        return isMaxEffect() ? 2 : 1;
    }

    public Label getLabel() {
        return comboLabel;
    }

    private void updateUI() {
        if (isMaxEffect()) {
            comboLabel.setText("MAX COMBO: " + combo + " (x2)");
            comboLabel.setTextFill(Color.GOLD);
            comboLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22));
            comboLabel.setEffect(new DropShadow(10, Color.RED));
        } else {
            comboLabel.setText("Combo: " + combo);
            comboLabel.setTextFill(Color.CYAN);
            comboLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
            comboLabel.setEffect(null);
        }
    }
}