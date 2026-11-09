package com.hhgaq.drivelog.log

/** 운전자 입력/주행 이벤트 종류 */
enum class EventType(val label: String) {
    BRAKE_ON("브레이크 밟음"),
    BRAKE_OFF("브레이크 뗌"),
    TURN_LEFT("왼쪽 깜빡이 켬"),
    TURN_RIGHT("오른쪽 깜빡이 켬"),
    TURN_OFF("깜빡이 끔"),
    ACCEL_ON("가속 시작"),
    ACCEL_OFF("가속 끝"),
    HARSH_ACCEL("급가속"),
    HARSH_BRAKE("급감속"),
    GEAR("기어 변경"),
    TRIP_START("기록 시작"),
    TRIP_END("기록 종료"),
}

enum class Turn { OFF, LEFT, RIGHT }

/** 가속 판단 근거: 실제 페달 값(Android 16+ 차량) 또는 속도 변화 추정 */
enum class AccelSource(val label: String) { NONE("없음"), PEDAL("가속 페달"), SPEED("속도 변화 추정") }

data class DriveEvent(
    val timeMs: Long,
    val type: EventType,
    /** 이벤트 시점 속도(km/h). 속도 신호가 없으면 null */
    val speedKmh: Float? = null,
    /** 직전 상태가 유지된 시간(예: 브레이크를 밟고 있던 시간) */
    val durationMs: Long? = null,
    val detail: String = "",
)

/** 화면에 보여줄 실시간 차량 상태 */
data class LiveState(
    val brake: Boolean? = null,
    val turn: Turn? = null,
    val speedKmh: Float? = null,
    val gear: String? = null,
    val accelerating: Boolean = false,
    val pedalPercent: Float? = null,
    val accelMs2: Float? = null,
    val accelSource: AccelSource = AccelSource.NONE,
)

/** 한 번의 기록(주행) 요약 */
data class TripSummary(
    val startMs: Long,
    val endMs: Long?,
    val brakeCount: Int,
    val brakeTotalMs: Long,
    val leftTurnCount: Int,
    val rightTurnCount: Int,
    val accelCount: Int,
    val harshAccelCount: Int,
    val harshBrakeCount: Int,
    val maxSpeedKmh: Float,
    val distanceKm: Double,
) {
    companion object {
        /** 이벤트 목록으로 횟수를 세고, 거리·최고속도는 속도 샘플 전체로 계산한 값을 받는다 */
        fun from(
            events: List<DriveEvent>,
            startMs: Long,
            endMs: Long?,
            distanceKm: Double,
            maxSpeedKmh: Float,
        ): TripSummary {
            return TripSummary(
                startMs = startMs,
                endMs = endMs,
                brakeCount = events.count { it.type == EventType.BRAKE_ON },
                brakeTotalMs = events.filter { it.type == EventType.BRAKE_OFF }.sumOf { it.durationMs ?: 0L },
                leftTurnCount = events.count { it.type == EventType.TURN_LEFT },
                rightTurnCount = events.count { it.type == EventType.TURN_RIGHT },
                accelCount = events.count { it.type == EventType.ACCEL_ON },
                harshAccelCount = events.count { it.type == EventType.HARSH_ACCEL },
                harshBrakeCount = events.count { it.type == EventType.HARSH_BRAKE },
                maxSpeedKmh = maxSpeedKmh,
                distanceKm = distanceKm,
            )
        }
    }
}
