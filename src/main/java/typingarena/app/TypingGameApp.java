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

public class TypingGameApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Typing Mini Game");

        Label title = new Label("멀티플레이 타자 미니게임 로비");
        title.setFont(Font.font("System", FontWeight.BOLD, 28));
        title.setWrapText(true);

        Label description = new Label(
                "준비된 미니게임:\n"
                        + "- 줄다리기 타자 대전 (Tug of War)\n\n"
                        + "시작 버튼을 누르면 새 창에서 게임이 실행됩니다."
        );
        description.setFont(Font.font(16));
        description.setWrapText(true);

        Button startBtn = new Button("줄다리기 게임 시작");
        startBtn.setFont(Font.font("System", FontWeight.BOLD, 18));
        startBtn.setOnAction(e -> launchTugOfWar(primaryStage));
        startBtn.setDefaultButton(true);

        VBox centerBox = new VBox(20, title, description, startBtn);
        centerBox.setAlignment(Pos.CENTER);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(40));
        root.setCenter(centerBox);

        Scene scene = new Scene(root, 640, 420);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void launchTugOfWar(Stage owner) {
        TugOfWarGame game = new TugOfWarGame();
        game.initOwner(owner);
        game.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
