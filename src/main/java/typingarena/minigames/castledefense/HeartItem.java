package typingarena.minigames.castledefense;

import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import java.io.InputStream;

public class HeartItem extends StackPane {
    
    private final String keyword = "하트";
    private final double speed = 1.5;
    
    private static Font cookieFont;

    // [New] 폰트 로딩
    static {
        try {
            InputStream is = HeartItem.class.getResourceAsStream("/fonts/CookieRun Regular.otf");
            if (is != null) {
                cookieFont = Font.loadFont(is, 20); // 하트에는 조금 작게
            }
        } catch (Exception e) {
            System.err.println("하트 아이템 폰트 로드 실패");
        }
    }

    public HeartItem(double startX, double startY) {
        Polygon heartShape = new Polygon();
        heartShape.getPoints().addAll(new Double[]{
            0.0, 5.0, -5.0, 0.0, -10.0, -5.0, -5.0, -10.0, 0.0, -5.0, 5.0, -10.0, 10.0, -5.0, 5.0, 0.0
        });
        heartShape.setFill(Color.PINK);
        heartShape.setStroke(Color.RED);
        heartShape.setStrokeWidth(1);
        
        // [수정] 크기 2.5배로 확대
        heartShape.setScaleX(2.5); 
        heartShape.setScaleY(2.5);

        Text text = new Text(keyword);
        text.setFill(Color.BLACK);
        
        // [New] 폰트 적용
        if (cookieFont != null) {
            text.setFont(cookieFont);
        } else {
            text.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        }
        
        // 글자가 하트 중앙에 잘 오도록 위치 미세 조정
        text.setTranslateY(-2); 

        this.getChildren().addAll(heartShape, text); 
        this.setTranslateX(startX);
        this.setTranslateY(startY);
    }

    public void move() {
        setTranslateX(getTranslateX() - speed);
    }
    
    public String getKeyword() {
        return keyword;
    }
}