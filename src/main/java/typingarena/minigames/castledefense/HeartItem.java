package typingarena.minigames.castledefense;

import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Text;

public class HeartItem extends StackPane {
    
    private final String keyword = "하트";
    private final double speed = 1.5;

    public HeartItem(double startX, double startY) {
        Polygon heartShape = new Polygon();
        heartShape.getPoints().addAll(new Double[]{
            0.0, 5.0, -5.0, 0.0, -10.0, -5.0, -5.0, -10.0, 0.0, -5.0, 5.0, -10.0, 10.0, -5.0, 5.0, 0.0
        });
        heartShape.setFill(Color.PINK);
        heartShape.setStroke(Color.RED);
        heartShape.setStrokeWidth(1);
        
        // [수정] 스케일 1.75로 조정
        heartShape.setScaleX(1.75); 
        heartShape.setScaleY(1.75);

        Text text = new Text(keyword);
        text.setFill(Color.BLACK);
        text.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;"); 

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