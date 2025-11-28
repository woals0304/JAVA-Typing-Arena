package typingarena.app;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import typingarena.minigames.castledefense.CastleDefenseGame;
import typingarena.minigames.landgrab.LandGrabGame;
import typingarena.minigames.tugofwar.TugOfWarGame;
import typingarena.net.NetClient;

public class TypingGameApp extends Application {

    private Stage primaryStage;
    private BorderPane root;
    private NetClient netClient;
    private String myNickname = "";

    private static final String BG_BEIGE = "#FDF5E6";

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG_BEIGE + ";");
        root.setPadding(new Insets(36));

        Scene scene = new Scene(root, 760, 560);
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
        applyPlainBackground();
        Label title = new Label("Typing Arena");
        title.setFont(Font.font("Malgun Gothic", FontWeight.EXTRA_BOLD, 34));
        title.setTextFill(Color.web("#2D2A32"));

        Label subtitle = new Label("싱글/멀티 대전을 선택해 빠른 타이핑을 겨뤄보세요.");
        subtitle.setFont(Font.font("Malgun Gothic", FontWeight.NORMAL, 15));
        subtitle.setTextFill(Color.web("#4E342E"));
        subtitle.setWrapText(true);
        subtitle.setAlignment(Pos.CENTER);

        Button singleBtn = createPrimaryButton("싱글 플레이", this::showSingleMenu);
        Button multiBtn = createPrimaryButton("멀티 플레이", this::showMultiMenu);

        Button logoutBtn = createSecondaryButton("로그아웃", this::logout);

        HBox buttons = new HBox(16, singleBtn, multiBtn);
        buttons.setAlignment(Pos.CENTER);

        Label tagline = new Label("타이핑으로 실력을 쌓고 친구들과 겨루세요 🏆");
        tagline.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 13));
        tagline.setTextFill(Color.web("#3E2723"));
        tagline.setAlignment(Pos.CENTER);

        VBox center = new VBox(18, title, subtitle, buttons, logoutBtn, tagline);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(10));
        VBox card = new VBox(center);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-background-radius: 18; -fx-border-color: rgba(0,0,0,0.08); -fx-border-radius: 18; -fx-border-width: 2; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 20, 0.2, 0, 8);");
        card.setPadding(new Insets(24));
        setCenterContent(card);
    }

    // === Multi Player Menu ===
    private void showMultiMenu() {
        applyPlainBackground();
        Label title = new Label("멀티 플레이");
        title.setFont(Font.font("Malgun Gothic", FontWeight.EXTRA_BOLD, 28));
        title.setTextFill(Color.web("#4E342E"));

        Label description = new Label(
                "온라인으로 친구나 다른 플레이어와 대결하세요.\n" +
                        "줄다리기 / 땅따먹기 매칭을 선택하면\n" +
                        "멀티 로비에서 자동 매칭을 시작합니다."
        );
        description.setFont(Font.font("Malgun Gothic", 16));
        description.setTextFill(Color.web("#6D4C41"));
        description.setLineSpacing(4);
        description.setAlignment(Pos.CENTER);

        Button openLobbyBtn = createMenuButton("멀티 로비 열기", this::openMultiLobby);
        Button backBtn = createSecondaryButton("◀ 메인으로", this::showMainMenu);

        VBox buttons = new VBox(12, openLobbyBtn, backBtn);
        buttons.setAlignment(Pos.CENTER);

        VBox center = new VBox(25, title, description, buttons);
        center.setAlignment(Pos.CENTER);
        setCenterContent(center);
    }

    // === Single Player Menu ===
    private void showSingleMenu() {
        applyPlainBackground();
        Label title = new Label("싱글 플레이 미니게임");
        title.setFont(Font.font("Malgun Gothic", FontWeight.EXTRA_BOLD, 28));
        title.setTextFill(Color.web("#4E342E"));

        Label description = new Label(
                "준비된 미니게임:\n\n" +
                        "1. 줄다리기 타자 대전 (Tug of War)\n" +
                        "2. 성 지키기 (Castle Defense)\n" +
                        "3. 칸 채우기 땅따먹기 (Land Grab)\n\n" +
                        "버튼을 누르면 각 게임이 새로운 창에서 실행됩니다."
        );
        description.setFont(Font.font("Malgun Gothic", 16));
        description.setTextFill(Color.web("#6D4C41"));
        description.setLineSpacing(4);
        description.setAlignment(Pos.CENTER);

        Button tugBtn = createMenuButton("줄다리기 게임 시작",
                () -> launchStage(new TugOfWarGame()));
        Button castleBtn = createMenuButton("성 지키기 게임 시작",
                () -> launchStage(new CastleDefenseGame()));
        Button landBtn = createMenuButton("땅따먹기 게임 시작",
                () -> launchStage(new LandGrabGame()));

        VBox buttons = new VBox(12, tugBtn, castleBtn, landBtn);
        buttons.setAlignment(Pos.CENTER);

        Button backBtn = createSecondaryButton("◀ 메인으로", this::showMainMenu);

        VBox center = new VBox(25, title, description, buttons, backBtn);
        center.setAlignment(Pos.CENTER);
        setCenterContent(center);
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
        btn.setMinWidth(200);
        btn.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 18));
        String normal = "-fx-background-color: linear-gradient(to bottom, #FFD54F, #FFB300); -fx-text-fill: #4E342E; -fx-border-color: #D18816; -fx-border-width: 1px; -fx-background-radius: 10; -fx-border-radius: 10;";
        String hover = "-fx-background-color: linear-gradient(to bottom, #FFECB3, #FFC107); -fx-text-fill: #4E342E; -fx-border-color: #BF7A10; -fx-border-width: 1px; -fx-background-radius: 10; -fx-border-radius: 10;";
        btn.setStyle(normal);
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
        btn.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 14));
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8D6E63;");
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void launchStage(Stage stage) {
        stage.initOwner(primaryStage);
        stage.show();
    }

    private void setCenterContent(Node node) {
        root.setCenter(node);
    }

    private void applyGradientBackground() {
        root.setStyle("-fx-background-color: " + BG_BEIGE + ";");
    }

    private void applyPlainBackground() {
        root.setStyle("-fx-background-color: " + BG_BEIGE + ";");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
