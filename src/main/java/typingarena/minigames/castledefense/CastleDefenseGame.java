package typingarena.minigames.castledefense;

// (JavaFX 및 애니메이션 임포트)
import javafx.animation.AnimationTimer;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert; // [추가] 팝업창
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;


// ( 단어 로딩(Gson) 임포트)
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
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
import java.util.List;
import java.util.Random;

public class CastleDefenseGame extends Stage {
    
    // ----------------------- 1. 게임 모델 및 상수 -----------------------
    
    private static final String WORD_RESOURCE = "words/ko.json";
    private static final String[] DEFAULT_WORDS = { "성", "몬스터", "방어", "실패" };
    private static final List<String> WORD_POOL = loadWordPool();
    
    private Castle castle = new Castle();
    private List<Monster> activeMonsters = new ArrayList<>();
    
    // [추가] 하트 아이템 리스트
    private List<HeartItem> activeHearts = new ArrayList<>(); 
    
    private final double GAME_WIDTH = 800;
    private final double GAME_HEIGHT = 500;
    
    private final double CASTLE_WALL_X_BOUNDARY = 120.0;

    private final double SPAWN_Y_MIN = GAME_HEIGHT * 0.25; // 몬스터 스폰 영역 (아래쪽)
    private final double SPAWN_Y_MAX = GAME_HEIGHT * 0.75;
    
    // [추가] 하트 스폰 영역 (위쪽)
    private final double SPAWN_HEART_Y = GAME_HEIGHT * 0.1; // 상단 10% 위치
    
    private final long SPAWN_INTERVAL_NS = 2_000_000_000L; // 2초 (몬스터)
    private long lastMonsterSpawnTime = 0;
    
    // [추가] 하트 스폰 간격 (예: 15초)
    private final long SPAWN_HEART_INTERVAL_NS = 15_000_000_000L; 
    private long lastHeartSpawnTime = 0;
    
    private final long GAME_DURATION_SECONDS = 60;

    private AnimationTimer gameLoop;
    private long gameStartTime; 

    private Rectangle flashOverlay; 
    private long damageFlashUntil = 0L; 

    // ----------------------- 2. 뷰 요소 (UI) -----------------------
    private Pane gamePane = new Pane();
    
    private Label timerLabel = new Label("00:00"); 
    private Label scoreLabel = new Label("Score: 0"); 
    private HBox heartsBox = new HBox(5); 
    
    private TextField inputField = new TextField();
    
    private final Button startButton = new Button("게임 시작");
    
    private Circle player; // [수정] 네모를 원으로
    private int score = 0;
    
