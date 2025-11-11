# src/main/java/typingarena 디렉터리

- 애플리케이션 전체 패키지 루트입니다.
- 주요 하위 패키지
  - `app` : 로비, 싱글/멀티 UI, MultiLobbyStage 등 프레젠테이션 레이어.
  - `minigames` : 각각의 게임 로직과 UI (tugofwar, castledefense, landgrab).
  - `net` : Gson 기반 경량 메시지(`Message`)와 `NetClient`.
  - `server` : 테스트용 TCP 로비 서버(`ServerMain`).
