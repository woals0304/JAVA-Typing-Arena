# src/main/java 디렉터리

- `typingarena` 패키지 기준으로 JavaFX 애플리케이션 코드가 배치됩니다.
- 하위 구조
  - `typingarena.app` : `TypingGameApp` 로비, 멀티매칭 창, 각 미니게임 Stage.
  - `typingarena.core` : 싱글/멀티가 공유하는 게임 엔진 로직(tugofwar, landgrab).
  - `typingarena.minigames` : 각 게임의 UI(View)와 싱글 플레이 컨트롤러.
  - `typingarena.net` : Gson 메시지 컨테이너와 TCP NetClient.
  - `typingarena.server` : 매칭/세션/인증(SQLite) 서버 엔트리포인트.
- 새 미니게임을 추가하려면 `typingarena.minigames.<game>` 패키지를 만들고 로비 버튼을 연결한 뒤 README를 갱신하세요.
