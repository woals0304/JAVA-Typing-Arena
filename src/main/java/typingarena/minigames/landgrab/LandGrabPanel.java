package typingarena.minigames.landgrab;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/**
 * [수정됨]
 * - draw: 획득한 타일(단어가 ""인 타일)은 텍스트를 그리지 않도록 수정
 */
public class LandGrabPanel extends Canvas {

    // ===== 1. 색상 정의 =====
    private static final Color BG_COLOR = Color.rgb(240, 240, 240);
    private static final Color GRID_LINE_COLOR = Color.rgb(200, 200, 200);
    private static final Color TILE_EMPTY_COLOR = Color.rgb(255, 255, 255);
    private static final Color TILE_PLAYER_COLOR = Color.rgb(60, 120, 255);
    private static final Color TILE_AI_COLOR = Color.rgb(220, 80, 80);

    private static final Color TEXT_EMPTY_COLOR = Color.rgb(30, 30, 30);
    // [수정] 캡처된 타일의 텍스트 색상 (이제 사용 안 함)
    // private static final Color TEXT_CAPTURED_COLOR = Color.rgb(200, 200, 200);
    private static final Color TEXT_TRAP_COLOR = Color.rgb(220, 80, 80);
    private static final Color TEXT_ON_PLAYER_TILE = Color.WHITE;
    private static final Color TEXT_ON_AI_TILE = Color.WHITE;

    private static final Color FLASH_HIT = Color.rgb(50, 200, 120);
    private static final Color FLASH_MISS = Color.rgb(220, 80, 80);

    // ===== 2. 상태 (RopePanel과 동일) =====
    private final LandGrabLogic logic;
    private boolean disposed = false;
    private Color flashColor = null;
    private long flashUntil = 0L;

    private final Font wordFont = Font.font("System", FontWeight.BOLD, 14);

    public LandGrabPanel(LandGrabLogic logic) {
        this.logic = logic;
        widthProperty().addListener((obs, oldV, newV) -> redraw());
        heightProperty().addListener((obs, oldV, newV) -> redraw());
    }

    public void redraw() {
        if (disposed) return;
        draw();
    }

    private void draw() {
        if (disposed) return;
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        // 1) 배경 그리기
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

                // 타일 배경색 칠하기
                switch (state) {
                    case PLAYER: gc.setFill(TILE_PLAYER_COLOR); break;
                    case AI:     gc.setFill(TILE_AI_COLOR);     break;
                    case EMPTY:  gc.setFill(TILE_EMPTY_COLOR);  break;
                }
                gc.fillRect(x, y, tileSizeW, tileSizeH);

                // --- [버그 2 수정] 단어 텍스트 그리기 (비어있지 않을 때만) ---
                String word = logic.getWord(r, c);
                if (word != null && !word.isEmpty()) {
                    LandGrabLogic.WordModifier modifier = logic.getModifier(r, c);

                    // 타일 상태에 따른 단어 색상 결정
                    if (state == LandGrabLogic.TileState.PLAYER) {
                        gc.setFill(TEXT_ON_PLAYER_TILE);
                    } else if (state == LandGrabLogic.TileState.AI) {
                        gc.setFill(TEXT_ON_AI_TILE);
                    } else { // EMPTY 상태일 때
                        if (modifier == LandGrabLogic.WordModifier.TRAP) {
                            gc.setFill(TEXT_TRAP_COLOR); // 트랩 단어
                        } else {
                            gc.setFill(TEXT_EMPTY_COLOR); // 일반 단어
                        }
                    }

                    gc.setFont(wordFont);
                    gc.setTextAlign(TextAlignment.CENTER);
                    gc.fillText(word, x + tileSizeW / 2, y + tileSizeH / 2 + 5);
                }
                // --- 수정 끝 ---
            }
        }

        // 3) 그리드 선 그리기
        gc.setStroke(GRID_LINE_COLOR);
        gc.setLineWidth(1);
        for (int i = 0; i <= LandGrabLogic.GRID_SIZE; i++) {
            gc.strokeLine(i * tileSizeW, 0, i * tileSizeW, h); // 세로선
            gc.strokeLine(0, i * tileSizeH, w, i * tileSizeH); // 가로선
        }

        // 4) 먹물 효과 (RopePanel과 동일)
        if (logic.getEffects().isBlindActive()) {
            gc.setGlobalAlpha(0.85);
            gc.setFill(Color.rgb(0, 0, 0, 0.85));
            gc.fillRoundRect(0, 0, w, h, 16, 16);
            gc.setGlobalAlpha(1.0);

            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("System", FontWeight.BOLD, 32));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("먹물!", w / 2, h / 2);
        }

        // 5) 정답/오답 플래시 (RopePanel과 동일)
        long now = System.currentTimeMillis();
        if (flashColor != null && now < flashUntil) {
            gc.setGlobalAlpha(0.18);
            gc.setFill(flashColor);
            gc.fillRect(0, 0, w, h);
            gc.setGlobalAlpha(1.0);
        }
    }

    // 정답 시 (초록 번쩍)
    public void flashHit() {
        if (disposed || getScene() == null) return;
        flashColor = FLASH_HIT;
        flashUntil = System.currentTimeMillis() + 120;
        redraw();
    }

    // 오답 시 (빨강 번쩍)
    public void flashMiss() {
        if (disposed || getScene() == null) return;
        flashColor = FLASH_MISS;
        flashUntil = System.currentTimeMillis() + 120;
        redraw();
    }

    public void dispose() {
        disposed = true;
        flashColor = null;
    }

    public void activate() {
        disposed = false;
        flashColor = null;
    }
}