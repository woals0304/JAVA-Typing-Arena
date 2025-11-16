package typingarena.server.auth;

import typingarena.server.db.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class AuthService {

    private DatabaseManager dbManager;

    public AuthService() {
        this.dbManager = DatabaseManager.getInstance();
    }

    // 1. 회원가입 로직
    public boolean register(String id, String pw, String nickname) {
        String salt = PasswordUtils.getSalt();
        String hash = PasswordUtils.hashPassword(pw, salt);

        if (hash == null) return false; // 해싱 실패

        String sql = "INSERT INTO users (id, pw, salt, nickname) VALUES (?, ?, ?, ?)";
        
        // [수정] Connection을 try() 밖으로 빼서 닫히지 않게 함
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, id);
            pstmt.setString(2, hash);
            pstmt.setString(3, salt);
            pstmt.setString(4, nickname);
            pstmt.executeUpdate();
            return true; // 성공
            
        } catch (SQLException e) {
            // ID 또는 닉네임 중복 (UNIQUE 제약조건 위배)
            System.err.println("회원가입 실패: " + e.getMessage());
            return false; 
        }
    }

    // 2. 로그인 로직
    public Map<String, Object> login(String id, String pw) {
        String sql = "SELECT pw, salt, nickname FROM users WHERE id = ?";
        
        // [수정] Connection을 try() 밖으로 빼서 닫히지 않게 함
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            
            // [수정] ResultSet도 try-with-resources로 안전하게 닫음
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) { // 유저가 존재하면
                    String storedHash = rs.getString("pw");
                    String salt = rs.getString("salt");
                    String nickname = rs.getString("nickname");
                    
                    // 비밀번호 검증
                    if (PasswordUtils.verifyPassword(pw, storedHash, salt)) {
                        // 로그인 성공!
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("id", id);
                        userData.put("nickname", nickname);
                        // 전적 정보도 같이 가져와서 넣어줌
                        userData.putAll(getGameRecords(id)); 
                        return userData;
                    }
                }
            }
            return null; // ID가 없거나 비밀번호가 틀림
            
        } catch (SQLException e) {
            System.err.println("로그인 DB 조회 실패: " + e.getMessage());
            return null;
        }
    }

    // 3. (헬퍼) 전적 가져오기
    public Map<String, Object> getGameRecords(String userId) {
        Map<String, Object> records = new HashMap<>();
        String sql = "SELECT game_type, wins, losses FROM game_records WHERE user_id = ?";
        
        // [수정] Connection을 try() 밖으로 빼서 닫히지 않게 함
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            
            // [수정] ResultSet도 try-with-resources로 안전하게 닫음
            try (ResultSet rs = pstmt.executeQuery()) {
                // 우선 모든 게임 전적을 0으로 초기화
                records.put("tug_of_war_wins", 0);
                records.put("tug_of_war_losses", 0);
                // (다른 게임도 있다면 추가...)

                while (rs.next()) {
                    String gameType = rs.getString("game_type");
                    int wins = rs.getInt("wins");
                    int losses = rs.getInt("losses");
                    
                    // DB에 있는 값으로 덮어쓰기
                    records.put(gameType + "_wins", wins);
                    records.put(gameType + "_losses", losses);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("전적 조회 실패: " + e.getMessage());
        }
        return records;
    }
}