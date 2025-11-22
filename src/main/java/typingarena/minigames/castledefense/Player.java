package typingarena.minigames.castledefense;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * 플레이어 캐릭터 클래스
 */
public class Player extends ImageView {

    public Player(Image image, double x, double y) {
        super();
        if (image != null) {
            this.setImage(image);
        }
        // 이미지 크기 조정 (필요시 수정)
        this.setFitWidth(64);
        this.setFitHeight(64);
        this.setPreserveRatio(true);
        this.setSmooth(false); // 도트 느낌 유지

        // 위치 설정 (중앙 정렬 보정)
        this.setLayoutX(x - 36); 
        this.setLayoutY(y - 32);
        
        this.setId("PLAYER"); // 식별자
    }
}