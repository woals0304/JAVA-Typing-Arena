package typingarena.minigames.castledefense;

import javafx.animation.AnimationTimer;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import com.google.gson.Gson;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

// TileManager의 상수를 편하게 쓰기 위해 import
import static typingarena.minigames.castledefense.TileManager.*;

public class CastleDefenseGame extends Stage {

    // [설정]
    private static final int TILE_SIZE = 64; 
    private static final int MAP_COLS = 16; 
    private static final int MAP_ROWS = 10; 
    private final double GAME_WIDTH = MAP_COLS * TILE_SIZE;
    private final double GAME_HEIGHT = MAP_ROWS * TILE_SIZE;

    // [매니저들]
    private final TileManager tileManager = new TileManager(); 
    private final ComboManager comboManager = new ComboManager(); 
    private HeartManager heartManager; // 하트 매니저 추가

    // [맵 데이터] (가독성 좋게 변수명 사용)
    private final int[][] mapData = {
        {G, G, G, G, G, G, G, G, G, G, G, G, G, G, G, G},
        {G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1},
        {G2, G2, G2, G2, G2, G2, G2, G2, G2, G2, G2, G2, G2, G2, G2, G2},
        {F1U, F4U, F2, PU, PU, PU, PU, PU, PU, PU, PU, PU, PU, PU, PU, PU}, 
        {P1, P, F, P, P, P1, P, P1, P, P, P, P1, P, P, P, P1},
        {P, P1, F, P, P, P1, P, P, P, P, P1, P, P, P, P1, P},
        {P1, P, F, P, P, P, P, P1, P, P, P, P, P, P, P1, P},
        {P, P1, F, P, P, P1, P, P, P, P, P1, P, P, P, P1, P},
        {F1D, F4D, F3, PD, PD, PD, PD, PD, PD, PD, PD, PD, PD, PD, PD, PD}, 
        {S, S, S1, S, S, S1, S, S1, S1, S, S, S1, S, S, S1, S}
    };

    // [게임 객체]
    private SimpleIntegerProperty castleHp = new SimpleIntegerProperty(3);
    private Player player;
    private List<Monster> activeMonsters = new ArrayList<>();
    
    // [UI]
    private Pane entityLayer;
    private TextField inputField;
    private Button startButton;
    private Label scoreLabel, timerLabel;
    private ProgressBar hpBar;
    private HBox heartsBox;
    private Rectangle flashOverlay;
    
    private boolean isRunning = false;
    private AnimationTimer gameLoop;
    private long lastMonsterSpawnTime = 0, lastHeartSpawnTime = 0, gameStartTime = 0, damageFlashUntil = 0;
    private int score = 0;
    
    private static final List<String> WORD_POOL = loadWordPool();

    public CastleDefenseGame() {
        startButton = new Button("게임 시작");
        startButton.setFont(Font.font("System", FontWeight.BOLD, 18));
        startButton.setOnAction(e -> startGame());

        BorderPane root = new BorderPane();
        root.setTop(createTopBar());

        StackPane gameCenter = new StackPane();
        gameCenter.setStyle("-fx-background-color: #222;");
        gameCenter.getChildren().addAll(createGameMap(), createEntityLayer());
        root.setCenter(gameCenter);
        root.setBottom(createBottomBar());

        // 하트 매니저 초기화 (Layer 생성 후 해야 함)
        heartManager = new HeartManager(entityLayer, GAME_WIDTH + 50, GAME_HEIGHT * 0.1);

        castleHp.addListener((obs, o, n) -> {
            hpBar.setProgress(n.doubleValue() / 3.0);
            updateHeartsUI();
        });

        Scene scene = new Scene(root, GAME_WIDTH, GAME_HEIGHT + 100);
        this.setTitle("Castle Defense");
        this.setScene(scene);
        this.setOnCloseRequest(e -> stopGame());
    }

    private Pane createEntityLayer() {
        entityLayer = new Pane();
        entityLayer.setPrefSize(GAME_WIDTH, GAME_HEIGHT);
        
        flashOverlay = new Rectangle(GAME_WIDTH, GAME_HEIGHT, Color.rgb(255, 0, 0, 0.3));
        flashOverlay.setVisible(false);
        flashOverlay.setMouseTransparent(true);

        player = new Player(150, GAME_HEIGHT / 2);
        entityLayer.getChildren().addAll(player, flashOverlay);
        return entityLayer;
    }

    private void startGame() {
        isRunning = true;
        score = 0;
        comboManager.reset();
        heartManager.clear(); // 하트 초기화 위임
        castleHp.set(3);
        scoreLabel.setText("Score: 0");
        timerLabel.setText("00:00");
        
        entityLayer.getChildren().removeIf(n -> !"PLAYER".equals(n.getId()) && n != flashOverlay);
        activeMonsters.clear();

        startButton.setDisable(true);
        inputField.setDisable(false);
        inputField.requestFocus();

        gameStartTime = System.nanoTime();
        gameLoop = new AnimationTimer() {
            @Override public void handle(long now) { update(now); }
        };
        gameLoop.start();
    }

    private void stopGame() {
        isRunning = false;
        if (gameLoop != null) gameLoop.stop();
        startButton.setDisable(false);
        inputField.setDisable(true);
    }

