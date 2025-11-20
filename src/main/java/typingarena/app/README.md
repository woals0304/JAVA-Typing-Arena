# src/main/java/typingarena/app 디렉터리

- JavaFX 애플리케이션 진입점(`TypingGameApp`)이 위치하며 로비 화면을 구성합니다.
- 메인 화면에서 싱글(줄다리기/성 지키기/땅따먹기) 또는 멀티를 선택합니다.
- `MultiLobbyStage`는 로그인/회원가입 → 게임 선택 플로우를 담당하며, `NetClient`로 서버에 매칭을 요청합니다.
  - 지원 게임: 줄다리기(PvP 완성), 땅따먹기(현재 P1 vs 서버 AI), 성 지키기 버튼은 비활성화.
- 멀티 Stage(`TugOfWarOnlineStage`, `LandGrabOnlineStage`)는 서버가 주는 세션 ID와 닉네임을 받아 UI를 갱신합니다.
- 새 미니게임을 추가하려면 버튼을 연결하고 README도 함께 갱신해 주세요.
