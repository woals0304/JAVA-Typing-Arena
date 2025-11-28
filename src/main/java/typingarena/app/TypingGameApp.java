package typingarena.app;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.effect.BlendMode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import typingarena.minigames.castledefense.CastleDefenseGame;
import typingarena.minigames.landgrab.LandGrabGame;
import typingarena.minigames.tugofwar.TugOfWarGame;
import typingarena.net.NetClient;

import java.io.InputStream;

public class TypingGameApp extends Application {

    private Stage primaryStage;
    private StackPane sceneRoot;
    private BorderPane root;
    private NetClient netClient;
    private String myNickname = "";

    private static final String TEXT_MAIN = "#4E342E";
    private static final String ACCENT = "#29B6F6";
    private static final String FRAME_DARK = "#3E2723";
    private static final String MAIN_BG = "/images/main_menu_background.png";
    private static final String MAIN_ILLUST = "/images/main_menu_illustration.png";

    private String gameFontFamily = "Malgun Gothic";
    private Font titleFont;
    private Font subtitleFont;
    private Font buttonFont;
    private Font overlineFont;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.root = new BorderPane();
        this.sceneRoot = new StackPane(root);
        initFonts();
        root.setPadding(new Insets(16));
        StackPane.setAlignment(root, Pos.CENTER);
        applyMainBackground(sceneRoot);

