package typingarena.minigames.castledefense;

import javafx.animation.AnimationTimer;
import javafx.animation.ScaleTransition;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.google.gson.Gson;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static typingarena.minigames.castledefense.TileManager.*;

public class CastleDefenseGame extends Stage {

    // --- [1] 설정 값 ---
    private static final int TILE_SIZE = 56; 
    private static final int MAP_COLS = 16; 
    private static final int MAP_ROWS = 9;  
    private final double GAME_WIDTH = MAP_COLS * TILE_SIZE;   // 896px
    private final double GAME_HEIGHT = MAP_ROWS * TILE_SIZE;  // 504px
    private final long GAME_DURATION_SECONDS = 60;

    // --- [2] UI 스타일 상수 ---
    private static final Color COLOR_P1 = Color.web("#29B6F6");
    private static final Color COLOR_P2 = Color.web("#EF5350");
    private static final Color THEME_STROKE = Color.web("#5D4037");
    private static final Color THEME_UI_BG = Color.web("#FFF8E1");
    private static final Color THEME_BOTTOM_BG = Color.web("#FFECB3");
    private static final Color COLOR_TIMER_BG = Color.web("#D7CCC8");
    private static final Color COLOR_GOLD_START = Color.web("#FFD54F");
    private static final Color COLOR_GOLD_END = Color.web("#FF6F00");
    private static final Color COMBO_BG_PURPLE_START = Color.web("#BA68C8");
    private static final Color COMBO_BG_PURPLE_END = Color.web("#7B1FA2");

    // --- [3] 매니저들 ---
    private final TileManager tileManager = new TileManager(); 
    private final ComboManager comboManager = new ComboManager(); 
    private HeartManager heartManager; 

