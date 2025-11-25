# 🪢 미니게임: 줄다리기 (Tug of War)
Typing Arena의 1:1 타자 대결 모드입니다. 60초 안에 밧줄을 상대 진영으로 밀어내거나 점수 우위를 확보하세요.

## 규칙과 목표
- 제한 시간: 60초, 밧줄 위치가 `+100`(왼쪽 승) 또는 `-100`(오른쪽 승)에 닿으면 즉시 종료.
- 입력: 중앙 입력창에 단어를 정확히 입력하고 Enter. 싱글 플레이는 `게임 시작` 버튼을 누르면 시작됩니다.
- 점수/콤보: 정답 시 `10 + 콤보*2` 점, 콤보는 연속 정답 시 증가, 오답은 콤보 0으로 초기화되고 밧줄이 뒤로 밀립니다.
- 밧줄 이동: 정답 시 전진, 오답·시간 경과 시 후퇴합니다. 싱글은 AI가 지속적으로 잡아당기며, 멀티는 상대 입력에 따라 밀립니다.

## 단어/모디파이어
- 단어 소스: `src/main/resources/words/ko.json` (싱글/멀티 공용).
- 모디파이어 비율: 일반 50% / 버프 25% / 트랩 25%.

### 버프/트랩 아이템
- 버프(파란 단어): 파워 그립(5초, 전진력 2배) 또는 앵커(3초, 상대/AI 견인력 20%만 적용).
- 트랩(빨간 단어): 먹물(3초, 단어 블라인드) 또는 자소 분리(4초, 단어를 자모로 분리 표시).
- 최근 발동 아이템은 HUD에 표시되며 색상 플래시로 강조됩니다.

## 싱글 플레이(Stage: `TugOfWarGame`)
- `GameLogic`이 타이머, 점수, 콤보, AI 견인력을 관리합니다.
- `TugOfWarMatchView` + `RopePanel`로 멀티와 동일한 UI를 재사용합니다.
- 게임 오버 시 오버레이에서 다시하기/닫기 선택 가능, 자동 닫힘 타이머가 30초 카운트됩니다.

## 온라인 플레이(Stage: `TugOfWarOnlineStage`)
- 서버 매칭 후 `GAME_START_BROADCAST` → `GAME_UPDATE_BROADCAST`로 밧줄 위치/단어/아이템 상태를 수신합니다.
- 입력은 `GAME_ACTION {"word":"..."}`로 전송, 기권은 `GAME_FORFEIT`를 보냅니다.
- 경기 종료 후 `재경기` 버튼 또는 오버레이를 눌러 `GAME_REMATCH_REQUEST` 전송. 한쪽만 보내면 상대가 `GAME_REMATCH_NOTICE`를 받으며, 양쪽이 동의하면 동일 sessionId로 즉시 재시작됩니다.

## 밸런스/소스 위치
- 싱글 로직 상수: `core/tugofwar/GameLogic.java` (`STEP_HIT`, `STEP_MISS`, `ENEMY_BASE` 등).
- 멀티 로직: `server/session/TugOfWarSession.java`에서 전진량/아이템 적용 및 전적 기록 관리.
- 공통 뷰: `tugofwar/TugOfWarMatchView.java`, 로프 렌더러 `tugofwar/RopePanel.java`.

## TODO 메모
- 멀티 매치 엔딩 UX 개선(전적/닉네임 표시, 오류 토스트).
- 밸런스 재측정 후 전진량·아이템 지속시간 튜닝.
- 실행 GIF/스크린샷을 README에 추가.
