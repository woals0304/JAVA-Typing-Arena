package com.typingarena.minigames.tugofwar;

import javax.swing.*;
import java.awt.*;

/**
 * TugOfWarGame
 * - JFrame + HUD + 입력창 + 아이템 버튼 + Swing Timer 연결만 담당.
 * - 실제 게임 규칙/상태는 GameLogic,
 *   실제 그리기는 RopePanel이 맡는다.
 *
 * 흐름:
 *  1) 시작 버튼 -> logic.startGame() -> gameTimer.start()
 *  2) 100ms마다 gameTimer -> logic.tick() -> HUD 갱신 -> RopePanel.repaint()
 *  3) 플레이어가 엔터 -> logic.submitAnswer() -> flashRight/flashLeft()
 *  4) 아이템 버튼 -> logic.usePowerGrip()/useAnchor()/useBlind()
 */
public class TugOfWarGame extends JFrame {

    private final GameLogic logic = new GameLogic();
    private final RopePanel ropePanel = new RopePanel(logic);

    // HUD 라벨들
    private final JLabel lblTime    = new JLabel("남은 시간: 60.0s");
    private final JLabel lblScore   = new JLabel("점수: 0");
    private final JLabel lblCombo   = new JLabel("콤보: 0");
    private final JLabel lblEffects = new JLabel("효과: 없음");

    // 입력창 / 버튼들
    private final JTextField tfInput   = new JTextField();
    private final JButton btnStart     = new JButton("게임 시작");
    private final JButton btnPowerGrip = new JButton("파워 그립 (힘 2배)");
    private final JButton btnAnchor    = new JButton("앵커 (안 밀림)");
    private final JButton btnBlind     = new JButton("먹물 (단어 가리기)");

    // 100ms마다 게임 한 틱씩 진행시키는 타이머
    // final이라 반드시 생성자에서 한 번만 할당돼야 함
    private final Timer gameTimer;

    public TugOfWarGame() {
        super("Typing Arena - 줄다리기");

        // 1) 타이머를 '가장 먼저' 초기화해서 final 필드 관련 에러를 없앤다.
        //    이 리스너 안에서 gameTimer.stop()을 직접 부르면
        //    초기화 순서 문제로 또 경고가 날 수 있으므로,
        //    ((Timer)e.getSource()).stop() 으로 자기 자신을 멈춘다.
        gameTimer = new Timer(100, e -> {
            String result = logic.tick(); // null이면 계속, 문자열이면 게임 끝 사유

            updateHUD();
            ropePanel.repaint();

            if (result != null) {
                // 게임 종료 처리
                ((Timer) e.getSource()).stop(); // 타이머 멈춤
                btnStart.setEnabled(true);

                JOptionPane.showMessageDialog(
                        this,
                        "게임 종료 (" + result + ")\n점수: " + logic.getScore()
                                + " / 콤보: " + logic.getCombo(),
                        "결과",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        });

        // 2) 나머지 UI 세팅
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);

        // ===== 상단 HUD =====
        JPanel top = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        Font hudFont = lblTime.getFont().deriveFont(Font.BOLD, 14f);
        lblTime.setFont(hudFont);
        lblScore.setFont(hudFont);
        lblCombo.setFont(hudFont);
        lblEffects.setFont(hudFont);
        top.add(lblTime);
        top.add(lblScore);
        top.add(lblCombo);
        top.add(lblEffects);

        // ===== 아이템 버튼 패널 =====
        JPanel itemPanel = new JPanel();
        itemPanel.setLayout(new BoxLayout(itemPanel, BoxLayout.Y_AXIS));
        itemPanel.setBorder(BorderFactory.createTitledBorder("아이템"));

        btnPowerGrip.setFont(btnPowerGrip.getFont().deriveFont(12f));
        btnAnchor.setFont(btnAnchor.getFont().deriveFont(12f));
        btnBlind.setFont(btnBlind.getFont().deriveFont(12f));

        itemPanel.add(btnPowerGrip);
        itemPanel.add(Box.createVerticalStrut(10));
        itemPanel.add(btnAnchor);
        itemPanel.add(Box.createVerticalStrut(10));
        itemPanel.add(btnBlind);

        // ===== 중앙(경기장 + 아이템 패널) =====
        ropePanel.setPreferredSize(new Dimension(800, 380));
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.add(ropePanel, BorderLayout.CENTER);
        centerWrapper.add(itemPanel, BorderLayout.EAST);

        // ===== 하단(입력창 + 시작 버튼) =====
        tfInput.setFont(tfInput.getFont().deriveFont(22f));
        JPanel bottom = new JPanel(new BorderLayout(10, 10));
        bottom.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JPanel south = new JPanel(new BorderLayout(10, 0));
        south.add(tfInput, BorderLayout.CENTER);
        south.add(btnStart, BorderLayout.EAST);
        bottom.add(south, BorderLayout.CENTER);

        // ===== 전체 레이아웃 배치 =====
        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(centerWrapper, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        // 3) 이벤트 바인딩

        // (a) 엔터로 답 제출
        tfInput.addActionListener(ev -> {
            String typed = tfInput.getText().trim();
            boolean correct = logic.submitAnswer(typed);

            if (correct) {
                ropePanel.flashRight();
            } else {
                ropePanel.flashLeft();
            }

            tfInput.setText("");
            tfInput.requestFocusInWindow();

            updateHUD();          // 점수/콤보/효과 갱신
            ropePanel.repaint();  // 화면 다시 그림
        });

        // (b) 게임 시작
        btnStart.addActionListener(ev -> {
            logic.startGame();    // 내부 상태 초기화, 단어 새로 뽑음
            btnStart.setEnabled(false);
            tfInput.requestFocusInWindow();

            updateHUD();
            ropePanel.repaint();

            gameTimer.start();    // 틱 루프 시작
        });

        // (c) 아이템: 파워 그립 (정답 힘 2배)
        btnPowerGrip.addActionListener(ev -> {
            logic.usePowerGrip();
            ropePanel.flashBuffColor(new Color(80,160,255)); // 파란 빛
            updateHUD();
            ropePanel.repaint();
        });

        // (d) 아이템: 앵커 (왼쪽으로 거의 안 밀림)
        btnAnchor.addActionListener(ev -> {
            logic.useAnchor();
            ropePanel.flashBuffColor(new Color(80,200,120)); // 초록 빛
            updateHUD();
            ropePanel.repaint();
        });

        // (e) 아이템: 먹물 (단어 가리기)
        btnBlind.addActionListener(ev -> {
            logic.useBlind();
            ropePanel.flashBuffColor(new Color(30,30,30));   // 어두운 빛
            updateHUD();
            ropePanel.repaint();
        });
    }

    // HUD 라벨들 업데이트
    private void updateHUD() {
        lblTime.setText(String.format("남은 시간: %.1fs", logic.getTimeMs() / 1000.0));
        lblScore.setText("점수: " + logic.getScore());
        lblCombo.setText("콤보: " + logic.getCombo());
        lblEffects.setText(logic.getEffects().describeEffects());
    }

    // 실행 진입점
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TugOfWarGame().setVisible(true));
    }
}
