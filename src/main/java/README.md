# src/main/java 디렉터리

- 패키지 구조에 맞춰 자바 소스 파일을 배치하는 공간입니다.
- 최상위 패키지는 `typingarena`이며, `app`에는 로비/창 진입점이, `minigames`에는 개별 게임 로직이 위치합니다.
- 새로운 미니게임을 추가할 때는 `typingarena.minigames.<game>` 패키지를 생성하고 `TypingGameApp`에서 진입점을 연결해 주세요.
