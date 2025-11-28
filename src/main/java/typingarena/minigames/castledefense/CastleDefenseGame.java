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
import javafx.scene.image.Image;
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

    // --- [1] 설정 값 (56px) ---
    protected static final int TILE_SIZE = 56; 
    protected static final int MAP_COLS = 16; 
    protected static final int MAP_ROWS = 9;  
    protected final double GAME_WIDTH = MAP_COLS * TILE_SIZE;   // 896px
    protected final double GAME_HEIGHT = MAP_ROWS * TILE_SIZE;  // 504px
    protected final long GAME_DURATION_SECONDS = 60;

    // --- [2] UI 스타일 상수 ---
    protected static final Color COLOR_P1 = Color.web("#29B6F6");
    protected static final Color COLOR_P2 = Color.web("#EF5350");
    protected static final Color THEME_STROKE = Color.web("#5D4037");
    protected static final Color THEME_UI_BG = Color.web("#FFF8E1");
    protected static final Color THEME_BOTTOM_BG = Color.web("#FFECB3");
    protected static final Color COLOR_TIMER_BG = Color.web("#D7CCC8");
    protected static final Color COLOR_GOLD_START = Color.web("#FFD54F");
    protected static final Color COLOR_GOLD_END = Color.web("#FF6F00");
    protected static final Color COMBO_BG_PURPLE_START = Color.web("#BA68C8");
    protected static final Color COMBO_BG_PURPLE_END = Color.web("#7B1FA2");

    // --- [3] 매니저들 ---
    protected final TileManager tileManager = new TileManager(); 
    protected final ComboManager comboManager = new ComboManager(); 
    protected HeartManager heartManager; 
    protected final CastleDefenseSoundManager soundManager = CastleDefenseSoundManager.getInstance();

    // --- [4] 맵 데이터 ---
    protected final int[][] mapData = {
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
    protected SimpleIntegerProperty castleHp = new SimpleIntegerProperty(3);
    protected Player player1; // 1P (나)
    protected Player player2; // 2P (상대 또는 멀티용)
    
    protected List<Monster> activeMonsters = new ArrayList<>();
    
    protected Image[] normalMonsterSprites; 
    protected Image[] fastMonsterSprites;   

    // --- [6] UI 컴포넌트 ---
    protected Pane entityLayer;
    protected TextField inputField;
    protected Rectangle flashOverlay;
    protected StackPane gameOverOverlay;
    protected StackPane gameStartOverlay;
    
    protected Label lblResultTitle;
    protected Label lblResultScore;
    
    protected Rectangle timerFill;
    protected Label lblTimeText;
    protected Text txtScore;
    protected Text txtHp;
    protected Polygon comboHexagon;
    protected Label lblComboValue;
    protected Label lblComboText;
    protected StackPane comboBadgePane;

    protected String gameFontFamily = "System";
    
    protected boolean isMultiplayer;

    protected boolean isRunning = false;
    protected AnimationTimer gameLoop;
    
    // 몬스터 스폰 타이머
    protected long lastNormalSpawnTime = 0;
    protected long lastFastSpawnTime = 0;
    protected long lastHeartSpawnTime = 0;
    
    protected long gameStartTime = 0;
    protected long damageFlashUntil = 0;
    protected int score = 0;
    
    protected static final List<String> WORD_POOL = loadWordPool();

    public CastleDefenseGame(boolean isMultiplayer) {
        this.isMultiplayer = isMultiplayer; 
        
        loadGameFont();
        
        normalMonsterSprites = Monster.loadAssets("M");
        fastMonsterSprites = Monster.loadAssets("F");

        soundManager.loadSound("sfx_start.wav");
        soundManager.loadSound("sfx_win.wav");
        soundManager.loadSound("sfx_lose.wav");
        soundManager.loadSound("attack.mp3");
        soundManager.loadSound("die.mp3");
        soundManager.loadSound("sfx_fever_start.wav");
        soundManager.loadSound("hp.mp3");

        BorderPane root = new BorderPane();
        root.setTop(buildUnifiedHeader());

        StackPane gameCenter = new StackPane();
        gameCenter.setStyle("-fx-background-color: #222;");
        gameCenter.getChildren().addAll(createGameMap(), createEntityLayer());
        root.setCenter(gameCenter);
        root.setBottom(createBottomBar());

        heartManager = new HeartManager(entityLayer, GAME_WIDTH + 50, GAME_HEIGHT * 0.15);

        castleHp.addListener((obs, o, n) -> txtHp.setText(String.valueOf(n)));

        initGameOverUI();
        initGameStartUI(); 
        
        StackPane mainRoot = new StackPane(root, gameOverOverlay, gameStartOverlay);

        Scene scene = new Scene(mainRoot, GAME_WIDTH, GAME_HEIGHT + 200); 
        this.setTitle("타자 디펜스 (Typing Defense)");
        this.setScene(scene);
        this.setResizable(false);
        
        this.setOnCloseRequest(e -> {
            stopGame();
            soundManager.stopBgm();
        });
    }

    public CastleDefenseGame() {
        this(false); 
    }

    protected void initGameStartUI() {
        gameStartOverlay = new StackPane();
        gameStartOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6);");
        gameStartOverlay.setVisible(true);

        VBox startBox = new VBox(15);
        startBox.setAlignment(Pos.CENTER);
        startBox.setMaxWidth(500);
        startBox.setPadding(new Insets(30));
        startBox.setStyle("-fx-background-color: #FFF8E1; -fx-background-radius: 40; -fx-border-color: #5D4037; -fx-border-width: 6px; -fx-border-radius: 40; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 20, 0, 0, 10);");

        Label lblTitle = new Label("TAJA DEFENSE");
        lblTitle.setFont(getGameFont(48));
        lblTitle.setTextFill(THEME_STROKE);

        String modeText = isMultiplayer ? "멀티 플레이 모드" : "싱글 플레이 모드";
        Label lblDesc = new Label(modeText + "\n단어를 입력하여 몬스터들을 공격하세요!");
        lblDesc.setFont(getGameFont(20));
        lblDesc.setTextFill(Color.web("#8D6E63"));
        lblDesc.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Button btnPlay = new Button("START");
        styleCookieButton(btnPlay, COLOR_P1);
        btnPlay.setPrefSize(200, 60);
        btnPlay.setFont(getGameFont(24));

        btnPlay.setOnAction(e -> startGame());

        startBox.getChildren().addAll(lblTitle, lblDesc, btnPlay);
        gameStartOverlay.getChildren().add(startBox);
    }
    
    protected Pane createEntityLayer() {
        entityLayer = new Pane();
        entityLayer.setPrefSize(GAME_WIDTH, GAME_HEIGHT);
        flashOverlay = new Rectangle(GAME_WIDTH, GAME_HEIGHT, Color.rgb(255, 0, 0, 0.3));
        flashOverlay.setVisible(false);
        flashOverlay.setMouseTransparent(true);
        
        double centerY = GAME_HEIGHT / 2;
        
        if (isMultiplayer) {
            player1 = new Player(60, centerY - 80, "/images/castledefense/Players/1P.png");
            player1.setId("PLAYER");
            
            player2 = new Player(60, centerY + 80, "/images/castledefense/Players/2P.png");
            player2.setId("PLAYER");
            
            entityLayer.getChildren().addAll(player1, player2, flashOverlay);
        } else {
            player1 = new Player(60, centerY, "/images/castledefense/Players/1P.png");
            player1.setId("PLAYER");
            player2 = null; 
            
            entityLayer.getChildren().addAll(player1, flashOverlay);
        }

        return entityLayer;
    }

    protected void startGame() {
        isRunning = true;
        score = 0;
        comboManager.reset();
        heartManager.clear(); 
        castleHp.set(3);
        txtScore.setText("0");
        lblTimeText.setText("60");
        updateComboVisuals(0);
        
        gameOverOverlay.setVisible(false);
        gameStartOverlay.setVisible(false);
        
        inputField.setDisable(false);
        entityLayer.getChildren().removeIf(n -> !"PLAYER".equals(n.getId()) && n != flashOverlay);
        activeMonsters.clear();
        
        inputField.requestFocus();
        
        soundManager.playBgm("RestNPeace.mp3", 0.2);
        soundManager.play("sfx_start.wav", 0.3);
        
        gameStartTime = System.nanoTime();
        
        lastNormalSpawnTime = gameStartTime;
        lastFastSpawnTime = gameStartTime;
        lastHeartSpawnTime = gameStartTime;

        gameLoop = new AnimationTimer() {
            @Override public void handle(long now) { update(now); }
        };
        gameLoop.start();
    }

    protected void stopGame() {
        isRunning = false;
        if (gameLoop != null) gameLoop.stop();
        soundManager.stopBgm();
        inputField.setDisable(true);
    }

    protected void update(long now) {
        if (!isRunning) return;

        long elapsed = (now - gameStartTime) / 1_000_000_000;
        long remaining = GAME_DURATION_SECONDS - elapsed;
        
        if (remaining <= 0) {
            remaining = 0;
            if (activeMonsters.isEmpty()) {
                stopGame(); 
                showGameOver(true); 
                return; 
            }
        }
        
        double ratio = (double) remaining / GAME_DURATION_SECONDS;
        timerFill.setWidth((200 - 4) * ratio);
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

        if (remaining > 0) {
        // [수정] 일반 몬스터: 6초마다 (6_000_000_000L)
        if (now - lastNormalSpawnTime > 6_000_000_000L) {
            spawnNormalMonster();
            lastNormalSpawnTime = now;
        }
        // [수정] 빠른 몬스터: 4초마다 (4_000_000_000L)
        if (now - lastFastSpawnTime > 4_000_000_000L) {
            spawnFastMonster();
            lastFastSpawnTime = now;
        }
        // 하트 아이템: 18초 유지 (변경 원하시면 20_000_000_000L 등으로 수정)
        if (now - lastHeartSpawnTime > 18_000_000_000L) { 
            spawnHeart(); 
            lastHeartSpawnTime = now; 
        }
    }

        moveEntities();

        if (castleHp.get() <= 0) { stopGame(); showGameOver(false); }
    }

    protected void spawnNormalMonster() {
    if (WORD_POOL.isEmpty()) return;
    String word = WORD_POOL.get(new Random().nextInt(WORD_POOL.size()));
    double y = GAME_HEIGHT * 0.2 + new Random().nextDouble() * (GAME_HEIGHT * 0.6); 
    
    // [수정] 속도를 1.5 -> 1.0으로 변경
    Monster m = new Monster(word, normalMonsterSprites, GAME_WIDTH + 50, y, 2, 1.0);
    
    activeMonsters.add(m);
    entityLayer.getChildren().add(m);
    m.toBack();
}