        Scene scene = new Scene(sceneRoot, 1024, 747);
        primaryStage.setTitle("Typing Mini Game");
        primaryStage.setScene(scene);
        primaryStage.show();
        showLogin();
    }

    private void showLogin() {
        LoginScreen login = new LoginScreen(primaryStage, (client, nickname) -> {
            this.netClient = client;
            this.myNickname = nickname;
            showMainMenu();
        });
        login.showAndWait();
    }

    // === Main Menu (Single / Multi) ===
    private void showMainMenu() {
        Label overline = new Label("ARENA SELECT");
        overline.setFont(overlineFont);
        overline.setStyle("-fx-text-fill: #E7B53B;");

        Label title = new Label("Typing Arena");
        title.setFont(titleFont);
        title.setStyle("-fx-text-fill: linear-gradient(#FFE082, #FFB300); -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 0, 0, 0, 2);");

        Label subtitle = new Label("싱글/멀티 대전을 선택해 빠른 타이핑을 겨뤄보세요.");
        subtitle.setFont(subtitleFont);
        subtitle.setTextFill(Color.web("#6D4C41"));
        subtitle.setWrapText(true);
        subtitle.setAlignment(Pos.CENTER);

        Button singleBtn = createPrimaryButton("⚔ 싱글 플레이", this::showSingleMenu);
        Button multiBtn = createPrimaryButton("🎮 멀티 플레이", this::showMultiMenu);

        Button logoutBtn = createSecondaryButton("로그아웃", this::logout);

        HBox buttons = new HBox(18, singleBtn, multiBtn);
        buttons.setAlignment(Pos.CENTER);

        Label tagline = new Label("타이핑으로 실력을 쌓고 친구들과 겨루세요 🏆");
        tagline.setFont(subtitleFont);
        tagline.setTextFill(Color.web("#5F4A3A"));
        tagline.setAlignment(Pos.CENTER);

        VBox center = new VBox(14, overline, title, subtitle, buttons, logoutBtn, tagline);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(10));
        center.setMaxWidth(600);
        setCenterContent(wrapWithIllustration(center));
        BorderPane.setAlignment(center, Pos.CENTER);
    }

    // === Multi Player Menu ===
    private void showMultiMenu() {
        Label overline = new Label("ONLINE MATCH");
        overline.setFont(overlineFont);
        overline.setStyle("-fx-text-fill: #FFE082;");

        Label title = new Label("멀티 플레이");
        title.setFont(titleFont);
        title.setStyle("-fx-text-fill: linear-gradient(#FFE082, #FFB300); -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 0, 0, 0, 3);");

        Label description = new Label(
                "온라인으로 친구나 다른 플레이어와 대결하세요.\n" +
                        "줄다리기 / 땅따먹기 매칭을 선택하면\n" +
                        "멀티 로비에서 자동 매칭을 시작합니다."
        );
        description.setFont(subtitleFont);
        description.setTextFill(Color.web("#6D4C41"));
        description.setLineSpacing(4);
        description.setAlignment(Pos.CENTER);

        Button openLobbyBtn = createMenuButton("🎮 멀티 로비 열기", this::openMultiLobby);
        Button backBtn = createSecondaryButton("◀ 메인으로", this::showMainMenu);

        VBox buttons = new VBox(12, openLobbyBtn, backBtn);
        buttons.setAlignment(Pos.CENTER);

        VBox center = new VBox(14, overline, title, description, buttons);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(10));
        setCenterContent(buildFramedCard(center, true));
    }

    // === Single Player Menu ===
    private void showSingleMenu() {
        Label overline = new Label("SOLO MODE");
        overline.setFont(overlineFont);
        overline.setStyle("-fx-text-fill: #FFD54F;");

        Label title = new Label("싱글 플레이 미니게임");
        title.setFont(titleFont);
        title.setStyle("-fx-text-fill: linear-gradient(#FFE082, #FFB300); -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 0, 0, 0, 3);");

        Label description = new Label(
                "준비된 미니게임:\n\n" +
                        "1. 줄다리기 타자 대전 (Tug of War)\n" +
                        "2. 성 지키기 (Castle Defense)\n" +
                        "3. 칸 채우기 땅따먹기 (Land Grab)\n\n" +
                        "버튼을 누르면 각 게임이 새로운 창에서 실행됩니다."
        );
        description.setFont(subtitleFont);
        description.setTextFill(Color.web("#6D4C41"));
        description.setLineSpacing(4);
        description.setAlignment(Pos.CENTER);

        Button tugBtn = createMenuButton("⚔ 줄다리기 게임 시작",
                () -> launchStage(new TugOfWarGame()));
        Button castleBtn = createMenuButton("🛡 성 지키기 게임 시작",
                () -> launchStage(new CastleDefenseGame()));
        Button landBtn = createMenuButton("🧭 땅따먹기 게임 시작",
                () -> launchStage(new LandGrabGame()));

        VBox buttons = new VBox(12, tugBtn, castleBtn, landBtn);
        buttons.setAlignment(Pos.CENTER);

        Button backBtn = createSecondaryButton("◀ 메인으로", this::showMainMenu);

        VBox center = new VBox(14, overline, title, description, buttons, backBtn);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(10));
        setCenterContent(buildFramedCard(center, true));
    }

    // === Multi Lobby ===
    private void openMultiLobby() {
        if (netClient == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "로그인 후 이용하세요.");
            alert.initOwner(primaryStage);
            alert.show();
            return;
        }
        MultiLobbyPane pane = new MultiLobbyPane(netClient, myNickname, this::showMainMenu);
        setCenterContent(pane);
    }

    private void logout() {
        try {
            if (netClient != null) {
                netClient.close();
            }
        } catch (Exception ignored) {}
        netClient = null;
        myNickname = "";
        showLogin();
    }

    // === Helpers ===
    private Button createPrimaryButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setMinWidth(240);
        btn.setFont(buttonFont);
        String fam = buttonFont.getFamily();
        int sz = (int) buttonFont.getSize();
        String normal = "-fx-background-color: linear-gradient(#FFCC80, #F59F42); -fx-text-fill: " + TEXT_MAIN + "; -fx-font-weight: bold; -fx-font-family: '" + fam + "'; -fx-font-size: " + sz + "px; -fx-padding: 12 22; -fx-background-radius: 14; -fx-border-color: rgba(0,0,0,0.15); -fx-border-radius: 14; -fx-border-width: 1;";
        String hover = "-fx-background-color: linear-gradient(#FFD59C, #F5A74F); -fx-text-fill: " + TEXT_MAIN + "; -fx-font-weight: bold; -fx-font-family: '" + fam + "'; -fx-font-size: " + sz + "px; -fx-padding: 12 22; -fx-background-radius: 14; -fx-border-color: rgba(0,0,0,0.2); -fx-border-radius: 14; -fx-border-width: 1;";
        btn.setStyle(normal);
        btn.setEffect(new DropShadow(12, Color.web("#00000033")));
        btn.setOnAction(e -> action.run());
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(normal));
        return btn;
    }

    private Button createMenuButton(String text, Runnable action) {
        Button btn = createPrimaryButton(text, action);
        btn.setMinWidth(280);
        return btn;
    }

    private Button createSecondaryButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setFont(buttonFont);
        String fam = buttonFont.getFamily();
        int sz = (int) buttonFont.getSize();
        String base = "-fx-background-color: linear-gradient(#F5F0E6, #E8DCC8); -fx-text-fill: " + TEXT_MAIN + "; -fx-font-weight: bold; -fx-font-family: '" + fam + "'; -fx-font-size: " + sz + "px; -fx-padding: 10 16; -fx-background-radius: 12; -fx-border-color: #D7CCC8; -fx-border-radius: 12; -fx-border-width: 1.2;";
        String hover = "-fx-background-color: linear-gradient(#FFF8E1, #E8DCC8); -fx-text-fill: " + TEXT_MAIN + "; -fx-font-weight: bold; -fx-font-family: '" + fam + "'; -fx-font-size: " + sz + "px; -fx-padding: 10 16; -fx-background-radius: 12; -fx-border-color: #D7CCC8; -fx-border-radius: 12; -fx-border-width: 1.2;";
        btn.setStyle(base);
        btn.setEffect(new DropShadow(10, Color.web("#00000022")));
        btn.setOnAction(e -> action.run());
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private Label createHudIcon(String icon, String startColor, String endColor, String tooltipText) {
        Label badge = new Label(icon);
        badge.setFont(Font.font(gameFontFamily, FontWeight.BOLD, 12));
        badge.setTextFill(Color.WHITE);
        badge.setStyle("-fx-background-color: linear-gradient(" + startColor + ", " + endColor + "); -fx-background-radius: 12; -fx-padding: 6 10; -fx-border-color: rgba(255,255,255,0.35); -fx-border-width: 1; -fx-border-radius: 12;");
        badge.setEffect(new DropShadow(10, Color.web("#00000033")));
        if (tooltipText != null && !tooltipText.isBlank()) {
            badge.setTooltip(new javafx.scene.control.Tooltip(tooltipText));
        }
        return badge;
    }

    private Label createDecoChip(String icon, String startColor, String endColor) {
        Label chip = new Label(icon);
        chip.setFont(Font.font(gameFontFamily, FontWeight.BOLD, 12));
        chip.setTextFill(Color.WHITE);
        chip.setStyle("-fx-background-color: linear-gradient(" + startColor + ", " + endColor + "); -fx-background-radius: 14; -fx-padding: 6 10; -fx-border-color: rgba(255,255,255,0.35); -fx-border-width: 1; -fx-border-radius: 14;");
        chip.setEffect(new DropShadow(12, Color.web("#00000044")));
        chip.setMouseTransparent(true);
        return chip;
    }

    private void launchStage(Stage stage) {
        stage.initOwner(primaryStage);
        stage.show();
    }

    private void setCenterContent(Node node) {
        root.setCenter(node);
    }

    private void applyMainBackground(StackPane target) {
        BackgroundFill fallbackFill = new BackgroundFill(Color.web("#F9E6C8"), CornerRadii.EMPTY, Insets.EMPTY);

        Image bgImage = loadImage(MAIN_BG);

        if (bgImage != null && !bgImage.isError()) {
            BackgroundSize size = new BackgroundSize(100, 100, true, true, false, true);
            BackgroundImage backgroundImage = new BackgroundImage(
                    bgImage,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    size
            );
            target.setBackground(new Background(new BackgroundFill[]{fallbackFill}, new BackgroundImage[]{backgroundImage}));
        } else {
            target.setBackground(new Background(fallbackFill));
        }
    }

    private StackPane wrapWithIllustration(Node content) {
        Image illustration = loadImage(MAIN_ILLUST);
        if (illustration == null || illustration.isError()) {
            return new StackPane(content);
        }

        ImageView iv = new ImageView(illustration);
        iv.setPreserveRatio(true);
        iv.setFitWidth(540);
        iv.setOpacity(0.94);
        iv.setBlendMode(BlendMode.MULTIPLY); // 흰 배경을 카드 배경색과 자연스럽게 섞어 보이게
        iv.setMouseTransparent(true);

        StackPane layer = new StackPane(iv, content);
        StackPane.setAlignment(iv, Pos.TOP_CENTER);
        StackPane.setAlignment(content, Pos.CENTER);
        StackPane.setMargin(content, new Insets(50, 0, 0, 0));
        return layer;
    }

    private Image loadImage(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            return is != null ? new Image(is) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private StackPane buildFramedCard(Node content, boolean withDecorations) {
        StackPane card = new StackPane(content);
        card.setPadding(new Insets(22, 26, 22, 26));
        card.setMaxWidth(760);
        card.setMaxHeight(520);
        card.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, rgba(255,243,224,0.96), rgba(250,220,180,0.96)); -fx-background-radius: 22; -fx-border-color: #5D4037; -fx-border-radius: 22; -fx-border-width: 3; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 18, 0.24, 0, 8);");

        StackPane frame = new StackPane();
        frame.setMaxWidth(820);
        frame.setMaxHeight(580);
        frame.setPadding(new Insets(16));
        frame.setStyle("-fx-background-color: linear-gradient(" + FRAME_DARK + " 0%, #2A1B14 100%); -fx-background-radius: 26; -fx-border-color: #5D4037; -fx-border-width: 2; -fx-border-radius: 26; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 28, 0.35, 0, 14);");
        frame.getChildren().add(card);
        BorderPane.setAlignment(frame, Pos.CENTER);

        return frame;
    }

    private void initFonts() {
        try (InputStream is = getClass().getResourceAsStream("/fonts/CookieRun Regular.otf")) {
            Font loaded = is != null ? Font.loadFont(is, 22) : null;
            if (loaded != null) {
                gameFontFamily = loaded.getFamily();
            }
        } catch (Exception ignored) {}
        titleFont = Font.font(gameFontFamily, FontWeight.EXTRA_BOLD, 30);
        subtitleFont = Font.font(gameFontFamily, FontWeight.NORMAL, 15);
        buttonFont = Font.font(gameFontFamily, FontWeight.BOLD, 16);
        overlineFont = Font.font(gameFontFamily, FontWeight.EXTRA_BOLD, 12);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
