package typingarena.minigames.landgrab;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.io.InputStream;
import java.util.List; // [신규] List 임포트

/**
 * [대규모 수정됨]
 * 1. ... (이전 수정 사항들) ...
 * 15. [신규] 창 크기 조절 시 애니메이션 위치가 어긋나는 버그 수정 (offsetX, offsetY 계산 추가)
 * 16. [신규] 여러 개의 먹물 타일을 동시에 그릴 수 있도록 draw() 메서드 수정 (List<BlindedTile> 사용)
 */
public class LandGrabPanel extends StackPane {

    // ===== 1. 색상 정의 (동일) =====
    private static final Color BG_COLOR = Color.rgb(240, 240, 240);
    private static final Color GRID_LINE_COLOR = Color.rgb(200, 200, 200);
    private static final Color TILE_EMPTY_COLOR = Color.rgb(255, 255, 255);
    private static final Color TILE_PLAYER_COLOR = Color.rgb(60, 120, 255);
    private static final Color TILE_AI_COLOR = Color.rgb(220, 80, 80);

    private static final Color TEXT_EMPTY_COLOR = Color.rgb(30, 30, 30);
    private static final Color TEXT_ON_CAPTURED_TILE = Color.rgb(100, 100, 100);
    private static final Color TEXT_TRAP_COLOR = Color.rgb(208, 68, 68);
    private static final Color TEXT_BUFF_COLOR = Color.rgb(0, 100, 200);

    private static final Color FLASH_HIT = Color.rgb(50, 200, 120);
    private static final Color FLASH_MISS = Color.rgb(220, 80, 80);

    // ===== 2. 상태 (동일) =====
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

    private final Font splashAnimationFont = loadCustomFont("fonts/CookieRun Regular.otf", 32);

    private final Image inkSplatImage = loadImage("images/ink_splat.png");