protected void spawnFastMonster() {
    if (WORD_POOL.isEmpty()) return;
    String word = WORD_POOL.get(new Random().nextInt(WORD_POOL.size()));
    double y = GAME_HEIGHT * 0.2 + new Random().nextDouble() * (GAME_HEIGHT * 0.6); 
    
    // [수정] 속도를 3.0 -> 2.0으로 변경
    Monster m = new Monster(word, fastMonsterSprites, GAME_WIDTH + 50, y, 1, 2.0);
    
    activeMonsters.add(m);
    entityLayer.getChildren().add(m);
    m.toBack();
}

    // [추가] 하트 생성 메서드 분리
    protected void spawnHeart() {
        heartManager.spawn();
    }

    protected void moveEntities() {
        Iterator<Monster> it = activeMonsters.iterator();
        while (it.hasNext()) {
            Monster m = it.next();
            m.move(); 
            
            if (m.hasReachedCastle()) {
                if (castleHp.get() > 0) castleHp.set(castleHp.get() - 1);
                damageFlashUntil = System.nanoTime() + 150_000_000L;
                comboManager.reset();
                updateComboVisuals(0);
                soundManager.play("attack.mp3", 0.5);
                entityLayer.getChildren().remove(m);
                it.remove();
            }
        }
        heartManager.update();
    }

    protected void handleInput() {
        if (!isRunning) return;
        String text = inputField.getText().trim();
        inputField.clear();
        inputField.requestFocus();
        if (text.isEmpty()) return;

        if (heartManager.checkInput(text, castleHp)) {
            soundManager.play("hp.mp3", 0.3);
            return;
        }

        for (Monster m : activeMonsters) {
            if (m.getWord().equalsIgnoreCase(text)) {
                comboManager.increase();
                int currentCombo = 0;
                try { currentCombo = Integer.parseInt(lblComboValue.getText()) + 1; } catch(Exception e) { currentCombo = 1; }
                updateComboVisuals(currentCombo);
                
                if (currentCombo == 10) {
                    soundManager.play("sfx_fever_start.wav", 0.6);
                }
                
                int damage = comboManager.isMaxEffect() ? 2 : 1;

                player1.attack(m, entityLayer, comboManager.isMaxEffect(), () -> {
                    boolean isDead = m.takeDamage(damage);
                    if (isDead) { 
                        soundManager.play("die.mp3", 0.5);
                        killMonster(m); 
                    } else { 
                        soundManager.play("attack.mp3", 0.5);
                        if (!WORD_POOL.isEmpty()) {
                            m.setWord(WORD_POOL.get(new Random().nextInt(WORD_POOL.size())));
                        }
                    }
                });
                
                m.setTargeted(true);
                return;
            }
        }
        comboManager.reset();
        updateComboVisuals(0);
    }

    protected void killMonster(Monster m) {
        if (activeMonsters.contains(m)) {
            entityLayer.getChildren().remove(m);
            activeMonsters.remove(m);
            score += 10 * comboManager.getScoreMultiplier();
            txtScore.setText(String.valueOf(score));
        }
    }

    // [추가] 외부(멀티플레이 상대방) 명령으로 몬스터 제거
    protected void removeMonsterByWord(String word) {
        Monster target = null;
        for (Monster m : activeMonsters) {
            if (m.getWord().equals(word)) {
                target = m;
                break;
            }
        }
        
        if (target != null) {
            entityLayer.getChildren().remove(target);
            activeMonsters.remove(target);
            // 상대방이 죽였을 때도 사운드 재생 (선택)
            soundManager.play("die.mp3", 0.5);
        }
    }

    protected GridPane createGameMap() {
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

    protected void initGameOverUI() {
        gameOverOverlay = new StackPane();
        gameOverOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6);");
        gameOverOverlay.setVisible(false);
        VBox gameOverBox = new VBox(20);
        gameOverBox.setAlignment(Pos.CENTER);
        gameOverBox.setMaxWidth(500);
        gameOverBox.setPadding(new Insets(30));
        gameOverBox.setStyle("-fx-background-color: #FFF8E1; -fx-background-radius: 40; -fx-border-color: #5D4037; -fx-border-width: 6px; -fx-border-radius: 40; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 20, 0, 0, 10);");
        lblResultTitle = new Label("GAME OVER");
        lblResultTitle.setFont(getGameFont(48));
        lblResultScore = new Label("최종 점수: 0");
        lblResultScore.setFont(getGameFont(24));
        lblResultScore.setTextFill(THEME_STROKE);
        Button btnRestart = new Button("다시 하기");
        styleCookieButton(btnRestart, COLOR_P1);
        btnRestart.setOnAction(e -> startGame());
        Button btnQuit = new Button("나가기");
        styleCookieButton(btnQuit, COLOR_P2);
        btnQuit.setOnAction(e -> close());
        HBox btnBox = new HBox(20, btnQuit, btnRestart);
        btnBox.setAlignment(Pos.CENTER);
        gameOverBox.getChildren().addAll(lblResultTitle, lblResultScore, btnBox);
        gameOverOverlay.getChildren().add(gameOverBox);
    }

    protected void styleCookieButton(Button btn, Color color) {
        btn.setFont(getGameFont(18));
        String hex = toHex(color);
        btn.setStyle("-fx-background-color: " + hex + "; -fx-text-fill: white; -fx-background-radius: 25; -fx-border-color: #5D4037; -fx-border-width: 2px; -fx-border-radius: 25; -fx-padding: 10 30; -fx-cursor: hand;");
        btn.setEffect(new DropShadow(3, color.darker()));
    }

    protected void showGameOver(boolean isWin) {
        soundManager.stopBgm();
        if (isWin) {
            soundManager.play("sfx_win.wav", 0.3);
            lblResultTitle.setText("VICTORY!");
            lblResultTitle.setTextFill(COLOR_GOLD_START);
        } else {
            soundManager.play("sfx_lose.wav", 0.3);
            lblResultTitle.setText("GAME OVER");
            lblResultTitle.setTextFill(Color.GRAY);
        }
        lblResultScore.setText("최종 점수: " + score);
        gameOverOverlay.setVisible(true);
        gameOverOverlay.toFront();
        inputField.setDisable(true);
    }

    protected void loadGameFont() { try { InputStream is = getClass().getResourceAsStream("/fonts/CookieRun Regular.otf"); if (is != null) { Font font = Font.loadFont(is, 20); gameFontFamily = font.getFamily(); } } catch (Exception e) {} }
    protected Font getGameFont(double size) { return Font.font(gameFontFamily, FontWeight.BOLD, size); }
    protected String toHex(Color c) { return String.format("#%02X%02X%02X", (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255)); }
    protected StackPane createTimerBar() { StackPane c = new StackPane(); Rectangle bg=new Rectangle(200,26); bg.setArcWidth(26); bg.setArcHeight(26); bg.setFill(COLOR_TIMER_BG); timerFill=new Rectangle(196,22); timerFill.setFill(COLOR_P1); StackPane w=new StackPane(timerFill); w.setMaxSize(200,26); w.setAlignment(Pos.CENTER_LEFT); w.setClip(new Rectangle(200,26) {{ setArcWidth(26); setArcHeight(26); }}); StackPane.setMargin(timerFill,new Insets(0,0,0,2)); Rectangle b=new Rectangle(200,26); b.setArcWidth(26); b.setArcHeight(26); b.setFill(Color.TRANSPARENT); b.setStroke(THEME_STROKE); b.setStrokeWidth(2); lblTimeText=new Label("60"); lblTimeText.setFont(getGameFont(16)); lblTimeText.setTextFill(THEME_STROKE); c.getChildren().addAll(bg,w,b,lblTimeText); return c; }
    protected StackPane createScoreBadge(Text t, Text v, Color c) { StackPane p=new StackPane(); Rectangle bg=new Rectangle(110,55); bg.setArcWidth(15); bg.setArcHeight(15); bg.setFill(Color.WHITE); bg.setStroke(c); bg.setStrokeWidth(3); Rectangle tag=new Rectangle(75,18); tag.setArcWidth(9); tag.setArcHeight(9); tag.setFill(c); StackPane np=new StackPane(tag,t); np.setTranslateY(-30); t.setFont(getGameFont(12)); t.setFill(Color.WHITE); v.setFont(getGameFont(32)); v.setFill(c); v.setTranslateY(4); p.getChildren().addAll(bg,v,np); return p; }
    protected void createComboHexagon() { comboBadgePane=new StackPane(); comboHexagon=new Polygon(0,17.5, 30.3,0, 60.6,17.5, 60.6,52.5, 30.3,70, 0,52.5); lblComboValue=new Label("0"); lblComboValue.setFont(getGameFont(28)); lblComboValue.setTextFill(Color.WHITE); lblComboText=new Label("COMBO"); lblComboText.setFont(getGameFont(10)); lblComboText.setTextFill(Color.web("#E1BEE7")); VBox b=new VBox(-1,lblComboValue,lblComboText); b.setAlignment(Pos.CENTER); comboBadgePane.getChildren().addAll(comboHexagon,b); updateComboVisuals(0); }
    protected void updateComboVisuals(int c) { lblComboValue.setText(String.valueOf(c)); boolean f=c>=10; if(f){ comboHexagon.setFill(new LinearGradient(0,0,1,1,true,CycleMethod.NO_CYCLE,new Stop(0,COLOR_GOLD_START),new Stop(1,COLOR_GOLD_END))); lblComboText.setText("FEVER!"); } else { comboHexagon.setFill(new LinearGradient(0,0,1,1,true,CycleMethod.NO_CYCLE,new Stop(0,COMBO_BG_PURPLE_START),new Stop(1,COMBO_BG_PURPLE_END))); lblComboText.setText("COMBO"); } 
        double baseScale = f ? 1.3 : 1.0; comboBadgePane.setScaleX(baseScale); comboBadgePane.setScaleY(baseScale);
        if(c>0){ ScaleTransition st=new ScaleTransition(Duration.millis(100),comboBadgePane); st.setFromX(baseScale); st.setFromY(baseScale); st.setToX(baseScale * 1.1); st.setToY(baseScale * 1.1); st.setAutoReverse(true); st.setCycleCount(2); st.play(); } }
    protected StackPane buildUnifiedHeader() { StackPane h=new StackPane(); h.setPadding(new Insets(8,0,10,0)); h.setAlignment(Pos.CENTER); h.setStyle("-fx-background-color:"+toHex(THEME_UI_BG)); Rectangle bg=new Rectangle(GAME_WIDTH-20,85); bg.setArcWidth(30); bg.setArcHeight(30); bg.setFill(Color.rgb(255,248,225,0.7)); bg.setStroke(Color.rgb(93,64,55,0.2)); bg.setStrokeWidth(2); GridPane g=new GridPane(); g.setAlignment(Pos.CENTER); g.setMaxWidth(GAME_WIDTH-40); g.setHgap(15); g.getColumnConstraints().addAll(new ColumnConstraints(){{setPercentWidth(33);setHalignment(HPos.CENTER);}}, new ColumnConstraints(){{setPercentWidth(34);setHalignment(HPos.CENTER);}}, new ColumnConstraints(){{setPercentWidth(33);setHalignment(HPos.CENTER);}}); VBox t=new VBox(5); t.setAlignment(Pos.CENTER); Label l=new Label("남은 시간"); l.setFont(getGameFont(14)); l.setTextFill(Color.GRAY); t.getChildren().addAll(l,createTimerBar()); HBox s=new HBox(10); s.setAlignment(Pos.CENTER); txtScore=new Text("0"); StackPane p1=createScoreBadge(new Text("SCORE"),txtScore,COLOR_P1); txtHp=new Text("3"); StackPane p2=createScoreBadge(new Text("HP"),txtHp,COLOR_P2); Text vs=new Text("AND"); vs.setFont(Font.font("Impact",30)); vs.setFill(Color.LIGHTGRAY); s.getChildren().addAll(p1,vs,p2); VBox c=new VBox(5); c.setAlignment(Pos.CENTER); createComboHexagon(); c.getChildren().add(comboBadgePane); g.add(t,0,0); g.add(s,1,0); g.add(c,2,0); h.getChildren().addAll(bg,g); return h; }
    protected HBox createBottomBar() { HBox b=new HBox(15); b.setAlignment(Pos.CENTER); b.setPadding(new Insets(15,20,15,20)); b.setStyle("-fx-background-color:"+toHex(THEME_BOTTOM_BG)+"; -fx-background-radius:40 40 0 0; -fx-border-color:"+toHex(THEME_STROKE)+"; -fx-border-width:4px 4px 0 4px; -fx-border-radius:40 40 0 0;"); inputField=new TextField(); inputField.setPromptText("단어 입력..."); inputField.setFont(getGameFont(20)); inputField.setAlignment(Pos.CENTER); inputField.setPrefWidth(400); HBox.setHgrow(inputField,Priority.ALWAYS); inputField.setStyle("-fx-background-radius:30; -fx-border-color:"+toHex(THEME_STROKE)+"; -fx-border-width:3px; -fx-border-radius:30; -fx-text-fill:#3E2723;"); inputField.setOnAction(e->handleInput()); b.getChildren().add(inputField); return b; }
    protected static List<String> loadWordPool() { try(InputStream in=CastleDefenseGame.class.getClassLoader().getResourceAsStream("words/ko.json")){ if(in==null)return Arrays.asList("자바","게임"); try(Reader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){ return new Gson().fromJson(r,WordList.class).words; } }catch(Exception e){ return Arrays.asList("자바","게임"); } }
    protected static final class WordList { List<String> words; }
}