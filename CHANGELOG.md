# Changelog

이 프로젝트의 주요 변경 사항을 기록합니다.
형식: [Keep a Changelog](https://keepachangelog.com/ko/1.1.0/)
버전 규칙: [Semantic Versioning](https://semver.org/lang/ko/) (MAJOR.MINOR.PATCH)

<!--
작성 규칙
- 최신 버전이 맨 위로 오게 쓴다.
- 작업 중인 변경은 [Unreleased]에 모았다가, 릴리스할 때 버전 번호와 날짜를 붙여 옮긴다.
- 날짜는 YYYY-MM-DD 형식으로 쓴다.
- 변경 하나마다 아래 네 줄로 쓴다.
    Q: 내가 내린 지시   — 지시한 사람이 적은 텍스트를 그대로 옮긴다. 고치거나 줄이지 않는다.
    했다             — 지시를 받고 한 일
    바꿨다           — 그 결과 바뀐 것 (파일·동작)
    머지했다          — 어느 PR·커밋으로 어느 브랜치에 들어갔는지, 날짜
- 지시 없이 작성자가 판단해서 한 변경은 Q에 "(지시 없음 — 작성자 판단)"이라고 쓴다.
- 관련 PR·이슈 번호와 작성자를 붙인다. 예: (#12, @강한)
- 쓰지 않는 분류 항목은 지운다.

지시 출처
- FEEDBACK = 검수 지시서 FEEDBACK.md (@kimgangan7890-spec, 2026-11-09, P0-1 = P0의 1번)
  review/feedback 브랜치에 있었으나 2026-11-12 브랜치를 삭제했다. 원문은 커밋 173f00d.
- 대화 = 사용자가 Claude Code에 직접 입력한 지시

분류
- Added      : 새 기능
- Changed    : 기존 기능의 변경
- Deprecated : 곧 제거될 기능
- Removed    : 제거된 기능
- Fixed      : 버그 수정
- Security   : 보안 관련 수정

버전 올리는 기준
- MAJOR (1.0.0 → 2.0.0) : 기존 사용 방식이 깨지는 변경
- MINOR (1.1.0 → 1.2.0) : 호환되는 기능 추가
- PATCH (1.1.0 → 1.1.1) : 호환되는 버그 수정

참고: 저장소에 아직 버전 태그가 없어서 비교 링크는 커밋 해시로 걸었다.
      v0.1.0 = 5351fe5, v0.2.0 = ecedac3. 태그를 만들면 링크를 태그로 바꾼다.
-->

## [Unreleased](https://github.com/hhgaq/hyundai-better-than-kia/compare/ecedac3...HEAD)

### Added

#### 지시를 담은 변경 이력(CHANGELOG.md) 작성

**Q: 내가 내린 지시** (대화, @kimgangan7890-spec)
> 요청: 내 지시를 담은 change로그 제작
> 프로젝트:hyundai-better-than-kia프로젝트 공동작업
> 조건:이번 요청에 한 해서 원본 문서 편집 금지

- **했다:** 커밋·PR·검수 지시서(FEEDBACK.md)를 모아 지시별로 어떻게 반영됐는지 정리했다.
- **바꿨다:** `CHANGELOG.md` 파일 하나만 새로 만들었다. 원본 문서·코드는 건드리지 않았다.
- **머지했다:** 머지 안 함. `review/changelog` 브랜치에만 있음 (`18e3f3d`, 2026-11-12)

**Q: 내가 내린 지시** (대화, @kimgangan7890-spec)
> 체인지 로그 형식 수정,
> [Keep a Changelog 양식 붙여넣음]
> 브랜치에만 작성 할것
> 형식에 사용자의 지시문을 포함할것

- **했다:** 붙여 준 Keep a Changelog 양식으로 다시 쓰고, 지시를 받은 변경마다 지시문을 붙였다.
- **바꿨다:** `CHANGELOG.md`를 버전(0.1.0, 0.2.0)·분류(Added·Changed·Fixed) 구조로 바꿨다.
- **머지했다:** 머지 안 함. `review/changelog` 브랜치에만 있음 (`9f24d3e`, 2026-11-12)

**Q: 내가 내린 지시** (대화, @kimgangan7890-spec)
> push 해줘

- **했다:** `review/changelog` 브랜치를 공동 저장소에 push했다.
- **바꿨다:** 원격에 `review/changelog` 브랜치가 생겼다. main은 그대로다.
- **머지했다:** 머지 안 함 (PR 없음)

**Q: 내가 내린 지시** (대화, @kimgangan7890-spec)
> review/feedback만 지우고 테스트 1 브랜치 만들어
> change log는 기존 양식에서
> Q:내가 내린 지시
> ~를 했고
> ~를 바꿧고
> ~머지했다
> 의 형식으로 변경, 단 지시문은 사용자가 적은 텍스트 그대로 적을것

- **했다:** 이 파일을 Q·했다·바꿨다·머지했다 형식으로 다시 쓰고, 지시문은 원문 그대로 옮겼다. main 최신 커밋(`8c2a33d`)에서 에뮬레이터 테스트 기록용 `test/emulator` 브랜치를 만들었다.
- **바꿨다:** `CHANGELOG.md` 형식. 원격에 `test/emulator` 브랜치가 생겼다. main은 그대로다.
- **머지했다:** 머지 안 함. `review/changelog` 브랜치에만 있음 (2026-11-12)

### Removed

#### review/feedback 브랜치 삭제

**Q: 내가 내린 지시** (대화, @kimgangan7890-spec)
> 에뮬레이터 파일, 그러니까 에뮬레이터 이 사용 후기라고 해야 되나? 아무튼 관련 문제사항 같은 거 적어두는 거는 기터브에 피드백 브랜치를 없애고 이 테스트 브랜치 하나 만들어서 그 브랜치에만 올릴 거야. 메인은 건들지 않아.

- **했다:** 공동 저장소에서 `review/feedback` 브랜치를 지웠다. FEEDBACK.md는 다른 브랜치로 옮기지 않았다(지시대로). 검수 지시 원문은 이 파일의 각 Q에 그대로 남아 있다.
- **바꿨다:** 원격 브랜치가 main, `review/changelog`, `test/emulator` 세 개가 됐다.
- **머지했다:** 해당 없음 (브랜치 삭제, 2026-11-12)

### 아직 반영되지 않은 지시

#### APK 크기 줄이기 (R8)

**Q: 내가 내린 지시** (FEEDBACK P2-10, @kimgangan7890-spec)
> **APK 29 MB** — `classes.dex` 18 MB, `classes6.dex` 10 MB. Pleos SDK가 끌고 오는 `kotlin-reflect`(약 1,900 클래스), Compose Material 2, Hilt, Gson 등이 그대로 들어갑니다. 제출용을 release + R8(`isMinifyEnabled = true`)로 빌드하면 크게 줄어듭니다. 단 SDK용 keep 규칙이 필요한지 확인해야 합니다.

- **했다:** release 빌드로는 바꿨지만(`0db8219`) R8은 켜지 않았다. SDK keep 규칙 확인이 먼저라 README 한계에 적어 둠.
- **바꿨다:** 아직 없음 (`isMinifyEnabled = false`, APK 약 22 MB)
- **머지했다:** 머지 안 함

#### 에뮬레이터 실기 확인

**Q: 내가 내린 지시** (FEEDBACK 실기 체크리스트, @kimgangan7890-spec)
> ## 실기 체크리스트 (에뮬레이터에서 10분)
>
> - [ ] 기어 D·속도 30 km/h에서 화면이 가려지지 않는가 (1번)
> - [ ] 속도 권한 허용 상태에서 가상 주행 → 급가속·급감속이 각 1회만 찍히는가 (2번)
> - [ ] 신호 연결 5개가 "수신 중"으로 바뀌고, Virtual Console 조작이 즉시 반영되는가 (3번)
> - [ ] 브레이크 밟은 채 기록 시작 → 떼기 → 요약의 브레이크 횟수·시간이 맞는가 (7번)
> - [ ] CSV 내보내기 버튼을 눌렀을 때 무언가 일어나는가 (5번)
> - [ ] 이전 빌드가 깔린 상태에서 새 APK `adb install -r`이 되는가 (4번)

- **했다:** README에 실기 체크리스트로 옮겨 두었다(#2). 실제 실행은 아직 아무도 안 했다. Pleos 에뮬레이터로 직접 확인하고 결과는 `test/emulator` 브랜치에 남길 예정.
- **바꿨다:** 아직 없음
- **머지했다:** 머지 안 함

## [0.2.0](https://github.com/hhgaq/hyundai-better-than-kia/compare/5351fe5...ecedac3) - 2026-11-11

### Added

#### 기어 기준 자동 기록

**Q: 내가 내린 지시** (지시 없음 — 작성자 판단, 접수양식과 앱 동작 맞추기)

- **했다:** 기어가 P에서 벗어나면 기록 시작, P로 돌아오면 종료·저장하게 했다. 가상 주행 중에는 적용하지 않는다. 테스트 3개 추가(총 16개).
- **바꿨다:** 자동 기록 동작, README 한 줄 소개를 접수양식과 같게 수정
- **머지했다:** PR #4로 main에 머지 (`ecedac3`, 2026-11-11, @hhgaq)

#### 기록 중 중간 저장

**Q: 내가 내린 지시** (FEEDBACK P1-6, @kimgangan7890-spec)
> ### 6. 기록 중 앱이 죽으면 그 주행이 통째로 사라짐
> - 위치: `DriveRepository.stopTrip()` — 저장은 종료 시 한 번만
> - README "알려진 한계"에 백그라운드 기록은 적혀 있지만, **화면이 켜져 있어도** 프로세스가 죽으면(크래시, 메모리 회수) 기록이 남지 않는다는 점은 빠져 있습니다.
> - 제안: 이벤트가 생길 때마다(또는 10초마다) 진행 중 파일에 덧붙여 쓰고, 다음 실행 때 미완료 기록을 복구. MVP라면 README 한계 항목에 한 줄 추가만으로도 충분합니다.

- **했다:** 기록 중 5초마다 중간 저장하고, 다음 실행 때 미완료 기록을 마감하게 했다. README 한계에도 적었다.
- **바꿨다:** `DriveRepository`, `TripStore`, README
- **머지했다:** PR #2로 main에 머지 (`9e4cb92`, 2026-11-10, @hhgaq)

#### 실기 체크리스트와 Vehicle 서비스 버전 확인 명령

**Q: 내가 내린 지시** (FEEDBACK P0-3, @kimgangan7890-spec)
> ### 3. Pleos Vehicle 서비스 최소 버전 요구 (실기 확인 필요)
> - 위치: APK 매니페스트(SDK가 자동 병합)
> - SDK가 `ai.pleos.playground.service.vehicle.min_version_name = 2.3.13`을 넣습니다. README는 "Pleos Connect Emulator 2.1.0"을 요구 사항으로 적었는데, 에뮬레이터 안 Vehicle 서비스가 2.3.13 미만이면 SDK 바인딩이 실패할 수 있습니다.
> - PR에도 "Pleos 에뮬레이터 실신호 수신은 아직 미확인"이라고 돼 있으니, 제출 전에 `adb shell dumpsys package ai.pleos.playground.service.vehicle | grep version`으로 버전을 확인하고 README에 적어 두세요.
> - 덧붙여 `PleosVehicleSource.start()`는 `vehicle.initialize()` 직후 바로 `register…`를 부릅니다. 초기화가 비동기 바인딩이면 첫 등록이 실패할 수 있으니, 실기에서 "연결 중"에서 멈추는지 같이 보세요.

- **했다:** README 실기 체크리스트에 버전 확인 명령을 넣었다. 실제 버전 값은 에뮬레이터에서 아직 확인하지 못해 적지 않았다.
- **바꿨다:** README, `PleosVehicleSource`
- **머지했다:** PR #2로 main에 머지 (`9e4cb92`, 2026-11-10, @hhgaq)

### Changed

#### 주행 중 화면 차단 방지와 주행 모드

**Q: 내가 내린 지시** (FEEDBACK P0-1, @kimgangan7890-spec)
> ### 1. 주행 중 앱 화면이 차단될 수 있음 (실기 확인 필요)
> - 위치: `app/src/main/AndroidManifest.xml` `MainActivity`
> - APK 매니페스트에 `distractionOptimized` 메타데이터가 없습니다.
> - Android Automotive는 차가 움직이거나 기어가 P가 아니면, 주행 중 사용 가능(DO)으로 표시되지 않은 액티비티를 "운전 중 사용 불가" 화면으로 가립니다. 이 앱은 **주행 중에 켜져 있어야 의미가 있는 앱**이라 치명적입니다.
> - 확인 방법: 에뮬레이터 Virtual Console에서 기어 D, 속도 30 km/h로 바꾼 뒤 화면이 가려지는지 봅니다.
> - 가려진다면 액티비티에 `<meta-data android:name="distractionOptimized" android:value="true"/>`를 넣고, Pleos 쪽 DO 허용 정책(마켓 심사 시 화이트리스트 여부)을 문서에서 확인하세요.
> - 함께: `<uses-feature android:name="android.hardware.type.automotive" android:required="true"/>`도 없습니다. 차량 전용 앱이면 넣는 게 표준입니다.

- **했다:** 매니페스트에 `distractionOptimized`와 차량 전용 `uses-feature`를 넣었다. 속도 5 km/h를 넘으면 큰 요약 숫자만 보이는 주행 모드를 추가했다.
- **바꿨다:** `AndroidManifest.xml`, `DriveLogScreen`. 이 선언 때문에 일반 핸드폰에는 설치되지 않는다.
- **머지했다:** PR #2로 main에 머지 (`9e4cb92`, 2026-11-10, @hhgaq)

#### 고정 서명 키와 release APK

**Q: 내가 내린 지시** (FEEDBACK P1-4, @kimgangan7890-spec)
> ### 4. 빌드마다 서명 키가 달라져 덮어쓰기 설치가 실패
> - 위치: `.github/workflows/publish-apk.yml`
> - APK는 `CN=Android Debug` 디버그 키로 서명돼 있습니다(v2 서명만, `debuggable=true`). GitHub 러너는 매번 새 디버그 키를 만들기 때문에, 다음 빌드 APK를 기존 설치 위에 `adb install -r` 하면 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`이 납니다. `versionCode`도 계속 1입니다.
> - 심사위원이 이전 버전을 깔아 둔 경우 그대로 막힙니다. 저장소 Secrets에 고정 키스토어를 넣어 서명하거나, README 설치 안내에 `adb uninstall com.hhgaq.drivelog` 먼저 하라고 적어 두세요.
> - `build.gradle.kts` 주석에 "Pleos Playground requires signing key rotation / lineage"가 있는데, 그렇다면 더더욱 고정 키가 필요합니다.

- **했다:** 저장소에 고정 디버그 키를 두고 versionCode를 CI 실행 번호로 바꿨다. 이후 제출 규정에 맞춰 release 키(`app/submission.keystore`)로 서명하는 release APK로 바꿨다. README에 `adb uninstall` 안내를 넣었다.
- **바꿨다:** `app/build.gradle.kts`, `app/debug.keystore`, `app/submission.keystore`, `publish-apk.yml`, README. APK 이름은 `apk/세교동세얼간이_나의운전저장소_release.apk`
- **머지했다:** 고정 키는 PR #2로 main에 머지 (`9e4cb92`, 2026-11-10). release APK는 PR #3을 닫고 main에 직접 push (`0db8219`, 2026-11-10, @hhgaq)

#### 터치 영역·글자 크기

**Q: 내가 내린 지시** (FEEDBACK P1-8, @kimgangan7890-spec)
> ### 8. 터치 영역·글자 크기
> - 위치: `DriveLogScreen.kt`
> - 버튼 높이 56dp, 이벤트 목록 14~16sp, 신호 연결 13sp입니다. 차량 UI 가이드는 터치 대상 최소 76dp, 본문 글자도 휴대폰보다 크게 권장합니다. 운전 중 볼 화면이라 심사에서 지적받기 쉬운 부분입니다.
> - 주행 중에는 이벤트 목록 대신 큰 상태 타일만 보여 주고, 목록은 정차(P) 때만 보여 주는 방식도 검토해 보세요(1번 운전 중 주의 분산 이슈와도 연결됩니다).

- **했다:** 버튼 터치 높이를 76dp, 글자를 16~22sp로 키웠다. 주행 모드는 P0-1과 함께 넣었다.
- **바꿨다:** `DriveLogScreen`
- **머지했다:** PR #2로 main에 머지 (`9e4cb92`, 2026-11-10, @hhgaq)

#### 권한 요청 방식

**Q: 내가 내린 지시** (FEEDBACK P1-9, @kimgangan7890-spec)
> ### 9. 권한 요청이 화면 재생성마다 반복
> - 위치: `MainActivity.onCreate()`
> - 속도 권한을 거부하면 `onCreate`가 다시 불릴 때마다(설정 변경, 재진입) 권한 요청이 다시 뜹니다. "다시 묻지 않음" 이후에는 조용히 실패하므로, 신호 연결 칸의 "권한 필요"를 눌러 설정으로 가는 경로가 있으면 좋습니다.

- **했다:** 속도 권한은 처음 한 번만 요청하고, "권한 필요"를 누르면 다시 요청하거나 설정으로 가게 했다.
- **바꿨다:** `MainActivity`, `DriveLogScreen`
- **머지했다:** PR #2로 main에 머지 (`9e4cb92`, 2026-11-10, @hhgaq)

#### "수신 중" 표시 시점

**Q: 내가 내린 지시** (FEEDBACK P2-11, @kimgangan7890-spec)
> **"수신 중" 표시가 실제 수신 전에 뜸** — `CarPropertySource.subscribe()`는 콜백 등록 성공만으로 "수신 중"을 표시합니다. 첫 값이 들어왔을 때 바꾸면 심사위원이 신호 연결을 더 믿을 수 있습니다.

- **했다:** 첫 값을 받았을 때만 "수신 중"으로 바꾸고, 그 전은 "등록됨 (값 대기)"로 표시하게 했다.
- **바꿨다:** `CarPropertySource`, `PleosVehicleSource`
- **머지했다:** PR #2로 main에 머지 (`9e4cb92`, 2026-11-10, @hhgaq)

#### 요약 재계산 주기

**Q: 내가 내린 지시** (FEEDBACK P2-14, @kimgangan7890-spec)
> **매 속도 샘플(10 Hz)마다 요약 전체 재계산** — `DriveRepository.onLiveState`에서 이벤트 목록 전체를 세 번씩 순회합니다. 긴 주행에서 UI가 무거워질 수 있으니 카운터를 누적하거나 이벤트 발생 시에만 갱신하세요.

- **했다:** 주행 요약 재계산을 1초에 한 번으로 줄였다.
- **바꿨다:** `DriveRepository`
- **머지했다:** PR #2로 main에 머지 (`9e4cb92`, 2026-11-10, @hhgaq)

#### APK 빌드 워크플로 동시 실행

**Q: 내가 내린 지시** (FEEDBACK P2-15, @kimgangan7890-spec)
> **워크플로 동시 실행** — `publish-apk.yml`에 `concurrency` 그룹이 없고 `git push` 전에 pull/rebase가 없습니다. mvp에 연속 푸시하면 두 번째 잡이 push 거부로 실패합니다. APK 바이너리를 매번 커밋하면 저장소도 계속 커지니, 제출 규정이 허용하면 Releases 첨부도 고려하세요.

- **했다:** `concurrency` 그룹과 push 전 rebase를 넣었다. Releases 첨부는 하지 않았다(제출 규정상 `apk/` 폴더에 둬야 함).
- **바꿨다:** `publish-apk.yml`
- **머지했다:** PR #2로 main에 머지 (`9e4cb92`, 2026-11-10, @hhgaq)

#### 팀명·앱 이름 확정

**Q: 내가 내린 지시** (지시 없음 — 작성자 판단)

- **했다:** 임시값이던 팀명·앱 이름을 확정했다.
- **바꿨다:** `gradle.properties`의 `submission.teamName`, `submission.appName` → 세교동세얼간이_나의운전저장소
- **머지했다:** main에 직접 push (`a8376a7`, 2026-11-10, @hhgaq)

### Fixed

#### 가상 주행과 실제 신호가 섞이던 문제

**Q: 내가 내린 지시** (FEEDBACK P0-2, @kimgangan7890-spec)
> ### 2. 가상 주행과 실제 속도 신호가 같은 기록기에 섞임
> - 위치: `DemoDriver.kt`, `CarPropertySource.kt:37`, `DriveRepository.kt:86-88`
> - 가상 주행은 `recorder.onSpeed()`를 직접 부르는데, 속도 권한을 허용한 상태면 `PERF_VEHICLE_SPEED`도 10 Hz로 같은 `recorder.onSpeed()`에 들어옵니다. 에뮬레이터에서 실제 속도는 0이므로 **40 → 0 → 40.4 → 0 …** 식으로 번갈아 들어갑니다.
> - 결과: 1초 창 가속도 계산이 깨져 급가속·급감속이 거짓으로 찍히고, 거리·최고 속도도 틀어집니다. PR 본문의 "가상 주행 결과 브레이크 4, 급가속 1, 급감속 1"은 단위 테스트 환경 결과라 실기와 다를 가능성이 큽니다.
> - 같은 문제가 브레이크·깜빡이에도 있습니다(실제 신호 변화가 오면 데모 상태를 덮어씀). 기어도 데모가 끝나면 "P"로 남습니다.
> - 제안: 가상 주행 중에는 실제 신호를 무시하는 플래그(예: `DriveRecorder`에 `source` 구분, 또는 데모 전용 recorder 인스턴스)를 두세요. 시연의 핵심 경로라 우선순위가 높습니다.

- **했다:** `DriveRecorder.Input.REAL/DEMO`로 신호 출처를 나눠, 가상 주행 중에는 실제 신호를 무시하고 끝나면 마지막 실제 값으로 복원하게 했다. 테스트 추가.
- **바꿨다:** `DriveRecorder`, `DemoDriver`, `DriveRepository`
- **머지했다:** PR #2로 main에 머지 (`9e4cb92`, 2026-11-10, @hhgaq)

#### 기록 시작 전부터 켜져 있던 입력

**Q: 내가 내린 지시** (FEEDBACK P1-7, @kimgangan7890-spec)
> ### 7. 기록 시작 시점에 이미 밟고 있던 브레이크·켜진 깜빡이
> - 위치: `DriveRecorder.startTrip()`, `onBrake()`, `onTurn()`
> - 기록 시작 전부터 브레이크를 밟고 있으면 `BRAKE_ON` 없이 `BRAKE_OFF`만 남고, `durationMs`에 기록 시작 전 시간까지 포함돼 `brakeTotalMs`가 부풀려집니다. 깜빡이도 같습니다.
> - 실제 시연 흐름(P에서 브레이크 밟은 채 기록 시작)과 정확히 겹치는 상황입니다. `startTrip()`에서 현재 상태가 켜져 있으면 시작 이벤트를 하나 찍고 `sinceMs`를 기록 시작 시각으로 맞추는 게 자연스럽습니다. 테스트 케이스 하나 추가를 권합니다.

- **했다:** 기록 시작 때 이미 켜진 입력은 켬 이벤트를 남기고, 유지 시간은 기록 시작 시각부터 계산하게 했다. 테스트 추가.
- **바꿨다:** `DriveRecorder`
- **머지했다:** PR #2로 main에 머지 (`9e4cb92`, 2026-11-10, @hhgaq)

#### CSV 한글 깨짐·공유 대상 없음

**Q: 내가 내린 지시** (FEEDBACK P1-5, @kimgangan7890-spec)
> ### 5. CSV 내보내기: 한글 깨짐 + 차량에 공유 대상이 없을 가능성
> - 위치: `TripStore.toCsv()`, `MainActivity.shareCsv()`
> - CSV에 UTF-8 BOM이 없어 엑셀에서 열면 `detail`의 한글(왼쪽/오른쪽, 페달 %)이 깨집니다. 앞에 `﻿`를 붙이면 해결됩니다.
> - `ACTION_SEND`로 텍스트를 보내는데, 차량 OS에는 받을 앱(메일·메신저)이 없는 경우가 많습니다. `runCatching`으로 예외를 삼켜서 **버튼을 눌러도 아무 반응이 없게** 보입니다. 실기 확인 후, 공유 대상이 없으면 파일 저장 또는 토스트 안내라도 띄우세요.
> - 쉼표가 든 `detail`은 따옴표로 감싸서 괜찮지만, 시간 열이 epoch ms라 사람이 읽기 어렵습니다. `HH:mm:ss.SSS` 열을 하나 추가하는 걸 권합니다.

- **했다:** CSV 앞에 UTF-8 BOM을 붙이고 읽을 수 있는 시각 열을 추가했다. 공유할 앱이 없으면 파일로 저장하고 토스트로 위치를 알려 주게 했다.
- **바꿨다:** `TripStore`, `MainActivity`
- **머지했다:** PR #2로 main에 머지 (`9e4cb92`, 2026-11-10, @hhgaq)

#### 신호 끊김을 브레이크 뗌으로 기록하던 문제

**Q: 내가 내린 지시** (FEEDBACK P2-12, @kimgangan7890-spec)
> **신호 끊김을 브레이크 뗌으로 처리** — `onBrake(null)`이 이전 값 true일 때 `BRAKE_OFF`를 남깁니다. 끊김은 별도 이벤트(또는 무시)로 두는 편이 기록이 정확합니다.

- **했다:** null 값은 무시하게 했다. 테스트 추가.
- **바꿨다:** `DriveRecorder`
- **머지했다:** PR #2로 main에 머지 (`9e4cb92`, 2026-11-10, @hhgaq)

#### 드문 속도 샘플로 가속도가 틀리던 문제

**Q: 내가 내린 지시** (FEEDBACK P2-13, @kimgangan7890-spec)
> **속도 샘플이 드문 경우 가속도 창이 1초를 넘음** — `speedSamples`를 최소 2개 유지하므로 샘플 간격이 5초면 5초 평균으로 계산됩니다. 창보다 긴 구간은 가속도를 `null`로 두는 게 안전합니다.

- **했다:** 구간이 창의 2배를 넘으면 가속도를 계산하지 않게 했다. 테스트 추가.
- **바꿨다:** `DriveRecorder`
- **머지했다:** PR #2로 main에 머지 (`9e4cb92`, 2026-11-10, @hhgaq)

## [0.1.0](https://github.com/hhgaq/hyundai-better-than-kia/tree/5351fe5) - 2026-11-09

### Added

#### 드라이브 로그 MVP (최초 릴리스)

**Q: 내가 내린 지시** (지시 없음 — 작성자 판단)

- **했다:** 브레이크·방향지시등(Pleos Vehicle SDK), 속도·기어·가속 페달(AAOS) 신호를 받아 주행 기록으로 남기는 앱을 만들었다. 가속 페달 값이 없으면 속도 변화로 가속·급가속·급감속을 판단한다. 가상 주행 시나리오, 기록 저장·요약·지난 기록·CSV 내보내기, 단위 테스트 7개.
- **바꿨다:** 앱 전체 신규 작성
- **머지했다:** PR #1로 main에 머지 (`0546056`, 2026-11-09, @hhgaq)

#### APK 자동 빌드

**Q: 내가 내린 지시** (지시 없음 — 작성자 판단)

- **했다:** `main`에 push하면 테스트 후 APK를 빌드해 `apk/`에 자동 커밋하는 GitHub Actions를 만들었다.
- **바꿨다:** `.github/workflows/build.yml`, `publish-apk.yml`
- **머지했다:** main에 직접 push (`5351fe5`, 2026-11-09, @hhgaq)
