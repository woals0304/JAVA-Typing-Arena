package typingarena.app;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import typingarena.net.Message;
import typingarena.net.NetClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 간단한 멀티플레이 로비: 서버에 연결해 방 목록을 보고 생성/입장한다.
 */
public class MultiLobbyStage extends Stage {

    private final TextField hostField = new TextField("127.0.0.1");
    private final TextField portField = new TextField("7777");
    private final TextField nicknameField = new TextField(defaultNickname());
    private final Label statusLabel = new Label("서버에 연결되지 않았습니다.");

    private final TableView<RoomEntry> roomTable = new TableView<>();
    private final ObservableList<RoomEntry> rooms = FXCollections.observableArrayList();

    private NetClient client;

    public MultiLobbyStage(Stage owner) {
        initOwner(owner);
        initModality(Modality.NONE);
        setTitle("멀티 플레이 로비");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setTop(buildConnectionPane());
        root.setCenter(buildRoomTable());
        root.setBottom(buildActionPane());

        Scene scene = new Scene(root, 760, 520);
        setScene(scene);

        setOnShown(e -> connect());
        setOnHidden(e -> disconnect());
        setOnCloseRequest(e -> disconnect());
    }

    private VBox buildConnectionPane() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        grid.add(new Label("Host"), 0, 0);
        grid.add(hostField, 1, 0);
        grid.add(new Label("Port"), 2, 0);
        grid.add(portField, 3, 0);
        grid.add(new Label("Nickname"), 0, 1);
        grid.add(nicknameField, 1, 1);
        GridPane.setColumnSpan(nicknameField, 3);

        hostField.setPrefWidth(160);
        portField.setPrefWidth(80);
        nicknameField.setPrefWidth(200);

        Button connectBtn = new Button("연결");
        connectBtn.setOnAction(e -> connect());
        Button disconnectBtn = new Button("연결 종료");
        disconnectBtn.setOnAction(e -> disconnect());
        HBox controls = new HBox(10, connectBtn, disconnectBtn);
        controls.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(10, grid, controls, statusLabel);
        box.setAlignment(Pos.CENTER_LEFT);
        statusLabel.setStyle("-fx-text-fill: #555555;");
        return box;
    }

    private TableView<RoomEntry> buildRoomTable() {
        TableColumn<RoomEntry, String> nameCol = new TableColumn<>("방 이름");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(360);

        TableColumn<RoomEntry, Number> playersCol = new TableColumn<>("인원");
        playersCol.setCellValueFactory(new PropertyValueFactory<>("players"));
        playersCol.setPrefWidth(120);

        roomTable.getColumns().addAll(nameCol, playersCol);
        roomTable.setItems(rooms);
        roomTable.setPlaceholder(new Label("표시할 방이 없습니다. 새로고침을 눌러보세요."));
        roomTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        return roomTable;
    }

    private VBox buildActionPane() {
        Button refreshBtn = new Button("방 새로고침");
        refreshBtn.setOnAction(e -> requestRoomList());

        Button createBtn = new Button("방 만들기");
        createBtn.setOnAction(e -> createRoom());

        Button joinBtn = new Button("방 들어가기");
        joinBtn.setOnAction(e -> joinSelectedRoom());

        Button closeBtn = new Button("닫기");
        closeBtn.setOnAction(e -> close());

        HBox row1 = new HBox(10, refreshBtn, createBtn, joinBtn);
        row1.setAlignment(Pos.CENTER_LEFT);
        HBox row2 = new HBox(closeBtn);
        row2.setAlignment(Pos.CENTER_RIGHT);
        VBox box = new VBox(10, row1, row2);
        box.setPadding(new Insets(15, 0, 0, 0));
        return box;
    }

    private void connect() {
        if (client != null) {
            setStatus("이미 서버에 연결되어 있습니다.");
            return;
        }
        try {
            int port = Integer.parseInt(portField.getText().trim());
            client = new NetClient(hostField.getText().trim(), port);
            client.setOnMessage(this::handleServerMessage);
            client.connect();
            setStatus("서버에 연결되었습니다.");
            requestRoomList();
        } catch (NumberFormatException e) {
            showError("포트 번호가 올바르지 않습니다.");
        } catch (IOException e) {
            setStatus("연결 실패: " + e.getMessage());
            client = null;
        }
    }

    private void disconnect() {
        if (client != null) {
            try {
                client.close();
            } catch (IOException ignored) {}
            client = null;
        }
    }

    private boolean ensureConnected() {
        if (client != null) return true;
        connect();
        return client != null;
    }

    private void requestRoomList() {
        if (!ensureConnected()) return;
        Message msg = Message.of("list_rooms");
        client.send(msg);
        setStatus("방 목록 요청 중...");
    }

    private void createRoom() {
        if (!ensureConnected()) return;
        TextInputDialog dialog = new TextInputDialog("새 방");
        dialog.setTitle("방 만들기");
        dialog.setHeaderText("생성할 방 이름을 입력하세요.");
        dialog.initOwner(this);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.showAndWait().ifPresent(name -> {
            if (name.isBlank()) name = "새 방";
            Message msg = Message.of("create_room");
            msg.roomName = name.trim();
            client.send(msg);
            setStatus("방 생성 요청: " + name.trim());
        });
    }

    private void joinSelectedRoom() {
        RoomEntry selected = roomTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("입장할 방을 선택하세요.");
            return;
        }
        if (!ensureConnected()) return;
        Message msg = Message.of("join_room");
        msg.roomId = selected.getRoomId();
        msg.nickname = nicknameField.getText().trim().isEmpty() ? defaultNickname() : nicknameField.getText().trim();
        client.send(msg);
        setStatus("방 입장 요청: " + selected.getName());
    }

    private void handleServerMessage(Message msg) {
        if (msg == null) return;
        Platform.runLater(() -> {
            switch (msg.type) {
                case "rooms" -> updateRoomTable(msg);
                case "joined" -> {
                    setStatus("방에 입장했습니다. roomId=" + msg.roomId);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION,
                            "방에 입장했습니다.\nroomId: " + msg.roomId + "\n(실제 게임 시작 로직은 추후 연동)");
                    alert.initOwner(this);
                    alert.show();
                }
                default -> {}
            }
        });
    }

    private void updateRoomTable(Message msg) {
        List<RoomEntry> updated = new ArrayList<>();
        if (msg.data != null) {
            Object listObj = msg.data.get("list");
            if (listObj instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        String roomId = String.valueOf(map.get("roomId"));
                        String name = String.valueOf(map.get("name"));
                        int players = toInt(map.get("players"));
                        updated.add(new RoomEntry(roomId, name, players));
                    }
                }
            }
        }
        rooms.setAll(updated);
        setStatus("방 목록 갱신 (" + updated.size() + "개)");
    }

    private int toInt(Object value) {
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.initOwner(this);
        alert.show();
    }

    private String defaultNickname() {
        return "Player" + (int)(Math.random() * 1000);
    }

    // === RoomEntry DTO ===
    public static class RoomEntry {
        private final StringProperty roomId = new SimpleStringProperty();
        private final StringProperty name = new SimpleStringProperty();
        private final IntegerProperty players = new SimpleIntegerProperty();

        public RoomEntry(String roomId, String name, int players) {
            this.roomId.set(roomId);
            this.name.set(name);
            this.players.set(players);
        }

        public String getRoomId() { return roomId.get(); }
        public String getName() { return name.get(); }
        public int getPlayers() { return players.get(); }

        public StringProperty nameProperty() { return name; }
        public IntegerProperty playersProperty() { return players; }
    }
}
