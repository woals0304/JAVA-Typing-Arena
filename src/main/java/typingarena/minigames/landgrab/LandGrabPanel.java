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

import typingarena.core.landgrab.LandGrabLogic;
import typingarena.core.landgrab.LandGrabEffects;
import typingarena.core.landgrab.LandGrabViewState;

import java.io.InputStream;
import java.util.List;

public class LandGrabPanel extends StackPane {

    private static final Color BG_COLOR = Color.rgb(240, 240, 240);
    private static final Color GRID_LINE_COLOR = Color.rgb(200, 200, 200);
    private static final Color TILE_EMPTY_COLOR = Color.rgb(255, 255, 255);
    private static final Color TILE_PLAYER_A_COLOR = Color.rgb(60, 120, 255);
    private static final Color TILE_PLAYER_B_COLOR = Color.rgb(220, 80, 80);

    private static final Color TEXT_EMPTY_COLOR = Color.rgb(30, 30, 30);
    private static final Color TEXT_ON_CAPTURED_TILE = Color.rgb(240, 240, 240);

    // [수정] 아이템 색상 통일 (진한 금색)
    private static final Color TEXT_ITEM_COLOR = Color.rgb(218, 165, 32);

    private static final Color FLASH_HIT = Color.rgb(50, 200, 120);
    private static final Color FLASH_MISS = Color.rgb(220, 80, 80);

    private LandGrabViewState state = new LandGrabViewState();
    private boolean disposed = false;

    private boolean isWordFlipped = false;
    // [수정] A와 B의 보호막 상태를 각각 관리
    private boolean barrierActiveA = false;
    private boolean barrierActiveB = false;

    private Color flashColor = null;
    private long flashUntil = 0L;
    private Color buffFlashColor = null;
    private long buffFlashUntil = 0L;

    private final Canvas canvas = new Canvas();
    private final Pane animationPane = new Pane();

    private final Font wordFont = Font.font("System", FontWeight.BOLD, 14);
    private final Font itemFont = Font.font("System", FontWeight.BOLD, FontPosture.ITALIC, 15);
    private final Font splashAnimationFont = loadCustomFont("fonts/CookieRun Regular.otf", 32);
    private final Image inkSplatImage = loadImage("images/ink_splat.png");

    public LandGrabPanel() {
        animationPane.setMouseTransparent(true);
        getChildren().addAll(canvas, animationPane);
        setAlignment(Pos.CENTER);
        widthProperty().addListener((obs, o, n) -> resizeCanvas(n.doubleValue(), getHeight()));
        heightProperty().addListener((obs, o, n) -> resizeCanvas(getWidth(), n.doubleValue()));
    }

    private void resizeCanvas(double w, double h) {
        if (w <= 0 || h <= 0) return;
        double size = Math.min(w, h);
        canvas.setWidth(size);
        canvas.setHeight(size);
        redraw();
    }

    // [수정] 배리어 상태 2개 받기
    public void setExtraEffects(boolean flipWords, boolean barrierA, boolean barrierB) {
        this.isWordFlipped = flipWords;
        this.barrierActiveA = barrierA;
        this.barrierActiveB = barrierB;
    }

    public void updateState(LandGrabViewState newState) {
        this.state = (newState == null) ? new LandGrabViewState() : newState;
        redraw();
    }

    public void redraw() {
        if (disposed) return;
        draw();
    }

