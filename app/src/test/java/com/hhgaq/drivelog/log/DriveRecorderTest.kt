package com.hhgaq.drivelog.log

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveRecorderTest {

    private var now = 1_000_000L
    private val events = mutableListOf<DriveEvent>()
    private val recorder = DriveRecorder(
        clock = { now },
        listener = object : DriveRecorder.Listener {
            override fun onLiveState(state: LiveState) {}
            override fun onEvent(event: DriveEvent) { events += event }
        },
    )

    private fun advance(ms: Long) { now += ms }
    private fun types() = events.map { it.type }

    /** km/h 속도를 100ms 간격으로 from → to 까지 seconds 동안 넣는다 */
    private fun ramp(fromKmh: Float, toKmh: Float, seconds: Float) {
        val steps = (seconds * 10).toInt()
        for (i in 1..steps) {
            advance(100)
            recorder.onSpeed((fromKmh + (toKmh - fromKmh) * i / steps) / 3.6f)
        }
    }

    @Test
    fun nothingIsLoggedBeforeRecordingStarts() {
        recorder.onBrake(true)
        recorder.onTurn(Turn.LEFT)
        assertTrue(events.isEmpty())
    }

    @Test
    fun brakePressAndReleaseRecordsDuration() {
        recorder.startTrip()
        recorder.onBrake(false)          // 초기값: 이벤트 없음
        recorder.onBrake(true)
        advance(1500)
        recorder.onBrake(false)
        assertEquals(listOf(EventType.TRIP_START, EventType.BRAKE_ON, EventType.BRAKE_OFF), types())
        assertEquals(1500L, events.last().durationMs)
    }

    @Test
    fun turnSignalOnOffRecordsSideAndDuration() {
        recorder.startTrip()
        recorder.onTurn(Turn.OFF)
        recorder.onTurn(Turn.LEFT)
        advance(3000)
        recorder.onTurn(Turn.OFF)
        recorder.onTurn(Turn.RIGHT)
        assertEquals(
            listOf(EventType.TRIP_START, EventType.TURN_LEFT, EventType.TURN_OFF, EventType.TURN_RIGHT),
            types(),
        )
        val off = events[2]
        assertEquals(3000L, off.durationMs)
        assertEquals("왼쪽", off.detail)
    }

    @Test
    fun gentleAccelerationIsAccelButNotHarsh() {
        recorder.startTrip()
        ramp(0f, 40f, 6f)                // 약 1.9 m/s²
        ramp(40f, 40f, 2f)               // 정속
        assertTrue(EventType.ACCEL_ON in types())
        assertTrue(EventType.ACCEL_OFF in types())
        assertTrue(EventType.HARSH_ACCEL !in types())
    }

    @Test
    fun hardAccelerationAndBrakingAreFlaggedOnce() {
        recorder.startTrip()
        ramp(0f, 30f, 3f)
        ramp(30f, 80f, 3f)               // 약 4.6 m/s²
        ramp(80f, 80f, 2f)
        ramp(80f, 30f, 2.5f)             // 약 -5.6 m/s²
        assertEquals(1, types().count { it == EventType.HARSH_ACCEL })
        assertEquals(1, types().count { it == EventType.HARSH_BRAKE })
        val summary = recorder.stopTrip()!!.summary
        assertEquals(80f, summary.maxSpeedKmh, 0.5f)
        assertTrue("distance=${summary.distanceKm}", summary.distanceKm in 0.10..0.17)
    }

    @Test
    fun pedalSignalTakesOverFromSpeedEstimate() {
        recorder.startTrip()
        recorder.onAcceleratorPedal(0f)
        recorder.onAcceleratorPedal(30f)
        ramp(0f, 20f, 2f)
        recorder.onAcceleratorPedal(1f)
        assertEquals(
            listOf(EventType.TRIP_START, EventType.ACCEL_ON, EventType.ACCEL_OFF),
            types().filter { it != EventType.HARSH_ACCEL },
        )
        assertEquals(AccelSource.PEDAL, recorder.currentState().accelSource)
    }

    @Test
    fun summaryCountsEvents() {
        recorder.startTrip()
        repeat(3) { recorder.onBrake(true); advance(500); recorder.onBrake(false); advance(500) }
        recorder.onTurn(Turn.LEFT); advance(1000); recorder.onTurn(Turn.OFF)
        recorder.onTurn(Turn.RIGHT); advance(1000); recorder.onTurn(Turn.OFF)
        val s = recorder.stopTrip()!!.summary
        assertEquals(3, s.brakeCount)
        assertEquals(1500L, s.brakeTotalMs)
        assertEquals(1, s.leftTurnCount)
        assertEquals(1, s.rightTurnCount)
        assertEquals(EventType.TRIP_END, events.last().type)
    }

    @Test
    fun brakeAlreadyPressedWhenTripStartsCountsFromStart() {
        recorder.onBrake(true)
        advance(10_000)                  // 기록 전 10초 동안 밟고 있음
        recorder.startTrip()
        advance(2000)
        recorder.onBrake(false)
        assertEquals(listOf(EventType.TRIP_START, EventType.BRAKE_ON, EventType.BRAKE_OFF), types())
        assertEquals(2000L, events.last().durationMs)
        val s = recorder.stopTrip()!!.summary
        assertEquals(1, s.brakeCount)
        assertEquals(2000L, s.brakeTotalMs)
    }

    @Test
    fun turnSignalAlreadyOnWhenTripStarts() {
        recorder.onTurn(Turn.LEFT)
        advance(5000)
        recorder.startTrip()
        advance(1000)
        recorder.onTurn(Turn.OFF)
        assertEquals(listOf(EventType.TRIP_START, EventType.TURN_LEFT, EventType.TURN_OFF), types())
        assertEquals(1000L, events.last().durationMs)
    }

    @Test
    fun signalLossIsNotTreatedAsRelease() {
        recorder.startTrip()
        recorder.onBrake(true)
        recorder.onBrake(null)
        recorder.onTurn(Turn.RIGHT)
        recorder.onTurn(null)
        assertEquals(listOf(EventType.TRIP_START, EventType.BRAKE_ON, EventType.TURN_RIGHT), types())
        assertEquals(true, recorder.currentState().brake)
    }

    @Test
    fun realSignalsAreIgnoredDuringDemo() {
        val demo = DriveRecorder.Input.DEMO
        recorder.onGear("D")
        recorder.beginDemo()
        recorder.startTrip()
        // 가상 주행 속도와 실제 속도(0)가 번갈아 들어와도 실제 값은 무시되어야 한다
        for (i in 1..30) {
            advance(100)
            recorder.onSpeed((i * 1.0f) / 3.6f, demo)   // 1초에 10 km/h씩 완만한 가속
            recorder.onSpeed(0f)                         // 실제 신호: 무시
            recorder.onBrake(true)                       // 실제 신호: 무시
        }
        assertTrue(EventType.HARSH_ACCEL !in types())
        assertTrue(EventType.HARSH_BRAKE !in types())
        assertTrue(EventType.BRAKE_ON !in types())
        assertEquals(30f, recorder.currentState().speedKmh!!, 0.1f)

        recorder.stopTrip()
        recorder.endDemo()
        // 가상 주행이 끝나면 마지막 실제 값으로 돌아온다
        assertEquals(true, recorder.currentState().brake)
        assertEquals("D", recorder.currentState().gear)
        assertEquals(null, recorder.currentState().speedKmh)
    }

    @Test
    fun sparseSpeedSamplesDoNotProduceAcceleration() {
        recorder.startTrip()
        recorder.onSpeed(0f)
        advance(5000)
        recorder.onSpeed(50f / 3.6f)     // 5초 만에 온 다음 샘플
        assertEquals(null, recorder.currentState().accelMs2)
    }

    @Test
    fun snapshotHasNoEndTime() {
        recorder.startTrip()
        recorder.onBrake(true)
        val snap = recorder.snapshot()!!
        assertEquals(null, snap.summary.endMs)
        assertEquals(1, snap.summary.brakeCount)
    }
}
