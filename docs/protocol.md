## 📜 Typing Arena 통신 규칙 (Protocol v1.1)

- 모든 메시지는 `Message.java` 형태의 JSON 한 줄(`\n` 구분)로 주고받습니다.
- 공통 필드: `type`(필수), `sessionId`(게임 세션), `roomId`/`roomName`(로비), `gameType`, `text`, `data`(Map).
- 기본 포트는 `7777`, 인코딩은 UTF-8 입니다.

### 0. 예시 Envelope
```json
{"type":"MATCH_REQUEST","data":{"gameType":"LAND_GRAB"}}
```

### 1. 인증 (Auth)
- **회원가입** `REGISTER_REQUEST` → `REGISTER_RESPONSE`
  - req.data: `{"id":"...", "pw":"...", "nickname":"..."}`
  - res.data: `{"success": true/false}`
- **로그인** `LOGIN_REQUEST` → `LOGIN_RESPONSE`
  - req.data: `{"id":"...", "pw":"..."}`
  - 성공: `{"success": true, "id":"...", "nickname":"...", "tug_of_war_wins":0, "tug_of_war_losses":0, ...}`
  - 실패: `{"success": false}`
  - 로그인 이후에만 매칭/게임 액션이 허용됩니다.

### 2. 로비/매칭
- **방 목록(Optional)** `LIST_ROOMS_REQUEST` → `LIST_ROOMS_RESPONSE` (`rooms: [{roomId,name,players}, ...]`)
- **매칭 요청** `MATCH_REQUEST` (`gameType`: `"TUG_OF_WAR"` | `"LAND_GRAB"`)
  - 서버 응답: `MATCH_WAITING` → 매칭 성사 시 두 명 모두에게 `MATCH_SUCCESS {gameType}` 후 즉시 `GAME_START_BROADCAST` 전송.
  - 취소: `MATCH_CANCEL` → `MATCH_CANCELLED`
  - 오류 시: `MATCH_REQUEST_ERROR {message}` 또는 `auth_ERROR`(미로그인).

### 3. 공통 게임 액션
- **입력 전송** `GAME_ACTION` with `data: {"word":"..."}` (줄다리기/땅따먹기 모두 단어 제출용).
- **기권** `GAME_FORFEIT` (sessionId가 없으면 현재 세션 기준). 상대는 `GAME_END_BROADCAST` 또는 `GAME_OPPONENT_LEFT`로 알림을 받습니다.
- **재경기 요청 (Land Grab 전용)** `GAME_REMATCH_REQUEST`
  - 한쪽만 요청: 상대에게 `GAME_REMATCH_NOTICE`
  - 양쪽 모두 요청: 기존 세션 ID로 `GAME_START_BROADCAST`가 다시 발행되며 보드/타이머가 초기화됩니다.

### 4. 게임별 브로드캐스트 페이로드

#### 4-1. 줄다리기 (TUG_OF_WAR)
- **GAME_START_BROADCAST**
  ```json
  {
    "type":"GAME_START_BROADCAST",
    "sessionId":"...",
    "data":{
      "gameType":"TUG_OF_WAR",
      "yourWord":"바람",
      "opponentWord":"???",
      "opponent":"상대닉",
      "timeMs":60000,
      "modifierSelf":"BUFF|TRAP|NEUTRAL",
      "effectsSelf":"효과: ...",
      "lastItemSelf":"없음",
      "blindSelf":false,
      "jamoSplitSelf":false
    }
  }
  ```
- **GAME_UPDATE_BROADCAST**
  - 주요 필드: `gameType`, `pos`(double, 왼쪽 +), `timeMs`, `yourWord`, `modifierSelf`, `scoreSelf`, `scoreOpponent`, `effectsSelf`, `lastItemSelf`, `blindSelf`, `jamoSplitSelf`.
- **GAME_END_BROADCAST**
  - `gameType`, `result`(`승리|패배|무승부`), `message`(종료 사유), `scoreSelf`, `scoreOpponent`, `pos`.
- 아이템 규칙 요약: 단어 모디파이어 50/25/25, 버프(파워그립, 앵커), 트랩(먹물 3초, 자소 분리 4초).

#### 4-2. 땅따먹기 (LAND_GRAB)
- **GAME_START_BROADCAST**
  - `data` 공통 필드:
    - `gameType:"LAND_GRAB"`, `timeMs`(double), `grid`/`words`/`modifiers`(10x10 배열, 빈칸은 빈 문자열), `ink_tiles:[{"r","c","until"}...]`
    - `scoreSelf`/`scoreOpponent`, `comboSelf`/`comboOpponent`
    - `barrier_a`/`barrier_b`(보호막 여부), `debuff:"FLIP_WORDS"`(혼란 시)
    - `animation_trigger` 선택적 `{type:"...", "r":-1, "c":-1}`
    - 시작 시에는 `players:["나","상대"]`가 함께 전송됩니다.
- **GAME_UPDATE_BROADCAST**
  - 위 필드 동일. `animation_trigger.type` 값 예시:
    - 버프: `BUFF_SPLASH`, `BUFF_BARRIER`, `BUFF_COMBO_GUARD`
    - 트랩: `TRAP_INK`, `TRAP_EMP`, `TRAP_CONFUSION`
    - 피격/연출: `ATTACK_INK`, `ATTACK_EMP`, `ATTACK_CONFUSION`, `OPP_SPLASH`, `OPP_BARRIER`, `OPP_COMBO_GUARD`
- **GAME_END_BROADCAST**
  - `gameType`, `result`(`승리|패배|무승부`), `message`(예: 시간 종료, 포기), `scoreSelf`, `scoreOpponent`.
- 룰 요약: 단어 비율 60/20/20, 버프(스플래시/보호막 5초/콤보가드 5초), 트랩(먹물 타일 블라인드, EMP로 상대 타일 최대 3개 중립화, 혼란으로 단어 좌우 뒤집기). 콤보 10 이상이면 상대 타일까지 바로 빼앗습니다.

### 5. 세션 종료/연결 해제
- 클라이언트 소켓이 끊기면 `ServerMain`이 진행 중 매칭을 취소하고, 참여 중인 세션에 `GAME_FORFEIT` 처리 후 세션을 정리합니다.
- Land Grab에서 종료 후 한 명이 바로 나가면 남은 사람에게 `GAME_OPPONENT_LEFT`가 전달됩니다.
