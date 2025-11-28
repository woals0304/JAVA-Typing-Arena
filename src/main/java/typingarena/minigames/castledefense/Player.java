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

    private final String imagePath; // [수정] 이미지 경로를 저장할 변수

    // [수정] 생성자에서 이미지 경로(imagePath)를 받음
    public Player(double x, double y, String imagePath) {
        super();
        this.imagePath = imagePath;
        loadPlayerImage();
        
        // 크기 72px
        this.setFitWidth(72);
        this.setFitHeight(72);
        this.setPreserveRatio(true);
        this.setSmooth(false);
        
        // 중심 좌표 보정 (36)
        this.setLayoutX(x - 36); 
        this.setLayoutY(y - 36);
        
        // ID는 필요하다면 외부에서 setID로 설정 (여기선 기본값 제거 또는 유지)
    }

    private void loadPlayerImage() {
        try (InputStream is = getClass().getResourceAsStream(imagePath)) {
            if (is != null) {
                this.setImage(new Image(is));
            } else {
                System.err.println("플레이어 이미지 로드 실패: " + imagePath);
            }
        } catch (Exception e) { 
            System.err.println("플레이어 이미지 오류: " + e.getMessage());
        }
    }

    public void attack(Monster target, Pane layer, boolean isMaxCombo, Runnable onHitCallback) {
        Color color = isMaxCombo ? Color.GOLD : Color.CYAN;
        double radius = isMaxCombo ? 11 : 7; 

        Circle projectile = new Circle(radius, color);
        projectile.setLayoutX(this.getLayoutX() + 36);
        projectile.setLayoutY(this.getLayoutY() + 36);

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