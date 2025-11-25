package typingarena.minigames.landgrab;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import typingarena.core.landgrab.LandGrabLogic;
import typingarena.core.landgrab.LandGrabViewState;

import java.io.InputStream;

public class LandGrabPanel extends StackPane {

    // [수정] 색상 상수 (일관성 유지)
    private static final Color BG_COLOR = Color.web("#FFF8E1");
    private static final Color TILE_EMPTY_BODY = Color.WHITE;
    private static final Color TILE_EMPTY_SHADOW = Color.web("#EFEBE9");
    private static final Color TILE_P1_BODY = Color.web("#29B6F6"); // 파랑 (나)
    private static final Color TILE_P1_SHADOW = Color.web("#0288D1");
    private static final Color TILE_P2_BODY = Color.web("#EF5350"); // 빨강 (상대)
    private static final Color TILE_P2_SHADOW = Color.web("#C62828");
    private static final Color TEXT_COLOR = Color.web("#4E342E");
    private static final Color TEXT_ITEM_COLOR = Color.web("#FF6F00");

    private LandGrabViewState state = new LandGrabViewState();
    private boolean disposed = false;

    // [추가] 내가 Player A인지 B인지 저장 (기본값 true)
    private boolean isPlayerA = true;

    private boolean isWordFlipped = false;
    private boolean barrierActiveA = false;
    private boolean barrierActiveB = false;

    private final Canvas canvas = new Canvas();
    private final Pane animationPane = new Pane();

    private Font wordFont;
    private Font itemFont;
    private final Font splashAnimationFont;

    public LandGrabPanel() {
        setMinSize(0, 0);
        splashAnimationFont = loadCustomFont("fonts/CookieRun Regular.otf", 32);
        updateDynamicFonts(15);

        animationPane.setMouseTransparent(true);
        getChildren().addAll(canvas, animationPane);
        setAlignment(Pos.CENTER);

        widthProperty().addListener((obs, o, n) -> resizeCanvas(n.doubleValue(), getHeight()));
        heightProperty().addListener((obs, o, n) -> resizeCanvas(getWidth(), n.doubleValue()));
    }

    // [추가] 외부에서 내 정체성을 설정하는 메서드
    public void setMyIdentity(boolean amIPlayerA) {
        this.isPlayerA = amIPlayerA;
        redraw();
    }

    private void resizeCanvas(double w, double h) {
        if (w <= 0 || h <= 0) return;
        double size = Math.min(w, h);
        canvas.setWidth(size);
        canvas.setHeight(size);
        double tileSizeH = size / LandGrabLogic.GRID_SIZE;
        double newFontSize = Math.max(10, tileSizeH * 0.22);
        updateDynamicFonts(newFontSize);
        redraw();
    }

    private void updateDynamicFonts(double size) {
        wordFont = loadCustomFont("fonts/CookieRun Regular.otf", size);
        itemFont = loadCustomFont("fonts/CookieRun Regular.otf", size * 1.1);
    }

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
        double gap = 4.0;

