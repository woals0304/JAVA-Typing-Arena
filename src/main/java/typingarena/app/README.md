# src/main/java/typingarena/app 디렉터리

- JavaFX 애플리케이션 진입점(`TypingGameApp`)이 위치하며 로비 화면을 구성합니다.
- 메인 화면에서 싱글(3개 미니게임) / 멀티(자동 매칭) 중 하나를 선택합니다.
- `MultiLobbyStage`는 `NetClient`를 사용해 서버와 통신하며, 선택한 게임 타입으로 자동 매칭을 요청합니다.
- 새 미니게임을 추가하려면 버튼을 연결하고 README도 함께 갱신해 주세요.
