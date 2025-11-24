# Typing Arena 미니게임 프로젝트

## 프로젝트 개요
- 멀티플레이 감성을 살린 타자 연습 미니게임 모음집입니다.
- 현재 제공 게임
  1. **줄다리기 타자 대전 (TugOfWarGame)** – 단어는 50% 일반 / 25% 버프 / 25% 트랩, 싱글/온라인 모두 지원.
  2. **성 지키기 (CastleDefenseGame)** – 난이도별 웨이브를 타이핑으로 방어하는 디펜스 모드.
  3. **땅따먹기 Land Grab (LandGrabGame)** – 10x10 보드에서 단어를 맞춰 타일을 점령하고 콤보·아이템을 터뜨리는 게임. 온라인 매칭까지 실시간 PvP로 동작합니다.
- 모든 게임은 JavaFX 기반 UI와 Gson 기반 단어 로더(`src/main/resources/words/ko.json`)를 공유합니다.

## 현재 구현 상태
- 싱글 플레이: 세 미니게임 모두 동작하며, 공용 word 리소스와 `core/*` 엔진을 사용합니다.
- 멀티 플레이: 로비에서 로그인/회원가입 후 자동 매칭. 줄다리기와 땅따먹기 모두 실시간 PvP로 진행되며 전적이 DB에 반영됩니다. 성 지키기 멀티 버튼은 비활성화.
- Land Grab 온라인: 10x10 보드를 실시간으로 양쪽에 브로드캐스트합니다. 콤보 10 이상이면 상대 타일을 바로 빼앗을 수 있고, 스플래시/보호막/콤보가드·먹물/EMP/혼란 아이템이 발동합니다. 재경기 요청(GAME_REMATCH_REQUEST)도 지원합니다.
- 서버/DB: `typingarena.server.ServerMain`이 SQLite `typing_arena.db`를 자동 생성하고 PBKDF2로 해싱한 비밀번호·전적(wins/losses)을 관리합니다. 초기화가 필요하면 DB 파일을 삭제 후 서버를 재실행하세요.
- 프로토콜: `docs/protocol.md`에 로그인/매칭/게임 전파·재경기·기권 플로우를 정리했습니다.

## 개발 환경 준비
1. **JDK 21 LTS 설치**
   - `java -version`으로 21 버전이 잡히는지 확인합니다.
2. **Maven 3.9 이상 설치**
   - `mvn -v`로 설치 여부를 확인합니다.
3. **프로젝트 클론 후 의존성 받기**
   ```bash
   git clone <repo-url>
   cd JAVA-Typing-Arena
   mvn -q dependency:resolve
   ```
4. **JavaFX 플랫폼 설정**
   - `pom.xml`의 `<javafx.platform>` 값을 실제 OS(`win`, `linux`, `mac`, `mac-aarch64`)에 맞춰 수정합니다.
5. 실행 중 JavaFX NullPointerException이 발생한다면 GPU 드라이버를 업데이트하거나 `-Dprism.order=sw` VM 옵션으로 소프트웨어 렌더링을 시도하세요.

## 빌드 및 실행
### CLI
```bash
mvn clean javafx:run
```
- 기본 실행 진입점은 `typingarena.app.TypingGameApp`입니다.
- 로비 화면의 “줄다리기 게임 시작” 버튼을 누르면 `TugOfWarGame` 창이 새로 열립니다.
- 초록색 단어(버프)·빨간색 단어(트랩)·검정색 단어(일반)를 입력해 게임을 진행합니다.
- IDE에서 실행할 경우 Maven 프로젝트로 임포트한 뒤 동일한 Main 클래스를 실행하면 됩니다.

### IDE (IntelliJ 예시)
1. *Open* → `pom.xml`.
2. Maven 패널에서 `typing-arena` 프로젝트 선택.
3. Run/Debug 구성에서 `typingarena.app.TypingGameApp`을 Main Class로 지정 후 실행.
4. 실행 환경에 따라 VM 옵션:
   ```
   --module-path %PATH_TO_FX% --add-modules javafx.controls,javafx.graphics
   ```
   (`%PATH_TO_FX%`는 JavaFX SDK의 `lib` 경로)

### 멀티 플레이 실행
1. 서버 실행 (DB 자동 생성)
   - 개발용: `mvn -q exec:java -Dexec.mainClass=typingarena.server.ServerMain`
   - 배포용 fat jar: `mvn -q -DskipTests package` 후 `java -jar target/server-fat.jar`
   - 루트에 `typing_arena.db`가 생성/갱신됩니다. 전적·계정 초기화가 필요하면 이 파일을 삭제하고 서버를 재시작하세요.
