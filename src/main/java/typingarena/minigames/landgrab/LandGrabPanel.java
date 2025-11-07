package typingarena.minigames.landgrab;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane; // [신규] 임포트
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * [대규모 수정됨]
 * 1. [룰 1] TILE_NEUTRAL_COLOR (회색) 제거
 * 2. [수정] extends Canvas -> extends StackPane (애니메이션을 위해)
 * 3. [수정] Canvas를 StackPane의 자식 멤버로 변경
 * 4. [수정] 애니메이션용 Pane (animationPane) 추가
 * 5. [룰 2] "스플래시!" 텍스트 그리는 로직 제거
 * 6. [룰 2] showSplashAnimation(r, c) 메서드 (애니메이션) 신규 추가
 */
public class LandGrabPanel extends StackPane { // [수정] extends StackPane

    // ===== 1. 색상 정의 (수정) =====
    private static final Color BG_COLOR = Color.rgb(240, 240, 240);
    private static final Color GRID_LINE_COLOR = Color.rgb(200, 200, 200);
    private static final Color TILE_EMPTY_COLOR = Color.rgb(255, 255, 255); // [룰 1] 이게 기본
    private static final Color TILE_PLAYER_COLOR = Color.rgb(60, 120, 255);
    private static final Color TILE_AI_COLOR = Color.rgb(220, 80, 80);
    // private static final Color TILE_NEUTRAL_COLOR = Color.rgb(200, 200, 200); // [수정] 제거

    private static final Color TEXT_EMPTY_COLOR = Color.rgb(30, 30, 30);
    private static final Color TEXT_ON_CAPTURED_TILE = Color.rgb(100, 100, 100);
    private static final Color TEXT_TRAP_COLOR = Color.rgb(208, 68, 68);
    private static final Color TEXT_BUFF_COLOR = Color.rgb(0, 100, 200);

    private static final Color FLASH_HIT = Color.rgb(50, 200, 120);
    private static final Color FLASH_MISS = Color.rgb(220, 80, 80);

    // ===== 2. 상태 (수정) =====
    private final LandGrabLogic logic;
    private boolean disposed = false;
    private Color flashColor = null;
    private long flashUntil = 0L;
    private Color buffFlashColor = null;
    private long buffFlashUntil = 0L;

    // [신규] StackPane의 자식으로 Canvas와 AnimationPane을 둠
    private final Canvas canvas = new Canvas();
    private final Pane animationPane = new Pane(); // 애니메이션 라벨이 올라갈 곳

    private final Font wordFont = Font.font("System", FontWeight.BOLD, 14);
    private final Font itemFont = Font.font("System", FontWeight.BOLD, FontPosture.ITALIC, 15);
    private final Font feedbackFont = Font.font("System", FontWeight.BOLD, 48);
    private final Font splashAnimationFont = Font.font("System", FontWeight.BOLD, 36); // [신규] 애니메이션용 폰트

    public LandGrabPanel(LandGrabLogic logic) {
        this.logic = logic;

        // [신규] Canvas와 AnimationPane을 StackPane(this)에 추가
        // animationPane이 Canvas 위에 오도록 순서 중요
        // animationPane은 마우스 이벤트를 가로채지 않도록 설정
        animationPane.setMouseTransparent(true);
        getChildren().addAll(canvas, animationPane);
        setAlignment(Pos.CENTER);

        // [수정] 리스너가 Canvas의 크기를 조절하도록 변경
        widthProperty().addListener((obs, oldV, newV) -> resizeCanvas(newV.doubleValue(), getHeight()));
        heightProperty().addListener((obs, oldV, newV) -> resizeCanvas(getWidth(), newV.doubleValue()));
    }

    /**
     * [신규] StackPane 크기에 맞춰 내부 Canvas 크기 조절
     */
    private void resizeCanvas(double w, double h) {
        if (w <= 0 || h <= 0) return;
        double size = Math.min(w, h);
        canvas.setWidth(size);
        canvas.setHeight(size);
        redraw();
    }

    public void redraw() {
        if (disposed) return;
        draw();
    }

    private void draw() {
        if (disposed) return;
        // [수정] canvas의 크기를 가져옴
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0) return;

        // [수정] canvas의 GraphicsContext를 가져옴
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        gc.setFill(BG_COLOR);
        gc.fillRect(0, 0, w, h);

        double tileSizeW = w / LandGrabLogic.GRID_SIZE;
        double tileSizeH = h / LandGrabLogic.GRID_SIZE;

        // 2) 10x10 타일 그리기 (상태에 따라)
        for (int r = 0; r < LandGrabLogic.GRID_SIZE; r++) {
            for (int c = 0; c < LandGrabLogic.GRID_SIZE; c++) {
                double x = c * tileSizeW;
                double y = r * tileSizeH;
                LandGrabLogic.TileState state = logic.getTileState(r, c);
                if (state == null) {
                    state = LandGrabLogic.TileState.EMPTY;
                }

                // [수정] 중립 타일 제거
                switch (state) {
                    case PLAYER: gc.setFill(TILE_PLAYER_COLOR); break;
                    case AI:     gc.setFill(TILE_AI_COLOR);     break;
                    case EMPTY:  gc.setFill(TILE_EMPTY_COLOR);  break;
                }
                gc.fillRect(x, y, tileSizeW, tileSizeH);

                // 단어 텍스트 그리기 (동일)
                String word = logic.getWord(r, c);
                if (word == null || word.isEmpty()) continue;
                LandGrabLogic.WordModifier modifier = logic.getModifier(r, c);

                if (modifier == LandGrabLogic.WordModifier.TRAP) {
                    gc.setFill(TEXT_TRAP_COLOR);
                    gc.setFont(itemFont);
                } else if (modifier == LandGrabLogic.WordModifier.BUFF) {
                    gc.setFill(TEXT_BUFF_COLOR);
                    gc.setFont(itemFont);
                } else {
                    gc.setFont(wordFont);
                    if (state == LandGrabLogic.TileState.PLAYER || state == LandGrabLogic.TileState.AI) {
                        gc.setFill(TEXT_ON_CAPTURED_TILE);
                    } else {
                        gc.setFill(TEXT_EMPTY_COLOR);
                    }
                }
                gc.setTextAlign(TextAlignment.CENTER);
                gc.fillText(word, x + tileSizeW / 2, y + tileSizeH / 2 + 5);
            }
        }

