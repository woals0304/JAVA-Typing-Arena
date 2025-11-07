package typingarena.minigames.landgrab;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.io.InputStream;

/**
 * [대규모 수정됨]
 * 1. [룰 1] TILE_NEUTRAL_COLOR (회색) 제거
 * 2. [수정] extends Canvas -> extends StackPane (애니메이션을 위해)
 * 3. [수정] Canvas를 StackPane의 자식 멤버로 변경
 * 4. [수정] 애니메이션용 Pane (animationPane) 추가
 * 5. [룰 2] "스플래시!" 텍스트 그리는 로직 제거
 * 6. [룰 2] showSplashAnimation(r, c) 메서드 (애니메이션) 신규 추가
 * 7. [신규] loadCustomFont 헬퍼 메서드 추가 (향후 폰트 교체 대비)
 * 8. [신규] showSplashAnimation에 CSS 스타일 적용
 * 9. [수정] 'CookieRun Regular' 폰트 적용 및 CSS 스타일 업데이트
 * 10. [수정] CSS 가독성 개선 (테두리 두께 및 그라데이션 색상 변경)
 */
public class LandGrabPanel extends StackPane {

    // ===== 1. 색상 정의 (수정) =====
    private static final Color BG_COLOR = Color.rgb(240, 240, 240);
    private static final Color GRID_LINE_COLOR = Color.rgb(200, 200, 200);
    private static final Color TILE_EMPTY_COLOR = Color.rgb(255, 255, 255);
    private static final Color TILE_PLAYER_COLOR = Color.rgb(60, 120, 255);
    private static final Color TILE_AI_COLOR = Color.rgb(220, 80, 80);
    // private static final Color TILE_NEUTRAL_COLOR = Color.rgb(200, 200, 200);

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

    private final Canvas canvas = new Canvas();
    private final Pane animationPane = new Pane();

    private final Font wordFont = Font.font("System", FontWeight.BOLD, 14);
    private final Font itemFont = Font.font("System", FontWeight.BOLD, FontPosture.ITALIC, 15);
    private final Font feedbackFont = Font.font("System", FontWeight.BOLD, 48);

    // ===== [수정] 쿠키런 폰트 로드, 크기 32로 조절 =====
    private final Font splashAnimationFont = loadCustomFont("fonts/CookieRun Regular.otf", 32);

    /**
     * [신규] resources 폴더에서 커스텀 폰트를 로드하는 헬퍼 메서드
     */
    private Font loadCustomFont(String fontPath, double size) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(fontPath)) {
            if (is == null) {
                System.err.println("커스텀 폰트 로드 실패 (파일 없음): " + fontPath);
                // 폰트 로드 실패 시, 기본 'System' 폰트로 대체
                return Font.font("System", FontWeight.BOLD, size);
            }
            Font loadedFont = Font.loadFont(is, size);
            if (loadedFont == null) {
                System.err.println("커스텀 폰트 파싱 실패: " + fontPath);
                return Font.font("System", FontWeight.BOLD, size);
            }
            System.out.println("커스텀 폰트 로드 성공: " + loadedFont.getName()); // (확인용)
            return loadedFont;
        } catch (Exception e) {
            e.printStackTrace();
            // 예외 발생 시에도 기본 'System' 폰트로 대체
            return Font.font("System", FontWeight.BOLD, size);
        }
    }

    public LandGrabPanel(LandGrabLogic logic) {
        this.logic = logic;

        animationPane.setMouseTransparent(true);
        getChildren().addAll(canvas, animationPane);
        setAlignment(Pos.CENTER);

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

        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0) return;

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

        // 4) 먹물 효과 (동일)
        if (logic.getEffects().isBlindActive()) {
            gc.setGlobalAlpha(0.85);
            gc.setFill(Color.rgb(0, 0, 0, 0.85));
            gc.fillRoundRect(0, 0, w, h, 16, 16);
            gc.setGlobalAlpha(1.0);
            gc.setFill(Color.WHITE);
            gc.setFont(feedbackFont);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("먹물!", w / 2, h / 2);
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
        splashLabel.setFont(splashAnimationFont); // [수정] 쿠키런 폰트(32pt) 적용
        splashLabel.setCache(true);

        // ===== [수정] CSS 스타일 (가독성 개선) =====
        splashLabel.setStyle(
                // 그라데이션 끝 색을 #B0D8FF -> #90C8FF 로 변경
                "-fx-text-fill: linear-gradient(from 0% 0% to 0% 100%, white 20%, #90C8FF 80%); " +
                        "-fx-stroke: #003366; " + // 외곽선 색: 어두운 파란색
                        "-fx-stroke-width: 2;" + // 외곽선 굵기: 1.5 -> 2 로 변경
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 4, 0.4, 0, 2);" // 부드러운 그림자
        );
        // ===================================

        // 2. 좌표 계산
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0) return;

        double tileSizeW = w / LandGrabLogic.GRID_SIZE;
        double tileSizeH = h / LandGrabLogic.GRID_SIZE;

        double startX = c * tileSizeW + (tileSizeW / 2);
        double startY = r * tileSizeH + (tileSizeH / 2);

        // ===== [수정] 폰트 크기 변경에 따른 오프셋 조절 =====
        splashLabel.setLayoutX(startX - 45);
        splashLabel.setLayoutY(startY - 15);

        // 3. 애니메이션 설정
        Duration duration = Duration.millis(1200);

        // Fade: 1.0 -> 0.0 (사라지기)
        FadeTransition ft = new FadeTransition(duration, splashLabel);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setDelay(Duration.millis(300));

        // Translate: Y축으로 -80 픽셀 이동 (위로 올라가기)
        TranslateTransition tt = new TranslateTransition(duration, splashLabel);
        tt.setByY(-80);
        tt.setCycleCount(1);

        // 4. 애니메이션 결합 및 실행
        ParallelTransition pt = new ParallelTransition(splashLabel, ft, tt);
        pt.setOnFinished(e -> {
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
        animationPane.getChildren().clear();
    }

    public void activate() {
        disposed = false;
        flashColor = null;
        buffFlashColor = null;
        redraw();
    }
}