2. 로비 접속/로그인
   - 클라이언트에서 `멀티 플레이` 버튼 클릭 → 기본 Host/Port는 `127.0.0.1:7777` (창이 열리면 자동 연결 시도).
   - `회원가입`으로 ID/PW/닉네임을 등록한 뒤, `로그인`해야 게임 선택 버튼이 활성화됩니다.
3. 게임 선택
   - **줄다리기**: 실시간 PvP. 서버가 단어/아이템을 배정하며 전적이 DB에 기록됩니다.
   - **땅따먹기**: 실시간 PvP. 먹물/EMP/혼란, 스플래시/보호막/콤보가드 아이템과 타일 점령 현황이 매 틱 동기화됩니다. 경기 종료 후 재경기 버튼으로 다시 시작할 수 있습니다.
   - **성 지키기**: 멀티 버튼은 비활성화 상태.
4. 기본 포트는 `7777`이며 외부 접속 시 방화벽 예외가 필요할 수 있습니다.


## 디렉터리 구조
```
JAVA프로젝트/
├─ README.md                # 프로젝트 개요 및 실행 가이드
├─ pom.xml                  # Maven 빌드 설정 (Java 21, JavaFX, Gson)
├─ typing_arena.db          # SQLite DB (서버가 자동 생성, 계정/전적 저장)
├─ src/
│  ├─ main/
│  │  ├─ java/
│  │  │  └─ typingarena/
│  │  │     ├─ app/        # 로비/싱글/멀티 Stage 진입점
│  │  │     ├─ core/       # 싱글/멀티가 공유하는 게임 엔진 (tugofwar, landgrab)
│  │  │     ├─ minigames/  # 각 미니게임 UI + 싱글 컨트롤러
│  │  │     ├─ net/        # Gson 메시지 컨테이너, NetClient
│  │  │     └─ server/     # 매칭/세션/인증/DB(ServerMain)
│  │  └─ resources/
│  │        └─ words/      # ko.json(게임 공용), ko_easy|medium|hard.txt(추가 단어 예시)
└─ docs/                    # 설계 문서, 발표 자료, 프로토콜
```

## 코딩 컨벤션
- 패키지 명은 모두 소문자, 클래스 명은 PascalCase를 사용합니다.
- UI 텍스트는 `TypingGameApp`에서 중앙 관리하고, 미니게임 내부에서는 게임 전용 메시지만 작성합니다.
- 하드코딩된 문자열과 상수는 가능한 `static final`로 추출해 재사용합니다.
- 아이템/단어 비율 등의 밸런스 값을 조정할 때는 `GameLogic`의 관련 상수를 수정하고 README를 업데이트해 주세요.
- PR 작성 시 변경 요약과 테스트 결과(수동 테스트 포함)를 간단히 남겨 주세요.

## 단어 리스트 커스터마이징
- `src/main/resources/words/ko.json` 안의 `"words"` 배열에 단어를 추가하면 바로 게임에 반영됩니다. 줄다리기/땅따먹기 싱글·온라인 모두 이 목록을 공유합니다.
- JSON 형식이므로 주석은 사용할 수 없습니다. 설명을 남기고 싶다면 README나 별도 문서를 활용하세요.
- 난이도별 단어 예시는 `ko_easy.txt`, `ko_medium.txt`, `ko_hard.txt`에 정리돼 있으며 필요 시 JSON으로 변환해 사용할 수 있습니다.
- 특수 단어 확률은 코드에서 관리됩니다. 현재 비율:
  - 줄다리기: 일반 50% / 버프 25% / 트랩 25%
  - 땅따먹기: 일반 60% / 버프 20% / 트랩 20%
- 줄다리기 트랩 효과: 먹물(3초, 단어 가림) 또는 자소 분리(JAMO_SPLIT, 4초, 단어를 자모로 분리해 표시).
- 땅따먹기 버프/트랩: 스플래시(인접 타일 추가 점령), 보호막(5초), 콤보가드(5초), 먹물(타일 시야 차단), EMP(상대 타일 최대 3개 중립화), 혼란(상대 단어 좌우 뒤집기).

## 향후 TODO
- [ ] 전적/닉네임을 로비 UI에 노출하고 멀티 에러 메시지 UX 개선
- [ ] 성 지키기 멀티 모드 로비/세션 추가
- [ ] Land Grab/Tug of War 밸런스(타이머, 아이템 지속시간, 점수 보정) 실측 후 튜닝
- [ ] 버프/트랩 아이템 종류 확장 및 시각 효과 강화
- [ ] 공용 설정/리소스 로더/스크립트 정리 및 CI 빌드 자동화
- [ ] README에 실행 GIF 또는 스크린샷 첨부
