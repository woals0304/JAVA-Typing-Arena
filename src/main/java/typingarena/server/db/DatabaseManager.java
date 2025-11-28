package typingarena.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static DatabaseManager instance;
    private Connection connection;
    
    // DB 파일 경로 (프로젝트 루트)
    private static final String DB_URL = "jdbc:sqlite:typing_arena.db";

    // 1. private 생성자
    private DatabaseManager() {
        try {
            connect();
            initTables();
            System.out.println("데이터베이스 연결 및 테이블 초기화 성공.");
        } catch (SQLException e) {
            System.err.println("데이터베이스 초기화 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 2. 싱글턴 인스턴스 반환 메서드
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    // 3. DB 연결 메서드
    private void connect() throws SQLException {
        connection = DriverManager.getConnection(DB_URL);
    }

    // 4. 테이블 생성 메서드 (업무 목록 2번)
    private void initTables() throws SQLException {
        // 유저 테이블 (id, pw, salt, 그리고 nickname 추가)
        String sqlCreateUsers = "CREATE TABLE IF NOT EXISTS users ("
                + " id TEXT PRIMARY KEY NOT NULL,"
                + " pw TEXT NOT NULL,"
                + " salt TEXT NOT NULL,"
                + " nickname TEXT UNIQUE NOT NULL"
                + ");";

        // 전적 테이블
        String sqlCreateGameRecords = "CREATE TABLE IF NOT EXISTS game_records ("
                + " record_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " user_id TEXT NOT NULL,"
                + " game_type TEXT NOT NULL,"
                + " wins INTEGER DEFAULT 0,"
                + " losses INTEGER DEFAULT 0,"
                + " FOREIGN KEY (user_id) REFERENCES users (id)"
                + ");";

        // Statement 객체를 try-with-resources 구문으로 안전하게 사용
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sqlCreateUsers);
            stmt.execute(sqlCreateGameRecords);
        }
    }
    
    // 5. 외부에서 Connection 객체를 쓸 수 있게 getter 제공
    public Connection getConnection() {
        return connection;
    }

   /**
     * 게임 전적을 업데이트 (또는 새로 생성)합니다.
     * @param userId 유저 ID
     * @param gameType "tug_of_war" 등 게임 타입
     * @param didWin 승리했으면 true, 패배했으면 false
     */
    public void updateGameRecord(String userId, String gameType, boolean didWin) {
        // SQLite의 UPSERT (Update or Insert) 기능 사용
        
        // 1. 레코드가 없으면 새로 삽입 (승/패는 0으로)
        String sqlInsert = "INSERT OR IGNORE INTO game_records (user_id, game_type, wins, losses) "
                         + "VALUES (?, ?, 0, 0)";
        
        // 2. 승/패 업데이트
        String sqlUpdate = didWin
            ? "UPDATE game_records SET wins = wins + 1 WHERE user_id = ? AND game_type = ?"
            : "UPDATE game_records SET losses = losses + 1 WHERE user_id = ? AND game_type = ?";

        // [수정] 'dbManager.getConnection()' 대신
        //      이 클래스의 'connection' 필드를 직접 사용합니다.
        try {
            // [수정] try-with-resources에서 Connection을 제거합니다.
            // (connection은 클래스 멤버이므로 여기서 닫으면 안 됨)
            
            // 트랜잭션 시작
            connection.setAutoCommit(false); 
            
            try (PreparedStatement pstmtInsert = connection.prepareStatement(sqlInsert);
                 PreparedStatement pstmtUpdate = connection.prepareStatement(sqlUpdate)) {
                
                // 1. INSERT OR IGNORE 실행
                pstmtInsert.setString(1, userId);
                pstmtInsert.setString(2, gameType);
                pstmtInsert.executeUpdate();
                
                // 2. UPDATE 실행
                pstmtUpdate.setString(1, userId);
                pstmtUpdate.setString(2, gameType);
                pstmtUpdate.executeUpdate();
                
                // 트랜잭션 커밋
                connection.commit();
                
            } catch (SQLException e) {
                connection.rollback(); // 오류 시 롤백
                System.err.println("전적 업데이트 실패: " + e.getMessage());
            } finally {
                connection.setAutoCommit(true); // 오토커밋 원상복구
            }
            
        } catch (SQLException e) {
            System.err.println("DB 연결 오류 (전적): " + e.getMessage());
        }
    }
}