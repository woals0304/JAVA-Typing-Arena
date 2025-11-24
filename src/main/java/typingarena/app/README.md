# src/main/java/typingarena/app 디렉터리

- JavaFX 애플리케이션 진입점(`TypingGameApp`)이 위치하며 로비 화면을 구성합니다.
- 메인 화면에서 싱글(줄다리기/성 지키기/땅따먹기) 또는 멀티를 선택합니다.
- `MultiLobbyStage`는 로그인/회원가입 → 게임 선택 플로우를 담당하며, `NetClient`로 서버에 매칭을 요청합니다.
  - 지원 게임: 줄다리기(PvP), 땅따먹기(PvP). 성 지키기 버튼은 비활성화.
  - 창이 열리면 기본 Host/Port(127.0.0.1:7777)로 자동 연결을 시도합니다.
- 멀티 Stage(`TugOfWarOnlineStage`, `LandGrabOnlineStage`)는 서버가 주는 세션 ID와 닉네임을 받아 UI를 갱신합니다. Land Grab은 경기 종료 후 재경기(GAME_REMATCH_REQUEST)를 보낼 수 있습니다.
- 새 미니게임을 추가하려면 버튼을 연결하고 README도 함께 갱신해 주세요.
