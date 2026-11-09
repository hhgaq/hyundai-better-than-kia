package com.hhgaq.drivelog.log

import kotlin.math.abs

/**
 * 차량 신호를 받아 실시간 상태를 갱신하고, 기록 중일 때 이벤트를 남긴다.
 *
 * 안드로이드 의존성이 없는 순수 Kotlin 클래스라서 단위 테스트로 검증할 수 있다.
 * 신호 콜백은 여러 스레드에서 올 수 있으므로 모든 진입점은 동기화한다.
 */
class DriveRecorder(
    private val clock: () -> Long = System::currentTimeMillis,
    private val listener: Listener,
    private val config: Config = Config(),
) {
    interface Listener {
        fun onLiveState(state: LiveState)
        fun onEvent(event: DriveEvent)
    }

    data class Config(
        /** 속도 추정 가속: 이 값 이상이면 가속 시작 (m/s²) */
        val accelOnMs2: Float = 0.8f,
        /** 속도 추정 가속: 이 값 미만이면 가속 끝 (m/s²) */
        val accelOffMs2: Float = 0.3f,
        /** 급가속 기준 (m/s²). 약 10.8 km/h를 1초 안에 올리는 정도 */
        val harshAccelMs2: Float = 3.0f,
        /** 급감속 기준 (m/s², 음수) */
        val harshBrakeMs2: Float = -3.5f,
        /** 가속도 계산에 쓰는 속도 샘플 구간 (ms) */
        val accelWindowMs: Long = 1000,
        /** 가속도를 계산하려면 필요한 최소 구간 (ms) */
        val accelMinSpanMs: Long = 400,
        /** 급가속/급감속을 연속으로 다시 기록하지 않는 시간 (ms) */
        val harshCooldownMs: Long = 3000,
        /** 페달 밟음 판단 (%) */
        val pedalOnPercent: Float = 5f,
        /** 페달 뗌 판단 (%) */
        val pedalOffPercent: Float = 2f,
    )

    data class Trip(val events: List<DriveEvent>, val summary: TripSummary)

    private var state = LiveState()

    private var brakeSinceMs: Long? = null
    private var turnSinceMs: Long? = null
    private var accelSinceMs: Long? = null
    private var lastHarshAccelMs = Long.MIN_VALUE / 2
    private var lastHarshBrakeMs = Long.MIN_VALUE / 2
    private var harshAccelArmed = true
    private var harshBrakeArmed = true
    private var pedalAvailable = false

    private val speedSamples = ArrayDeque<Pair<Long, Float>>() // (timeMs, m/s)

    var isRecording = false
        private set
    private var tripStartMs = 0L
    private val tripEvents = mutableListOf<DriveEvent>()
    private var distanceM = 0.0
    private var maxSpeedKmh = 0f
    private var lastDistSample: Pair<Long, Float>? = null

    @Synchronized
    fun currentState(): LiveState = state

    @Synchronized
    fun currentEvents(): List<DriveEvent> = tripEvents.toList()

    @Synchronized
    fun currentSummary(): TripSummary? =
        if (isRecording) TripSummary.from(tripEvents, tripStartMs, null, distanceM / 1000.0, maxSpeedKmh) else null

    // ---------------------------------------------------------------- trip

    @Synchronized
    fun startTrip() {
        if (isRecording) return
        tripEvents.clear()
        distanceM = 0.0
        maxSpeedKmh = 0f
        lastDistSample = null
        tripStartMs = clock()
        isRecording = true
        emit(EventType.TRIP_START)
    }

    @Synchronized
    fun stopTrip(): Trip? {
        if (!isRecording) return null
        emit(EventType.TRIP_END)
        isRecording = false
        val events = tripEvents.toList()
        return Trip(events, TripSummary.from(events, tripStartMs, clock(), distanceM / 1000.0, maxSpeedKmh))
    }

    // ---------------------------------------------------------------- signals

    @Synchronized
    fun onBrake(pressed: Boolean?) {
        val prev = state.brake
        if (pressed == prev) return
        update(state.copy(brake = pressed))
        val now = clock()
        if (pressed == true) {
            brakeSinceMs = now
            emit(EventType.BRAKE_ON)
        } else if (prev == true) {
            emit(EventType.BRAKE_OFF, durationMs = brakeSinceMs?.let { now - it })
            brakeSinceMs = null
        }
    }

    @Synchronized
    fun onTurn(turn: Turn?) {
        val prev = state.turn
        if (turn == prev) return
        update(state.copy(turn = turn))
        val now = clock()
        when (turn) {
            Turn.LEFT, Turn.RIGHT -> {
                turnSinceMs = now
                emit(if (turn == Turn.LEFT) EventType.TURN_LEFT else EventType.TURN_RIGHT)
            }
            Turn.OFF, null -> if (prev == Turn.LEFT || prev == Turn.RIGHT) {
                emit(
                    EventType.TURN_OFF,
                    durationMs = turnSinceMs?.let { now - it },
                    detail = if (prev == Turn.LEFT) "왼쪽" else "오른쪽",
                )
                turnSinceMs = null
            }
        }
    }

    @Synchronized
    fun onGear(gear: String?) {
        if (gear == state.gear) return
        val prev = state.gear
        update(state.copy(gear = gear))
        if (gear != null) emit(EventType.GEAR, detail = if (prev == null) gear else "$prev → $gear")
    }

    /** 가속 페달 밟힘 정도(0~100%). 이 값이 한 번이라도 오면 가속 판단은 페달 기준으로 바뀐다 */
    @Synchronized
    fun onAcceleratorPedal(percent: Float?) {
        if (percent == null) return
        pedalAvailable = true
        val on = when {
            percent >= config.pedalOnPercent -> true
            percent <= config.pedalOffPercent -> false
            else -> state.accelerating
        }
        update(state.copy(pedalPercent = percent, accelSource = AccelSource.PEDAL))
        setAccelerating(on, detail = "페달 ${percent.toInt()}%")
    }

    /** 차량 속도 (m/s). 후진 시 음수가 올 수 있어 절댓값을 쓴다 */
    @Synchronized
    fun onSpeed(speedMs: Float?) {
        if (speedMs == null) return
        val now = clock()
        val v = abs(speedMs)
        val kmh = v * 3.6f

        if (isRecording) {
            lastDistSample?.let { (t, pv) ->
                val dt = (now - t) / 1000.0
                if (dt in 0.0..10.0) distanceM += (pv + v) / 2.0 * dt
            }
            lastDistSample = now to v
            if (kmh > maxSpeedKmh) maxSpeedKmh = kmh
        }

        speedSamples.addLast(now to v)
        while (speedSamples.size > 2 && now - speedSamples.first().first > config.accelWindowMs) {
            speedSamples.removeFirst()
        }
        val (t0, v0) = speedSamples.first()
        val span = now - t0
        val accel = if (span >= config.accelMinSpanMs) (v - v0) / (span / 1000f) else null

        update(
            state.copy(
                speedKmh = kmh,
                accelMs2 = accel,
                accelSource = if (pedalAvailable) AccelSource.PEDAL else AccelSource.SPEED,
            )
        )
        if (accel == null) return

        if (!pedalAvailable) {
            val on = when {
                accel >= config.accelOnMs2 -> true
                accel < config.accelOffMs2 -> false
                else -> state.accelerating
            }
            setAccelerating(on, detail = "%.1f m/s²".format(accel))
        }

        // 한 번의 급가속/급감속 구간은 한 번만 기록한다.
        // 가속도가 기준의 절반 아래로 돌아오면 다시 감지할 수 있게 된다.
        if (accel < config.harshAccelMs2 / 2) harshAccelArmed = true
        if (accel > config.harshBrakeMs2 / 2) harshBrakeArmed = true

        if (harshAccelArmed && accel >= config.harshAccelMs2 && now - lastHarshAccelMs >= config.harshCooldownMs) {
            harshAccelArmed = false
            lastHarshAccelMs = now
            emit(EventType.HARSH_ACCEL, detail = "%.1f m/s²".format(accel))
        }
        if (harshBrakeArmed && accel <= config.harshBrakeMs2 && now - lastHarshBrakeMs >= config.harshCooldownMs) {
            harshBrakeArmed = false
            lastHarshBrakeMs = now
            emit(EventType.HARSH_BRAKE, detail = "%.1f m/s²".format(accel))
        }
    }

    // ---------------------------------------------------------------- internals

    private fun setAccelerating(on: Boolean, detail: String) {
        if (on == state.accelerating) return
        val now = clock()
        update(state.copy(accelerating = on))
        if (on) {
            accelSinceMs = now
            emit(EventType.ACCEL_ON, detail = detail)
        } else {
            emit(EventType.ACCEL_OFF, durationMs = accelSinceMs?.let { now - it })
            accelSinceMs = null
        }
    }

    private fun update(newState: LiveState) {
        state = newState
        listener.onLiveState(newState)
    }

    private fun emit(type: EventType, durationMs: Long? = null, detail: String = "") {
        if (!isRecording) return
        val event = DriveEvent(clock(), type, state.speedKmh, durationMs, detail)
        tripEvents += event
        listener.onEvent(event)
    }
}
