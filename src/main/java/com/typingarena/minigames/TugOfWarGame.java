package com.typingarena.minigames;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

/**
 * 줄다리기 타자연습 – 싱글 플레이 프로토타입 (Swing)
 * - 정답을 입력하면 밧줄이 오른쪽으로 이동
 * - 시간이 흐를수록 "상대팀의 밀어내는 힘"이 커짐(자동으로 왼쪽으로 밀림)
 * - 오른쪽 끝에 먼저 도달하면 승리, 왼쪽 끝이면 패배, 시간이 끝나면 점수 비교
 *
 * ★ 확장 포인트(멀티플레이 서버 연동 시)
 *   - 정답 판정을 서버에서 수행하고, 이동 이벤트만 브로드캐스트
 *   - 이 파일의 onCorrect(), onMiss()에서 네트워크 메시지 전송하도록 변경
 */
public class TugOfWarGame extends JFrame {

    // ===== 게임 상태 =====
    private final Random rnd = new Random();
    private String targetWord = "apple";      // 현재 목표 단어
    private double pos = 0.0;                 // 밧줄 위치(-100 ~ +100) 0은 중앙
    private int score = 0;                    // 점수
    private int combo = 0;                    // 콤보
    private int timeMs = 60_000;              // 남은 시간(ms) — 60초
    private boolean running = false;          // 게임 진행 여부

    // 난이도/밸런스 파라미터
    private final double STEP_HIT = 12.0;     // 정답 시 오른쪽으로 이동량
    private final double STEP_MISS = 8.0;     // 오답 시 왼쪽으로 이동량
    private final double ENEMY_BASE = 0.08;   // 상대 기본 압박(틱당)
    private final double ENEMY_GROW = 0.00015;// 초마다 압박 증가치(틱당으로 환산)

    // ===== UI 컴포넌트 =====
    private final RopePanel ropePanel = new RopePanel();
    private final JLabel lblWord   = new JLabel("word", SwingConstants.CENTER);
    private final JTextField tfInput = new JTextField();
    private final JLabel lblTime  = new JLabel("남은 시간: 60.0s");
    private final JLabel lblScore = new JLabel("점수: 0");
    private final JLabel lblCombo = new JLabel("콤보: 0");
    private final JButton btnStart = new JButton("게임 시작");

    private Timer gameTimer;  // 100ms 틱 타이머

    public TugOfWarGame() {
        super("Typing Arena - 줄다리기 (싱글)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        // 상단 정보 바
        JPanel top = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));
        top.add(lblTime);
        top.add(lblScore);
        top.add(lblCombo);

        // 중앙: 밧줄 그리는 패널
        ropePanel.setPreferredSize(new Dimension(900, 380));

        // 하단: 단어/입력/시작버튼
        lblWord.setFont(lblWord.getFont().deriveFont(Font.BOLD, 28f));
        tfInput.setFont(tfInput.getFont().deriveFont(22f));
        JPanel bottom = new JPanel(new BorderLayout(10, 10));
        bottom.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        bottom.add(lblWord, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(10, 0));
        south.add(tfInput, BorderLayout.CENTER);
        south.add(btnStart, BorderLayout.EAST);
        bottom.add(south, BorderLayout.SOUTH);

        // 전체 레이아웃
        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(ropePanel, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        // 입력: Enter 치면 판정
        tfInput.addActionListener(e -> submitAnswer());

        // 시작/재시작 버튼
        btnStart.addActionListener(e -> startGame());

        // 타이머(100ms 간격) — 시간 감소 + 적 압박 + 화면 갱신
        gameTimer = new Timer(100, e -> onTick());
    }

    // ===== 게임 루프 =====
    private void startGame() {
        // 게임 변수 초기화
        pos = 0.0;
        score = 0;
        combo = 0;
        timeMs = 60_000;
        running = true;
        lblScore.setText("점수: 0");
        lblCombo.setText("콤보: 0");
        nextWord();
        tfInput.setText("");
        tfInput.requestFocusInWindow();
        btnStart.setEnabled(false);
        gameTimer.start();
        ropePanel.repaint();
    }

    private void endGame(String reason) {
        running = false;
        gameTimer.stop();
        btnStart.setEnabled(true);
        // 결과 안내
        String msg = "게임 종료 (" + reason + ")\n점수: " + score + " / 콤보: " + combo;
        JOptionPane.showMessageDialog(this, msg, "결과", JOptionPane.INFORMATION_MESSAGE);
    }

    private void onTick() {
        if (!running) return;

        // 시간 감소
        timeMs -= 100;
        if (timeMs < 0) timeMs = 0;

        // 남은 시간 표시 (소수 1자리)
        lblTime.setText(String.format("남은 시간: %.1fs", timeMs / 1000.0));

        // "상대팀 압박" — 시간이 지날수록 조금씩 강해짐
        double elapsedSec = (60_000 - timeMs) / 1000.0;
        double enemyPushPerTick = ENEMY_BASE + ENEMY_GROW * elapsedSec * 100; // 감각적 조정
        pos -= enemyPushPerTick;

        // 위치 클램프
        if (pos > 100) pos = 100;
        if (pos < -100) pos = -100;

        // 승패 체크
        if (pos >= 100) {
            ropePanel.repaint();
            endGame("승리! 오른쪽 끝 도달");
            return;
        }
        if (pos <= -100) {
            ropePanel.repaint();
            endGame("패배… 왼쪽 끝 도달");
            return;
        }
        if (timeMs == 0) {
            ropePanel.repaint();
            // 시간 종료 — 중앙에서 어느 쪽에 가까운지로 연출
            String result = pos > 0 ? "시간 종료: 근소한 승리" : (pos < 0 ? "시간 종료: 근소한 패배" : "무승부");
            endGame(result);
            return;
        }

        // 화면 갱신
        ropePanel.repaint();
    }

