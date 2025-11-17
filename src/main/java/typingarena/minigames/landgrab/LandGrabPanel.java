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

// [신규] core 패키지의 클래스들을 import
import typingarena.core.landgrab.LandGrabLogic; // (Enum을 사용하기 위해 import)
import typingarena.core.landgrab.LandGrabEffects;
import typingarena.core.landgrab.LandGrabViewState; // [신규] ViewState import

import java.io.InputStream;
import java.util.List;

/**
 * [대규모 리팩토링됨]
 * 1. 'RopePanel'처럼 '바보' View 역할만 수행
 * 2. 생성자에서 'coreLogic' 참조 제거
 * 3. 'updateState(LandGrabViewState state)' 메서드 신규 추가
 * 4. draw() 메서드가 'coreLogic'이 아닌 'state' 변수를 참조하여 그림
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

    // ===== 2. 상태 (수정) =====
    // [제거] coreLogic 참조 제거
    // private final LandGrabLogic logic;

    // [신규] 'RopePanel'처럼 ViewState를 가짐
    private LandGrabViewState state = new LandGrabViewState(); // (빈 화면으로 초기화)

    private boolean disposed = false;
    private Color flashColor = null;
    private long flashUntil = 0L;
    private Color buffFlashColor = null;
    private long buffFlashUntil = 0L;

    private final Canvas canvas = new Canvas();
    private final Pane animationPane = new Pane();

    // (폰트, 이미지 로딩 동일)
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
            // System.out.println("커스텀 폰트 로드 성공: " + loadedFont.getName());
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

    /**
     * [수정] 생성자: 이제 아무것도 주입받지 않음
     */
    public LandGrabPanel() {
        // [제거] this.logic = logic;

        animationPane.setMouseTransparent(true);
        getChildren().addAll(canvas, animationPane);
        setAlignment(Pos.CENTER);

        widthProperty().addListener((obs, oldV, newV) -> resizeCanvas(newV.doubleValue(), getHeight()));
        heightProperty().addListener((obs, oldV, newV) -> resizeCanvas(getWidth(), newV.doubleValue()));
    }

    // (resizeCanvas, redraw 동일)
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

    /**
     * [신규] 'RopePanel.updateState'와 동일한 역할
     * Controller가 이 메서드를 호출하여 화면을 갱신합니다.
     */
    public void updateState(LandGrabViewState newState) {
        if (newState == null) {
            this.state = new LandGrabViewState(); // (빈 상태로)
        } else {
            this.state = newState;
        }
        redraw(); // 새 상태를 받았으니 화면을 다시 그림
    }


    /**
     * [수정] draw 메서드: 모든 'logic.' 호출을 'this.state.'으로 변경
     */
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
                // [수정] this.state에서 상태 가져오기
                LandGrabLogic.TileState state = this.state.getTileState(r, c);

                if (state == null) {
                    state = LandGrabLogic.TileState.EMPTY; // (NPE 방어)
                }

                switch (state) {
                    case PLAYER: gc.setFill(TILE_PLAYER_COLOR); break;
                    case AI:     gc.setFill(TILE_AI_COLOR);     break;
                    case EMPTY:  gc.setFill(TILE_EMPTY_COLOR);  break;
                }
                gc.fillRect(x, y, tileSizeW, tileSizeH);

                // [수정] this.state에서 상태 가져오기
                String word = this.state.getWord(r, c);
                if (word == null || word.isEmpty()) continue;
                // [수정] this.state에서 상태 가져오기
                LandGrabLogic.WordModifier modifier = this.state.getModifier(r, c);

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

        // [수정] this.state에서 상태 가져오기
        List<LandGrabEffects.BlindedTile> blindedTiles = this.state.getActiveBlindedTiles();
        if (blindedTiles != null && !blindedTiles.isEmpty() && inkSplatImage != null) {
            for (LandGrabEffects.BlindedTile tile : blindedTiles) {
                int r = tile.r();
                int c = tile.c();
                double x = c * tileSizeW;
                double y = r * tileSizeH;
                gc.drawImage(inkSplatImage, x, y, tileSizeW, tileSizeH);
            }
        }

        // (플래시 로직 동일)
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

    // (showSplashAnimation, showInkSplashAnimation, flashHit, flashMiss,
    //  flashBuffColor, dispose, activate...
    //  ...이하 모든 헬퍼 메서드는 원본과 동일)

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