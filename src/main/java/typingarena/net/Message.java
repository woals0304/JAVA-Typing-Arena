package typingarena.net;

import java.util.Map;

/** 단순 JSON 메시지 컨테이너 (Gson과 호환되게 POJO) */
public class Message {
    public String type;         // "MATCH_REQUEST", "GAME_UPDATE_BROADCAST", ...
    public String roomId;       // 대상 방
    public String roomName;     // 새 방 이름
    public String nickname;     // 닉네임
    public String sessionId;    // 게임 세션 ID
    public String gameType;     // 게임 종류 (예: TUG_OF_WAR)
    public String text;         // 기타 텍스트 (채팅 등)
    public Map<String, Object> data; // 추가 페이로드

    public Message() {}
    public Message(String type) { this.type = type; }

    public static Message of(String type) { return new Message(type); }
}
