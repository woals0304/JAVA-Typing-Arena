package com.typingarena.minigames.tugofwar;

import javax.swing.*;
import java.awt.*;
import java.awt.AlphaComposite;

/**
 * RopePanel
 * - 경기장(밧줄, 승리/패배 라인, YOU 말판)과
 * - 현재 단어(currentWord) 표시까지 그림.
 *
 * 먹물(blind) 효과가 활성화된 경우,
 * 단어가 그려진 그 영역 위에만 반투명 검은 사각형을 씌워서 가린다.
 * (이게 "화면 중앙"이 아니라 "단어 출력 부분만 가려달라"는 요구사항 반영)
 */
public class RopePanel extends JPanel {

    private final GameLogic logic;

    // 정답/오답 순간 번쩍 (초록/빨강)
    private Color flashColor = null;
    private long flashUntil = 0L;

    // 아이템 사용 순간 번쩍 (파워그립/앵커/먹물 버튼 눌렀을 때)
    private Color buffFlashColor = null;
    private long buffFlashUntil = 0L;

    public RopePanel(GameLogic logic) {
        this.logic = logic;
        setBackground(new Color(245, 248, 252));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int centerY = h / 2;

        double pos = logic.getPos();
        ActiveEffects eff = logic.getEffects();

        // 1) 왼/오른쪽 영역 & 중앙선
        g.setColor(new Color(235, 242, 247));
        g.fillRect(0, centerY - 60, w/2, 120);

        g.setColor(new Color(225, 240, 235));
        g.fillRect(w/2, centerY - 60, w/2, 120);

        g.setColor(new Color(210, 220, 230));
        g.fillRect(w/2 - 3, centerY - 120, 6, 240);

        // 2) 밧줄
        g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(120, 90, 60));
        g.drawLine(60, centerY, w - 60, centerY);

        // 3) 승리/패배 라인
        g.setColor(new Color(200, 80, 80));
        g.drawLine(60, centerY - 80, 60, centerY + 80);        // 왼쪽 (패배선)
        g.setColor(new Color(80, 160, 80));
        g.drawLine(w - 60, centerY - 80, w - 60, centerY + 80); // 오른쪽 (승리선)

        // 4) "YOU" 말판
        double rangePx = (w - 160) / 2.0;  // pos -100~100 -> 픽셀 변환
        int markerX = (int)(w/2 + (pos / 100.0) * rangePx);
        int markerY = centerY;

        g.setColor(new Color(60, 120, 255));
        g.fillOval(markerX - 16, markerY - 16, 32, 32);

        g.setColor(Color.WHITE);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 14f));
        drawCenteredString(g, "YOU", new Rectangle(markerX - 18, markerY - 32, 36, 14));

        // 5) 현재 단어 텍스트 (로프 아래쪽에 크게)
        String word = logic.getCurrentWord();
        Font wordFont = g.getFont().deriveFont(Font.BOLD, 28f);
        g.setFont(wordFont);

        FontMetrics fmWord = g.getFontMetrics();
        int wordWidth = fmWord.stringWidth(word);
        int wordX = (w - wordWidth) / 2;
        int wordBaseY = centerY + 140; // 말판 아래쪽에 배치

        // 단어 글자 (밑에 먹물 깔기 전, 원래 텍스트)
        g.setColor(new Color(30, 30, 30));
        g.drawString(word, wordX, wordBaseY);

        // 6) 먹물(blind) 효과가 활성화되면
        // 단어가 표시되는 그 사각형만 까맣게 덮는다.
        if (eff.isBlindActive()) {
            int pad = 8;
            int rectX = wordX - pad;
            int rectY = wordBaseY - fmWord.getAscent() - pad;
            int rectW = wordWidth + pad * 2;
            int rectH = fmWord.getHeight() + pad * 2;

            // 반투명 검은 박스
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRoundRect(rectX, rectY, rectW, rectH, 16, 16);

            // "먹물!" 텍스트 표시
            g.setColor(Color.WHITE);
            g.setFont(g.getFont().deriveFont(Font.BOLD, 14f));
            FontMetrics fm2 = g.getFontMetrics();
            String blindMsg = "먹물!";
            int msgW = fm2.stringWidth(blindMsg);
            int msgX = rectX + (rectW - msgW) / 2;
            int msgY = rectY + (rectH - fm2.getHeight()) / 2 + fm2.getAscent();
            g.drawString(blindMsg, msgX, msgY);
        }

        // 7) 정답/오답 플래시 (전체 화면 살짝 번쩍)
        long now = System.currentTimeMillis();
        if (flashColor != null && now < flashUntil) {
            g.setColor(flashColor);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
            g.fillRect(0, 0, w, h);
            g.setComposite(AlphaComposite.SrcOver);
        }

        // 8) 아이템 사용 순간 플래시
        if (buffFlashColor != null && now < buffFlashUntil) {
            g.setColor(buffFlashColor);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
            g.fillRect(0, 0, w, h);
            g.setComposite(AlphaComposite.SrcOver);
        }
    }

    private void drawCenteredString(Graphics2D g, String text, Rectangle rect) {
        FontMetrics fm = g.getFontMetrics();
        int x = rect.x + (rect.width - fm.stringWidth(text)) / 2;
        int y = rect.y + ((rect.height - fm.getHeight()) / 2) + fm.getAscent();
        g.drawString(text, x, y);
    }

    // 정답 시 (초록 번쩍)
    public void flashRight() {
        flashColor = new Color(50, 200, 120);
        flashUntil = System.currentTimeMillis() + 120;
        repaint();
    }

    // 오답 시 (빨강 번쩍)
    public void flashLeft() {
        flashColor = new Color(220, 80, 80);
        flashUntil = System.currentTimeMillis() + 120;
        repaint();
    }

    // 아이템 눌렀을 때 (파워그립/앵커/먹물 버튼)
    public void flashBuffColor(Color c) {
        buffFlashColor = c;
        buffFlashUntil = System.currentTimeMillis() + 200;
        repaint();
    }
}
