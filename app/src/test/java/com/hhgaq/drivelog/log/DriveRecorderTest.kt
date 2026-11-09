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
}
