package com.typingarena.app;

import com.typingarena.minigames.TugOfWarGame;

import javax.swing.*;
import java.awt.*;

public class TypingGameApp extends JFrame {

    public TypingGameApp() {
        super("Typing Mini Game");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);

        JPanel lobby = new JPanel(new BorderLayout(0, 20));
        lobby.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        JLabel title = new JLabel("멀티플레이 타자 미니게임 로비", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        lobby.add(title, BorderLayout.NORTH);

        JTextArea description = new JTextArea(
                "준비된 미니게임:\n" +
                "- 줄다리기 타자 대전 (Tug of War)\n\n" +
                "시작 버튼을 누르면 새 창에서 게임이 실행됩니다."
        );
        description.setEditable(false);
        description.setOpaque(false);
        description.setFont(description.getFont().deriveFont(14f));
        lobby.add(description, BorderLayout.CENTER);

        JButton startBtn = new JButton("줄다리기 게임 시작");
        startBtn.setFont(startBtn.getFont().deriveFont(Font.BOLD, 16f));
        startBtn.addActionListener(e -> launchTugOfWar());
        lobby.add(startBtn, BorderLayout.SOUTH);

        add(lobby);
    }

    private void launchTugOfWar() {
        SwingUtilities.invokeLater(() -> new TugOfWarGame().setVisible(true));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TypingGameApp().setVisible(true));
    }
}
