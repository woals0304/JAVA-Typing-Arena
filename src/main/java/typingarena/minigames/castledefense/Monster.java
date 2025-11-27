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

    static {
        loadFont();
    }

    private static void loadFont() {
        try {
            InputStream is = Monster.class.getResourceAsStream("/fonts/CookieRun Regular.otf");
            if (is != null) {
                // [수정] 폰트 크기 13 -> 20으로 확대
                cookieFont = Font.loadFont(is, 20);
            }
        } catch (Exception e) {
            System.err.println("몬스터 폰트 로드 실패");
        }
    }

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
    private Image[] sprites;
    private int currentFrame = 0;
    private int frameDelay = 0;

    public Monster(String word, Image[] sprites, double x, double y) {
        this.word = word;
        this.sprites = sprites;
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
            // [수정] 폰트 로드 실패 시 기본 폰트도 20으로
            wordLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        }

        wordLabel.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-padding: 2px 6px; -fx-background-radius: 6;");
        
        // [수정] 글자 위치를 더 위로 올림 (-35 -> -45)
        wordLabel.setTranslateY(-45); 
        
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
}