    // (loadCustomFont, loadImage 헬퍼 메서드들은 동일)
    private Font loadCustomFont(String fontPath, double size) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(fontPath)) {
            if (is == null) {
                System.err.println("커스텀 폰트 로드 실패 (파일 없음): " + fontPath);
                return Font.font("System", FontWeight.BOLD, size);
            }
            Font loadedFont = Font.loadFont(is, size);
            if (loadedFont == null) {
                System.err.println("커스텀 폰트 파싱 실패: " + fontPath);
                return Font.font("System", FontWeight.BOLD, size);
            }
            System.out.println("커스텀 폰트 로드 성공: " + loadedFont.getName());
            return loadedFont;
        } catch (Exception e) {
            e.printStackTrace();
            return Font.font("System", FontWeight.BOLD, size);
        }
    }

    private Image loadImage(String imagePath) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(imagePath)) {
            if (is == null) {
                System.err.println("이미지 로드 실패 (파일 없음): " + imagePath);
                return null;
            }
            return new Image(is);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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

    // (draw 메서드는 NullPointerException 수정된 버전과 동일)
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

        for (int r = 0; r < LandGrabLogic.GRID_SIZE; r++) {
            for (int c = 0; c < LandGrabLogic.GRID_SIZE; c++) {
                double x = c * tileSizeW;
                double y = r * tileSizeH;
                LandGrabLogic.TileState state = logic.getTileState(r, c);

                if (state == null) {
                    continue;
                }

                switch (state) {
                    case PLAYER: gc.setFill(TILE_PLAYER_COLOR); break;
                    case AI:     gc.setFill(TILE_AI_COLOR);     break;
                    case EMPTY:  gc.setFill(TILE_EMPTY_COLOR);  break;
                }
                gc.fillRect(x, y, tileSizeW, tileSizeH);

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

        gc.setStroke(GRID_LINE_COLOR);
        gc.setLineWidth(1);
        for (int i = 0; i <= LandGrabLogic.GRID_SIZE; i++) {
            gc.strokeLine(i * tileSizeW, 0, i * tileSizeW, h);
            gc.strokeLine(0, i * tileSizeH, w, i * tileSizeH);
        }

        // ===== 4) 먹물 효과 (대규모 수정) =====

        // [수정] 단일 좌표 -> List<BlindedTile>로 변경
        List<LandGrabEffects.BlindedTile> blindedTiles = logic.getEffects().getActiveBlindedTiles();
        if (blindedTiles != null && !blindedTiles.isEmpty() && inkSplatImage != null) {

            // [수정] List에 있는 모든 타일에 대해 반복
            for (LandGrabEffects.BlindedTile tile : blindedTiles) {
                int r = tile.r();
                int c = tile.c();

                double x = c * tileSizeW;
                double y = r * tileSizeH;

                // 먹물 이미지를 타일 크기에 정확히 맞춰서 그리기
                gc.drawImage(inkSplatImage, x, y, tileSizeW, tileSizeH);
            }
        }
        // ===================================


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
     * [신규] '스플래시!' 애니메이션
     */
    public void showSplashAnimation(int r, int c) {
        if (disposed || getScene() == null) return;

        Label splashLabel = new Label("스플래시!");
        splashLabel.setFont(splashAnimationFont);
        splashLabel.setCache(true);

        splashLabel.setStyle(
                "-fx-text-fill: linear-gradient(from 0% 0% to 0% 100%, white 20%, #90C8FF 80%); " +
                        "-fx-stroke: #003366; " +
                        "-fx-stroke-width: 2;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 4, 0.4, 0, 2);"
        );

        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0) return;

        double offsetX = (this.getWidth() - w) / 2.0;
        double offsetY = (this.getHeight() - h) / 2.0;

        double tileSizeW = w / LandGrabLogic.GRID_SIZE;
        double tileSizeH = h / LandGrabLogic.GRID_SIZE;

        double startX = c * tileSizeW + (tileSizeW / 2);
        double startY = r * tileSizeH + (tileSizeH / 2);

        splashLabel.setLayoutX(offsetX + startX - 45);
        splashLabel.setLayoutY(offsetY + startY - 15);

        Duration duration = Duration.millis(1200);

        FadeTransition ft = new FadeTransition(duration, splashLabel);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setDelay(Duration.millis(300));

        TranslateTransition tt = new TranslateTransition(duration, splashLabel);
        tt.setByY(-80);
        tt.setCycleCount(1);

        ParallelTransition pt = new ParallelTransition(splashLabel, ft, tt);
        pt.setOnFinished(e -> {
            animationPane.getChildren().remove(splashLabel);
        });

        animationPane.getChildren().add(splashLabel);
        pt.play();
    }

    /**
     * [신규] '먹물!' 애니메이션
     */
    public void showInkSplashAnimation(int r, int c) {
        if (disposed || getScene() == null) return;

        Label inkLabel = new Label("먹물!");
        inkLabel.setFont(splashAnimationFont);
        inkLabel.setCache(true);

        inkLabel.setStyle(
                "-fx-text-fill: linear-gradient(from 0% 0% to 0% 100%, #BBBBBB 20%, #444444 80%); " +
                        "-fx-stroke: #000000; " +
                        "-fx-stroke-width: 2.5;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 6, 0.6, 0, 3);"
        );

        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0) return;

        double offsetX = (this.getWidth() - w) / 2.0;
        double offsetY = (this.getHeight() - h) / 2.0;

        double tileSizeW = w / LandGrabLogic.GRID_SIZE;
        double tileSizeH = h / LandGrabLogic.GRID_SIZE;

        double startX = c * tileSizeW + (tileSizeW / 2);
        double startY = r * tileSizeH + (tileSizeH / 2);

        inkLabel.setLayoutX(offsetX + startX - 45);
        inkLabel.setLayoutY(offsetY + startY - 15);

        Duration duration = Duration.millis(1200);

        FadeTransition ft = new FadeTransition(duration, inkLabel);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setDelay(Duration.millis(300));

        TranslateTransition tt = new TranslateTransition(duration, inkLabel);
        tt.setByY(-80);
        tt.setCycleCount(1);

        ParallelTransition pt = new ParallelTransition(inkLabel, ft, tt);
        pt.setOnFinished(e -> {
            animationPane.getChildren().remove(inkLabel);
        });

        animationPane.getChildren().add(inkLabel);
        pt.play();
    }


    // (flashHit, flashMiss, flashBuffColor, dispose, activate 메서드 동일)
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