    // --- [4] 맵 데이터 ---
    private final int[][] mapData = {
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

    // --- [5] 게임 객체 ---
    private SimpleIntegerProperty castleHp = new SimpleIntegerProperty(3);
    private Player player;
    private List<Monster> activeMonsters = new ArrayList<>();
    
    // --- [6] UI 컴포넌트 ---
    private Pane entityLayer;
    private TextField inputField;
    private Button startButton;
    private Rectangle flashOverlay;
    
    private Rectangle timerFill;
    private Label lblTimeText;
    private Text txtScore;
    private Text txtHp;
    private Polygon comboHexagon;
    private Label lblComboValue;
    private Label lblComboText;
    private StackPane comboBadgePane;

    private String gameFontFamily = "System";

    private boolean isRunning = false;
    private AnimationTimer gameLoop;
    private long lastMonsterSpawnTime = 0, lastHeartSpawnTime = 0, gameStartTime = 0, damageFlashUntil = 0;
    private int score = 0;
    
    private static final List<String> WORD_POOL = loadWordPool();

    public CastleDefenseGame() {
        loadGameFont();

        startButton = new Button("게임 시작");
        startButton.setOnAction(e -> startGame());

        BorderPane root = new BorderPane();
        root.setTop(buildUnifiedHeader());

        StackPane gameCenter = new StackPane();
        gameCenter.setStyle("-fx-background-color: #222;");
        gameCenter.getChildren().addAll(createGameMap(), createEntityLayer());
        root.setCenter(gameCenter);
        root.setBottom(createBottomBar());

        heartManager = new HeartManager(entityLayer, GAME_WIDTH + 50, GAME_HEIGHT * 0.15);

        castleHp.addListener((obs, o, n) -> txtHp.setText(String.valueOf(n)));

        Scene scene = new Scene(root, GAME_WIDTH, GAME_HEIGHT + 180); 
        this.setTitle("Castle Defense");
        this.setScene(scene);
        this.setOnCloseRequest(e -> stopGame());
    }

    private void loadGameFont() {
        try {
            InputStream is = getClass().getResourceAsStream("/fonts/CookieRun Regular.otf");
            if (is != null) {
                Font font = Font.loadFont(is, 20); 
                gameFontFamily = font.getFamily();
            }
        } catch (Exception e) {
            System.err.println("폰트 로드 실패: " + e.getMessage());
        }
    }

    private Font getGameFont(double size) {
        return Font.font(gameFontFamily, FontWeight.BOLD, size);
    }

    private StackPane buildUnifiedHeader() {
        StackPane headerContainer = new StackPane(); 
        headerContainer.setPadding(new Insets(8, 0, 10, 0)); 
        headerContainer.setAlignment(Pos.CENTER);
        headerContainer.setStyle("-fx-background-color: " + toHex(THEME_UI_BG) + ";");

        Rectangle bg = new Rectangle(GAME_WIDTH - 20, 85); 
        bg.setArcWidth(30); bg.setArcHeight(30); 
        bg.setFill(Color.rgb(255, 248, 225, 0.7)); 
        bg.setStroke(Color.rgb(93, 64, 55, 0.2)); 
        bg.setStrokeWidth(2);

        GridPane grid = new GridPane(); 
        grid.setAlignment(Pos.CENTER); 
        grid.setMaxWidth(GAME_WIDTH - 40); 
        grid.setHgap(15); 

        ColumnConstraints col1 = new ColumnConstraints(); col1.setPercentWidth(33); col1.setHalignment(HPos.CENTER);
        ColumnConstraints col2 = new ColumnConstraints(); col2.setPercentWidth(34); col2.setHalignment(HPos.CENTER);
        ColumnConstraints col3 = new ColumnConstraints(); col3.setPercentWidth(33); col3.setHalignment(HPos.CENTER);
        grid.getColumnConstraints().addAll(col1, col2, col3);

        VBox timeBox = new VBox(5); 
        timeBox.setAlignment(Pos.CENTER);
        Label lblTimeTitle = new Label("남은 시간"); 
        lblTimeTitle.setFont(getGameFont(14)); 
        lblTimeTitle.setTextFill(Color.GRAY);
        timeBox.getChildren().addAll(lblTimeTitle, createTimerBar());

        HBox scoreBox = new HBox(10); 
        scoreBox.setAlignment(Pos.CENTER);
        txtScore = new Text("0");
        StackPane p1 = createScoreBadge(new Text("SCORE"), txtScore, COLOR_P1);
        txtHp = new Text("3");
        StackPane p2 = createScoreBadge(new Text("HP"), txtHp, COLOR_P2);
        Text txtVs = new Text("VS"); 
        txtVs.setFont(Font.font("Impact", 30)); 
        txtVs.setFill(Color.LIGHTGRAY); 
        txtVs.setEffect(new DropShadow(2, Color.WHITE));
        scoreBox.getChildren().addAll(p1, txtVs, p2);

        VBox comBox = new VBox(5);
        comBox.setAlignment(Pos.CENTER);
        createComboHexagon();
        comBox.getChildren().add(comboBadgePane);

        grid.add(timeBox, 0, 0); 
        grid.add(scoreBox, 1, 0); 
        grid.add(comBox, 2, 0);

        headerContainer.getChildren().addAll(bg, grid); 
        return headerContainer;
    }

    private HBox createBottomBar() {
        HBox box = new HBox(15);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(15, 20, 15, 20));
        box.setStyle(
            "-fx-background-color: " + toHex(THEME_BOTTOM_BG) + ";" +
            "-fx-background-radius: 40 40 0 0;" +
            "-fx-border-color: " + toHex(THEME_STROKE) + ";" +
            "-fx-border-width: 4px 4px 0 4px;" +
            "-fx-border-radius: 40 40 0 0;"
        );
        
        inputField = new TextField();
        inputField.setPromptText("단어 입력...");
        inputField.setFont(getGameFont(20));
        inputField.setAlignment(Pos.CENTER);
        inputField.setPrefWidth(400); 
        HBox.setHgrow(inputField, Priority.ALWAYS); 

        inputField.setStyle(
            "-fx-background-radius: 30;" +
            "-fx-background-color: white;" +
            "-fx-border-color: " + toHex(THEME_STROKE) + ";" +
            "-fx-border-width: 3px;" +
            "-fx-border-radius: 30;" +
            "-fx-text-fill: #3E2723;" +
            "-fx-prompt-text-fill: gray;"
        );
        inputField.setOnAction(e -> handleInput());

        startButton.setFont(getGameFont(18));
        startButton.setStyle(
            "-fx-background-color: " + toHex(COLOR_P1) + ";" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 25;" +
            "-fx-border-color: " + toHex(THEME_STROKE) + ";" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 25;" +
            "-fx-padding: 8 25;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 3, 0, 0, 2);"
        );
        
        box.getChildren().addAll(inputField, startButton);
        return box;
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X", 
            (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
    }

    private StackPane createTimerBar() {
        StackPane container = new StackPane();
        double w = 200, h = 26, stroke = 2;

        Rectangle bg = new Rectangle(w, h);
        bg.setArcWidth(h); bg.setArcHeight(h);
        bg.setFill(COLOR_TIMER_BG);

        timerFill = new Rectangle(w - stroke*2, h - stroke*2);
        timerFill.setArcWidth(0); timerFill.setArcHeight(0);
        timerFill.setFill(COLOR_P1);

        Rectangle clip = new Rectangle(w, h);
        clip.setArcWidth(h); clip.setArcHeight(h);

        StackPane fillWrapper = new StackPane(timerFill);
        fillWrapper.setMaxSize(w, h);
        fillWrapper.setAlignment(Pos.CENTER_LEFT);
        fillWrapper.setClip(clip);
        StackPane.setMargin(timerFill, new Insets(0, 0, 0, stroke));

        Rectangle border = new Rectangle(w, h);
        border.setArcWidth(h); border.setArcHeight(h);
        border.setFill(Color.TRANSPARENT);
        border.setStroke(THEME_STROKE);
        border.setStrokeWidth(stroke);
        border.setStrokeType(StrokeType.INSIDE);

        lblTimeText = new Label("60");
        lblTimeText.setFont(getGameFont(16));
        lblTimeText.setTextFill(THEME_STROKE);

        container.getChildren().addAll(bg, fillWrapper, border, lblTimeText);
        return container;
    }

    private StackPane createScoreBadge(Text title, Text value, Color color) {
        StackPane p = new StackPane(); 
        p.setPrefSize(110, 55); 
        
        Rectangle bg = new Rectangle(110, 55); 
        bg.setArcWidth(15); bg.setArcHeight(15); 
        bg.setFill(Color.WHITE); 
        bg.setStroke(color); 
        bg.setStrokeWidth(3); 
        bg.setEffect(new DropShadow(2, Color.rgb(0,0,0,0.1)));
        
        Rectangle tag = new Rectangle(75, 18); 
        tag.setArcWidth(9); tag.setArcHeight(9); 
        tag.setFill(color);
        
        StackPane namePane = new StackPane(tag, title); 
        namePane.setTranslateY(-30); 
        
        title.setFont(getGameFont(12)); 
        title.setFill(Color.WHITE);
        
        value.setFont(getGameFont(32)); 
        value.setFill(color); 
        value.setTranslateY(4);
        
        p.getChildren().addAll(bg, value, namePane); 
        return p;
    }

    private void createComboHexagon() {
        comboBadgePane = new StackPane();
        comboHexagon = new Polygon();
        double size = 38.0; 
        comboHexagon.getPoints().addAll(
            0.0, size/2,
            size*0.866, 0.0,
            size*1.732, size/2,
            size*1.732, size*1.5,
            size*0.866, size*2.0,
            0.0, size*1.5
        );
        
        lblComboValue = new Label("0");
        lblComboValue.setFont(getGameFont(28)); 
        lblComboValue.setTextFill(Color.WHITE);
        
        lblComboText = new Label("COMBO");
        lblComboText.setFont(getGameFont(10)); 
        lblComboText.setTextFill(Color.web("#E1BEE7"));
        
        VBox box = new VBox(-1, lblComboValue, lblComboText);
        box.setAlignment(Pos.CENTER);
        
        comboBadgePane.getChildren().addAll(comboHexagon, box);
        updateComboVisuals(0);
    }

    private void updateComboVisuals(int combo) {
        boolean isFever = (combo >= 10);
        lblComboValue.setText(String.valueOf(combo));
        
        if (isFever) {
            comboHexagon.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, new Stop(0, COLOR_GOLD_START), new Stop(1, COLOR_GOLD_END)));
            lblComboText.setText("FEVER!");
            lblComboText.setTextFill(Color.WHITE);
            comboHexagon.setEffect(new Glow(0.7));
        } else {
            comboHexagon.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, new Stop(0, COMBO_BG_PURPLE_START), new Stop(1, COMBO_BG_PURPLE_END)));
            lblComboText.setText("COMBO");
            lblComboText.setTextFill(Color.web("#E1BEE7"));
            comboHexagon.setEffect(new DropShadow(5, Color.rgb(0,0,0,0.2)));
        }
        
        if (combo > 0) {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), comboBadgePane);
            st.setFromX(1.0); st.setFromY(1.0); 
            st.setToX(1.1); st.setToY(1.1); 
            st.setAutoReverse(true); st.setCycleCount(2); 
            st.play();
        }
    }

    private Pane createEntityLayer() {
        entityLayer = new Pane();
        entityLayer.setPrefSize(GAME_WIDTH, GAME_HEIGHT);
        flashOverlay = new Rectangle(GAME_WIDTH, GAME_HEIGHT, Color.rgb(255, 0, 0, 0.3));
        flashOverlay.setVisible(false);
        flashOverlay.setMouseTransparent(true);
        
        // [수정] 플레이어 위치를 120 -> 60으로 (울타리 안쪽)
        player = new Player(60, GAME_HEIGHT / 2); 
        entityLayer.getChildren().addAll(player, flashOverlay);
        return entityLayer;
    }

    private void startGame() {
        isRunning = true;
        score = 0;
        comboManager.reset();
        heartManager.clear(); 
        castleHp.set(3);
        txtScore.setText("0");
        lblTimeText.setText("60");
        updateComboVisuals(0);
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
        long remaining = GAME_DURATION_SECONDS - elapsed;
        
        if (remaining <= 0) { stopGame(); showGameOver(true); return; }
        
        double ratio = (double) remaining / GAME_DURATION_SECONDS;
        double maxW = 200 - 4; 
        timerFill.setWidth(maxW * ratio);
        lblTimeText.setText(String.valueOf(remaining));
        
        if (remaining <= 10) {
            timerFill.setFill(COLOR_P2);
            lblTimeText.setTextFill(Color.RED);
        } else {
            timerFill.setFill(COLOR_P1);
            lblTimeText.setTextFill(THEME_STROKE);
        }

        if (damageFlashUntil > 0) {
            flashOverlay.setVisible(now < damageFlashUntil);
            if (now >= damageFlashUntil) damageFlashUntil = 0;
        }

        if (now - lastMonsterSpawnTime > 2_000_000_000L) { spawnMonster(); lastMonsterSpawnTime = now; }
        if (now - lastHeartSpawnTime > 15_000_000_000L) { 
            heartManager.spawn(); 
            lastHeartSpawnTime = now; 
        }

        moveEntities();

        if (castleHp.get() <= 0) { stopGame(); showGameOver(false); }
    }

    private void spawnMonster() {
        if (WORD_POOL.isEmpty()) return;
        String word = WORD_POOL.get(new Random().nextInt(WORD_POOL.size()));
        double y = GAME_HEIGHT * 0.2 + new Random().nextDouble() * (GAME_HEIGHT * 0.6); 
        Monster m = new Monster(word, Monster.loadAssets(), GAME_WIDTH + 50, y);
        activeMonsters.add(m);
        entityLayer.getChildren().add(m);
        m.toBack();
    }

    private void moveEntities() {
        Iterator<Monster> it = activeMonsters.iterator();
        while (it.hasNext()) {
            Monster m = it.next();
            m.move(2.0); 
            if (m.hasReachedCastle()) {
                if (castleHp.get() > 0) castleHp.set(castleHp.get() - 1);
                damageFlashUntil = System.nanoTime() + 150_000_000L;
                comboManager.reset();
                updateComboVisuals(0);
                entityLayer.getChildren().remove(m);
                it.remove();
            }
        }
        heartManager.update();
    }

    private void handleInput() {
        if (!isRunning) return;
        String text = inputField.getText().trim();
        inputField.clear();
        inputField.requestFocus();
        if (text.isEmpty()) return;

        if (heartManager.checkInput(text, castleHp)) return;

        for (Monster m : activeMonsters) {
            if (m.getWord().equalsIgnoreCase(text)) {
                comboManager.increase();
                int currentCombo = 0;
                try { currentCombo = Integer.parseInt(lblComboValue.getText()) + 1; } catch(Exception e) { currentCombo = 1; }
                updateComboVisuals(currentCombo);
                player.attack(m, entityLayer, comboManager.isMaxEffect(), () -> killMonster(m));
                m.setTargeted(true);
                return;
            }
        }
        comboManager.reset();
        updateComboVisuals(0);
    }

    private void killMonster(Monster m) {
        if (activeMonsters.contains(m)) {
            entityLayer.getChildren().remove(m);
            activeMonsters.remove(m);
            score += 10 * comboManager.getScoreMultiplier();
            txtScore.setText(String.valueOf(score));
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