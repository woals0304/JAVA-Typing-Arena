package typingarena.minigames.castledefense;

import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Text;

// 몬스터와 유사하지만, '하트' 아이템 전용 클래스
public class HeartItem extends StackPane {
    
    private final String keyword = "하트";
    // [수정] 하트 아이템의 이동 속도 (원래 3.0 -> 1.5로 절반 감소)
    private final double speed = 1.5; 
    private boolean isAlive = true;

    public HeartItem(double startX, double startY) {
        
        // 1. Polygon을 사용해 하트 모양 그리기
        Polygon heartShape = new Polygon();
        heartShape.getPoints().addAll(new Double[]{
            0.0, 5.0,  // 아래쪽 뾰족한 점
            -5.0, 0.0, // 왼쪽 중간
            -10.0, -5.0, // 왼쪽 위
            -5.0, -10.0, // 왼쪽 위 굴곡
            0.0, -5.0,  // 가운데 위 쏙 들어간 점
            5.0, -10.0, // 오른쪽 위 굴곡
            10.0, -5.0, // 오른쪽 위
            5.0, 0.0   // 오른쪽 중간
        });
        heartShape.setFill(Color.PINK); // 분홍색 하트
        heartShape.setStroke(Color.RED);
        heartShape.setStrokeWidth(1);
        // [수정] 하트 크기 1.5배 -> 2.0배로 증가
        heartShape.setScaleX(2.0); 
        heartShape.setScaleY(2.0);

        // 2. "하트" 텍스트
        Text text = new Text(keyword);
        text.setFill(Color.BLACK);
        // 텍스트 크기도 하트 크기에 맞춰 약간 키움
        text.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;"); 

        // 3. 하트 모양 위에 텍스트 겹치기
        this.getChildren().addAll(heartShape, text); 
        
        // 4. 스폰 위치 설정
        this.setTranslateX(startX);
        this.setTranslateY(startY);
    }

    // 왼쪽으로 이동
    public void move() {
        if (isAlive) {
            setTranslateX(getTranslateX() - speed);
        }
    }
    
    // Getter 및 Setter
    public String getKeyword() {
        return keyword;
    }

    public void kill() {
        this.isAlive = false;
    }

    public boolean isAlive() {
        return isAlive;
    }
}