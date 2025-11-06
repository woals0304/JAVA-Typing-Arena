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
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import typingarena.minigames.landgrab.LandGrabEffects.ItemType;

/**
 * [수정됨]
 * 1. [룰 1] '중립' 점수판(lblNeutralScore) 관련 코드 모두 제거
 * 2. [신규] Logic의 스플래시 콜백을 Panel의 애니메이션 메서드에 연결
 * 3. [수정] Panel이 StackPane으로 변경됨에 따라 불필요한 resizePanel 메서드 및 리스너 제거
 */
public class LandGrabGame extends Stage {

    private final LandGrabLogic logic = new LandGrabLogic();
    private final LandGrabPanel landGrabPanel = new LandGrabPanel(logic); // Panel이 StackPane임

    // ===== 2. HUD 라벨 (수정) =====
    private final Label lblTime = new Label("남은 시간: 60.0s");
    private final Label lblMyScore = new Label("나: 0칸");
    private final Label lblAiScore = new Label("AI: 0칸");
    // private final Label lblNeutralScore = new Label("중립: 0칸"); // [수정] 제거
    private final Label lblCombo = new Label("콤보: 0"); // (어제 추가한 것)
    private final Label lblEffects = new Label("효과: 없음");
    private final Label lblLastItem = new Label("최근 아이템: 없음");

    private final TextField inputField = new TextField();
    private final Button startButton = new Button("게임 시작");
    private final Timeline gameLoop;

    private long lastItemNotifiedAt = 0L;

    public LandGrabGame() {
        setTitle("Typing Arena - 땅따먹기");
        initModality(Modality.NONE);
        BorderPane root = new BorderPane();

        // [신규] Logic에서 스플래시 이벤트가 발생하면 Panel의 애니메이션을 호출하도록 연결
        // (UI 요소가 생성된 후, 게임 루프가 시작되기 전에 설정)
        logic.setOnSplashCallback((coords) -> {
            if (landGrabPanel != null && landGrabPanel.getScene() != null) {
                // coords[0] = r, coords[1] = c
                landGrabPanel.showSplashAnimation(coords[0], coords[1]);
            }
        });

        // ===== 상단 HUD (수정) =====
        // [수정] lblNeutralScore 제거
        HBox top = new HBox(18, lblTime, lblMyScore, lblAiScore, lblCombo, lblEffects, lblLastItem);
        top.setAlignment(Pos.CENTER);
        top.setPadding(new Insets(12, 24, 12, 24));
        Font hudFont = Font.font("System", FontWeight.BOLD, 16);
        lblTime.setFont(hudFont);
        lblMyScore.setFont(hudFont);
        lblAiScore.setFont(hudFont);
        // lblNeutralScore.setFont(hudFont); // [수정] 제거
        lblCombo.setFont(hudFont); // (어제 추가한 것)
        lblEffects.setFont(hudFont);
        lblLastItem.setFont(hudFont);

        // ===== 중앙(경기장) (수정) =====
        // LandGrabPanel이 StackPane이 되었고, 내부에서 Canvas 크기를 스스로 조절함.
        // 따라서 centerWrapper는 패딩 역할만 하도록 하고, 수동 리사이즈 로직(resizePanel) 제거.
        StackPane centerWrapper = new StackPane(landGrabPanel);
        centerWrapper.setPadding(new Insets(0, 24, 0, 24));
        centerWrapper.setMinSize(300, 300);
        // [수정] resizePanel() 및 관련 리스너 제거 (StackPane이 자동 처리)
        // centerWrapper.widthProperty().addListener(...);
        // centerWrapper.heightProperty().addListener(...);

        // ===== 하단(입력창 + 시작 버튼) (동일) =====
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
        Scene scene = new Scene(root, 700, 800);
        setScene(scene);

        // ===== 이벤트 바인딩 (동일) =====
        inputField.setOnAction(e -> handleSubmit());
        scene.setOnMouseClicked(e -> {
            if (!inputField.isDisabled()) inputField.requestFocus();
        });
        setOnShown(e -> {
            landGrabPanel.activate();
            // landGrabPanel.redraw(); // activate()가 redraw()를 호출하도록 3단계에서 수정됨
        });
        gameLoop = new Timeline(new KeyFrame(Duration.millis(100), e -> onTick()));
        gameLoop.setCycleCount(Timeline.INDEFINITE);
        setOnCloseRequest(e -> gameLoop.stop());
        setOnHidden(e -> {
            gameLoop.stop();
            landGrabPanel.dispose();
        });

        updateHUD();
    }

    // [수정] resizePanel 메서드 전체 제거
    // (LandGrabPanel(StackPane)이 내부의 Canvas 크기를 스스로 조절함)

    private void startGame() {
        logic.startGame();
        startButton.setDisable(true);
        inputField.setDisable(false);
        inputField.clear();
        inputField.requestFocus();
        lastItemNotifiedAt = 0L;
        updateHUD();
        landGrabPanel.redraw(); // 게임 시작 시 패널 즉시 갱신
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
            landGrabPanel.flashHit();
            // Panel 갱신은 다음 onTick (0.1초 이내)에서 처리됨
        } else {
            landGrabPanel.flashMiss();
        }
        inputField.clear();
        inputField.requestFocus();
        updateHUD();
    }

    private void onTick() {
        if (!isShowing()) {
            gameLoop.stop();
            return;
        }
        String result = logic.tick();
        updateHUD();
        landGrabPanel.redraw(); // 0.1초마다 패널 갱신 (애니메이션과 무관하게 상태 변경)
        if (result != null) {
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
        // [수정] 중립 점수 제거
        alert.setContentText(message + "\n나: " + logic.getScorePlayer() + "칸 / AI: " + logic.getScoreAI() + "칸");
        alert.initOwner(getOwner() != null ? getOwner() : this);
        alert.show();
    }

    /**
     * [수정됨] HUD 갱신 (중립 점수 제거)
     */
    private void updateHUD() {
        if (!isShowing()) {
            return;
        }

        lblTime.setText(String.format("남은 시간: %.1fs", logic.getTimeMs() / 1000.0));
        lblMyScore.setText("나: " + logic.getScorePlayer() + "칸");
        lblAiScore.setText("AI: " + logic.getScoreAI() + "칸");
        // lblNeutralScore.setText("중립: " + logic.getScoreNeutral() + "칸"); // [수정] 제거
        lblCombo.setText("콤보: " + logic.getCombo());
        lblEffects.setText(logic.getEffects().describeEffects());

        ItemType itemType = logic.getEffects().getLastActivatedItem();
        lblLastItem.setText("최근 아이템: " + formatItemLabel(itemType));

        long activatedAt = logic.getEffects().getLastItemActivatedAt();
        if (itemType != ItemType.NONE && activatedAt > lastItemNotifiedAt) {
            lastItemNotifiedAt = activatedAt;
            landGrabPanel.flashBuffColor(colorForItem(itemType));
        }
    }

    private String formatItemLabel(ItemType itemType) {
        return switch (itemType) {
            case BUFF_SPLASH -> "스플래시"; // [수정] 이름 변경
            case TRAP_BLIND -> "먹물";
            default -> "없음";
        };
    }

    private Color colorForItem(ItemType itemType) {
        return switch (itemType) {
            case BUFF_SPLASH -> Color.rgb(80, 160, 255);
            case TRAP_BLIND -> Color.rgb(30, 30, 30);
            default -> Color.TRANSPARENT;
        };
    }
}