package typingarena.minigames.landgrab;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * '땅따먹기'의 메인 창, UI, 게임루프, 입력 처리 (TugOfWarGame.java와 동일한 '결')
 * Logic(두뇌)과 Panel(캔버스)을 조립하고 제어함.
 */
public class LandGrabGame extends Stage {

    // ===== 1. Model / View 소유 (TugOfWarGame과 동일) =====
    private final LandGrabLogic logic = new LandGrabLogic();
    private final LandGrabPanel landGrabPanel = new LandGrabPanel(logic);

    // ===== 2. HUD 라벨 (땅따먹기용으로 수정) =====
    private final Label lblTime = new Label("남은 시간: 60.0s");
    private final Label lblMyScore = new Label("나: 0칸");
    private final Label lblAiScore = new Label("AI: 0칸");
    private final Label lblCombo = new Label("콤보: 0");
    private final Label lblEffects = new Label("효과: 없음");

    // ===== 3. UI 컨트롤 =====
    private final TextField inputField = new TextField();
    private final Button startButton = new Button("게임 시작");
    private final Timeline gameLoop;

    public LandGrabGame() {
        setTitle("Typing Arena - 땅따먹기");
        initModality(Modality.NONE);

        BorderPane root = new BorderPane();

        // ===== 상단 HUD (라벨만 수정) =====
        HBox top = new HBox(18, lblTime, lblMyScore, lblAiScore, lblCombo, lblEffects);
        top.setAlignment(Pos.CENTER);
        top.setPadding(new Insets(12, 24, 12, 24));
        Font hudFont = Font.font("System", FontWeight.BOLD, 16);
        lblTime.setFont(hudFont);
        lblMyScore.setFont(hudFont);
        lblAiScore.setFont(hudFont);
        lblCombo.setFont(hudFont);
        lblEffects.setFont(hudFont);

        // ===== 중앙(경기장) (RopePanel -> LandGrabPanel) =====
        landGrabPanel.setWidth(600);  // 땅따먹기는 정사각형이 좋음
        landGrabPanel.setHeight(600);
        StackPane centerWrapper = new StackPane(landGrabPanel);
        centerWrapper.setPadding(new Insets(0, 24, 0, 24));
        centerWrapper.setMinSize(300, 300);

        // 창 크기 조절 시 Panel도 같이 조절 (TugOfWarGame과 동일)
        // 정사각형 비율을 유지하도록 수정
        centerWrapper.widthProperty().addListener((obs, oldV, newV) -> resizePanel(centerWrapper));
        centerWrapper.heightProperty().addListener((obs, oldV, newV) -> resizePanel(centerWrapper));


        // ===== 하단(입력창 + 시작 버튼) (TugOfWarGame과 동일) =====
        inputField.setFont(Font.font("System", FontWeight.NORMAL, 22));
        inputField.setPromptText("단어를 입력하고 Enter 키를 누르세요");
        inputField.setDisable(true);

        startButton.setFont(Font.font("System", FontWeight.BOLD, 18));
        startButton.setOnAction(e -> startGame());

        HBox bottomContent = new HBox(12, inputField, startButton);
        bottomContent.setAlignment(Pos.CENTER);
        HBox.setHgrow(inputField, Priority.ALWAYS);

        BorderPane bottom = new BorderPane();
        bottom.setPadding(new Insets(16, 24, 16, 24));
        bottom.setCenter(bottomContent);

        root.setTop(top);
        root.setCenter(centerWrapper);
        root.setBottom(bottom);

        // Scene 크기 조절 (가로가 더 넓도록)
        Scene scene = new Scene(root, 700, 800);
        setScene(scene);

        // ===== 이벤트 바인딩 (TugOfWarGame과 동일) =====
        inputField.setOnAction(e -> handleSubmit());
        scene.setOnMouseClicked(e -> {
            if (!inputField.isDisabled()) {
                inputField.requestFocus();
            }
        });

        setOnShown(e -> {
            landGrabPanel.activate();
            landGrabPanel.redraw();
        });

        // 게임 루프: 100ms마다 tick (TugOfWarGame과 동일)
        gameLoop = new Timeline(new KeyFrame(Duration.millis(100), e -> onTick()));
        gameLoop.setCycleCount(Timeline.INDEFINITE);
        setOnCloseRequest(e -> gameLoop.stop());
        setOnHidden(e -> {
            gameLoop.stop();
            landGrabPanel.dispose();
        });

        updateHUD();
    }

    // 캔버스가 정사각형 비율을 유지하도록 리사이즈 로직 수정
    private void resizePanel(StackPane wrapper) {
        double w = wrapper.getWidth();
        double h = wrapper.getHeight();
        if (w <= 0 || h <= 0) return;

        double size = Math.min(w, h); // 가로/세로 중 더 짧은 쪽에 맞춤
        landGrabPanel.setWidth(size);
        landGrabPanel.setHeight(size);
        landGrabPanel.redraw();
    }

    private void startGame() {
        logic.startGame();
        startButton.setDisable(true);
        inputField.setDisable(false);
        inputField.clear();
        inputField.requestFocus();

        updateHUD();
        landGrabPanel.redraw();
        gameLoop.playFromStart();
    }

    private void handleSubmit() {
        if (!logic.isRunning()) {
            inputField.clear();
            return;
        }

        String typed = inputField.getText().trim();
        boolean correct = logic.submitAnswer(typed);

        if (correct) {
            landGrabPanel.flashHit(); // 정답 효과
        } else {
            // landGrabPanel.flashMiss(); // 오답 효과는 굳이?
        }

        inputField.clear();
        inputField.requestFocus();

        updateHUD();
        // redraw()는 onTick에서 어차피 하므로 여기서는 생략 (해도 됨)
    }

    private void onTick() {
        if (!isShowing()) {
            gameLoop.stop();
            return;
        }

        String result = logic.tick();
        updateHUD();
        landGrabPanel.redraw(); // 매 틱마다 캔버스 새로 그리기

        if (result != null) { // 게임 종료
            gameLoop.stop();
            startButton.setDisable(false);
            inputField.setDisable(true);
            showResultDialog(result);
        }
    }

    private void showResultDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("결과");
        alert.setHeaderText("게임 종료");
        alert.setContentText(message + "\n나: " + logic.getScorePlayer() + "칸 / AI: " + logic.getScoreAI() + "칸");
        alert.initOwner(getOwner() != null ? getOwner() : this);
        alert.show();
    }

    private void updateHUD() {
        if (!isShowing()) {
            return;
        }

        lblTime.setText(String.format("남은 시간: %.1fs", logic.getTimeMs() / 1000.0));
        lblMyScore.setText("나: " + logic.getScorePlayer() + "칸");
        lblAiScore.setText("AI: " + logic.getScoreAI() + "칸");
        lblCombo.setText("콤보: " + logic.getCombo());
        lblEffects.setText(logic.getEffects().describeEffects());
    }
}