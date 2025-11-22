package typingarena.minigames.castledefense;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * 몬스터 클래스 (이미지 + 단어 텍스트)
 */
public class Monster extends StackPane {

    private String word;
    private Label wordLabel;
    private ImageView imageView;
    private boolean isAlive = true;

    public Monster(String word, Image image, double x, double y) {
        this.word = word;
        this.setLayoutX(x);
        this.setLayoutY(y);

        // 1. 몬스터 이미지
        if (image != null) {
            imageView = new ImageView(image);
            imageView.setFitWidth(48);  // 몬스터 크기
            imageView.setFitHeight(48);
            this.getChildren().add(imageView);
        } else {
            // 이미지가 없으면 빨간 원으로 대체 (비상용)
            Circle fallback = new Circle(20, Color.RED);
            fallback.setStroke(Color.BLACK);
            this.getChildren().add(fallback);
        }

        // 2. 단어 텍스트 (머리 위에 둥둥)
        wordLabel = new Label(word);
        wordLabel.setTextFill(Color.WHITE);
        wordLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        wordLabel.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-padding: 2px; -fx-background-radius: 4;");
        wordLabel.setTranslateY(-35); // 이미지 위로 올리기

        this.getChildren().add(wordLabel);
    }

    public void move(double speed) {
        this.setLayoutX(this.getLayoutX() - speed);
    }

    public void setTargeted(boolean targeted) {
        if (targeted) {
            wordLabel.setTextFill(Color.RED); // 타겟팅 되면 글자색 빨강
            wordLabel.setStyle("-fx-background-color: rgba(255,255,255,0.8); -fx-padding: 2px; -fx-background-radius: 4;");
        }
    }

    public void kill() {
        isAlive = false;
        // (필요시 사망 이펙트 추가 가능)
    }

    public String getWord() { return word; }
    public boolean isAlive() { return isAlive; }
    
    // 투사체 충돌 판정용 중앙 좌표
    public double getCenterX() { return this.getLayoutX() + 24; }
    public double getCenterY() { return this.getLayoutY() + 24; }
}