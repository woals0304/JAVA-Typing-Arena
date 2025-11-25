package typingarena.minigames.castledefense;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.io.InputStream;

public class Monster extends StackPane {

    private static final String FOLDER_PATH = "/images/castledefense/Mosters/"; // 오타 경로 유지

    // 메인 게임에서 호출할 정적 리소스 로더
    public static Image[] loadAssets() {
        Image[] sprites = new Image[3];
        for (int i = 0; i < 3; i++) {
            String path = String.format("%sM%d.png", FOLDER_PATH, (i + 1));
            try (InputStream is = Monster.class.getResourceAsStream(path)) {
                if (is != null) sprites[i] = new Image(is);
            } catch (Exception e) { e.printStackTrace(); }
        }
        return sprites;
    }

    private String word;
    private Label wordLabel;
    private ImageView imageView;
    
    // 애니메이션
    private Image[] sprites;
    private int currentFrame = 0;
    private int frameDelay = 0;

    public Monster(String word, Image[] sprites, double x, double y) {
        this.word = word;
        this.sprites = sprites;
        this.setLayoutX(x);
        this.setLayoutY(y);

        // 이미지
        if (sprites != null && sprites.length > 0 && sprites[0] != null) {
            imageView = new ImageView(sprites[0]);
            imageView.setFitWidth(48);
            imageView.setFitHeight(48);
            this.getChildren().add(imageView);
        } else {
            this.getChildren().add(new Circle(20, Color.RED));
        }

        // 단어 라벨
        wordLabel = new Label(word);
        wordLabel.setTextFill(Color.WHITE);
        wordLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        wordLabel.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-padding: 2px; -fx-background-radius: 4;");
        wordLabel.setTranslateY(-35);
        this.getChildren().add(wordLabel);
    }

    public void move(double speed) {
        this.setLayoutX(this.getLayoutX() - speed);
        animate();
    }

    private void animate() {
        if (sprites == null) return;
        frameDelay++;
        if (frameDelay >= 10) {
            frameDelay = 0;
            currentFrame = (currentFrame + 1) % sprites.length;
            if (imageView != null) imageView.setImage(sprites[currentFrame]);
        }
    }

    public boolean hasReachedCastle() {
        return this.getLayoutX() < 100;
    }

    public void setTargeted(boolean targeted) {
        if (targeted) {
            wordLabel.setTextFill(Color.RED);
            wordLabel.setStyle("-fx-background-color: rgba(255,255,255,0.8); -fx-padding: 2px; -fx-background-radius: 4;");
        }
    }

    public String getWord() { return word; }
    public double getCenterX() { return this.getLayoutX() + 24; }
    public double getCenterY() { return this.getLayoutY() + 24; }
}