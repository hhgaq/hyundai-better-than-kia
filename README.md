# 드라이브 로그 (Drive Log)

운전자의 **브레이크, 가속(엑셀), 방향지시등(깜빡이)** 입력을 실시간으로 인식해 주행 기록으로 남기는 Pleos Connect 차량용 앱입니다.
현대자동차그룹 차량용 앱 개발 공모전 제출용 MVP입니다.

## 기능

- 실시간 상태 표시: 속도, 기어, 브레이크, 좌/우 깜빡이, 가속 여부
- 주행 기록: 기록 시작부터 종료까지 입력 이벤트를 시간순으로 저장합니다
  - 브레이크 밟음/뗌과 밟고 있던 시간
  - 깜빡이 켬/끔, 방향, 켜져 있던 시간
  - 가속 시작/끝, 급가속, 급감속
  - 기어 변경
- 주행 요약: 거리, 최고 속도, 브레이크 횟수, 깜빡이 횟수, 급가속·급감속 횟수
- 지난 기록 목록과 상세 보기, CSV 내보내기, 삭제
- **가상 주행**: 차량 입력 없이 출발 → 차선 변경 → 급가속 → 급제동 → 우회전 → 정차 시나리오를 재생해 기록 기능을 확인합니다

## 신호를 어디서 받나

| 신호 | 출처 | 권한 |
|---|---|---|
| 브레이크 페달 (밟음/뗌) | Pleos Vehicle SDK `Brake.registerBrakePedalStatus` | `pleos.car.permission.CAR_DRIVE` |
| 방향지시등 (좌/우/끔) | Pleos Vehicle SDK `TurnSignal.registerTurnSignalState` | `pleos.car.permission.CAR_LIGHTS` |
| 차량 속도 | AAOS `CarPropertyManager` `PERF_VEHICLE_SPEED` | `android.car.permission.CAR_SPEED` (실행 중 허용) |
| 기어 | AAOS `GEAR_SELECTION` (Pleos SDK의 Gear API는 deprecated) | `android.car.permission.CAR_POWERTRAIN` |
| 가속 페달 | AAOS `ACCELERATOR_PEDAL_COMPRESSION_PERCENTAGE` (Android 16 이상 차량) | `android.car.permission.CAR_POWERTRAIN` |

**엑셀(가속) 처리**: Pleos Vehicle SDK 2.3.2에는 가속 페달 API가 없습니다. 차량이 가속 페달 값을 주면 그 값을 쓰고(5% 이상이면 가속),
없으면 최근 1초간의 속도 변화로 가속도를 계산해 판단합니다.

| 판단 | 기준 |
|---|---|
| 가속 시작 / 끝 | 0.8 m/s² 이상 / 0.3 m/s² 미만 |
| 급가속 | 3.0 m/s² 이상 (약 1초에 11 km/h) |
| 급감속 | -3.5 m/s² 이하 |

기준값은 `DriveRecorder.Config`에서 바꿀 수 있습니다. 급가속·급감속은 한 구간에 한 번만 기록됩니다.

## 빌드와 실행

요구 사항: Android Studio, JDK 17, Pleos Connect Emulator 2.1.0 (API 34) — [설치 가이드](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)

```bash
./gradlew assembleDebug        # APK: app/build/outputs/apk/debug/
./gradlew installDebug         # 실행 중인 Pleos 에뮬레이터에 설치
./gradlew testDebugUnitTest    # 기록 로직 단위 테스트
```

GitHub에 푸시하면 Actions가 테스트와 APK 빌드를 자동으로 하고, 결과 APK를 `drive-log-debug-apk` 아티팩트로 올립니다.

## 확인 방법

1. 앱을 실행하고 차량 속도 권한을 허용합니다.
2. 왼쪽 아래 **신호 연결**에서 각 신호가 `수신 중`인지 확인합니다.
3. **가상 주행**을 누르면 약 40초 동안 시나리오가 재생되고, 끝나면 자동으로 저장되어 **지난 기록**에 나타납니다.
4. 실제 신호는 **기록 시작**을 누른 뒤 에뮬레이터의 Virtual Console에서 브레이크·깜빡이·속도를 바꿔 확인합니다.

## 구조

```
app/src/main/java/com/hhgaq/drivelog/
├── log/
│   ├── DriveEvent.kt         이벤트·실시간 상태·요약 모델
│   ├── DriveRecorder.kt      신호 → 상태/이벤트 변환 (안드로이드 의존성 없음, 단위 테스트 대상)
│   └── TripStore.kt          주행 1건 = JSON 파일 1개, CSV 내보내기
├── vehicle/
│   ├── PleosVehicleSource.kt 브레이크·깜빡이 (Pleos Vehicle SDK)
│   ├── CarPropertySource.kt  속도·기어·가속 페달 (AAOS Car API)
│   └── DemoDriver.kt         가상 주행 시나리오
├── ui/                        Compose 화면
├── DriveRepository.kt         앱 전체 상태
└── MainActivity.kt
```

## 알려진 한계 (MVP)

- 앱이 실행 중일 때만 기록합니다. 백그라운드 기록(포그라운드 서비스)은 다음 단계입니다.
- 가속 페달 값이 없는 차량에서는 가속을 속도 변화로 추정하므로, 오르막·내리막에서는 실제 페달 조작과 다를 수 있습니다.
- Pleos App Market 배포 시에는 사용하는 권한을 사전에 협의·승인받아야 합니다.