    // ===== 입력 판정 =====
    private void submitAnswer() {
        if (!running) return;

        String typed = tfInput.getText().trim();
        if (typed.isEmpty()) return;

        if (typed.equalsIgnoreCase(targetWord)) {
            onCorrect();
        } else {
            onMiss();
        }
        tfInput.setText("");
        tfInput.requestFocusInWindow();
    }

    private void onCorrect() {
        // 정답: 점수/콤보/위치 보상
        combo++;
        int deltaScore = 10 + (combo * 2); // 콤보가 높을수록 점수 추가
        score += deltaScore;
        pos += STEP_HIT;

        lblScore.setText("점수: " + score);
        lblCombo.setText("콤보: " + combo);

        // 다음 단어
        nextWord();
        ropePanel.flashRight(); // 시각 효과(오른쪽 번쩍)
    }

    private void onMiss() {
        // 오답: 콤보 끊김 + 왼쪽 페널티
        combo = 0;
        lblCombo.setText("콤보: 0");
        pos -= STEP_MISS;
        ropePanel.flashLeft(); // 시각 효과(왼쪽 번쩍)
    }

    // ===== 단어 생성(점점 어려워짐) =====
    private void nextWord() {
        // 경과 시간에 따라 단어 길이를 4~8자로 점증
        int elapsed = 60_000 - timeMs;
        int minLen = 4 + Math.min(elapsed / 15_000, 3); // 0~3 → 4~7
        int maxLen = Math.min(minLen + 1, 8);
        targetWord = randomWord(minLen, maxLen);
        lblWord.setText(targetWord);
    }

    private String randomWord(int minLen, int maxLen) {
        // 아주 간단한 풀 — 실제로는 사전을 쓰면 좋음
        String[] pool = {
            "apple","note","river","korea","typing","banana","window","socket","orange","system",
            "thread","packet","object","combo","vector","method","class","random","matrix","buffer",
            "friend","music","guitar","soccer","player","winner","castle","dragon","danger","shield",
            "future","simple","mobile","attack","defense","victory","balance","energy","memory","rocket",
            "coffee","school","winter","summer","spring","autumn","family","forest","desert","thunder"
        };
        // 길이가 조건에 맞는 것만 후보
        int tries = 100;
        for (int t = 0; t < tries; t++) {
            String w = pool[rnd.nextInt(pool.length)];
            if (w.length() >= minLen && w.length() <= maxLen) return w;
        }
        // 혹시 못 찾으면 그냥 임의 선택
        return pool[rnd.nextInt(pool.length)];
    }

    // ===== RopePanel: 밧줄과 말판 그리기 =====
    private class RopePanel extends JPanel {
        private Color flashColor = null;  // 잠깐 반짝이는 효과 색
        private long flashUntil = 0L;

        RopePanel() {
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

            // 가운데 선(중앙)
            g.setColor(new Color(210, 220, 230));
            g.fillRect(w/2 - 3, centerY - 120, 6, 240);

            // 왼쪽/오른쪽 영역
            g.setColor(new Color(235, 242, 247));
            g.fillRect(0, centerY - 60, w/2 - 3, 120);
            g.setColor(new Color(225, 240, 235));
            g.fillRect(w/2 + 3, centerY - 60, w/2 - 3, 120);

            // 밧줄 (수평 라인)
            g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(120, 90, 60));
            g.drawLine(60, centerY, w - 60, centerY);

            // 마커 위치 계산(-100~100 → 패널 좌표)
            double rangePx = (w - 160) / 2.0;
            int markerX = (int)(w/2 + (pos / 100.0) * rangePx);
            int markerY = centerY;

            // 마커(원) + 글자
            g.setColor(new Color(60, 120, 255));
            g.fillOval(markerX - 16, markerY - 16, 32, 32);
            g.setColor(Color.WHITE);
            g.setFont(g.getFont().deriveFont(Font.BOLD, 14f));
            drawCenteredString(g, "YOU", new Rectangle(markerX - 18, markerY - 32, 36, 14));

            // 좌/우 끝선(승패 라인)
            g.setColor(new Color(200, 80, 80));
            g.drawLine(60, centerY - 80, 60, centerY + 80);            // 패배선
            g.setColor(new Color(80, 160, 80));
            g.drawLine(w - 60, centerY - 80, w - 60, centerY + 80);    // 승리선

            // 반짝 효과
            long now = System.currentTimeMillis();
            if (flashColor != null && now < flashUntil) {
                g.setColor(flashColor);
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

        void flashRight() {
            flashColor = new Color(50, 200, 120);
            flashUntil = System.currentTimeMillis() + 120;
            repaint();
        }

        void flashLeft() {
            flashColor = new Color(220, 80, 80);
            flashUntil = System.currentTimeMillis() + 120;
            repaint();
        }
    }

    // ===== 메인 =====
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TugOfWarGame().setVisible(true));
    }
}
