package com.hhgaq.drivelog

import android.content.Context
import com.hhgaq.drivelog.log.DriveEvent
import com.hhgaq.drivelog.log.DriveRecorder
import com.hhgaq.drivelog.log.LiveState
import com.hhgaq.drivelog.log.TripStore
import com.hhgaq.drivelog.log.TripSummary
import com.hhgaq.drivelog.vehicle.CarPropertySource
import com.hhgaq.drivelog.vehicle.DemoDriver
import com.hhgaq.drivelog.vehicle.PleosVehicleSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 앱 전체에서 하나만 쓰는 저장소. 화면이 다시 만들어져도 기록이 끊기지 않도록
 * Application 범위에 둔다.
 */
class DriveRepository(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val store = TripStore(context.filesDir)

    private val _live = MutableStateFlow(LiveState())
    val live: StateFlow<LiveState> = _live.asStateFlow()

    private val _events = MutableStateFlow<List<DriveEvent>>(emptyList())
    val events: StateFlow<List<DriveEvent>> = _events.asStateFlow()

    private val _summary = MutableStateFlow<TripSummary?>(null)
    val summary: StateFlow<TripSummary?> = _summary.asStateFlow()

    private val _recording = MutableStateFlow(false)
    val recording: StateFlow<Boolean> = _recording.asStateFlow()

    private val _demoRunning = MutableStateFlow(false)
    val demoRunning: StateFlow<Boolean> = _demoRunning.asStateFlow()

    /** 신호 이름 → 연결 상태 (화면 하단 표시용) */
    private val _signals = MutableStateFlow<Map<String, String>>(
        linkedMapOf(
            PleosVehicleSource.SIGNAL_BRAKE to "대기",
            PleosVehicleSource.SIGNAL_TURN to "대기",
            CarPropertySource.SIGNAL_SPEED to "대기",
            CarPropertySource.SIGNAL_GEAR to "대기",
            CarPropertySource.SIGNAL_PEDAL to "대기",
        )
    )
    val signals: StateFlow<Map<String, String>> = _signals.asStateFlow()

    private val _trips = MutableStateFlow<List<TripSummary>>(emptyList())
    val trips: StateFlow<List<TripSummary>> = _trips.asStateFlow()

    private val recorder = DriveRecorder(listener = object : DriveRecorder.Listener {
        override fun onLiveState(state: LiveState) {
            _live.value = state
            if (_recording.value) _summary.value = currentSummarySafe()
        }

        override fun onEvent(event: DriveEvent) {
            _events.update { it + event }
        }
    })

    private val report: (String, String) -> Unit = { signal, status ->
        _signals.update { LinkedHashMap(it).apply { put(signal, status) } }
    }
    private val pleos = PleosVehicleSource(context, recorder, report)
    private val car = CarPropertySource(context, recorder, report)
    private val demo = DemoDriver(recorder)

    init {
        refreshTrips()
    }

    fun connect(hasSpeedPermission: Boolean) {
        pleos.start()
        car.start(hasSpeedPermission)
    }

    fun startTrip() {
        recorder.startTrip()
        _events.value = recorder.currentEvents()
        _summary.value = currentSummarySafe()
        _recording.value = true
    }

    fun stopTrip() {
        stopDemo()
        val trip = recorder.stopTrip() ?: return
        _recording.value = false
        _events.value = trip.events
        _summary.value = trip.summary
        scope.launch {
            store.save(trip)
            refreshTrips()
        }
    }

    /** 가상 주행: 기록을 시작하고 시나리오가 끝나면 기록도 끝낸다 */
    fun startDemo() {
        if (demo.isRunning) return
        if (!_recording.value) startTrip()
        _demoRunning.value = true
        demo.start(scope) {
            _demoRunning.value = false
            stopTrip()
        }
    }

    fun stopDemo() {
        demo.stop()
        _demoRunning.value = false
    }

    fun tripEvents(startMs: Long): List<DriveEvent> = store.events(startMs)

    fun tripCsv(startMs: Long): String = store.toCsv(startMs)

    fun deleteTrip(startMs: Long) {
        scope.launch {
            store.delete(startMs)
            refreshTrips()
        }
    }

    private fun refreshTrips() {
        scope.launch { _trips.value = store.list() }
    }

    private fun currentSummarySafe(): TripSummary? = recorder.currentSummary()
}
