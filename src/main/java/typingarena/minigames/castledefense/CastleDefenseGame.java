package typingarena.minigames.castledefense;

// (JavaFX 및 애니메이션 임포트)
import javafx.animation.AnimationTimer;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
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
    
    private final double GAME_WIDTH = 800;
    private final double GAME_HEIGHT = 500;
    
    private final double CASTLE_WALL_X_BOUNDARY = 80.0;

    private final double SPAWN_Y_MIN = GAME_HEIGHT * 0.25; // 25% ~ 75% 사이
    private final double SPAWN_Y_MAX = GAME_HEIGHT * 0.75;
    
    private final long SPAWN_INTERVAL_NS = 2_000_000_000L;
    private long lastMonsterSpawnTime = 0;
    
    private AnimationTimer gameLoop;
    private long gameStartTime; // 게임 시작 시간 기록

    // 줄다리기 게임의 '번쩍' 효과 로직
    private Rectangle flashOverlay; // 화면 전체를 덮을 빨간 사각형
    private long damageFlashUntil = 0L; // 번쩍 효과가 끝나는 시간

    // ----------------------- 2. 뷰 요소 (UI) -----------------------
    private Pane gamePane = new Pane();
    
    // UI 요소들
    private Label timerLabel = new Label("00:00"); // 상단 중앙
    private Label scoreLabel = new Label("Score: 0"); // 우측 상단
    private HBox heartsBox = new HBox(5); // 우측 상단 (하트 담을 곳)
    
    private TextField inputField = new TextField();
    
    private Rectangle player; 
    private int score = 0;
    
    public CastleDefenseGame() {
        initModality(Modality.NONE);
        
        BorderPane root = new BorderPane();

        // 1. 상단 UI (시간)
        HBox topLeftUI = new HBox(timerLabel);
        topLeftUI.setAlignment(Pos.CENTER);
        topLeftUI.setPadding(new Insets(5));
        timerLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: white; -fx-font-weight: bold;");

        // 2. 상단 UI (점수, 하트)
        HBox topRightUI = new HBox(10, scoreLabel, heartsBox);
        topRightUI.setAlignment(Pos.CENTER_RIGHT);
        topRightUI.setPadding(new Insets(5));
        scoreLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: white; -fx-font-weight: bold;");
        updateHeartsUI(); // 초기 하트 3개 그리기

        // 3. BorderPane을 사용해 상단 UI를 양쪽으로 배치
        BorderPane topUIBar = new BorderPane();
        topUIBar.setLeft(topLeftUI); // 시간(좌측 상단처럼 보이지만 중앙 정렬됨)
        topUIBar.setCenter(new Label("")); // 빈 공간
        topUIBar.setRight(topRightUI); // 점수, 하트 (우측 상단)
        topUIBar.setStyle("-fx-background-color: #1bbc4eff;"); // 상단 바 배경색
        
        root.setTop(topUIBar); // BorderPane 상단에 배치
        
        gamePane.setPrefSize(GAME_WIDTH, GAME_HEIGHT);
        gamePane.setStyle("-fx-background-color: #5e8b5fff;");
        
        flashOverlay = new Rectangle(GAME_WIDTH, GAME_HEIGHT);
        flashOverlay.setFill(Color.rgb(220, 80, 80, 0.4)); // 반투명 빨간색 (0.4 = 40%)
        flashOverlay.setVisible(false); // 처음엔 숨김

        // 성 벽 그리기
        drawCastleWalls();

        // 6. 플레이어(초록 네모) 위치 (성 벽 안)
        player = new Rectangle(40, 40,  Color.web("#62b2e8ff"));
        player.setStroke(Color.BLACK);
        player.setTranslateX(CASTLE_WALL_X_BOUNDARY - 50); // 성 벽 안쪽(X=30)
        player.setTranslateY(GAME_HEIGHT / 2 - 20); // 화면 중앙
        gamePane.getChildren().add(player);

        gamePane.getChildren().add(flashOverlay);
        
        root.setCenter(gamePane); // BorderPane 중앙에 배치

        // 7. 하단 입력 바
        inputField.setOnAction(e -> handleUserInput(inputField.getText()));
        inputField.setPromptText("여기에 단어를 입력하세요...");
        
        inputField.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;"); 
        
        inputField.setPrefHeight(50); // 높이 50으로 키움
        inputField.setMaxWidth(GAME_WIDTH * 0.2); // 최대 너비를 게임 화면의 60%로 제한
        inputField.setMinWidth(GAME_WIDTH * 0.2); //  최소 너비도 동일하게 설정

        // 입력창을 담을 HBox 생성 (하단 중앙 정렬용)
        HBox bottomBox = new HBox(inputField);
        bottomBox.setAlignment(Pos.CENTER); // HBox 내부에서 inputField를 중앙 정렬
        bottomBox.setPadding(new Insets(10, 20, 10, 20)); // 상하좌우 여백
        bottomBox.setStyle("-fx-background-color: #1bbc4eff;"); // 상단 바와 동일한 배경색

        root.setBottom(bottomBox); // BorderPane 하단에 배치

        castle.hpProperty().addListener((obs, oldVal, newVal) -> updateHeartsUI());
        
        startGameLoop();

        setTitle("🏰 성 방어 타자 게임");
        setScene(new Scene(root, GAME_WIDTH, GAME_HEIGHT + 36 + 70)); // 상단 UI바, 하단 UI바 높이 반영
        
        setOnCloseRequest(e -> {
            if (gameLoop != null) {
                gameLoop.stop();
            }
        });
    }

    // 성 벽을 그리는 메서드
    private void drawCastleWalls() {
        Color wallColor = Color.web("#3498db"); // 파란색
        double wallThickness = 20;

        // 성 벽 그리기 (간략화된 버전)
        // 오른쪽 기둥 (판정선)
        Rectangle rightPillar = new Rectangle(CASTLE_WALL_X_BOUNDARY - wallThickness, 0, wallThickness, GAME_HEIGHT);
        rightPillar.setFill(wallColor);
        
        // 위쪽 지붕
        Rectangle topRoof = new Rectangle(wallThickness, 0, CASTLE_WALL_X_BOUNDARY - wallThickness, wallThickness);
        topRoof.setFill(wallColor);
        // 아래쪽 바닥
        Rectangle bottomRoof = new Rectangle(wallThickness, GAME_HEIGHT - wallThickness, CASTLE_WALL_X_BOUNDARY - wallThickness, wallThickness);
        bottomRoof.setFill(wallColor);

        gamePane.getChildren().addAll( rightPillar, topRoof, bottomRoof);
    }
    
    // ----------------------- 3. 게임 루프 (핵심) -----------------------
    private void startGameLoop() {
        gameStartTime = System.nanoTime(); // 게임 시작 시간 기록
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (castle.isDestroyed()) {
                    stop();
                    showGameOver();
                    return;
                }
                
                updateTimer(now); 
                // 하트가 닳았을 때 '번쩍' 효과 처리 
                if (damageFlashUntil > 0L) {
                    if (now < damageFlashUntil) {
                        flashOverlay.setVisible(true); // 1. 보여주기
                    } else {
                        flashOverlay.setVisible(false); // 2. 숨기기
                        damageFlashUntil = 0L; // 3. 타이머 리셋
                    }
                }

                if (now - lastMonsterSpawnTime > SPAWN_INTERVAL_NS) {
                    spawnMonster();
                    lastMonsterSpawnTime = now;
                }
                moveAndCheckMonsters();
            }
        };
        gameLoop.start();
    }
    
    // 타이머 업데이트 메서드 (상단 중앙)
    private void updateTimer(long now) {
        long elapsedSeconds = (now - gameStartTime) / 1_000_000_000;
        long minutes = elapsedSeconds / 60;
        long seconds = elapsedSeconds % 60;
        timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
    }
    
    // ----------------------- 4. 몬스터 관리 로직 -----------------------
    
    private void spawnMonster() {
        Random random = new Random();
        
        if (WORD_POOL.isEmpty()) return;
        String keyword = WORD_POOL.get(random.nextInt(WORD_POOL.size()));
        
        double startX = GAME_WIDTH - 50;

        // 스폰 영역 내에서만 스폰
        double spawnHeightRange = SPAWN_Y_MAX - SPAWN_Y_MIN;
        double startY = SPAWN_Y_MIN + random.nextDouble() * spawnHeightRange;

        Monster monster = new Monster(keyword, startX, startY);
        activeMonsters.add(monster);
        gamePane.getChildren().add(monster);

        // 몬스터가 다른 요소(플레이어, 벽) 뒤에 그려지도록 맨 뒤로 보냄
        monster.toBack();
    }
    
    private void moveAndCheckMonsters() {
        List<Monster> monstersToRemove = new ArrayList<>();
        
        for (Monster monster : activeMonsters) {
            if (monster.isAlive()) {
                monster.move();

                // 몬스터가 성 벽(CASTLE_WALL_X_BOUNDARY)에 닿았을 때 HP 감소 판정
                if (monster.getTranslateX() <= CASTLE_WALL_X_BOUNDARY) { 
                    castle.takeDamage();

                    // '번쩍' 효과 타이머 시작 (0.15초)
                    damageFlashUntil = System.nanoTime() + 150_000_000L;

                    System.out.println("⚠️ 성이 공격 받았습니다! 남은 HP: " + castle.getHp());
                    monstersToRemove.add(monster);
                }
            } else {
                monstersToRemove.add(monster);
            }
        }
        removeMonsters(monstersToRemove);
    }
    
    private void removeMonsters(List<Monster> monsters) {
        gamePane.getChildren().removeAll(monsters);
        activeMonsters.removeAll(monsters);
    }

    // ----------------------- 5. 사용자 입력 및 처치 로직 -----------------------
    
    private void handleUserInput(String input) {
        String trimmedInput = input.trim();
        if (trimmedInput.isEmpty()) return;
        
        Monster matchedMonster = activeMonsters.stream()
            .filter(monster -> monster.isAlive() && !monster.isTargeted() && monster.getKeyword().equals(trimmedInput))
            .findFirst()
            .orElse(null);

        if (matchedMonster != null) {
            matchedMonster.setTargeted(true);
            launchProjectile(matchedMonster);
            
        } else {
            System.out.println("❌ 오타 또는 대상이 없습니다.");
        }
        
        inputField.clear();
    }
    
    private void launchProjectile(Monster targetMonster) {
        Circle projectile = new Circle(5, Color.CYAN);
        projectile.setTranslateX(player.getTranslateX() + player.getWidth() / 2);
        projectile.setTranslateY(player.getTranslateY() + player.getHeight() / 2);
        
        gamePane.getChildren().add(projectile);

        TranslateTransition tt = new TranslateTransition(Duration.millis(400), projectile);
        
        tt.setToX(targetMonster.getTranslateX() + targetMonster.getWidth() / 2);
        tt.setToY(targetMonster.getTranslateY() + targetMonster.getHeight() / 2);

        tt.setOnFinished(e -> {
            gamePane.getChildren().remove(projectile);
            
            targetMonster.kill();
            targetMonster.setStyle("-fx-background-color: green; -fx-opacity: 0.5;");
            
            score += 10;
            updateScoreLabel(); // 스코어만 갱신
            
            System.out.println("✅ 명중! '" + targetMonster.getKeyword() + "' 몬스터 처치!");
        });

        tt.play();
    }
    
    // ----------------------- 6. UI 및 게임 종료 -----------------------

    // 스코어만 갱신하는 메서드 (우측 상단)
    private void updateScoreLabel() {
        scoreLabel.setText(String.format("Score: %d", score));
    }

    // 하트 UI를 그리는 메서드 (우측 상단)
    private void updateHeartsUI() {
        heartsBox.getChildren().clear(); // 기존 하트 모두 제거
        for (int i = 0; i < castle.getHp(); i++) {
            heartsBox.getChildren().add(createHeartIcon());
        }
    }

    // 하트 아이콘 생성 메서드 (간단한 '❤️' 이모지 사용)
    private Label createHeartIcon() {
        Label heartLabel = new Label("❤️");
        heartLabel.setStyle("-fx-font-size: 20px;");
        return heartLabel;
    }
    
    private void showGameOver() {
        if (gameLoop != null) {
            gameLoop.stop();
        }
        inputField.setDisable(true);
        // 게임 오버 시 상단 UI에 최종 점수 표시
        scoreLabel.setText(String.format("최종 점수: %d", score));
        scoreLabel.setTextFill(Color.YELLOW); // 노란색으로 강조
        System.out.println("게임 오버! 최종 점수: " + score);
    }

    // ----------------------- 7. 단어 로딩-----------------------
    
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