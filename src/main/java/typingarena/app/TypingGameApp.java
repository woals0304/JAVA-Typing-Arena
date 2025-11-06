package typingarena.app;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import typingarena.minigames.tugofwar.TugOfWarGame;
import typingarena.minigames.castledefense.CastleDefenseGame; // 성지키기 게임 임포트

public class TypingGameApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Typing Mini Game");

        Label title = new Label("멀티플레이 타자 미니게임 로비");
        title.setFont(Font.font("System", FontWeight.BOLD, 28));
        title.setWrapText(true);

        Label description = new Label(
                "준비된 미니게임:\n"
                        + "- 줄다리기 타자 대전 (Tug of War)\n"
                        + "- 성 지키기 (Castle Defense)\n\n" // 2. 게임 설명 추가
                        + "시작 버튼을 누르면 새 창에서 게임이 실행됩니다."
        );
        description.setFont(Font.font(16));
        description.setWrapText(true);

        Button startTugOfWarBtn = new Button("줄다리기 게임 시작");
        startTugOfWarBtn.setFont(Font.font("System", FontWeight.BOLD, 18));
        startTugOfWarBtn.setOnAction(e -> launchTugOfWar(primaryStage));
        startTugOfWarBtn.setDefaultButton(true);

        // 3. '성 지키기' 시작 버튼 추가
        Button startCastleDefenseBtn = new Button("성 지키기 게임 시작");
        startCastleDefenseBtn.setFont(Font.font("System", FontWeight.BOLD, 18));
        startCastleDefenseBtn.setOnAction(e -> launchCastleDefense(primaryStage));

        // 4. VBox에 두 버튼 모두 추가
        VBox centerBox = new VBox(20, title, description, startTugOfWarBtn, startCastleDefenseBtn);
        centerBox.setAlignment(Pos.CENTER);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(40));
        root.setCenter(centerBox);

        // 5. 버튼이 늘어났으니 세로 길이 약간 늘리기
        Scene scene = new Scene(root, 640, 480); 
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void launchTugOfWar(Stage owner) {
        TugOfWarGame game = new TugOfWarGame();
        game.initOwner(owner);
        game.show();
    }

    // 6. '성 지키기' 실행 메서드 추가
    private void launchCastleDefense(Stage owner) {
        CastleDefenseGame game = new CastleDefenseGame();
        game.initOwner(owner);
        game.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}