        // 3) 그리드 선 그리기 (동일)
        gc.setStroke(GRID_LINE_COLOR);
        gc.setLineWidth(1);
        for (int i = 0; i <= LandGrabLogic.GRID_SIZE; i++) {
            gc.strokeLine(i * tileSizeW, 0, i * tileSizeW, h);
            gc.strokeLine(0, i * tileSizeH, w, i * tileSizeH);
        }

        // --- [룰 2] 피드백 그리기 (제거) ---
        // [수정] logic.getEffects().isSplashTextActive() 관련 코드 모두 제거

        // 4) 먹물 효과 (동일)
        if (logic.getEffects().isBlindActive()) {
            gc.setGlobalAlpha(0.85);
            gc.setFill(Color.rgb(0, 0, 0, 0.85));
            gc.fillRoundRect(0, 0, w, h, 16, 16);
            gc.setGlobalAlpha(1.0);
            gc.setFill(Color.WHITE);
            gc.setFont(feedbackFont); // [수정] 더 큰 폰트
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("먹물!", w / 2, h / 2); // [수정] 중앙 정렬
        }

        // 5) 정답/오답/아이템 플래시 (동일)
        long now = System.currentTimeMillis();
        if (flashColor != null && now < flashUntil) {
            gc.setGlobalAlpha(0.18);
            gc.setFill(flashColor);
            gc.fillRect(0, 0, w, h);
            gc.setGlobalAlpha(1.0);
        }
        if (buffFlashColor != null && now < buffFlashUntil) {
            gc.setGlobalAlpha(0.18);
            gc.setFill(buffFlashColor);
            gc.fillRect(0, 0, w, h);
            gc.setGlobalAlpha(1.0);
        }
    }

    /**
     * [신규] 스플래시 애니메이션을 (r, c) 좌표에서 시작
     */
    public void showSplashAnimation(int r, int c) {
        if (disposed || getScene() == null) return;

        // 1. Label 생성
        Label splashLabel = new Label("스플래시!");
        splashLabel.setFont(splashAnimationFont);
        splashLabel.setTextFill(Color.rgb(80, 160, 255));
        splashLabel.setCache(true); // 애니메이션 성능 향상

        // 2. 좌표 계산
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0) return; // 캔버스 크기가 0이면 중단

        double tileSizeW = w / LandGrabLogic.GRID_SIZE;
        double tileSizeH = h / LandGrabLogic.GRID_SIZE;

        // 타일의 중앙 좌표
        double startX = c * tileSizeW + (tileSizeW / 2);
        double startY = r * tileSizeH + (tileSizeH / 2);

        // Label의 너비/높이를 고려하여 중앙 정렬
        // (Label이 생성된 직후에는 너비/높이가 0일 수 있으므로 Pos.CENTER 사용)
        // 여기서는 animationPane에 바로 추가하므로 LayoutX/Y 사용
        splashLabel.setLayoutX(startX - 50); // (텍스트 너비에 맞게 대략적인 오프셋)
        splashLabel.setLayoutY(startY - 20); // (텍스트 높이에 맞게 대략적인 오프셋)

        // 3. 애니메이션 설정
        Duration duration = Duration.millis(1200); // 1.2초간 지속

        // Fade: 1.0 -> 0.0 (사라지기)
        FadeTransition ft = new FadeTransition(duration, splashLabel);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setDelay(Duration.millis(300)); // 0.3초 대기 후 사라지기 시작

        // Translate: Y축으로 -80 픽셀 이동 (위로 올라가기)
        TranslateTransition tt = new TranslateTransition(duration, splashLabel);
        tt.setByY(-80);
        tt.setCycleCount(1);

        // 4. 애니메이션 결합 및 실행
        ParallelTransition pt = new ParallelTransition(splashLabel, ft, tt);
        pt.setOnFinished(e -> {
            // 애니메이션 종료 시 Pane에서 제거
            animationPane.getChildren().remove(splashLabel);
        });

        // 5. Label을 Pane에 추가하고 애니메이션 시작
        animationPane.getChildren().add(splashLabel);
        pt.play();
    }


    // (이하 동일)
    public void flashHit() {
        if (disposed || getScene() == null) return;
        flashColor = FLASH_HIT;
        flashUntil = System.currentTimeMillis() + 120;
        redraw();
    }
    public void flashMiss() {
        if (disposed || getScene() == null) return;
        flashColor = FLASH_MISS;
        flashUntil = System.currentTimeMillis() + 120;
        redraw();
    }
    public void flashBuffColor(Color color) {
        if (disposed || getScene() == null) return;
        buffFlashColor = color;
        buffFlashUntil = System.currentTimeMillis() + 200;
        redraw();
    }

    public void dispose() {
        disposed = true;
        flashColor = null;
        buffFlashColor = null;
        animationPane.getChildren().clear(); // [신규] 애니메이션 정리
    }

    public void activate() {
        disposed = false;
        flashColor = null;
        buffFlashColor = null;
        redraw(); // [수정] 활성화 시 다시 그리기
    }
}
