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

public class TypingGameApp extends Application {

    private Stage primaryStage;
    private BorderPane root;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.root = new BorderPane();
        root.setStyle("-fx-background-color: #FDF5E6;");
        root.setPadding(new Insets(36));

        Scene scene = new Scene(root, 760, 560);
        primaryStage.setTitle("Typing Mini Game");
        primaryStage.setScene(scene);
        showMainMenu();
        primaryStage.show();
    }

    // === Main Menu (Single / Multi) ===
    private void showMainMenu() {
        Label title = new Label("멀티플레이 타자 미니게임 로비");
        title.setFont(Font.font("Malgun Gothic", FontWeight.EXTRA_BOLD, 30));
        title.setTextFill(Color.web("#4E342E"));

        Label subtitle = new Label("싱글 플레이 또는 멀티 플레이를 선택하세요.");
        subtitle.setFont(Font.font("Malgun Gothic", FontWeight.NORMAL, 16));
        subtitle.setTextFill(Color.web("#6D4C41"));

        Button singleBtn = createPrimaryButton("싱글 플레이", this::showSingleMenu);
        Button multiBtn = createPrimaryButton("멀티 플레이", this::openMultiLobby);

        HBox buttons = new HBox(20, singleBtn, multiBtn);
        buttons.setAlignment(Pos.CENTER);

        VBox center = new VBox(25, title, subtitle, buttons);
        center.setAlignment(Pos.CENTER);
        setCenterContent(center);
    }

    // === Single Player Menu ===
    private void showSingleMenu() {
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
        try {
            MultiLobbyStage lobby = new MultiLobbyStage(primaryStage);
            lobby.show();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "멀티 로비를 열 수 없습니다.\n" + e.getMessage());
            alert.initOwner(primaryStage);
            alert.show();
        }
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

    public static void main(String[] args) {
        launch(args);
    }
}
