package typingarena.minigames.tugofwar;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import typingarena.core.tugofwar.GameLogic;

public class RopePanel extends Canvas {

    private static final Color SKY_TOP = Color.web("#FFF8E1");
    private static final Color SKY_BOTTOM = Color.web("#FFEBD1");
    private static final Color GROUND_TOP = Color.web("#E0D3C2");
    private static final Color GROUND_BOTTOM = Color.web("#C9B69F");
    private static final Color LEFT_ZONE = Color.web("#FFF3E0");
    private static final Color RIGHT_ZONE = Color.web("#FFE0B2");
    private static final Color CENTER_LINE = Color.web("#D7CCC8");
    private static final Color ROPE_SHADOW = Color.web("#4E342E");
    private static final Color ROPE_LIGHT = Color.web("#D2A26A");
    private static final Color FLAG_COLOR = Color.web("#FF7043");
    private static final Color WIN_LINE = Color.web("#29B6F6");
    private static final Color LOSE_LINE = Color.web("#EF5350");
    private static final Color PLAYER_COLOR = Color.web("#FFB74D");
    private static final Color TEAM_LEFT = Color.web("#64B5F6");
    private static final Color TEAM_RIGHT = Color.web("#EF9A9A");
    private static final Color TEXT_DEFAULT = Color.web("#4E342E");
    private static final Color FLASH_HIT = Color.web("#A5D6A7");
    private static final Color FLASH_MISS = Color.web("#EF9A9A");
    private static final char[] CHO = {'\u3131','\u3132','\u3134','\u3137','\u3138','\u3139','\u3141','\u3142','\u3143','\u3145','\u3146','\u3147','\u3148','\u3149','\u314a','\u314b','\u314c','\u314d','\u314e'};
    private static final char[] JUNG = {'\u314f','\u3150','\u3151','\u3152','\u3153','\u3154','\u3155','\u3156','\u3157','\u3158','\u3159','\u315a','\u315b','\u315c','\u315d','\u315e','\u315f','\u3160','\u3161','\u3162','\u3163'};
    private static final char[] JONG = {'\0','\u3131','\u3132','\u3133','\u3134','\u3135','\u3136','\u3137','\u3139','\u313a','\u313b','\u313c','\u313d','\u313e','\u313f','\u3140','\u3141','\u3142','\u3144','\u3145','\u3146','\u3147','\u3148','\u314a','\u314b','\u314c','\u314d','\u314e'};

    private boolean disposed = false;
    private TugOfWarViewState state = new TugOfWarViewState();

    // 정답/오답 순간 번쩍 (초록/빨강)
    private Color flashColor = null;
    private long flashUntil = 0L;

    // 아이템 사용 순간 번쩍 (파워그립/앵커/먹물 버튼)
    private Color buffFlashColor = null;
    private long buffFlashUntil = 0L;

    public RopePanel() {
        widthProperty().addListener((obs, oldV, newV) -> redraw());
        heightProperty().addListener((obs, oldV, newV) -> redraw());
    }

