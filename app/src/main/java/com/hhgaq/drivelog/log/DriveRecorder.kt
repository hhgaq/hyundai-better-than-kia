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
        /** 기어가 P에서 벗어나 기록이 자동으로 시작됨 */
        fun onAutoTripStarted() {}
        /** 기어가 P로 돌아와 기록이 자동으로 끝남 */
        fun onAutoTripStopped(trip: Trip) {}
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
        /** 기어가 P에서 벗어나면 기록을 자동으로 시작하고, P로 돌아오면 끝낸다 */
        val autoTrip: Boolean = false,
    )

    data class Trip(val events: List<DriveEvent>, val summary: TripSummary)

    /** 신호 출처. 가상 주행 중에는 실제 차량 신호를 무시해서 두 입력이 섞이지 않게 한다 */
    enum class Input { REAL, DEMO }

    /** 가상 주행이 끝나면 되돌릴 마지막 실제 신호 값 */
    private var realBrake: Boolean? = null
    private var realTurn: Turn? = null
    private var realGear: String? = null
    private var realPedal: Float? = null
    var isDemoActive = false
        private set

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

    /** 기록 중인 주행의 현재까지 내용 (중간 저장용, endMs 없음) */
    @Synchronized
    fun snapshot(): Trip? =
        if (isRecording) Trip(tripEvents.toList(), currentSummary()!!) else null

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

        // 기록 시작 전부터 밟고 있던 브레이크·켜져 있던 깜빡이·진행 중인 가속은
        // 시작 시각 기준으로 켬 이벤트를 남겨서 횟수와 유지 시간이 맞게 한다
        val now = tripStartMs
        if (state.brake == true) {
            brakeSinceMs = now
            emit(EventType.BRAKE_ON, detail = ALREADY_ON)
        }
        when (state.turn) {
            Turn.LEFT -> { turnSinceMs = now; emit(EventType.TURN_LEFT, detail = ALREADY_ON) }
            Turn.RIGHT -> { turnSinceMs = now; emit(EventType.TURN_RIGHT, detail = ALREADY_ON) }
            else -> {}
        }
        if (state.accelerating) {
            accelSinceMs = now
            emit(EventType.ACCEL_ON, detail = ALREADY_ON)
        }
    }

    /** 가상 주행 시작: 이 시점부터 실제 신호는 기록에 반영하지 않는다 */
    @Synchronized
    fun beginDemo() {
        if (isDemoActive) return
        isDemoActive = true
        speedSamples.clear()
        lastDistSample = null
        pedalAvailable = false  // 가상 주행은 속도로 가속을 판단한다
        brakeSinceMs = null
        turnSinceMs = null
        accelSinceMs = null
        update(LiveState())     // 가상 주행은 빈 상태에서 시작한다
    }

    /** 가상 주행 끝: 가속도 창을 비우고 마지막 실제 신호 값으로 되돌린다 */
    @Synchronized
    fun endDemo() {
        if (!isDemoActive) return
        isDemoActive = false
        speedSamples.clear()
        lastDistSample = null
        brakeSinceMs = null
        turnSinceMs = null
        accelSinceMs = null
        pedalAvailable = realPedal != null
        // 이벤트 없이 상태만 되돌린다. 속도는 다음 실제 샘플이 오면 채워진다
        update(
            LiveState(
                brake = realBrake,
                turn = realTurn,
                gear = realGear,
                pedalPercent = realPedal,
                accelSource = if (pedalAvailable) AccelSource.PEDAL else AccelSource.NONE,
            )
        )
    }

    private fun accept(input: Input): Boolean =
        if (input == Input.REAL) !isDemoActive else isDemoActive

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
    fun onBrake(pressed: Boolean?, input: Input = Input.REAL) {
        if (input == Input.REAL && pressed != null) realBrake = pressed
        if (accept(input)) applyBrake(pressed)
    }

    /** null(신호 끊김)은 뗌으로 보지 않고 무시한다 */
    private fun applyBrake(pressed: Boolean?) {
        if (pressed == null) return
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
    fun onTurn(turn: Turn?, input: Input = Input.REAL) {
        if (input == Input.REAL && turn != null) realTurn = turn
        if (accept(input)) applyTurn(turn)
    }

    private fun applyTurn(turn: Turn?) {
        if (turn == null) return
        val prev = state.turn
        if (turn == prev) return
        update(state.copy(turn = turn))
        val now = clock()
        when (turn) {
            Turn.LEFT, Turn.RIGHT -> {
                turnSinceMs = now
                emit(if (turn == Turn.LEFT) EventType.TURN_LEFT else EventType.TURN_RIGHT)
            }
            Turn.OFF -> if (prev == Turn.LEFT || prev == Turn.RIGHT) {
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
    fun onGear(gear: String?, input: Input = Input.REAL) {
        if (input == Input.REAL && gear != null) realGear = gear
        if (accept(input)) applyGear(gear)
    }

    private fun applyGear(gear: String?) {
        if (gear == null || gear == state.gear) return
        val prev = state.gear
        update(state.copy(gear = gear))

        // 자동 기록: 가상 주행 중에는 적용하지 않는다
        val auto = config.autoTrip && !isDemoActive
        if (auto && gear != GEAR_PARK && !isRecording) {
            startTrip()
            listener.onAutoTripStarted()
        }
        emit(EventType.GEAR, detail = if (prev == null) gear else "$prev → $gear")
        // 주행하다가 P에 넣었을 때만 끝낸다 (앱을 켠 직후 처음 받은 P 값에는 끝내지 않음)
        if (auto && gear == GEAR_PARK && prev != null && prev != GEAR_PARK && isRecording) {
            stopTrip()?.let { listener.onAutoTripStopped(it) }
        }
    }

    /** 가속 페달 밟힘 정도(0~100%). 이 값이 한 번이라도 오면 가속 판단은 페달 기준으로 바뀐다 */
    @Synchronized
    fun onAcceleratorPedal(percent: Float?, input: Input = Input.REAL) {
        if (percent == null) return
        if (input == Input.REAL) realPedal = percent
        if (!accept(input)) return
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
    fun onSpeed(speedMs: Float?, input: Input = Input.REAL) {
        if (speedMs == null || !accept(input)) return
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
        // 샘플이 드물어 구간이 창의 2배를 넘으면 가속도를 계산하지 않는다
        val accel = if (span >= config.accelMinSpanMs && span <= config.accelWindowMs * 2) {
            (v - v0) / (span / 1000f)
        } else {
            null
        }

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

    private companion object {
        const val ALREADY_ON = "기록 시작 때 이미 켜짐"
        const val GEAR_PARK = "P"
    }

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
