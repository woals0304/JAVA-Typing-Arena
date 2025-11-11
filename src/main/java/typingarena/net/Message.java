package typingarena.net;

import java.util.Map;

/** 단순 JSON 메시지 컨테이너 (Gson과 호환되게 POJO) */
public class Message {
    public String type;      // "list_rooms", "rooms", "create_room", "join_room", ...
    public String roomId;    // 대상 방
    public String roomName;  // 새 방 이름
    public String nickname;  // 닉네임
    public String text;      // 기타 텍스트 (예: 채팅/입력)
    public Map<String, Object> data; // 추가 페이로드

    public Message() {}
    public Message(String type) { this.type = type; }

    public static Message of(String type) { return new Message(type); }
}
