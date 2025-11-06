package typingarena.app;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox; // [수정] HBox 대신 VBox를 임포트
import javafx.scene.paint.Color; // [수정] 텍스트 색상용 임포트
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

// 3개 게임 모두 임포트
import typingarena.minigames.tugofwar.TugOfWarGame;
import typingarena.minigames.castledefense.CastleDefenseGame;
import typingarena.minigames.landgrab.LandGrabGame;

public class TypingGameApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Typing Mini Game");

        // --- 1. 제목 ---
        Label title = new Label("멀티플레이 타자 미니게임 로비");
        title.setFont(Font.font("System", FontWeight.BOLD, 30));
        title.setTextFill(Color.rgb(30, 30, 30)); // 진한 회색 텍스트

        // --- 2. 설명 ---
        Label description = new Label(
                "준비된 미니게임:\n\n" // [수정] 목록 앞에 한 줄 띄우기
                        + "1. 줄다리기 타자 대전 (Tug of War)\n"
                        + "2. 성 지키기 (Castle Defense)\n"
                        + "3. 칸 채우기 땅따먹기 (Land Grab)\n\n"
                        + "시작 버튼을 누르면 새 창에서 게임이 실행됩니다."
        );
        description.setFont(Font.font("System", FontWeight.NORMAL, 16));
        description.setTextFill(Color.rgb(100, 100, 100)); // 연한 회색 텍스트
        description.setAlignment(Pos.CENTER);
        description.setLineSpacing(4); // 줄 간격 추가

        // --- 3. 버튼 생성 및 스타일링 ---

        // [수정] 공용 버튼 스타일 (밝고 미니멀한 파스텔톤)
        String commonBtnStyle = "-fx-background-color: #EBF5FF; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 16px; " +
                "-fx-background-radius: 8; " + // 둥근 모서리
                "-fx-border-color: #B0D7FF; " +
                "-fx-border-radius: 8; " +
                "-fx-border-width: 1px;";

        // [수정] 마우스 올렸을 때 스타일
        String hoverBtnStyle = "-fx-background-color: #D6EAFF; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 16px; " +
                "-fx-background-radius: 8; " +
                "-fx-border-color: #0078FF; " + // 테두리 강조
                "-fx-border-radius: 8; " +
                "-fx-border-width: 1px;";

        // 줄다리기 버튼
        Button startTugOfWarBtn = new Button("줄다리기 게임 시작");
        startTugOfWarBtn.setStyle(commonBtnStyle);
        startTugOfWarBtn.setMinWidth(280); // [수정] 최소 너비 지정
        startTugOfWarBtn.setOnAction(e -> launchTugOfWar(primaryStage));
        startTugOfWarBtn.setOnMouseEntered(e -> startTugOfWarBtn.setStyle(hoverBtnStyle));
        startTugOfWarBtn.setOnMouseExited(e -> startTugOfWarBtn.setStyle(commonBtnStyle));

        // 성 지키기 버튼
        Button startCastleDefenseBtn = new Button("성 지키기 게임 시작");
        startCastleDefenseBtn.setStyle(commonBtnStyle);
        startCastleDefenseBtn.setMinWidth(280); // [수정] 최소 너비 지정
        startCastleDefenseBtn.setOnAction(e -> launchCastleDefense(primaryStage));
        startCastleDefenseBtn.setOnMouseEntered(e -> startCastleDefenseBtn.setStyle(hoverBtnStyle));
        startCastleDefenseBtn.setOnMouseExited(e -> startCastleDefenseBtn.setStyle(commonBtnStyle));

        // 땅따먹기 버튼
        Button startLandGrabBtn = new Button("땅따먹기 게임 시작");
        startLandGrabBtn.setStyle(commonBtnStyle);
        startLandGrabBtn.setMinWidth(280); // [수정] 최소 너비 지정
        startLandGrabBtn.setOnAction(e -> launchLandGrab(primaryStage));
        startLandGrabBtn.setOnMouseEntered(e -> startLandGrabBtn.setStyle(hoverBtnStyle));
        startLandGrabBtn.setOnMouseExited(e -> startLandGrabBtn.setStyle(commonBtnStyle));

        // --- 4. 레이아웃 (VBox: 세로 정렬) ---

        // [수정] HBox 대신 VBox를 사용해 버튼들을 세로로 쌓음
        VBox buttonBox = new VBox(15, startTugOfWarBtn, startCastleDefenseBtn, startLandGrabBtn);
        buttonBox.setAlignment(Pos.CENTER);

        // 전체 레이아웃
        VBox centerBox = new VBox(25, title, description, buttonBox); // 간격 조정
        centerBox.setAlignment(Pos.CENTER);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #FFFFFF;"); // [수정] 밝은 흰색 배경
        root.setPadding(new Insets(40));
        root.setCenter(centerBox);

        // [수정] 3개 버튼이 세로로 쌓였으므로 높이를 늘림 (640 x 520)
        Scene scene = new Scene(root, 640, 520);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // --- 5. 실행 메서드 (3개 모두 포함) ---

    // 1. 줄다리기
    private void launchTugOfWar(Stage owner) {
        TugOfWarGame game = new TugOfWarGame();
        game.initOwner(owner);
        game.show();
    }

    // 2. 성 지키기
    private void launchCastleDefense(Stage owner) {
        CastleDefenseGame game = new CastleDefenseGame();
        game.initOwner(owner);
        game.show();
    }

    // 3. 땅따먹기
    private void launchLandGrab(Stage owner) {
        LandGrabGame game = new LandGrabGame();
        game.initOwner(owner);
        game.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}