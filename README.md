# Typing Arena 미니게임 프로젝트

## 프로젝트 개요
- 멀티플레이 감성을 살린 타자 연습 미니게임 모음집입니다.
- 현재 구현된 게임은 줄다리기 타자 대전(`TugOfWarGame`) 하나이며, JavaFX 기반 UI와 Gson 기반 단어 로더를 사용해 확장이 용이하도록 구성했습니다.
- 단어는 `src/main/resources/words/ko.json`에서 불러오며, 70% 확률로 일반 단어, 30% 확률로 버프/트랩 단어가 등장합니다.
- 버프 단어를 맞추면 파워그립·앵커 중 하나가 자동으로 발동하고, 트랩 단어를 맞추면 먹물 효과가 걸립니다. HUD에서 최근 발동 아이템을 확인할 수 있습니다.

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

## 디렉터리 구조
```
JAVA프로젝트/
├─ README.md                # 프로젝트 개요 및 실행 가이드
├─ pom.xml                  # Maven 빌드 설정 (Java 21, JavaFX, Gson)
├─ src/
│  ├─ main/
│  │  ├─ java/
│  │  │  └─ typingarena/
│  │  │     ├─ app/      # 엔트리 포인트, 공용 UI
│  │  │     └─ minigames/# 각 미니게임 구현 (현재 tugofwar)
│  │  └─ resources/
│  │        └─ words/    # ko.json(게임 사용), ko_easy|medium|hard.txt(추가 단어 예시)
└─ docs/                    # 설계 문서 및 참고 자료
```

## 코딩 컨벤션
- 패키지 명은 모두 소문자, 클래스 명은 PascalCase를 사용합니다.
- UI 텍스트는 `TypingGameApp`에서 중앙 관리하고, 미니게임 내부에서는 게임 전용 메시지만 작성합니다.
- 하드코딩된 문자열과 상수는 가능한 `static final`로 추출해 재사용합니다.
- 아이템/단어 비율 등의 밸런스 값을 조정할 때는 `GameLogic`의 관련 상수를 수정하고 README를 업데이트해 주세요.
- PR 작성 시 변경 요약과 테스트 결과(수동 테스트 포함)를 간단히 남겨 주세요.

## 단어 리스트 커스터마이징
- `src/main/resources/words/ko.json` 안의 `"words"` 배열에 단어를 추가하면 바로 게임에 반영됩니다.
- JSON 형식이므로 주석은 사용할 수 없습니다. 설명을 남기고 싶다면 README나 별도 문서를 활용하세요.
- 난이도별 단어 예시는 `ko_easy.txt`, `ko_medium.txt`, `ko_hard.txt`에 정리돼 있으며 필요 시 JSON으로 변환해 사용하세요.

## 향후 TODO
- [ ] 로비 화면에서 다중 미니게임 선택 UI 제공
- [ ] 버프/트랩 아이템 종류 확장 및 시각 효과 강화
- [ ] 멀티플레이용 네트워크 레이어 설계 (`docs/` 하위에 문서 추가)
- [ ] 공용 설정/리소스 로더 클래스 설계
- [ ] 빌드/테스트 자동화를 위한 스크립트(`scripts/`) 마련
- [ ] README에 실행 GIF 또는 스크린샷 첨부

궁금한 사항이나 제안이 있으면 PR 또는 이슈를 통해 공유해 주세요. 함께 프로젝트를 발전시켜 봅시다! 🙌