    public void updateState(TugOfWarViewState newState) {
        if (newState == null) return;
        this.state = new TugOfWarViewState(
                Math.max(-100, Math.min(100, newState.pos)),
                newState.currentWord != null ? newState.currentWord : "",
                newState.modifier != null ? newState.modifier : GameLogic.WordModifier.NEUTRAL,
                newState.blindActive,
                newState.jamoSplitActive
        );
        redraw();
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

        // 하늘/땅 배경
        Paint sky = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, SKY_TOP), new Stop(1, SKY_BOTTOM));
        gc.setFill(sky);
        gc.fillRect(0, 0, w, h);

        double groundTop = h * 0.62;
        Paint ground = new LinearGradient(0, groundTop / h, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, GROUND_TOP), new Stop(1, GROUND_BOTTOM));
        gc.setFill(ground);
        gc.fillRect(0, groundTop, w, h - groundTop);

        double centerY = h / 2.0;

        // 1) 왼/오른쪽 영역 & 중앙선
        double zoneHeight = 120;
        gc.setFill(LEFT_ZONE);
        gc.fillRect(0, centerY - zoneHeight / 2, w / 2, zoneHeight);

        gc.setFill(RIGHT_ZONE);
        gc.fillRect(w / 2, centerY - zoneHeight / 2, w / 2, zoneHeight);

        gc.setFill(CENTER_LINE);
        gc.fillRect((w / 2) - 3, centerY - 120, 6, 240);

        drawCrowd(gc, w, centerY);

        // 2) 밧줄 + 말뚝
        double ropeStartX = 80;
        double ropeEndX = w - 80;
        double ropeY = centerY;

        drawAnchors(gc, ropeStartX, ropeEndX, ropeY);
        drawRope(gc, ropeStartX, ropeEndX, ropeY, state.pos);

        // 3) 승리/패배 라인
        gc.setStroke(LOSE_LINE);
        gc.setLineWidth(3);
        gc.strokeLine(60, centerY - 80, 60, centerY + 80);

        gc.setStroke(WIN_LINE);
        gc.strokeLine(w - 60, centerY - 80, w - 60, centerY + 80);

        // 4) "YOU" 말판
        double rangePx = (w - 160) / 2.0;
        double markerX = w / 2.0 + (state.pos / 100.0) * rangePx;
        double markerY = centerY;

        gc.setFill(PLAYER_COLOR);
        gc.fillOval(markerX - 16, markerY - 16, 32, 32);

        gc.setFill(Color.WHITE);
        Font playerFont = Font.font("System", FontWeight.BOLD, 14);
        gc.setFont(playerFont);
        drawCenteredText(gc, "YOU", markerX - 16, markerY - 16, 32, 32);

        // 5) 현재 단어 텍스트
        String word = state.currentWord;
        if (state.jamoSplitActive) {
            word = splitHangulToJamo(word);
        }
        Font wordFont = Font.font("System", FontWeight.BOLD, 32);
        gc.setFont(wordFont);

        Text wordNode = new Text(word);
        wordNode.setFont(wordFont);
        double wordWidth = wordNode.getLayoutBounds().getWidth();
        double wordHeight = wordNode.getLayoutBounds().getHeight();
        double wordX = (w - wordWidth) / 2.0;
        double wordYOffset = Math.min(140, h * 0.22);
        double wordBaseY = centerY + wordYOffset;

        Color wordColor = TEXT_DEFAULT;
        GameLogic.WordModifier modifier = state.modifier;
        if (modifier == GameLogic.WordModifier.BUFF) {
            wordColor = Color.rgb(46, 160, 92);
        } else if (modifier == GameLogic.WordModifier.TRAP) {
            wordColor = Color.rgb(208, 68, 68);
        }
        gc.setFill(wordColor);
        gc.fillText(word, wordX, wordBaseY);

        // 6) 먹물 효과
        if (state.blindActive) {
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

    private void drawCrowd(GraphicsContext gc, double w, double centerY) {
        gc.setGlobalAlpha(0.22);
        gc.setFill(Color.web("#BCAAA4"));
        double crowdY = centerY - 100;
        double spacing = 24;
        for (double x = 10; x < w; x += spacing) {
            double radius = 6 + (x % 36) / 18.0;
            gc.fillOval(x, crowdY + (x % 16) * 0.4, radius, radius);
        }
        gc.setGlobalAlpha(1.0);
    }

    private void drawAnchors(GraphicsContext gc, double startX, double endX, double y) {
        gc.setFill(Color.web("#8D6E63"));
        gc.fillRoundRect(startX - 18, y - 40, 12, 80, 6, 6);
        gc.fillRoundRect(endX + 6, y - 40, 12, 80, 6, 6);

        gc.setFill(Color.web("#6D4C41"));
        gc.fillRoundRect(startX - 24, y - 8, 22, 16, 8, 8);
        gc.fillRoundRect(endX + 2, y - 8, 22, 16, 8, 8);
    }

    private void drawRope(GraphicsContext gc, double startX, double endX, double y, double pos) {
        double offset = (pos / 100.0) * 12; // 밧줄이 당겨진 느낌
        double sag = 8 + Math.abs(pos) * 0.05;

        // 밧줄 그림자
        gc.setStroke(ROPE_SHADOW);
        gc.setLineWidth(14);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.beginPath();
        gc.moveTo(startX, y);
        gc.bezierCurveTo(wobble(startX, offset), y + sag, wobble(endX, offset), y + sag, endX, y);
        gc.stroke();

        // 밝은 밧줄 겉면
        gc.setStroke(ROPE_LIGHT);
        gc.setLineWidth(10);
        gc.beginPath();
        gc.moveTo(startX, y);
        gc.bezierCurveTo(wobble(startX, offset), y + sag * 0.6, wobble(endX, offset), y + sag * 0.6, endX, y);
        gc.stroke();

        // 꼬임 느낌
        gc.setStroke(Color.rgb(255, 255, 255, 0.6));
        gc.setLineWidth(2.8);
        double segment = 18;
        for (double x = startX + 6; x < endX - 6; x += segment) {
            double twistY = y + Math.sin((x + offset) * 0.15) * 5;
            gc.strokeLine(x, twistY - 4, x + segment * 0.5, twistY + 4);
        }

        // 중앙 깃발
        double rangePx = (endX - startX) / 2.0;
        double flagX = (startX + endX) / 2.0 + (pos / 100.0) * rangePx;
        gc.setFill(FLAG_COLOR);
        gc.fillPolygon(new double[]{flagX, flagX, flagX + 18}, new double[]{y - 12, y + 12, y}, 3);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokePolygon(new double[]{flagX, flagX, flagX + 18}, new double[]{y - 12, y + 12, y}, 3);

        // 양 팀 손잡이
        gc.setFill(TEAM_LEFT);
        gc.fillRoundRect(startX - 10, y - 14, 18, 28, 6, 6);
        gc.setFill(TEAM_RIGHT);
        gc.fillRoundRect(endX - 8, y - 14, 18, 28, 6, 6);
    }

    private double wobble(double x, double offset) {
        return x + Math.sin(x * 0.02 + offset * 0.1) * 6 + offset * 0.4;
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

    private String splitHangulToJamo(String text) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            int code = ch - 0xAC00;
            if (code >= 0 && code < 11172) { // 현대 한글 범위
                int choIndex = code / (21 * 28);
                int jungIndex = (code % (21 * 28)) / 28;
                int jongIndex = code % 28;
                if (!first) sb.append(' ');
                sb.append(CHO[choIndex]).append(' ').append(JUNG[jungIndex]);
                if (jongIndex > 0) {
                    sb.append(' ').append(JONG[jongIndex]);
                }
                first = false;
            } else {
                if (!first) sb.append(' ');
                sb.append(ch);
                first = false;
            }
        }
        return sb.toString();
    }
}
