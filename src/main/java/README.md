# src/main/java 디렉터리

- `typingarena` 패키지 기준으로 JavaFX 애플리케이션 코드가 배치됩니다.
- 하위 구조
  - `typingarena.app` : `TypingGameApp` 로비와 각 미니게임 진입점.
  - `typingarena.minigames.tugofwar` / `castledefense` / `landgrab` : 각 게임별 로직, 패널, HUD.
- 새 미니게임을 추가하려면 `typingarena.minigames.<game>` 패키지를 만들고 로비에서 버튼을 연결한 뒤 README를 갱신하세요.