    public CastleDefenseGame() {
        initModality(Modality.NONE);
        
        BorderPane root = new BorderPane();

        // (상단 UI 바 설정 ... 기존과 동일)
        HBox topLeftUI = new HBox(timerLabel);
        topLeftUI.setAlignment(Pos.CENTER);
        topLeftUI.setPadding(new Insets(5));
        timerLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: white; -fx-font-weight: bold;");
        HBox topRightUI = new HBox(10, scoreLabel, heartsBox);
        topRightUI.setAlignment(Pos.CENTER_RIGHT);
        topRightUI.setPadding(new Insets(5));
        scoreLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: white; -fx-font-weight: bold;");
        updateHeartsUI(); 
        BorderPane topUIBar = new BorderPane();
        topUIBar.setLeft(topLeftUI); 
        topUIBar.setCenter(new Label("")); 
        topUIBar.setRight(topRightUI); 
        topUIBar.setStyle("-fx-background-color: #57ff8cff;"); 
        root.setTop(topUIBar); 
        
        // (중앙 게임 화면 ... 기존과 동일)
        gamePane.setPrefSize(GAME_WIDTH, GAME_HEIGHT);
        gamePane.setStyle("-fx-background-color: #ffffffff;");
        flashOverlay = new Rectangle(GAME_WIDTH, GAME_HEIGHT);
        flashOverlay.setFill(Color.rgb(220, 80, 80, 0.4)); 
        flashOverlay.setVisible(false); 
        drawCastleWalls();
       player = new Circle(20, Color.web("#87c7f1ff"));
        player.setStroke(Color.BLACK);
        player.setId("PLAYER"); // [추가] ★★★ 플레이어에게 "PLAYER"라는 이름표를 붙여줍니다 ★★★
        player.setTranslateX(CASTLE_WALL_X_BOUNDARY - 60);
        player.setTranslateY(GAME_HEIGHT / 2);
        gamePane.getChildren().add(player);
        gamePane.getChildren().add(flashOverlay);
        root.setCenter(gamePane); 

        // (하단 입력 바 ... 기존과 동일)
        inputField.setOnAction(e -> handleUserInput(inputField.getText()));
        inputField.setPromptText("여기에 단어를 입력하세요...");
        inputField.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;"); 
        inputField.setDisable(true); 
        inputField.setPrefHeight(50); 
        inputField.setMaxWidth(GAME_WIDTH * 0.4); 
        inputField.setMinWidth(GAME_WIDTH * 0.4); 
        startButton.setFont(Font.font("System", FontWeight.BOLD, 18));
        startButton.setOnAction(e -> startGame());
        HBox bottomBox = new HBox(10, inputField, startButton); 
        bottomBox.setAlignment(Pos.CENTER); 
        bottomBox.setPadding(new Insets(10, 20, 10, 20)); 
        bottomBox.setStyle("-fx-background-color: #ffffffff;");
        root.setBottom(bottomBox); 

        // 6. 리스너 및 게임 루프 정의
        castle.hpProperty().addListener((obs, oldVal, newVal) -> updateHeartsUI());
        
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // (승/패 조건 확인 ... 기존과 동일)
                if (castle.isDestroyed()) {
                    stop();
                    showGameOver(false); 
                    return;
                }
                long elapsedSeconds = (now - gameStartTime) / 1_000_000_000;
                if (elapsedSeconds >= GAME_DURATION_SECONDS) {
                    stop();
                    showGameOver(true); 
                    return;
                }
                
                updateTimer(now); 
                 
                // (번쩍 효과 ... 기존과 동일)
                if (damageFlashUntil > 0L) {
                    if (now < damageFlashUntil) {
                        flashOverlay.setVisible(true); 
                    } else {
                        flashOverlay.setVisible(false); 
                        damageFlashUntil = 0L; 
                    }
                }

                // (몬스터 스폰 ... 기존과 동일)
                if (now - lastMonsterSpawnTime > SPAWN_INTERVAL_NS) {
                    spawnMonster();
                    lastMonsterSpawnTime = now;
                }
                
                // [추가] 하트 스폰 로직
                if (now - lastHeartSpawnTime > SPAWN_HEART_INTERVAL_NS) {
                    spawnHeartItem();
                    lastHeartSpawnTime = now;
                }

                moveAndCheckMonsters();
                
                // [추가] 하트 이동 로직
                moveAndCheckHeartItems();
            }
        };

        setTitle("🏰 성 방어 타자 게임");
        setScene(new Scene(root, GAME_WIDTH, GAME_HEIGHT + 36 + 70)); 
        
        setOnCloseRequest(e -> {
            if (gameLoop != null) {
                gameLoop.stop();
            }
        });
    }

    private void drawCastleWalls() {
        // (기존과 동일)
        Color wallColor = Color.web("#3498db"); 
        double wallThickness = 30;
        Rectangle rightPillar = new Rectangle(CASTLE_WALL_X_BOUNDARY - wallThickness, 0, wallThickness, GAME_HEIGHT);
        rightPillar.setFill(wallColor);
        Rectangle topRoof = new Rectangle(wallThickness, 0, CASTLE_WALL_X_BOUNDARY - wallThickness, wallThickness);
        topRoof.setFill(wallColor);
        Rectangle bottomRoof = new Rectangle(wallThickness, GAME_HEIGHT - wallThickness, CASTLE_WALL_X_BOUNDARY - wallThickness, wallThickness);
        bottomRoof.setFill(wallColor);
        gamePane.getChildren().addAll( rightPillar, topRoof, bottomRoof);
    }
    
    // ----------------------- 3. 게임 시작 메서드 -----------------------
    private void startGame() {
        // (기존 리셋 로직 ... )
        score = 0;
        damageFlashUntil = 0L;
        castle.hpProperty().set(3);
        updateScoreLabel();
        updateHeartsUI();
        timerLabel.setText("00:00");
        scoreLabel.setTextFill(Color.WHITE); 
        flashOverlay.setVisible(false);
        
        // [수정] 몬스터뿐만 아니라 하트 아이템도 모두 제거
        gamePane.getChildren().removeIf(node -> node instanceof Monster || (node instanceof Circle && !"PLAYER".equals(node.getId())));
        activeHearts.clear(); // [추가]

        // [추가] 스폰 타이머 리셋
        lastMonsterSpawnTime = 0L;
        lastHeartSpawnTime = 0L; // [추가]

        // (게임 시작 ... 기존과 동일)
        startButton.setDisable(true); 
        inputField.setDisable(false); 
        inputField.requestFocus(); 
        
        gameStartTime = System.nanoTime(); 
        gameLoop.start(); 
    }

    // (타이머 업데이트 ... 기존과 동일)
    private void updateTimer(long now) {
        long elapsedSeconds = (now - gameStartTime) / 1_000_000_000;
        long minutes = elapsedSeconds / 60;
        long seconds = elapsedSeconds % 60;
        
        if (elapsedSeconds >= GAME_DURATION_SECONDS) {
            timerLabel.setText(String.format("%02d:%02d", GAME_DURATION_SECONDS / 60, GAME_DURATION_SECONDS % 60));
        } else {
            timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
        }
    }
    
    // ----------------------- 4. 몬스터/아이템 관리 로직 -----------------------
    
    // (spawnMonster ... 기존과 동일)
    private void spawnMonster() {
        Random random = new Random();
        if (WORD_POOL.isEmpty()) return;
        String keyword = WORD_POOL.get(random.nextInt(WORD_POOL.size()));
        double startX = GAME_WIDTH - 50;
        double spawnHeightRange = SPAWN_Y_MAX - SPAWN_Y_MIN;
        double startY = SPAWN_Y_MIN + random.nextDouble() * spawnHeightRange;
        Monster monster = new Monster(keyword, startX, startY);
        activeMonsters.add(monster);
        gamePane.getChildren().add(monster);
        monster.toBack();
    }
    
    // [추가] 하트 아이템 스폰 메서드
    private void spawnHeartItem() {
        double startX = GAME_WIDTH - 50; // 오른쪽 끝
        double startY = SPAWN_HEART_Y;   // 화면 상단 (10%)
        
        HeartItem heart = new HeartItem(startX, startY);
        activeHearts.add(heart);
        gamePane.getChildren().add(heart);
    }
    
    // (moveAndCheckMonsters ... 기존과 동일)
    private void moveAndCheckMonsters() {
        List<Monster> monstersToRemove = new ArrayList<>();
        for (Monster monster : activeMonsters) {
            if (monster.isAlive()) {
                monster.move();
                if (monster.getTranslateX() <= CASTLE_WALL_X_BOUNDARY) { 
                    castle.takeDamage();
                    damageFlashUntil = System.nanoTime() + 150_000_000L;
                    monstersToRemove.add(monster);
                }
            } else {
                monstersToRemove.add(monster);
            }
        }
        removeMonsters(monstersToRemove);
    }
    
    // [추가] 하트 아이템 이동 및 제거 메서드
    private void moveAndCheckHeartItems() {
        List<HeartItem> itemsToRemove = new ArrayList<>();
        for (HeartItem item : activeHearts) {
            if (item.isAlive()) {
                item.move();
                // [추가] 화면 왼쪽 밖으로 나가면 제거 (성에는 충돌 안 함)
                if (item.getTranslateX() < -50) { 
                    itemsToRemove.add(item);
                }
            } else {
                // (kill() 당한 하트도 제거)
                itemsToRemove.add(item);
            }
        }
        // 화면과 리스트에서 제거
        gamePane.getChildren().removeAll(itemsToRemove);
        activeHearts.removeAll(itemsToRemove);
    }
    
    private void removeMonsters(List<Monster> monsters) {
        gamePane.getChildren().removeAll(monsters);
        activeMonsters.removeAll(monsters);
    }

    // ----------------------- 5. 사용자 입력 및 처치 로직 -----------------------
    
    // [대폭 수정] 
    private void handleUserInput(String input) {
        if (inputField.isDisabled()) return;
        String trimmedInput = input.trim();
        if (trimmedInput.isEmpty()) {
            inputField.clear(); // 입력창이 비어도 클리어
            return;
        }

        // --- [추가] 1. '하트' 아이템 먼저 확인 ---
        if (trimmedInput.equals("하트")) {
            // 살아있는 하트 아이템을 찾음 (선착순 1개)
            HeartItem matchedHeart = activeHearts.stream()
                .filter(HeartItem::isAlive)
                .findFirst()
                .orElse(null);
                
            if (matchedHeart != null) {
                matchedHeart.kill(); // 1. 하트 아이템 제거 (다음 루프에서 정리됨)
                castle.addHp();      // 2. HP 1 증가 (Castle.java에 추가된 메서드)
                updateHeartsUI();    // 3. 하트 UI 갱신
                
                // (명중 이펙트 - 하트를 흰색으로 번쩍이게 함)
                matchedHeart.setStyle("-fx-effect: dropshadow(gaussian, white, 10, 0.8, 0, 0);");

                inputField.clear();
                return; // 하트 처리 완료, 몬스터 검색 안 함
            }
        }
        // --- 하트 처리 끝 ---

        // 2. (기존 로직) 몬스터 확인
        Monster matchedMonster = activeMonsters.stream()
            .filter(monster -> monster.isAlive() && !monster.isTargeted() && monster.getKeyword().equals(trimmedInput))
            .findFirst()
            .orElse(null);

        if (matchedMonster != null) {
            matchedMonster.setTargeted(true);
            launchProjectile(matchedMonster);
        }
        
        inputField.clear();
    }
    
    // (launchProjectile ... 기존과 동일)
    private void launchProjectile(Monster targetMonster) {
        Circle projectile = new Circle(5, Color.CYAN);
        projectile.setTranslateX(player.getTranslateX());
        projectile.setTranslateY(player.getTranslateY());
        gamePane.getChildren().add(projectile);
        TranslateTransition tt = new TranslateTransition(Duration.millis(400), projectile);
        tt.setToX(targetMonster.getTranslateX() + targetMonster.getWidth() / 2);
        tt.setToY(targetMonster.getTranslateY() + targetMonster.getHeight() / 2);
        tt.setOnFinished(e -> {
            gamePane.getChildren().remove(projectile);
            targetMonster.kill();
            targetMonster.setStyle("-fx-background-color: green; -fx-opacity: 0.5;");
            score += 10;
            updateScoreLabel();
        });
        tt.play();
    }
    
    // ----------------------- 6. UI 및 게임 종료 -----------------------

    // (updateScoreLabel ... 기존과 동일)
    private void updateScoreLabel() {
        scoreLabel.setText(String.format("Score: %d", score));
    }

    // (updateHeartsUI ... 기존과 동일)
    private void updateHeartsUI() {
        heartsBox.getChildren().clear(); 
        for (int i = 0; i < castle.getHp(); i++) {
            heartsBox.getChildren().add(createHeartIcon());
        }
    }

    // (createHeartIcon ... 기존과 동일)
    private Label createHeartIcon() {
        Label heartLabel = new Label("❤️");
        heartLabel.setStyle("-fx-font-size: 20px;");
        return heartLabel;
    }
    
   // (showGameOver ... 님의 요청대로 '승/패' 분기)
   private void showGameOver(boolean isVictory) { 
       if (gameLoop != null) {
           gameLoop.stop();
       }
       inputField.setDisable(true);
       startButton.setDisable(false); 
       
       String finalMessage = String.format("최종 점수: %d", score);
       scoreLabel.setText(finalMessage);
       scoreLabel.setTextFill(Color.YELLOW); 
       
       if (isVictory) {
           showResultDialog("승리! 성을 지켰습니다.", finalMessage);
       } else {
           showResultDialog("패배! 성이 부서졌습니다.", finalMessage);
       }
   }
   
   // [추가] 님의 요청: '게임 종료' 팝업창 띄우기 (줄다리기 게임 참조)
   private void showResultDialog(String header, String content) {
       Alert alert = new Alert(Alert.AlertType.INFORMATION);
       alert.setTitle("결과");
       alert.setHeaderText(header); 
       alert.setContentText(content); 
       alert.initOwner(this);
       alert.show();
   }

    // ----------------------- 7. 단어 로딩 (기존과 동일) -----------------------
    
    private static List<String> loadWordPool() {
        List<String> words = loadWordsFromClasspath();
        if (words.isEmpty()) {
            words = loadWordsFromFilesystem();
        }
        if (words.isEmpty()) {
            words = new ArrayList<>(Arrays.asList(DEFAULT_WORDS));
        }
        System.out.println("성 지키기: " + words.size() + "개의 단어를 로드했습니다.");
        return Collections.unmodifiableList(words);
    }

    private static List<String> loadWordsFromClasspath() {
        try (InputStream in = CastleDefenseGame.class.getClassLoader().getResourceAsStream(WORD_RESOURCE)) {
            if (in == null) return Collections.emptyList();
            try (Reader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return parseWordList(reader);
            }
        } catch (IOException | JsonSyntaxException | JsonIOException e) {
            return Collections.emptyList();
        }
    }

    private static List<String> loadWordsFromFilesystem() {
        Path path = Paths.get("src", "main", "resources").resolve(WORD_RESOURCE);
        if (!Files.exists(path)) return Collections.emptyList();
        
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return parseWordList(reader);
        } catch (IOException | JsonSyntaxException | JsonIOException e) {
            return Collections.emptyList();
        }
    }

    private static List<String> parseWordList(Reader reader) {
        Gson gson = new Gson();
        WordList data = gson.fromJson(reader, WordList.class);
        if (data == null || data.words == null) return Collections.emptyList();
        List<String> words = new ArrayList<>();
        for (String word : data.words) {
            if (word != null && !word.trim().isEmpty()) {
                words.add(word.trim());
            }
        }
        return words;
    }

    private static final class WordList {
        List<String> words;
    }
}