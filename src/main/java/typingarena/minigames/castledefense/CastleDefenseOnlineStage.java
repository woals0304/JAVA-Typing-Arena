package typingarena.minigames.castledefense;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import typingarena.net.Message;
import typingarena.net.NetClient;
import java.util.HashMap;
import java.util.Map;

public class CastleDefenseOnlineStage extends CastleDefenseGame {

    private final NetClient client;
    private String sessionId;
    
    private boolean isPlayer1 = false; 
    private final Map<Integer, Monster> monsterMap = new HashMap<>();

    private boolean updatingFromServer = false;

    public CastleDefenseOnlineStage(NetClient client) {
        super(true);
        this.client = client;
        
        this.gameStartOverlay.setVisible(true);
        this.gameStartOverlay.getChildren().clear();
        javafx.scene.control.Label waiting = new javafx.scene.control.Label("매칭 성공! 대기 중...");
        waiting.setStyle("-fx-font-size: 24px; -fx-text-fill: white; -fx-font-weight: bold;");
        this.gameStartOverlay.getChildren().add(waiting);

        this.setOnCloseRequest(e -> { sendForfeit(); stopGame(); });
        
        this.castleHp.addListener((obs, oldV, newV) -> {
            if (isPlayer1 && !updatingFromServer && newV.intValue() < oldV.intValue()) {
                int damage = oldV.intValue() - newV.intValue();
                sendAction("damage", damage);
            }
        });
    }

    @Override protected void spawnNormalMonster() {} 
    @Override protected void spawnFastMonster() {}
    @Override protected void spawnHeart() {}

    public void handleMessage(Message msg) {
        if (msg == null || msg.type == null) return;
        Platform.runLater(() -> {
            switch (msg.type) {
                case "GAME_START_BROADCAST" -> handleGameStart(msg);
                case "GAME_UPDATE_BROADCAST" -> handleGameUpdate(msg);
                case "GAME_SPAWN_BROADCAST" -> handleSpawn(msg); 
                case "MONSTER_KILLED" -> handleRemoteKill(msg);
                case "MONSTER_UPDATE" -> handleMonsterUpdate(msg);
                case "OPPONENT_ATTACK" -> handleOpponentAttack(msg);
                case "GAME_END_BROADCAST" -> handleGameEnd(msg);
            }
        });
    }

