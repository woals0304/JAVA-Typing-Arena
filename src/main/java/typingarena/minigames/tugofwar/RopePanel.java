package typingarena.minigames.tugofwar;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class RopePanel extends Canvas {

    private static final Color BACKGROUND = Color.rgb(245, 248, 252);
    private static final Color LEFT_ZONE = Color.rgb(235, 242, 247);
    private static final Color RIGHT_ZONE = Color.rgb(225, 240, 235);
    private static final Color CENTER_LINE = Color.rgb(210, 220, 230);
    private static final Color ROPE_COLOR = Color.rgb(120, 90, 60);
    private static final Color WIN_LINE = Color.rgb(80, 160, 80);
    private static final Color LOSE_LINE = Color.rgb(200, 80, 80);
    private static final Color PLAYER_COLOR = Color.rgb(60, 120, 255);
    private static final Color TEXT_DEFAULT = Color.rgb(30, 30, 30);
    private static final Color FLASH_HIT = Color.rgb(50, 200, 120);
    private static final Color FLASH_MISS = Color.rgb(220, 80, 80);

    private final GameLogic logic;
    private boolean disposed = false;

    // 정답/오답 순간 번쩍 (초록/빨강)
    private Color flashColor = null;
    private long flashUntil = 0L;

    // 아이템 사용 순간 번쩍 (파워그립/앵커/먹물 버튼)
    private Color buffFlashColor = null;
    private long buffFlashUntil = 0L;

    public RopePanel(GameLogic logic) {
        this.logic = logic;

        widthProperty().addListener((obs, oldV, newV) -> redraw());
        heightProperty().addListener((obs, oldV, newV) -> redraw());
    }

    public void redraw() {
        if (disposed) {
            return;
        }
        draw();
    }

    private void draw() {
        if (disposed) {
            return;
        }
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        gc.setFill(BACKGROUND);
        gc.fillRect(0, 0, w, h);

        double centerY = h / 2.0;
        ActiveEffects effects = logic.getEffects();

        // 1) 왼/오른쪽 영역 & 중앙선
        double zoneHeight = 120;
        gc.setFill(LEFT_ZONE);
        gc.fillRect(0, centerY - zoneHeight / 2, w / 2, zoneHeight);

        gc.setFill(RIGHT_ZONE);
        gc.fillRect(w / 2, centerY - zoneHeight / 2, w / 2, zoneHeight);

        gc.setFill(CENTER_LINE);
        gc.fillRect((w / 2) - 3, centerY - 120, 6, 240);

        // 2) 밧줄
        gc.setStroke(ROPE_COLOR);
        gc.setLineWidth(8);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.strokeLine(60, centerY, w - 60, centerY);

        // 3) 승리/패배 라인
        gc.setStroke(LOSE_LINE);
        gc.setLineWidth(3);
        gc.strokeLine(60, centerY - 80, 60, centerY + 80);

        gc.setStroke(WIN_LINE);
        gc.strokeLine(w - 60, centerY - 80, w - 60, centerY + 80);

        // 4) "YOU" 말판
        double rangePx = (w - 160) / 2.0;
        double markerX = w / 2.0 + (logic.getPos() / 100.0) * rangePx;
        double markerY = centerY;

        gc.setFill(PLAYER_COLOR);
        gc.fillOval(markerX - 16, markerY - 16, 32, 32);

        gc.setFill(Color.WHITE);
        Font playerFont = Font.font("System", FontWeight.BOLD, 14);
        gc.setFont(playerFont);
        drawCenteredText(gc, "YOU", markerX - 16, markerY - 16, 32, 32);

        // 5) 현재 단어 텍스트
        String word = logic.getCurrentWord();
        Font wordFont = Font.font("System", FontWeight.BOLD, 32);
        gc.setFont(wordFont);

        Text wordNode = new Text(word);
        wordNode.setFont(wordFont);
        double wordWidth = wordNode.getLayoutBounds().getWidth();
        double wordHeight = wordNode.getLayoutBounds().getHeight();
        double wordX = (w - wordWidth) / 2.0;
        double wordBaseY = centerY + 140;

        Color wordColor = TEXT_DEFAULT;
        GameLogic.WordModifier modifier = logic.getCurrentWordModifier();
        if (modifier == GameLogic.WordModifier.BUFF) {
            wordColor = Color.rgb(46, 160, 92);
        } else if (modifier == GameLogic.WordModifier.TRAP) {
            wordColor = Color.rgb(208, 68, 68);
        }
        gc.setFill(wordColor);
        gc.fillText(word, wordX, wordBaseY);

        // 6) 먹물 효과
        if (effects.isBlindActive()) {
            double pad = 12;
            double rectX = wordX - pad;
            double rectY = wordBaseY - wordHeight - pad;
            double rectW = wordWidth + pad * 2;
            double rectH = wordHeight + pad * 2;

            gc.setGlobalAlpha(0.85);
            gc.setFill(Color.rgb(0, 0, 0, 0.85));
            gc.fillRoundRect(rectX, rectY, rectW, rectH, 16, 16);
            gc.setGlobalAlpha(1.0);

            gc.setFill(Color.WHITE);
            Font blindFont = Font.font("System", FontWeight.BOLD, 16);
            gc.setFont(blindFont);
            Text blindText = new Text("먹물!");
            blindText.setFont(blindFont);
            double txtW = blindText.getLayoutBounds().getWidth();
            double txtH = blindText.getLayoutBounds().getHeight();
            double txtX = rectX + (rectW - txtW) / 2.0;
            double txtY = rectY + (rectH - txtH) / 2.0 + txtH;
            gc.fillText("먹물!", txtX, txtY - 4);
        }

        long now = System.currentTimeMillis();

        // 7) 정답/오답 플래시
        if (flashColor != null && now < flashUntil) {
            gc.setGlobalAlpha(0.18);
            gc.setFill(flashColor);
            gc.fillRect(0, 0, w, h);
            gc.setGlobalAlpha(1.0);
        }

        // 8) 아이템 플래시
        if (buffFlashColor != null && now < buffFlashUntil) {
            gc.setGlobalAlpha(0.18);
            gc.setFill(buffFlashColor);
            gc.fillRect(0, 0, w, h);
            gc.setGlobalAlpha(1.0);
        }
    }

    private void drawCenteredText(GraphicsContext gc, String text, double x, double y, double width, double height) {
        Text helper = new Text(text);
        helper.setFont(gc.getFont());
        double textWidth = helper.getLayoutBounds().getWidth();
        double textHeight = helper.getLayoutBounds().getHeight();
        double drawX = x + (width - textWidth) / 2.0;
        double drawY = y + (height - textHeight) / 2.0 + textHeight;
        gc.fillText(text, drawX, drawY - 2);
    }

    // 정답 시 (초록 번쩍)
    public void flashRight() {
        if (disposed || getScene() == null) return;
        flashColor = FLASH_HIT;
        flashUntil = System.currentTimeMillis() + 120;
        redraw();
    }

    // 오답 시 (빨강 번쩍)
    public void flashLeft() {
        if (disposed || getScene() == null) return;
        flashColor = FLASH_MISS;
        flashUntil = System.currentTimeMillis() + 120;
        redraw();
    }

    // 아이템 사용
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
    }

    public void activate() {
        disposed = false;
        flashColor = null;
        buffFlashColor = null;
    }
}
