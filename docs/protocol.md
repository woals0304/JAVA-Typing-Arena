## 📜 Typing Arena 통신 규칙 (Protocol v1.0)

모든 통신은 `Message.java` 객체를 `Gson`으로 변환한 JSON 문자열을 사용합니다. (`\n`으로 각 메시지를 구분합니다.)

### 1. 인증 (Auth)

### ➡️ 로그인 요청 (Client → Server)

- `type`: `"LOGIN_REQUEST"`
- `data`: `{"id": "...", "pw": "..."}`

JSON

`{
  "type": "LOGIN_REQUEST",
  "data": {
    "id": "myUserId",
    "pw": "myPassword123"
  }
}`

### ⬅️ 로그인 응답 (Server → Client)

- `type`: `"LOGIN_RESPONSE"`
- `data`: `{"success": true/false, "message": "...", "records": [...]}`

JSON

`{
  "type": "LOGIN_RESPONSE",
  "data": {
    "success": true,
    "message": "로그인 성공!",
    "nickname": "TypingGod",
    "records": [
      {"game": "LandGrab", "wins": 10, "losses": 2},
      {"game": "TugOfWar", "wins": 5, "losses": 8}
    ]
  }
}`

### ➡️ 회원가입 요청 (Client → Server)

- `type`: `"REGISTER_REQUEST"`
- `data`: `{"id": "...", "pw": "...", "nickname": "..."}` (닉네임도 같이 받습니다)

JSON

`{
  "type": "REGISTER_REQUEST",
  "data": {
    "id": "newPlayer",
    "pw": "strongPass!123",
    "nickname": "새로운용사"
  }
}`

### ⬅️ 회원가입 응답 (Server → Client)

- `type`: `"REGISTER_RESPONSE"`
- `data`: `{"success": true/false, "message": "..."}`

> (성공 시)
>

JSON

`{
  "type": "REGISTER_RESPONSE",
  "data": {
    "success": true,
    "message": "회원가입 성공! 로그인 화면에서 로그인해주세요."
  }
}`

> (실패 시: ID 중복 등)
>

JSON

`{
  "type": "REGISTER_RESPONSE",
  "data": {
    "success": false,
    "message": "오류: 이미 존재하는 ID입니다."
  }
}`

---

### 2. 로비 (Lobby)

### ➡️ 방 목록 요청 (Client → Server)

- `type`: `"LIST_ROOMS_REQUEST"` (기존 `list_rooms`에서 명확하게 변경)

JSON

`{
  "type": "LIST_ROOMS_REQUEST"
}`

### ⬅️ 방 목록 응답 (Server → Client)

- `type`: `"LIST_ROOMS_RESPONSE"` (기존 `rooms`에서 변경)
- `data`: `{"rooms": [...]}`

JSON

`{
  "type": "LIST_ROOMS_RESPONSE",
  "data": {
    "rooms": [
      {"roomId": "uuid-123", "name": "A+ 받자", "players": 1, "gameType": "LandGrab"},
      {"roomId": "uuid-456", "name": "초보만", "players": 2, "gameType": "TugOfWar"}
    ]
  }
}`

*(방 생성/입장도 `CREATE_ROOM_REQUEST`, `JOIN_ROOM_REQUEST` 등으로 명확하게 만드세요.)*

---

### 3. 게임 플레이 (Game)

### ➡️ 게임 시작 요청 (Client → Server)

- **(방장이 누름)**
- `type`: `"GAME_START_REQUEST"`

JSON

`{
  "type": "GAME_START_REQUEST"
}`

### ⬅️ 게임 시작 전파 (Server → All Clients in Room)

- **"게임을 시작합니다! 로딩하세요!"**
- `type`: `"GAME_START_BROADCAST"`
- `data`: `{"gameType": "LandGrab"}`

JSON

`{
  "type": "GAME_START_BROADCAST",
  "data": {
    "gameType": "LandGrab",
    "players": ["PlayerA_Nick", "PlayerB_Nick"]
  }
}`

### ➡️ 단어 입력 (Client → Server)

- `type`: `"GAME_ACTION"`
- `data`: `{"word": "..."}`

JSON

`{
  "type": "GAME_ACTION",
  "data": {
    "word": "바나나"
  }
}`

### ⬅️ 게임 상태 갱신 전파 (Server → All Clients in Room)

- **(가장 중요!)** 서버가 판단한 **"사실"**만 전송합니다.
- `type`: `"GAME_UPDATE_BROADCAST"`
- `data`: `(게임별로 상이)`

### 🌟 "땅따먹기 (LandGrab)" 갱신 예시

> (일반 단어 성공 시)
>

JSON

`{
  "type": "GAME_UPDATE_BROADCAST",
  "data": {
    "game": "LandGrab",
    "tiles_changed": [
      {"r": 3, "c": 4, "owner": "PlayerA_Nick"}
    ],
    "scores": {"PlayerA_Nick": 15, "PlayerB_Nick": 12},
    "animation_trigger": null
  }
}`

> (버프/트랩 단어 성공 시 - 예: 스플래시)
>

JSON

`{
  "type": "GAME_UPDATE_BROADCAST",
  "data": {
    "game": "LandGrab",
    "tiles_changed": [
      {"r": 3, "c": 4, "owner": "PlayerA_Nick"},
      {"r": 3, "c": 5, "owner": "PlayerA_Nick"}
    ],
    "scores": {"PlayerA_Nick": 16, "PlayerB_Nick": 12},
    "animation_trigger": {
      "type": "SPLASH",
      "r": 3,
      "c": 4
    }
  }
}`

> (먹물 트랩 성공 시)
>

JSON

`{
  "type": "GAME_UPDATE_BROADCAST",
  "data": {
    "game": "LandGrab",
    "tiles_changed": [
      {"r": 5, "c": 5, "owner": "PlayerA_Nick"}
    ],
    "ink_tiles_added": [
      {"r": 1, "c": 1}
    ],
    "scores": {"PlayerA_Nick": 17, "PlayerB_Nick": 12},
    "animation_trigger": {
      "type": "INK_SPLASH",
      "r": 5,
      "c": 5
    }
  }
}`

*(참고: `ink_tiles_removed` 같은 필드도 추가로 정의해서 먹물 효과가 끝났음을 알릴 수 있습니다.)*

### 🌟 "줄다리기 (TugOfWar)" 갱신 예시

> (일반 단어 성공 시)
>

JSON

`{
  "type": "GAME_UPDATE_BROADCAST",
  "data": {
    "game": "TugOfWar",
    "pos": -15.5,
    "timeMs": 42100,
    "yourWord": "바람",
    "modifierSelf": "NEUTRAL",
    "blindSelf": false,
    "jamoSplitSelf": false,
    "animation_trigger": null
  }
}`
