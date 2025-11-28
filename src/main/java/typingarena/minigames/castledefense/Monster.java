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

    private static final String FOLDER_PATH = "/images/castledefense/Mosters/";
    private static Font cookieFont;

    private int hp;       // 체력 (생성 시 결정)
    private double speed; // 이동 속도 (생성 시 결정)

    static {
        loadFont();
    }

    private static void loadFont() {
        try {
            InputStream is = Monster.class.getResourceAsStream("/fonts/CookieRun Regular.otf");
            if (is != null) {
                cookieFont = Font.loadFont(is, 20);
            }
        } catch (Exception e) {
            System.err.println("몬스터 폰트 로드 실패");
        }
    }

    public static Image[] loadAssets(String prefix) {
        Image[] sprites = new Image[3];
        for (int i = 0; i < 3; i++) {
            String path = String.format("%s%s%d.png", FOLDER_PATH, prefix, (i + 1));
            try (InputStream is = Monster.class.getResourceAsStream(path)) {
                if (is != null) sprites[i] = new Image(is);
            } catch (Exception e) { e.printStackTrace(); }
        }
        return sprites;
    }

    private String word;
    private Label wordLabel;
    private ImageView imageView;
    private Image[] sprites;
    private int currentFrame = 0;
    private int frameDelay = 0;

    public Monster(String word, Image[] sprites, double x, double y, int hp, double speed) {
        this.word = word;
        this.sprites = sprites;
        this.hp = hp;
        this.speed = speed;
        
        this.setLayoutX(x);
        this.setLayoutY(y);

        if (sprites != null && sprites.length > 0 && sprites[0] != null) {
            imageView = new ImageView(sprites[0]);
            imageView.setFitWidth(56);
            imageView.setFitHeight(56);
            this.getChildren().add(imageView);
        } else {
            this.getChildren().add(new Circle(25, Color.RED));
        }

        wordLabel = new Label(word);
        wordLabel.setTextFill(Color.WHITE);
        
        if (cookieFont != null) {
            wordLabel.setFont(cookieFont);
        } else {
            wordLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        }

        wordLabel.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-padding: 2px 6px; -fx-background-radius: 6;");
        wordLabel.setTranslateY(-45); 
        this.getChildren().add(wordLabel);
    }

    public void setWord(String newWord) {
        this.word = newWord;
        this.wordLabel.setText(newWord);
    }

    public boolean takeDamage(int damage) {
        hp -= damage;
        if (hp > 0) {
            this.setOpacity(0.5);
            imageView.setEffect(new javafx.scene.effect.ColorAdjust(0, 0.5, 0, 0.5)); 
            
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(100));
            pause.setOnFinished(e -> {
                this.setOpacity(1.0);
                imageView.setEffect(null);
            });
            pause.play();
            
            return false;
        }
        return true;
    }

    public void move() {
        this.setLayoutX(this.getLayoutX() - this.speed);
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
        return this.getLayoutX() < 90; 
    }

    public void setTargeted(boolean targeted) {
        if (targeted) {
            wordLabel.setTextFill(Color.RED);
            wordLabel.setStyle("-fx-background-color: rgba(255,255,255,0.8); -fx-padding: 2px 6px; -fx-background-radius: 6;");
        }
    }

    public String getWord() { return word; }
    public double getCenterX() { return this.getLayoutX() + 28; }
    public double getCenterY() { return this.getLayoutY() + 28; }

    // [추가] 외부에서 HP를 확인할 수 있도록 Getter 추가
    public int getHp() {
        return hp;
    }
}