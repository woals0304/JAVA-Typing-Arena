# src/main/java/typingarena/minigames 디렉터리

- 타자 미니게임 구현을 모아두는 패키지입니다. 현재 구성:
  1. `tugofwar` – 버프/트랩 단어로 로프를 당기는 줄다리기. `GameLogic`이 아이템 비율(7:3), HUD, 로프 상태를 관리합니다.
  2. `castledefense` – 타이핑으로 적 웨이브를 막는 성 지키기 모드. 난이도/스테이지 관리 로직이 포함됩니다.
  3. `landgrab` – 10x10 보드를 차지하는 땅따먹기. `LandGrabLogic`과 `LandGrabPanel`이 타일 상태, 스플래시 애니메이션, 아이템을 담당합니다.
- 공통 리소스(`words/ko.json`)를 사용하므로 단어/아이템 규칙을 바꿀 때는 두 게임 모두 영향받을 수 있습니다.
- 새로운 게임을 추가하면 README에 간단한 설명과 의존 리소스를 기록하고, 로비(`TypingGameApp`) 버튼도 함께 업데이트해 주세요.
