package typingarena.app;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import typingarena.core.tugofwar.GameLogic;
import typingarena.minigames.tugofwar.RopePanel;
import typingarena.minigames.tugofwar.TugOfWarMatchView;
import typingarena.minigames.tugofwar.TugOfWarViewState;
import typingarena.net.Message;
import typingarena.net.NetClient;

import java.util.Locale;
import java.util.Map;

/**
 * 서버와 통신해 온라인 줄다리기 경기를 표시하는 Stage.
 */
public class TugOfWarOnlineStage extends Stage {

    private final NetClient client;
    private final TugOfWarMatchView view = new TugOfWarMatchView();
    private final RopePanel ropePanel = view.getRopePanel();
    private final Button surrenderBtn = new Button("기권");
    private final Button rematchBtn = new Button("재경기");
    private final javafx.scene.control.Label rematchStatus = view.getRematchStatusLabel();

    private String sessionId;
    private boolean running = false;

    public TugOfWarOnlineStage(NetClient client) {
        this.client = client;
        setTitle("온라인 줄다리기");

        view.getControlBox().getChildren().add(surrenderBtn);
        view.getControlBox().getChildren().add(rematchBtn);
        surrenderBtn.setDisable(true);
        rematchBtn.setDisable(true);
        surrenderBtn.setOnAction(e -> sendForfeit());
        rematchBtn.setOnAction(e -> sendRematchRequest());
        rematchStatus.setText("");
        view.setRematchStatus("", false);

        view.getInputField().setOnAction(e -> submitWord());

        // 싱글 플레이 화면과 동일한 폭/높이로 맞춰 UI가 작게 보이지 않도록 조정
        Scene scene = new Scene(view.getRoot(), 1000, 600);
        setScene(scene);

        setOnCloseRequest(e -> {
            if (running) {
                sendForfeit();
            }
        });
    }

    public void handleMessage(Message msg) {
        if (msg == null || msg.type == null) return;
        String type = msg.type.toUpperCase(Locale.ROOT);
        Platform.runLater(() -> {
            switch (type) {
                case "GAME_START_BROADCAST" -> handleStart(msg);
                case "GAME_UPDATE_BROADCAST" -> handleUpdate(msg);
                case "GAME_END_BROADCAST" -> handleEnd(msg);
                case "GAME_REMATCH_NOTICE" -> handleRematchNotice();
                default -> {}
            }
        });
    }

    private void handleStart(Message msg) {
        this.sessionId = msg.sessionId;
        running = true;
        view.getInputField().setDisable(false);
        surrenderBtn.setDisable(false);
        rematchBtn.setDisable(true);
        rematchBtn.setText("재경기");
        rematchStatus.setText("");
        view.setRematchStatus("", false);
        view.getInputField().clear();
        view.getInputField().requestFocus();

        Map<String, Object> data = msg.data;
        if (data != null) {
            view.setTimeText(formatTime(toDouble(data.get("timeMs"))));
            view.setScoreText("점수: 0");
            view.setComboText("매칭 모드");
            view.setPosText("위치: 0.0");
            view.setEffectsText(valueOf(data.getOrDefault("effectsSelf", "효과: 없음")));
            view.setLastItemText("최근 아이템: 없음");
            TugOfWarViewState state = new TugOfWarViewState(
                    0.0,
                    valueOf(data.get("yourWord")),
                    parseModifier(data.get("modifierSelf")),
                    Boolean.TRUE.equals(data.get("blindSelf")),
                    Boolean.TRUE.equals(data.get("jamoSplitSelf"))
            );
            updateRopeState(state);
        }
        if (!isShowing()) show();
    }

    private void handleUpdate(Message msg) {
        if (!running || sessionId == null || !sessionId.equals(msg.sessionId)) return;
        Map<String, Object> data = msg.data;
        if (data == null) return;

        view.setTimeText(formatTime(toDouble(data.get("timeMs"))));
        view.setScoreText(String.format("점수 (나/상대): %d / %d",
                toInt(data.get("scoreSelf")),
                toInt(data.get("scoreOpponent"))));
        view.setComboText("온라인 경기 진행 중");
        double pos = toDouble(data.get("pos"));
        view.setPosText(String.format("위치: %.1f", pos));
        view.setEffectsText(valueOf(data.getOrDefault("effectsSelf", "효과: 없음")));
        view.setLastItemText("최근 아이템: " + valueOf(data.get("lastItemSelf")));

        TugOfWarViewState state = new TugOfWarViewState(
                pos,
                valueOf(data.get("yourWord")),
                parseModifier(data.get("modifierSelf")),
                Boolean.TRUE.equals(data.get("blindSelf")),
                Boolean.TRUE.equals(data.get("jamoSplitSelf"))
        );
        updateRopeState(state);
    }

    private void handleEnd(Message msg) {
        if (sessionId == null || !sessionId.equals(msg.sessionId)) return;
        running = false;
        view.getInputField().setDisable(true);
        surrenderBtn.setDisable(true);
        rematchBtn.setDisable(false);
        view.setRematchStatus("재경기 가능", true);
        Map<String, Object> data = msg.data;
        if (data != null) {
            view.setEffectsText(valueOf(data.get("result")));
            view.setLastItemText(valueOf(data.get("message")));
        }
    }

    private void handleRematchNotice() {
        rematchBtn.setDisable(false);
        rematchBtn.setText("상대가 재경기를 원합니다");
        view.setRematchStatus("상대가 재경기를 원합니다", true);
    }

    private void submitWord() {
        if (!running || sessionId == null) return;
        String typed = view.getInputField().getText().trim();
        if (typed.isEmpty()) return;
        view.getInputField().clear();
        Message action = Message.of("GAME_ACTION");
        action.sessionId = sessionId;
        action.data = Map.of("word", typed);
        client.send(action);
    }

    private void sendForfeit() {
        if (!running || sessionId == null) return;
        Message msg = Message.of("GAME_FORFEIT");
        msg.sessionId = sessionId;
        client.send(msg);
        running = false;
        view.getInputField().setDisable(true);
        surrenderBtn.setDisable(true);
        rematchBtn.setDisable(true);
    }

    private void sendRematchRequest() {
        if (sessionId == null) return;
        Message msg = Message.of("GAME_REMATCH_REQUEST");
        msg.sessionId = sessionId;
        client.send(msg);
        rematchBtn.setText("재경기 요청됨");
        rematchBtn.setDisable(true);
        view.setRematchStatus("재경기 요청 보냄", false);
    }

    private void updateRopeState(TugOfWarViewState state) {
        ropePanel.updateState(state);
    }

    private String valueOf(Object obj) {
        return obj == null ? "-" : String.valueOf(obj);
    }

    private String formatTime(double ms) {
        return String.format("남은 시간: %.1fs", ms / 1000.0);
    }

    private double toDouble(Object obj) {
        if (obj instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(obj));
        } catch (Exception e) {
            return 0;
        }
    }

    private int toInt(Object obj) {
        if (obj instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(obj));
        } catch (Exception e) {
            return 0;
        }
    }

    private GameLogic.WordModifier parseModifier(Object obj) {
        if (obj instanceof String s) {
            try {
                return GameLogic.WordModifier.valueOf(s);
            } catch (IllegalArgumentException ignored) {}
        }
        return GameLogic.WordModifier.NEUTRAL;
    }
}
