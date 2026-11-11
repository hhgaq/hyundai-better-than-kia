# Changelog

이 프로젝트의 주요 변경 사항을 기록합니다.
형식: [Keep a Changelog](https://keepachangelog.com/ko/1.1.0/)
버전 규칙: [Semantic Versioning](https://semver.org/lang/ko/) (MAJOR.MINOR.PATCH)

<!--
작성 규칙
- 최신 버전이 맨 위로 오게 쓴다.
- 작업 중인 변경은 [Unreleased]에 모았다가, 릴리스할 때 버전 번호와 날짜를 붙여 옮긴다.
- 날짜는 YYYY-MM-DD 형식으로 쓴다.
- 한 줄에 변경 하나씩, 사용자나 팀원이 알아야 할 내용만 적는다. 커밋 메시지를 그대로 복사하지 않는다.
- 관련 PR·이슈 번호와 작성자를 줄 끝에 붙인다. 예: (#12, @강한)
- 지시를 받아 한 변경은 바로 아래에 지시문을 한 줄 붙인다.
  형식: > 지시 [출처 번호] @지시자: "지시 내용"
  출처: FEEDBACK = review/feedback 브랜치의 FEEDBACK.md (P0-1 = P0의 1번)
- 쓰지 않는 분류 항목은 지운다.

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

아직 반영되지 않은 지시:

> 지시 [FEEDBACK P2-10] @kimgangan7890-spec: "제출용을 release + R8(`isMinifyEnabled = true`)로 빌드하면 크게 줄어듭니다. 단 SDK용 keep 규칙이 필요한지 확인해야 합니다."

> 지시 [FEEDBACK P0-3] @kimgangan7890-spec: "제출 전에 `adb shell dumpsys package ai.pleos.playground.service.vehicle | grep version`으로 버전을 확인하고 README에 적어 두세요." — 확인 명령은 README에 들어갔고, 실제 버전 값은 아직 안 적힘

> 지시 [FEEDBACK 실기 체크리스트] @kimgangan7890-spec: "에뮬레이터에서 10분" — 아직 아무도 실행하지 않음

## [0.2.0](https://github.com/hhgaq/hyundai-better-than-kia/compare/5351fe5...ecedac3) - 2026-11-11

### Added

- 기어를 P에서 옮기면 기록 시작, P에 넣으면 종료·저장하는 자동 기록. 가상 주행 중에는 적용하지 않음 (#4, @hhgaq)
- 속도 5 km/h를 넘으면 큰 요약 숫자만 보이는 주행 모드 (#2, @hhgaq)
  > 지시 [FEEDBACK P1-8] @kimgangan7890-spec: "주행 중에는 이벤트 목록 대신 큰 상태 타일만 보여 주고, 목록은 정차(P) 때만 보여 주는 방식도 검토해 보세요"
- 기록 중 5초마다 중간 저장, 비정상 종료 뒤 다음 실행 때 미완료 기록 마감 (#2, @hhgaq)
  > 지시 [FEEDBACK P1-6] @kimgangan7890-spec: "이벤트가 생길 때마다(또는 10초마다) 진행 중 파일에 덧붙여 쓰고, 다음 실행 때 미완료 기록을 복구"
- CSV에 사람이 읽을 수 있는 시각 열 추가 (#2, @hhgaq)
  > 지시 [FEEDBACK P1-5] @kimgangan7890-spec: "`HH:mm:ss.SSS` 열을 하나 추가하는 걸 권합니다."
- README 실기 체크리스트와 Vehicle 서비스 버전 확인 명령 (#2, @hhgaq)
  > 지시 [FEEDBACK P0-3] @kimgangan7890-spec: "버전을 확인하고 README에 적어 두세요."

### Changed

- 주행 중에도 화면이 가려지지 않도록 `distractionOptimized` 선언, 차량 전용 `uses-feature` 추가 (#2, @hhgaq)
  > 지시 [FEEDBACK P0-1] @kimgangan7890-spec: "액티비티에 `<meta-data android:name="distractionOptimized" android:value="true"/>`를 넣고 … `uses-feature`도 없습니다. 차량 전용 앱이면 넣는 게 표준입니다."
- 제출용 APK를 release 빌드로 바꾸고 `apk/세교동세얼간이_나의운전저장소_release.apk`로 내보냄. 공모전 제출 규정 반영 (#3, @hhgaq)
- 서명 키를 저장소에 고정해 덮어쓰기 설치가 되게 함. versionCode는 CI 실행 번호. 이전 버전 설치 기기는 한 번 `adb uninstall` 필요 (#2, #3, @hhgaq)
  > 지시 [FEEDBACK P1-4] @kimgangan7890-spec: "저장소 Secrets에 고정 키스토어를 넣어 서명하거나, README 설치 안내에 `adb uninstall com.hhgaq.drivelog` 먼저 하라고 적어 두세요."
- 버튼 터치 높이 76dp, 글자 16~22sp로 키움 (#2, @hhgaq)
  > 지시 [FEEDBACK P1-8] @kimgangan7890-spec: "차량 UI 가이드는 터치 대상 최소 76dp, 본문 글자도 휴대폰보다 크게 권장합니다."
- 속도 권한은 처음 한 번만 요청하고, "권한 필요"를 누르면 다시 요청하거나 설정으로 이동 (#2, @hhgaq)
  > 지시 [FEEDBACK P1-9] @kimgangan7890-spec: "신호 연결 칸의 '권한 필요'를 눌러 설정으로 가는 경로가 있으면 좋습니다."
- 신호 연결은 첫 값을 받았을 때만 "수신 중", 그 전은 "등록됨 (값 대기)"로 표시 (#2, @hhgaq)
  > 지시 [FEEDBACK P2-11] @kimgangan7890-spec: "첫 값이 들어왔을 때 바꾸면 심사위원이 신호 연결을 더 믿을 수 있습니다."
- 주행 요약 재계산을 1초에 한 번으로 줄임 (#2, @hhgaq)
  > 지시 [FEEDBACK P2-14] @kimgangan7890-spec: "카운터를 누적하거나 이벤트 발생 시에만 갱신하세요."
- APK 빌드 워크플로에 동시 실행 방지와 push 전 rebase 추가 (#2, @hhgaq)
  > 지시 [FEEDBACK P2-15] @kimgangan7890-spec: "`publish-apk.yml`에 `concurrency` 그룹이 없고 `git push` 전에 pull/rebase가 없습니다."
- 팀명·앱 이름을 세교동세얼간이_나의운전저장소로 확정 (@hhgaq)
- README 한 줄 소개를 접수양식과 같게 맞춤 (#4, @hhgaq)

### Fixed

- 가상 주행 중 실제 차량 신호가 섞여 급가속·급감속이 거짓으로 찍히던 문제. 가상 주행이 끝나면 마지막 실제 값으로 복원 (#2, @hhgaq)
  > 지시 [FEEDBACK P0-2] @kimgangan7890-spec: "가상 주행 중에는 실제 신호를 무시하는 플래그 … 를 두세요. 시연의 핵심 경로라 우선순위가 높습니다."
- 기록 시작 전부터 밟고 있던 브레이크·켜 둔 깜빡이가 켬 이벤트 없이 남고 유지 시간이 부풀려지던 문제 (#2, @hhgaq)
  > 지시 [FEEDBACK P1-7] @kimgangan7890-spec: "`startTrip()`에서 현재 상태가 켜져 있으면 시작 이벤트를 하나 찍고 `sinceMs`를 기록 시작 시각으로 맞추는 게 자연스럽습니다. 테스트 케이스 하나 추가를 권합니다."
- CSV를 엑셀에서 열면 한글이 깨지던 문제(UTF-8 BOM), 공유할 앱이 없을 때 아무 반응이 없던 문제(파일 저장 후 위치 안내) (#2, @hhgaq)
  > 지시 [FEEDBACK P1-5] @kimgangan7890-spec: "공유 대상이 없으면 파일 저장 또는 토스트 안내라도 띄우세요."
- 신호가 끊겼을 때 브레이크를 뗀 것으로 기록되던 문제 (#2, @hhgaq)
  > 지시 [FEEDBACK P2-12] @kimgangan7890-spec: "끊김은 별도 이벤트(또는 무시)로 두는 편이 기록이 정확합니다."
- 속도 샘플이 드물 때 가속도가 1초보다 긴 구간 평균으로 계산되던 문제 (#2, @hhgaq)
  > 지시 [FEEDBACK P2-13] @kimgangan7890-spec: "창보다 긴 구간은 가속도를 `null`로 두는 게 안전합니다."

## [0.1.0](https://github.com/hhgaq/hyundai-better-than-kia/tree/5351fe5) - 2026-11-09

### Added

- 최초 릴리스: 브레이크·방향지시등(Pleos Vehicle SDK), 속도·기어·가속 페달(AAOS) 신호 수신 (#1, @hhgaq)
- 가속 페달 값이 없는 차량은 속도 변화로 가속·급가속·급감속 판단 (#1, @hhgaq)
- 주행 기록 저장, 주행 요약, 지난 기록, CSV 내보내기 (#1, @hhgaq)
- 차량 입력 없이 시연하는 가상 주행 시나리오 (#1, @hhgaq)
- `main` 푸시 때 테스트 후 APK를 빌드해 `apk/`에 자동 커밋하는 GitHub Actions (@hhgaq)