        for (int r = 0; r < LandGrabLogic.GRID_SIZE; r++) {
            for (int c = 0; c < LandGrabLogic.GRID_SIZE; c++) {
                double x = c * tileSizeW + gap / 2;
                double y = r * tileSizeH + gap / 2;
                double tw = tileSizeW - gap;
                double th = tileSizeH - gap;

                LandGrabLogic.TileState ts = this.state.getTileState(r, c);
                if (ts == null) ts = LandGrabLogic.TileState.EMPTY;

                // [수정] 타일 색상 결정 로직 변경
                drawJellyTile(gc, x, y, tw, th, ts);

                // 보호막 글로우도 내 시점에 맞춰 그림
                boolean isMyTile = (isPlayerA && ts == LandGrabLogic.TileState.PLAYER_A) || (!isPlayerA && ts == LandGrabLogic.TileState.PLAYER_B);
                boolean isOppTile = (isPlayerA && ts == LandGrabLogic.TileState.PLAYER_B) || (!isPlayerA && ts == LandGrabLogic.TileState.PLAYER_A);

                boolean myBarrier = isPlayerA ? barrierActiveA : barrierActiveB;
                boolean oppBarrier = isPlayerA ? barrierActiveB : barrierActiveA;

                if ((isMyTile && myBarrier) || (isOppTile && oppBarrier)) {
                    drawBarrierGlow(gc, x, y, tw, th);
                }

                String word = this.state.getWord(r, c);
                if (word != null && !word.isEmpty()) {
                    if (isWordFlipped) word = new StringBuilder(word).reverse().toString();
                    LandGrabLogic.WordModifier modifier = this.state.getModifier(r, c);
                    gc.setTextAlign(TextAlignment.CENTER);
                    gc.setTextBaseline(VPos.CENTER);

                    if (modifier != LandGrabLogic.WordModifier.NEUTRAL) {
                        gc.setFont(itemFont); gc.setFill(TEXT_ITEM_COLOR);
                        gc.fillText(word, x + tw / 2, y + th / 2);
                        double dotSize = Math.max(4, tw * 0.1);
                        gc.setFill(Color.ORANGE); gc.fillOval(x + tw - dotSize - 2, y + 4, dotSize, dotSize);
                    } else {
                        gc.setFont(wordFont); gc.setFill(TEXT_COLOR);
                        gc.fillText(word, x + tw / 2, y + th / 2);
                    }
                }
            }
        }
        // (이펙트 그리기 코드 생략 - 기존과 동일)
        // ...
    }

    // [핵심 수정] 타일 색상을 '나' 기준으로 렌더링
    private void drawJellyTile(GraphicsContext gc, double x, double y, double w, double h, LandGrabLogic.TileState ts) {
        Color bodyColor = TILE_EMPTY_BODY;
        Color shadowColor = TILE_EMPTY_SHADOW;

        if (ts != LandGrabLogic.TileState.EMPTY) {
            boolean isMe = (isPlayerA && ts == LandGrabLogic.TileState.PLAYER_A) || (!isPlayerA && ts == LandGrabLogic.TileState.PLAYER_B);

            if (isMe) {
                bodyColor = TILE_P1_BODY; // 무조건 파랑
                shadowColor = TILE_P1_SHADOW;
            } else {
                bodyColor = TILE_P2_BODY; // 무조건 빨강
                shadowColor = TILE_P2_SHADOW;
            }
        }

        double arc = w * 0.25;
        gc.setFill(shadowColor); gc.fillRoundRect(x, y + (h * 0.08), w, h, arc, arc);
        gc.setFill(bodyColor); gc.fillRoundRect(x, y, w, h, arc, arc);

        if (ts != LandGrabLogic.TileState.EMPTY) {
            gc.setFill(Color.rgb(255, 255, 255, 0.3));
            gc.fillRoundRect(x + (w * 0.1), y + (h * 0.05), w * 0.8, h * 0.4, arc, arc);
        }
    }

    private void drawBarrierGlow(GraphicsContext gc, double x, double y, double w, double h) {
        gc.setStroke(Color.GOLD); gc.setLineWidth(Math.max(2, w * 0.05));
        double arc = w * 0.25; gc.strokeRoundRect(x - 2, y - 2, w + 4, h + 4, arc, arc);
    }

    public void showSplashAnimation(int r, int c) { showFloatingText("스플래시!", r, c, "white", "#90C8FF"); }
    public void showInkSplashAnimation(int r, int c) { showFloatingText("먹물!", r, c, "#BBBBBB", "#444444"); }

    public void showFloatingText(String text, int r, int c, String color1, String color2) {
        if (disposed || getScene() == null) return;
        Label label = new Label(text);
        label.setFont(splashAnimationFont);
        label.setStyle("-fx-text-fill: linear-gradient(from 0% 0% to 0% 100%, " + color1 + " 20%, " + color2 + " 80%); " +
                "-fx-stroke: black; -fx-stroke-width: 1px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 3, 0.5, 0, 2);");

        // (애니메이션 로직 동일)
        double w = canvas.getWidth(); double h = canvas.getHeight();
        double tileSizeW = w / LandGrabLogic.GRID_SIZE; double tileSizeH = h / LandGrabLogic.GRID_SIZE;
        if (r < 0 || c < 0) { label.setLayoutX((getWidth() - 100) / 2); label.setLayoutY(getHeight() / 2); }
        else { label.setLayoutX((getWidth()-w)/2 + c*tileSizeW + tileSizeW/2 - 40); label.setLayoutY((getHeight()-h)/2 + r*tileSizeH + tileSizeH/2 - 20); }

        FadeTransition ft = new FadeTransition(Duration.millis(1200), label); ft.setFromValue(1.0); ft.setToValue(0.0);
        TranslateTransition tt = new TranslateTransition(Duration.millis(1200), label); tt.setByY(-60);
        ParallelTransition pt = new ParallelTransition(label, ft, tt);
        pt.setOnFinished(e -> animationPane.getChildren().remove(label));
        animationPane.getChildren().add(label); pt.play();
    }

    public void flashHit() { if (!disposed) redraw(); }
    public void flashMiss() { if (!disposed) redraw(); }
    public void activate() { disposed = false; redraw(); }
    public void dispose() { disposed = true; animationPane.getChildren().clear(); }

    private Font loadCustomFont(String fontPath, double size) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(fontPath)) {
            if (is == null) return Font.font("Malgun Gothic", size);
            return Font.loadFont(is, size);
        } catch (Exception e) { return Font.font("System", size); }
    }
}