    private void update(long now) {
        if (!isRunning) return;

        long elapsed = (now - gameStartTime) / 1_000_000_000;
        if (elapsed >= 60) { stopGame(); showGameOver(true); return; }
        timerLabel.setText(String.format("%02d:%02d", elapsed / 60, elapsed % 60));

        if (damageFlashUntil > 0) {
            flashOverlay.setVisible(now < damageFlashUntil);
            if (now >= damageFlashUntil) damageFlashUntil = 0;
        }

        if (now - lastMonsterSpawnTime > 2_000_000_000L) { spawnMonster(); lastMonsterSpawnTime = now; }
        if (now - lastHeartSpawnTime > 15_000_000_000L) { 
            heartManager.spawn(); // 하트 스폰 위임
            lastHeartSpawnTime = now; 
        }

        moveEntities();

        if (castleHp.get() <= 0) { stopGame(); showGameOver(false); }
    }

    private void spawnMonster() {
        if (WORD_POOL.isEmpty()) return;
        String word = WORD_POOL.get(new Random().nextInt(WORD_POOL.size()));
        double y = GAME_HEIGHT * 0.25 + new Random().nextDouble() * (GAME_HEIGHT * 0.5);
        
        Monster m = new Monster(word, Monster.loadAssets(), GAME_WIDTH + 50, y);
        activeMonsters.add(m);
        entityLayer.getChildren().add(m);
        m.toBack();
    }

    private void moveEntities() {
        // 몬스터 이동
        Iterator<Monster> it = activeMonsters.iterator();
        while (it.hasNext()) {
            Monster m = it.next();
            m.move(2.0);

            if (m.hasReachedCastle()) {
                if (castleHp.get() > 0) castleHp.set(castleHp.get() - 1);
                damageFlashUntil = System.nanoTime() + 150_000_000L;
                comboManager.reset();
                entityLayer.getChildren().remove(m);
                it.remove();
            }
        }
        // 하트 이동 (위임)
        heartManager.update();
    }

    private void handleInput() {
        if (!isRunning) return;
        String text = inputField.getText().trim();
        inputField.clear();
        inputField.requestFocus();
        if (text.isEmpty()) return;

        // 1. 하트 아이템 체크 (위임)
        if (heartManager.checkInput(text, castleHp)) {
            return; // 하트를 먹었으면 종료
        }

        // 2. 몬스터 체크
        for (Monster m : activeMonsters) {
            if (m.getWord().equalsIgnoreCase(text)) {
                comboManager.increase();
                player.attack(m, entityLayer, comboManager.isMaxEffect(), () -> killMonster(m));
                m.setTargeted(true);
                return;
            }
        }
        comboManager.reset();
    }

    private void killMonster(Monster m) {
        if (activeMonsters.contains(m)) {
            entityLayer.getChildren().remove(m);
            activeMonsters.remove(m);
            score += 10 * comboManager.getScoreMultiplier();
            scoreLabel.setText("Score: " + score);
        }
    }

    private HBox createTopBar() {
        HBox box = new HBox(20);
        box.setPadding(new Insets(10));
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: #333;");

        hpBar = new ProgressBar(1.0);
        hpBar.setStyle("-fx-accent: #FF5555;");
        scoreLabel = new Label("Score: 0");
        scoreLabel.setTextFill(Color.WHITE);
        scoreLabel.setFont(Font.font(20));
        timerLabel = new Label("00:00");
        timerLabel.setTextFill(Color.WHITE);
        timerLabel.setFont(Font.font(20));
        heartsBox = new HBox(5);
        updateHeartsUI();

        box.getChildren().addAll(new Label("HP:"), hpBar, heartsBox, comboManager.getLabel(), timerLabel, scoreLabel);
        return box;
    }

    private HBox createBottomBar() {
        HBox box = new HBox(15);
        box.setPadding(new Insets(15));
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: #333;");
        
        inputField = new TextField();
        inputField.setPromptText("단어 입력...");
        inputField.setPrefWidth(400);
        inputField.setFont(Font.font(18));
        inputField.setOnAction(e -> handleInput());
        
        box.getChildren().addAll(inputField, startButton);
        return box;
    }

    private void updateHeartsUI() {
        if (heartsBox == null) return;
        heartsBox.getChildren().clear();
        for (int i = 0; i < castleHp.get(); i++) {
            Label heart = new Label("❤️");
            heart.setStyle("-fx-font-size: 20px;");
            heartsBox.getChildren().add(heart);
        }
    }

    private GridPane createGameMap() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        for (int r = 0; r < MAP_ROWS; r++) {
            for (int c = 0; c < MAP_COLS; c++) {
                int type = mapData[r][c];
                StackPane s = new StackPane();
                
                ImageView base = new ImageView(tileManager.getBaseImage(type));
                base.setFitWidth(TILE_SIZE); base.setFitHeight(TILE_SIZE);
                s.getChildren().add(base);
                
                javafx.scene.image.Image topImg = tileManager.getTopImage(type);
                if (topImg != null) {
                    ImageView top = new ImageView(topImg);
                    top.setFitWidth(TILE_SIZE); top.setFitHeight(TILE_SIZE);
                    s.getChildren().add(top);
                }
                grid.add(s, c, r);
            }
        }
        return grid;
    }

    private void showGameOver(boolean isVictory) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText(isVictory ? "승리!" : "패배...");
        alert.setContentText("최종 점수: " + score);
        alert.show();
    }

    private static List<String> loadWordPool() {
        try (InputStream in = CastleDefenseGame.class.getClassLoader().getResourceAsStream("words/ko.json")) {
            if (in == null) return Arrays.asList("성", "방어", "자바", "게임");
            try (Reader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                Gson gson = new Gson();
                WordList data = gson.fromJson(reader, WordList.class);
                return (data != null && data.words != null) ? data.words : Arrays.asList("오류");
            }
        } catch (Exception e) { return Arrays.asList("성", "방어", "자바", "게임"); }
    }
    private static final class WordList { List<String> words; }
}