    private void handleSpawn(Message msg) {
        if (!isRunning || msg.data == null) return;
        try {
            int spawnId = ((Number) msg.data.get("spawnId")).intValue();
            String type = (String) msg.data.get("spawnType");
            String word = (String) msg.data.get("word");
            double yRatio = ((Number) msg.data.get("yRatio")).doubleValue();
            double y = GAME_HEIGHT * yRatio; 

            if ("NORMAL".equals(type)) {
                // 속도 1.0 (보통)
                createAndRegisterMonster(spawnId, word, normalMonsterSprites, y, 2, 1.0); 
            } else if ("FAST".equals(type)) {
                // 속도 2.0 (빠름)
                createAndRegisterMonster(spawnId, word, fastMonsterSprites, y, 1, 2.0);
            } else if ("HEART".equals(type)) {
                heartManager.spawn();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void createAndRegisterMonster(int id, String word, javafx.scene.image.Image[] sprites, double y, int hp, double speed) {
        Monster m = new Monster(word, sprites, GAME_WIDTH + 50, y, hp, speed);
        m.setUserData(id); 
        monsterMap.put(id, m);
        activeMonsters.add(m);
        entityLayer.getChildren().add(m);
        m.toBack();
    }

    private void handleRemoteKill(Message msg) {
        if (msg.data != null) {
            int id = ((Number) msg.data.get("killId")).intValue();
            Monster m = monsterMap.remove(id);
            if (m != null) {
                entityLayer.getChildren().remove(m);
                activeMonsters.remove(m);
                soundManager.play("die.mp3", 0.3);
            }
        }
    }

    @Override
    protected void handleInput() {
        if (!isRunning) return;
        String text = inputField.getText().trim();
        inputField.clear();
        inputField.requestFocus();
        if (text.isEmpty()) return;

        if (heartManager.checkInput(text, castleHp)) {
            soundManager.play("hp.mp3", 0.3);
            sendAction("heal", 1);
            return;
        }

        for (Monster m : activeMonsters) {
            if (m.getWord().equalsIgnoreCase(text)) {
                Player myPlayer = isPlayer1 ? player1 : player2;
                
                myPlayer.attack(m, entityLayer, comboManager.isMaxEffect(), () -> {
                    handleLocalHit(m); 
                });
                
                if (m.getUserData() instanceof Integer spawnId) {
                    sendAction("attack", 0, "targetId", spawnId);
                }
                
                m.setTargeted(true);
                comboManager.increase();
                updateComboVisuals(Integer.parseInt(lblComboValue.getText()) + 1);
                return;
            }
        }
        comboManager.reset();
        updateComboVisuals(0);
    }

    private void handleLocalHit(Monster m) {
        if (!(m.getUserData() instanceof Integer spawnId)) return;

        int damage = comboManager.isMaxEffect() ? 2 : 1;
        boolean isDead = m.getHp() <= damage;
        
        if (isDead) {
            monsterMap.remove(spawnId);
            int points = 10 * comboManager.getScoreMultiplier();
            sendAction("killId", spawnId, "scoreAdd", points);
            
            entityLayer.getChildren().remove(m);
            activeMonsters.remove(m);
            soundManager.play("die.mp3", 0.5);
        } else {
            m.takeDamage(damage); 
            soundManager.play("attack.mp3", 0.5);
            // 안 죽었으면 서버에 단어 변경 요청은 하지 않음 (단순 데미지 처리)
            // 필요 시 여기에 단어 변경 요청 로직 추가 가능
        }
    }

    private void handleMonsterUpdate(Message msg) {
        if (msg.data == null) return;
        int id = ((Number) msg.data.get("hitId")).intValue();
        String newWord = (String) msg.data.get("newWord");
        
        Monster m = monsterMap.get(id);
        if (m != null) {
            m.setWord(newWord);
            m.takeDamage(0); 
        }
    }

    private void handleOpponentAttack(Message msg) {
        if (msg.data == null) return;
        Player oppPlayer = isPlayer1 ? player2 : player1;
        
        if (msg.data.containsKey("targetId")) {
            int targetId = ((Number) msg.data.get("targetId")).intValue();
            Monster target = monsterMap.get(targetId);
            if (target != null) {
                oppPlayer.attack(target, entityLayer, false, () -> {});
            }
        }
    }

    @Override protected void killMonster(Monster m) {}

    private void handleGameStart(Message msg) {
        this.sessionId = msg.sessionId;
        this.gameStartOverlay.setVisible(false);
        this.gameOverOverlay.setVisible(false);
        
        this.entityLayer.getChildren().removeIf(n -> n instanceof Monster);
        this.activeMonsters.clear();
        this.monsterMap.clear();

        if (msg.data != null) {
            this.isPlayer1 = Boolean.TRUE.equals(msg.data.get("isPlayer1"));
            if (msg.data.containsKey("teamHp")) {
                int initHp = ((Number) msg.data.get("teamHp")).intValue();
                updatingFromServer = true;
                this.castleHp.set(initHp);
                updatingFromServer = false;
            }
        }
        super.startGame();
    }

    private void handleGameUpdate(Message msg) {
        if (!isRunning || msg.data == null) return;
        if (msg.data.containsKey("teamScore")) {
            int tScore = ((Number) msg.data.get("teamScore")).intValue();
            this.score = tScore;
            this.txtScore.setText(String.valueOf(tScore));
        }
        if (msg.data.containsKey("teamHp")) {
            int tHp = ((Number) msg.data.get("teamHp")).intValue();
            updatingFromServer = true;
            this.castleHp.set(tHp);
            updatingFromServer = false;
        }
    }

    private void handleGameEnd(Message msg) {
        stopGame();
        boolean isWin = "승리".equals(msg.data.get("result"));
        String message = (String) msg.data.get("message");
        super.showGameOver(isWin);
        this.lblResultTitle.setText(isWin ? "VICTORY!" : "DEFEAT");
        if (message != null) this.lblResultScore.setText(message);
        
        // [핵심] "다시 하기" 버튼 찾아서 아예 안 보이게 숨김 (나가기만 남음)
        removeRestartButton();
    }
    
    // "다시 하기" 버튼을 찾아서 숨기는 메서드
    private void removeRestartButton() {
        if (gameOverOverlay == null) return;
        gameOverOverlay.getChildren().forEach(n -> {
            if (n instanceof VBox box) {
                box.getChildren().forEach(child -> {
                    if (child instanceof HBox hbox) {
                        hbox.getChildren().forEach(btnNode -> {
                            if (btnNode instanceof Button btn) {
                                if (btn.getText().contains("다시 하기")) {
                                    btn.setVisible(false); // 화면에서 숨김
                                    btn.setManaged(false); // 자리도 차지하지 않게 함
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    private void sendAction(String key, int value) {
        if (sessionId == null) return;
        Message action = Message.of("GAME_ACTION");
        action.sessionId = sessionId;
        action.data = Map.of(key, value);
        client.send(action);
    }
    
    private void sendAction(String key, int value, String key2, Object value2) {
        if (sessionId == null) return;
        Message action = Message.of("GAME_ACTION");
        action.sessionId = sessionId;
        action.data = Map.of(key, value, key2, value2);
        client.send(action);
    }

    private void sendForfeit() {
        if (sessionId == null) return;
        Message msg = Message.of("GAME_FORFEIT");
        msg.sessionId = sessionId;
        client.send(msg);
    }
}