    private void draw() {
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

                LandGrabLogic.TileState ts = this.state.getTileState(r, c);
                if (ts == null) ts = LandGrabLogic.TileState.EMPTY;

                switch (ts) {
                    case PLAYER_A: gc.setFill(TILE_PLAYER_A_COLOR); break;
                    case PLAYER_B: gc.setFill(TILE_PLAYER_B_COLOR); break;
                    default:       gc.setFill(TILE_EMPTY_COLOR);  break;
                }
                gc.fillRect(x, y, tileSizeW, tileSizeH);

                // [수정] 개별 타일 보호막 테두리 그리기
                if (ts == LandGrabLogic.TileState.PLAYER_A && barrierActiveA) {
                    drawBarrierBorder(gc, x, y, tileSizeW, tileSizeH);
                } else if (ts == LandGrabLogic.TileState.PLAYER_B && barrierActiveB) {
                    drawBarrierBorder(gc, x, y, tileSizeW, tileSizeH);
                }

                String word = this.state.getWord(r, c);
                if (word != null && !word.isEmpty()) {
                    if (isWordFlipped) {
                        word = new StringBuilder(word).reverse().toString();
                    }

                    LandGrabLogic.WordModifier modifier = this.state.getModifier(r, c);

                    // [수정] 아이템 색상 통일
                    if (modifier != LandGrabLogic.WordModifier.NEUTRAL) {
                        gc.setFill(TEXT_ITEM_COLOR);
                        gc.setFont(itemFont);
                    } else {
                        gc.setFont(wordFont);
                        if (ts == LandGrabLogic.TileState.PLAYER_A || ts == LandGrabLogic.TileState.PLAYER_B) {
                            gc.setFill(TEXT_ON_CAPTURED_TILE);
                        } else {
                            gc.setFill(TEXT_EMPTY_COLOR);
                        }
                    }
                    gc.setTextAlign(TextAlignment.CENTER);
                    gc.fillText(word, x + tileSizeW / 2, y + tileSizeH / 2 + 5);
                }
            }
        }

        gc.setStroke(GRID_LINE_COLOR);
        gc.setLineWidth(1);
        for (int i = 0; i <= LandGrabLogic.GRID_SIZE; i++) {
            gc.strokeLine(i * tileSizeW, 0, i * tileSizeW, h);
            gc.strokeLine(0, i * tileSizeH, w, i * tileSizeH);
        }

        // 먹물 그리기
        List<LandGrabEffects.BlindedTile> blindedTiles = this.state.getActiveBlindedTiles();
        if (blindedTiles != null && !blindedTiles.isEmpty() && inkSplatImage != null) {
            for (LandGrabEffects.BlindedTile tile : blindedTiles) {
                gc.drawImage(inkSplatImage, tile.c() * tileSizeW, tile.r() * tileSizeH, tileSizeW, tileSizeH);
            }
        }

        // 플래시
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

    // [신규] 테두리 그리기 헬퍼
    private void drawBarrierBorder(GraphicsContext gc, double x, double y, double w, double h) {
        gc.setStroke(Color.GOLD);
        gc.setLineWidth(4);
        gc.strokeRect(x + 2, y + 2, w - 4, h - 4); // 안쪽으로 살짝 들여서 그림
    }

    public void showSplashAnimation(int r, int c) { showFloatingText("스플래시!", r, c, "white", "#90C8FF"); }
    public void showInkSplashAnimation(int r, int c) { showFloatingText("먹물!", r, c, "#BBBBBB", "#444444"); }

    public void showFloatingText(String text, int r, int c, String color1, String color2) {
        if (disposed || getScene() == null) return;
        Label label = new Label(text);
        label.setFont(splashAnimationFont);
        label.setStyle("-fx-text-fill: linear-gradient(from 0% 0% to 0% 100%, " + color1 + " 20%, " + color2 + " 80%); -fx-stroke: black; -fx-stroke-width: 1px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 3, 0.5, 0, 2);");

        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0) return;
        double tileSizeW = w / LandGrabLogic.GRID_SIZE;
        double tileSizeH = h / LandGrabLogic.GRID_SIZE;

        if (r < 0 || c < 0) {
            label.setLayoutX((getWidth() - 100) / 2);
            label.setLayoutY(getHeight() / 2);
        } else {
            label.setLayoutX((getWidth()-w)/2 + c*tileSizeW + tileSizeW/2 - 40);
            label.setLayoutY((getHeight()-h)/2 + r*tileSizeH + tileSizeH/2 - 20);
        }

        FadeTransition ft = new FadeTransition(Duration.millis(1200), label);
        ft.setFromValue(1.0); ft.setToValue(0.0);
        TranslateTransition tt = new TranslateTransition(Duration.millis(1200), label);
        tt.setByY(-60);

        ParallelTransition pt = new ParallelTransition(label, ft, tt);
        pt.setOnFinished(e -> animationPane.getChildren().remove(label));
        animationPane.getChildren().add(label);
        pt.play();
    }

    public void flashHit() { flash(FLASH_HIT, 120); }
    public void flashMiss() { flash(FLASH_MISS, 120); }
    public void flashBuffColor(Color c) {
        buffFlashColor = c;
        buffFlashUntil = System.currentTimeMillis() + 200;
        redraw();
    }

    private void flash(Color c, int ms) {
        if (disposed) return;
        flashColor = c;
        flashUntil = System.currentTimeMillis() + ms;
        redraw();
    }

    public void activate() { disposed = false; redraw(); }
    public void dispose() { disposed = true; animationPane.getChildren().clear(); }

    private Font loadCustomFont(String fontPath, double size) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(fontPath)) {
            if (is == null) return Font.font("System", FontWeight.BOLD, size);
            return Font.loadFont(is, size);
        } catch (Exception e) { return Font.font("System", FontWeight.BOLD, size); }
    }
    private Image loadImage(String imagePath) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(imagePath)) {
            if (is == null) return null;
            return new Image(is);
        } catch (Exception e) { return null; }
    }
}