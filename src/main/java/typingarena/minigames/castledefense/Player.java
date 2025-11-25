package typingarena.minigames.castledefense;

import javafx.animation.TranslateTransition;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.effect.Glow;
import javafx.util.Duration;
import java.io.InputStream;

public class Player extends ImageView {

    // 플레이어가 자신의 이미지를 관리함
    private static final String IMAGE_PATH = "/images/castledefense/Players/1P.png";

    public Player(double x, double y) {
        super();
        loadPlayerImage(); // 생성 시 이미지 로드
        
        this.setFitWidth(64);
        this.setFitHeight(64);
        this.setPreserveRatio(true);
        this.setSmooth(false);
        this.setLayoutX(x - 36); 
        this.setLayoutY(y - 32);
        this.setId("PLAYER");
    }

    private void loadPlayerImage() {
        try (InputStream is = getClass().getResourceAsStream(IMAGE_PATH)) {
            if (is != null) this.setImage(new Image(is));
        } catch (Exception e) { 
            System.err.println("플레이어 이미지 로드 실패: " + IMAGE_PATH);
        }
    }

    // 공격 기능 (투사체 발사)
    public void attack(Monster target, Pane layer, boolean isMaxCombo, Runnable onHitCallback) {
        Color color = isMaxCombo ? Color.GOLD : Color.CYAN;
        double radius = isMaxCombo ? 12 : 8;

        Circle projectile = new Circle(radius, color);
        projectile.setLayoutX(this.getLayoutX() + 32);
        projectile.setLayoutY(this.getLayoutY() + 32);

        if (isMaxCombo) projectile.setEffect(new Glow(0.8));

        layer.getChildren().add(projectile);

        TranslateTransition tt = new TranslateTransition(Duration.millis(300), projectile);
        tt.setToX(target.getCenterX() - projectile.getLayoutX());
        tt.setToY(target.getCenterY() - projectile.getLayoutY());

        tt.setOnFinished(e -> {
            layer.getChildren().remove(projectile);
            if (onHitCallback != null) onHitCallback.run();
        });
        tt.play();
    }
}