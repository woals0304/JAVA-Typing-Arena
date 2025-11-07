package typingarena.minigames.castledefense;

import javafx.geometry.Pos;
import javafx.scene.layout.StackPane; // 텍스트를 네모 위에 올리기 위해 필요
import javafx.scene.layout.VBox; // 머리와 몸통을 수직으로 쌓기 위해
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

// 1. VBox를 상속받아 "머리"와 "몸통"을 수직으로 쌓습니다.
public class Monster extends VBox { 
    private final String keyword;
    private final double speed = 1.75;
    private boolean isAlive = true;
    private boolean isTargeted = false;

    public Monster(String keyword, double startX, double startY) {
        this.keyword = keyword;
        
        // 2. [추가] 님의 요청: "세로로 기다란 네모" (머리/안테나)
        Rectangle head = new Rectangle(40, 80, Color.RED); // 폭 40, 높이 80 (세로로 김)
        head.setArcWidth(5);
        head.setArcHeight(5);
        
        // 3. [수정] 님의 요청: "단어 텍스트 네모" (몸통)
        // StackPane을 사용해서 네모(몸통)와 텍스트(단어)를 겹칩니다.
        StackPane bodyPane = new StackPane();
        Rectangle bodyRect = new Rectangle(100, 30, Color.DARKRED); // 폭 100, 높이 30
        bodyRect.setArcWidth(10);
        bodyRect.setArcHeight(10);
        
        Text text = new Text(keyword);
        text.setFill(Color.WHITE);
        text.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        bodyPane.getChildren().addAll(bodyRect, text); // 네모 위에 텍스트를 올림
        
        // 4. VBox 설정: 머리(위), 몸통(아래) 순서로 쌓기
        this.setSpacing(2); // 머리와 몸통 사이 간격
        this.setAlignment(Pos.CENTER); // 가운데 정렬
        this.getChildren().addAll(head, bodyPane); // 머리를 위에, 몸통(단어)을 아래에 추가
        
        this.setTranslateX(startX);
        this.setTranslateY(startY);
    }

    // 살아있고, 피격 대기 상태가 아닐 때만 이동
    public void move() {
        if (isAlive && !isTargeted) {
            this.setTranslateX(this.getTranslateX() - speed);
        }
    }
    
    public String getKeyword() {
        return keyword;
    }

    public void kill() {
        this.isAlive = false;
    }

    public boolean isAlive() {
        return isAlive;
    }
    
    public void setTargeted(boolean targeted) {
        this.isTargeted = targeted;
    }

    public boolean isTargeted() {
        return isTargeted;
    }
}