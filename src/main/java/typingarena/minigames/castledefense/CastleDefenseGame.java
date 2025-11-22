package typingarena.minigames.castledefense;

import javafx.animation.AnimationTimer;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class CastleDefenseGame extends Stage {

    // --- [1] 설정 값 ---
    private static final int TILE_SIZE = 64; 
    private static final int MAP_COLS = 16; 
    private static final int MAP_ROWS = 10; 
    private final double GAME_WIDTH = MAP_COLS * TILE_SIZE;
    private final double GAME_HEIGHT = MAP_ROWS * TILE_SIZE;

    // --- [2] 타일 타입 정의 ---
    private static final int G=0, G1=1, G2=7, P=2, PD=3, P1=4, PU=5, F=8, F1=9, F2=10, F3=11, F4=12, F1U=13, F4U=14, F1D=15, F4D=16, S=17, S1=18;

    // --- [3] 맵 데이터 ---
    private final int[][] mapData = {
        {G, G, G, G, G, G, G, G, G, G, G, G, G, G, G, G},
        {G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1},
        {G2, G2, G2, G2, G2, G2, G2, G2, G2, G2, G2, G2, G2, G2, G2, G2},
        {F1U, F4U, F2, PU, PU, PU, PU, PU, PU, PU, PU, PU, PU, PU, PU, PU}, 
        {P1, P, F, P, P, P1, P, P1, P, P, P, P1, P, P, P, P1},
        {P, P1, F, P, P1, P, P, P, P1, P, P1, P, P1, P, P1, P},
        {P1, P, F, P, P, P, P, P1, P, P, P, P, P, P, P1, P},
        {P, P1, F, P, P, P1, P, P, P, P, P1, P, P, P, P1, P},
        {F1D, F4D, F3, PD, PD, PD, PD, PD, PD, PD, PD, PD, PD, PD, PD, PD}, 
        {S, S, S1, S, S, S1, S, S1, S1, S, S, S1, S, S, S1, S}
    };

    // --- [4] 리소스 변수 ---
    private Image groundImage, groundImage1, groundImage2, pathImage, pathImage1, pathImageU, pathImageD;
    private Image fenceImage, fenceImage1, fenceImage2, fenceImage3, fenceImage4;
    private Image castleImage, castleImage1;
    private Image playerImage, monsterImage; // 캐릭터 이미지

    // [경로 설정]
    private final String BASE_PATH = "/images/castledefense/Tiles/";
    private final String PLAYER_PATH = "/images/castledefense/Players/1P.png";   
    private final String MONSTER_PATH = "/images/castledefense/Monsters/M1.png"; 

    // --- [5] 게임 로직 변수 ---
    private static final String WORD_RESOURCE = "words/ko.json";
    private static final String[] DEFAULT_WORDS = { "성", "몬스터", "방어", "실패" };
    private static final List<String> WORD_POOL = loadWordPool();
    
    private Castle castle = new Castle();
    private List<Monster> activeMonsters = new ArrayList<>();
    private List<HeartItem> activeHearts = new ArrayList<>();
    
    private Player player; 
    
    private Pane entityLayer;
    private TextField inputField;
    private Button startButton; // 여기서 선언만 하고 초기화는 생성자나 메서드에서
    private Label scoreLabel;
    private Label timerLabel; 
    private ProgressBar hpBar;
    private HBox heartsBox;   
    private Rectangle flashOverlay;

    private boolean isRunning = false;
    private AnimationTimer gameLoop;
    private long lastMonsterSpawnTime = 0;
    private long lastHeartSpawnTime = 0;
    private long gameStartTime = 0;
    private final long GAME_DURATION_SECONDS = 60;
    private long damageFlashUntil = 0;
    private int score = 0;

    // --- 생성자 ---
    public CastleDefenseGame() {
        loadResources();

        // [수정] startButton 초기화 (여기서 먼저 생성해야 함)
        startButton = new Button("게임 시작");
        startButton.setFont(Font.font("System", FontWeight.BOLD, 18));
        // startButton.setOnAction(e -> startGame()); // createBottomBar에서 설정하므로 여기선 생략 가능

        BorderPane root = new BorderPane();
        root.setTop(createTopBar());

        StackPane gameCenter = new StackPane();
        gameCenter.setStyle("-fx-background-color: #222;");
        
        // 1. 맵
        GridPane mapLayer = createGameMap();
        
        // 2. 유닛 레이어
        entityLayer = new Pane();
        entityLayer.setPrefSize(GAME_WIDTH, GAME_HEIGHT);
        
        // 피격 효과
        flashOverlay = new Rectangle(GAME_WIDTH, GAME_HEIGHT, Color.rgb(255, 0, 0, 0.3));
        flashOverlay.setVisible(false);
        flashOverlay.setMouseTransparent(true);

        // 플레이어 생성 (Player 클래스 사용)
        double playerX = 150; // 성 근처
        double playerY = GAME_HEIGHT / 2;
        player = new Player(playerImage, playerX, playerY);
        
        entityLayer.getChildren().addAll(player, flashOverlay);
        gameCenter.getChildren().addAll(mapLayer, entityLayer);
        root.setCenter(gameCenter);

        // 하단 입력창
        root.setBottom(createBottomBar());

        // 이벤트 리스너
        castle.hpProperty().addListener((obs, o, n) -> {
            hpBar.setProgress(n.doubleValue() / 3.0); // HP바 업데이트
            updateHeartsUI(); // 하트 아이콘 업데이트
        });

        Scene scene = new Scene(root, GAME_WIDTH, GAME_HEIGHT + 100);
        this.setTitle("Castle Defense");
        this.setScene(scene);
        this.setOnCloseRequest(e -> stopGame());
    }

    // --- 게임 루프 ---
    private void startGame() {
        isRunning = true;
        score = 0;
        castle.hpProperty().set(3);
        scoreLabel.setText("Score: 0");
        timerLabel.setText("00:00");
        
        // 몬스터/아이템 초기화 (플레이어와 flashOverlay 제외하고 다 삭제)
        entityLayer.getChildren().removeIf(n -> 
            !"PLAYER".equals(n.getId()) && n != flashOverlay
        );
        activeMonsters.clear();
        activeHearts.clear();

        startButton.setDisable(true);
        inputField.setDisable(false);
        inputField.requestFocus(); // [중요] 입력창 포커스

        gameStartTime = System.nanoTime();
        
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update(now);
            }
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

        // 타이머 업데이트
        long elapsedSeconds = (now - gameStartTime) / 1_000_000_000;
        if (elapsedSeconds >= GAME_DURATION_SECONDS) {
            stopGame();
            showGameOver(true);
            return;
        }
        long minutes = elapsedSeconds / 60;
        long seconds = elapsedSeconds % 60;
        timerLabel.setText(String.format("%02d:%02d", minutes, seconds));

        // 피격 효과 처리
        if (damageFlashUntil > 0) {
            if (now < damageFlashUntil) flashOverlay.setVisible(true);
            else {
                flashOverlay.setVisible(false);
                damageFlashUntil = 0;
            }
        }

        // 1. 몬스터 스폰 (2초)
        if (now - lastMonsterSpawnTime > 2_000_000_000L) {
            spawnMonster();
            lastMonsterSpawnTime = now;
        }
        // 2. 하트 스폰 (15초)
        if (now - lastHeartSpawnTime > 15_000_000_000L) {
            spawnHeartItem();
            lastHeartSpawnTime = now;
        }

        // 3. 이동 및 충돌 체크
        moveEntities();
        
        // 4. 게임 오버 체크
        if (castle.isDestroyed()) {
            stopGame();
            showGameOver(false);
        }
    }

    private void spawnMonster() {
        if (WORD_POOL.isEmpty()) return;
        String word = WORD_POOL.get(new Random().nextInt(WORD_POOL.size()));
        
        double y = GAME_HEIGHT * 0.25 + new Random().nextDouble() * (GAME_HEIGHT * 0.5);
        // 분리된 Monster 클래스 사용
        Monster m = new Monster(word, monsterImage, GAME_WIDTH + 50, y);
        
        activeMonsters.add(m);
        entityLayer.getChildren().add(m);
        m.toBack(); 
    }

    private void spawnHeartItem() {
        HeartItem h = new HeartItem(GAME_WIDTH + 50, GAME_HEIGHT * 0.1);
        activeHearts.add(h);
        entityLayer.getChildren().add(h);
    }

    private void moveEntities() {
        // 몬스터 이동
        Iterator<Monster> it = activeMonsters.iterator();
        while (it.hasNext()) {
            Monster m = it.next();
            m.move(2.0); // 속도

            if (m.getLayoutX() < 100) { // 성벽 도달
                castle.takeDamage();
                damageFlashUntil = System.nanoTime() + 150_000_000L; // 0.15초간 번쩍
                entityLayer.getChildren().remove(m);
                it.remove();
            }
        }
        
        // 하트 이동
        Iterator<HeartItem> hit = activeHearts.iterator();
        while (hit.hasNext()) {
            HeartItem h = hit.next();
            h.move();
            if (h.getTranslateX() < -50) {
                entityLayer.getChildren().remove(h);
                hit.remove();
            }
        }
    }

    // --- [입력 처리] ---
    private void handleInput() {
        if (!isRunning) return;
        
        String text = inputField.getText().trim();
        inputField.clear(); // 입력창 비우기
        
        if (text.isEmpty()) return;

        // 1. 하트 체크
        for (HeartItem h : activeHearts) {
            if (text.equals("하트")) {
                castle.addHp();
                entityLayer.getChildren().remove(h);
                activeHearts.remove(h);
                return;
            }
        }

        // 2. 몬스터 체크
        for (Monster m : activeMonsters) {
            if (m.getWord().equalsIgnoreCase(text)) {
                launchProjectile(m);
                m.setTargeted(true); 
                return; 
            }
        }
    }

    private void launchProjectile(Monster target) {
        Circle projectile = new Circle(8, Color.CYAN);
        projectile.setLayoutX(player.getLayoutX() + 32);
        projectile.setLayoutY(player.getLayoutY() + 32);
        entityLayer.getChildren().add(projectile);

        TranslateTransition tt = new TranslateTransition(Duration.millis(300), projectile);
        // Monster 클래스에 getCenterX(), getCenterY() 메서드가 있다고 가정
        tt.setToX(target.getCenterX() - projectile.getLayoutX());
        tt.setToY(target.getCenterY() - projectile.getLayoutY());
        
        tt.setOnFinished(e -> {
            entityLayer.getChildren().remove(projectile);
            if (activeMonsters.contains(target)) {
                entityLayer.getChildren().remove(target);
                activeMonsters.remove(target);
                score += 10;
                scoreLabel.setText("Score: " + score);
            }
        });
        tt.play();
    }

    // --- UI 생성 ---
    private HBox createBottomBar() {
        HBox box = new HBox(15);
        box.setPadding(new Insets(15));
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: #333;");

        inputField = new TextField();
        inputField.setPromptText("단어 입력...");
        inputField.setPrefWidth(400);
        inputField.setFont(Font.font(18));
        inputField.setOnAction(e -> handleInput()); // 엔터키 처리

        Button atkBtn = new Button("공격");
        atkBtn.setOnAction(e -> {
            handleInput();
            inputField.requestFocus();
        });

        // [수정] startButton은 생성자에서 이미 초기화됨. 여기선 이벤트만 연결.
        startButton.setOnAction(e -> startGame());

        box.getChildren().addAll(inputField, atkBtn, startButton);
        return box;
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

        box.getChildren().addAll(new Label("HP:"), hpBar, heartsBox, timerLabel, scoreLabel);
        return box;
    }
    
    private void updateHeartsUI() {
        if (heartsBox == null) return;
        heartsBox.getChildren().clear();
        for (int i = 0; i < castle.getHp(); i++) {
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
                
                ImageView base = new ImageView();
                setupImageView(base);
                
                // 바닥 로직
                Image img = groundImage;
                if (type == P || type == F) img = pathImage;
                else if (type == PU || type == F1U) img = pathImageU;
                else if (type == PD || type == F1D) img = pathImageD;
                else if (type == G1) img = groundImage1;
                else if (type == G2) img = groundImage2;
                else if (type == P1) img = pathImage1;
                // ... (필요한 매핑 추가)
                base.setImage(img);
                s.getChildren().add(base);
                
                // 상단 로직
                if (type >= F && type <= S1) { 
                    ImageView top = new ImageView();
                    setupImageView(top);
                    if (type == F) top.setImage(fenceImage);
                    else if (type == F1) top.setImage(fenceImage1);
                    else if (type == F2) top.setImage(fenceImage2);
                    else if (type == F3) top.setImage(fenceImage3);
                    else if (type == F4) top.setImage(fenceImage4);
                    else if (type == F1U) top.setImage(fenceImage1);
                    else if (type == F4U) top.setImage(fenceImage4);
                    else if (type == F1D) top.setImage(fenceImage1);
                    else if (type == F4D) top.setImage(fenceImage4);
                    else if (type == S) top.setImage(castleImage);
                    else if (type == S1) top.setImage(castleImage1);
                    
                    if (top.getImage() != null) s.getChildren().add(top);
                }
                grid.add(s, c, r);
            }
        }
        return grid;
    }
    
    private void setupImageView(ImageView v) {
        v.setFitWidth(TILE_SIZE); v.setFitHeight(TILE_SIZE);
        v.setPreserveRatio(true); v.setSmooth(false);
    }

    private void loadResources() {
        groundImage = loadImage(BASE_PATH + "G.png");
        groundImage1 = loadImage(BASE_PATH + "G1.png");
        groundImage2 = loadImage(BASE_PATH + "G2.png");
        pathImage = loadImage(BASE_PATH + "P.png");
        pathImage1 = loadImage(BASE_PATH + "P1.png");
        pathImageU = loadImage(BASE_PATH + "PU.png");
        pathImageD = loadImage(BASE_PATH + "PD.png");
        fenceImage = loadImage(BASE_PATH + "F.png");
        fenceImage1 = loadImage(BASE_PATH + "F1.png");
        fenceImage2 = loadImage(BASE_PATH + "F2.png");
        fenceImage3 = loadImage(BASE_PATH + "F3.png");
        fenceImage4 = loadImage(BASE_PATH + "F4.png");
        castleImage = loadImage(BASE_PATH + "S.png");
        castleImage1 = loadImage(BASE_PATH + "S1.png");
        playerImage = loadImage(PLAYER_PATH);
        monsterImage = loadImage(MONSTER_PATH);
    }

    private Image loadImage(String path) {
        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is == null) return null;
            return new Image(is);
        } catch (Exception e) { return null; }
    }
    
    private void showGameOver(boolean isVictory) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText(isVictory ? "승리!" : "패배...");
        alert.setContentText("최종 점수: " + score);
        alert.show();
    }

    // --- 단어 로딩 (기존) ---
    private static List<String> loadWordPool() {
        List<String> words = loadWordsFromClasspath();
        if (words.isEmpty()) words = loadWordsFromFilesystem();
        if (words.isEmpty()) words = new ArrayList<>(Arrays.asList(DEFAULT_WORDS));
        return Collections.unmodifiableList(words);
    }

    private static List<String> loadWordsFromClasspath() {
        try (InputStream in = CastleDefenseGame.class.getClassLoader().getResourceAsStream(WORD_RESOURCE)) {
            if (in == null) return Collections.emptyList();
            try (Reader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return parseWordList(reader);
            }
        } catch (Exception e) { return Collections.emptyList(); }
    }

    private static List<String> loadWordsFromFilesystem() {
        Path path = Paths.get("src", "main", "resources").resolve(WORD_RESOURCE);
        if (!Files.exists(path)) return Collections.emptyList();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return parseWordList(reader);
        } catch (IOException e) { return Collections.emptyList(); }
    }

    private static List<String> parseWordList(Reader reader) {
        Gson gson = new Gson();
        WordList data = gson.fromJson(reader, WordList.class);
        if (data == null || data.words == null) return Collections.emptyList();
        List<String> words = new ArrayList<>();
        for (String word : data.words) {
            if (word != null && !word.trim().isEmpty()) words.add(word.trim());
        }
        return words;
    }

    private static final class WordList {
        List<String> words;
    }
}