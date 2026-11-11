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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * 앱 전체에서 하나만 쓰는 저장소. 화면이 다시 만들어져도 기록이 끊기지 않도록
 * Application 범위에 둔다.
 */
class DriveRepository(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val store = TripStore(appContext.filesDir)

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

    @Volatile private var lastSummaryMs = 0L

    private val recorder: DriveRecorder = DriveRecorder(config = DriveRecorder.Config(autoTrip = true), listener = object : DriveRecorder.Listener {
        override fun onLiveState(state: LiveState) {
            _live.value = state
            // 속도는 초당 10번 오므로 요약(거리·최고 속도)은 1초에 한 번만 다시 계산한다
            val now = System.currentTimeMillis()
            if (_recording.value && now - lastSummaryMs >= SUMMARY_INTERVAL_MS) refreshSummary(now)
        }

        override fun onEvent(event: DriveEvent) {
            _events.update { it + event }
            refreshSummary(System.currentTimeMillis())
        }

        override fun onAutoTripStarted() = onTripStarted()

        override fun onAutoTripStopped(trip: DriveRecorder.Trip) = onTripFinished(trip)
    })

    private val report: (String, String) -> Unit = { signal, status ->
        _signals.update { LinkedHashMap(it).apply { put(signal, status) } }
    }
    private val pleos = PleosVehicleSource(appContext, recorder, report)
    private val car = CarPropertySource(appContext, recorder, report)
    private val demo = DemoDriver(recorder)
    private var autosaveJob: Job? = null

    init {
        ioScope.launch {
            store.recoverUnfinished(activeStartMs = null)
            _trips.value = store.list()
        }
    }

    fun connect(hasSpeedPermission: Boolean) {
        pleos.start()
        car.start(hasSpeedPermission)
    }

    fun startTrip() {
        if (_recording.value) return
        recorder.startTrip()
        onTripStarted()
    }

    private fun onTripStarted() {
        _events.value = recorder.currentEvents()
        _recording.value = true
        refreshSummary(System.currentTimeMillis())
        startAutosave()
    }

    private fun onTripFinished(trip: DriveRecorder.Trip) {
        autosaveJob?.cancel()
        _recording.value = false
        _events.value = trip.events
        _summary.value = trip.summary
        ioScope.launch {
            store.save(trip)
            _trips.value = store.list()
        }
    }

    fun stopTrip() {
        val trip = recorder.stopTrip()
        if (demo.isRunning || recorder.isDemoActive) stopDemoInternal()
        if (trip == null) return
        onTripFinished(trip)
    }

    /** 가상 주행: 실제 신호를 잠시 무시하고 새 기록을 시작한다. 시나리오가 끝나면 기록도 끝낸다 */
    fun startDemo() {
        if (demo.isRunning) return
        if (_recording.value) stopTrip()
        recorder.beginDemo()
        startTrip()
        _demoRunning.value = true
        demo.start(scope) { stopTrip() }
    }

    fun stopDemo() {
        if (_recording.value && recorder.isDemoActive) stopTrip() else stopDemoInternal()
    }

    private fun stopDemoInternal() {
        demo.stop()
        recorder.endDemo()
        _demoRunning.value = false
    }

    fun tripEvents(startMs: Long): List<DriveEvent> = store.events(startMs)

    fun tripCsv(startMs: Long): String = store.toCsv(startMs)

    /** 공유할 앱이 없을 때: 앱 전용 외부 저장소(없으면 내부)에 CSV 파일로 저장 */
    fun exportCsvFile(startMs: Long): File {
        val dir = appContext.getExternalFilesDir("exports") ?: File(appContext.filesDir, "exports")
        return store.exportCsv(startMs, dir)
    }

    fun deleteTrip(startMs: Long) {
        ioScope.launch {
            store.delete(startMs)
            _trips.value = store.list()
        }
    }

    /** 기록 중에는 몇 초마다 진행 중인 기록을 저장해서, 앱이 죽어도 그때까지의 기록이 남게 한다 */
    private fun startAutosave() {
        autosaveJob?.cancel()
        autosaveJob = ioScope.launch {
            while (isActive) {
                delay(AUTOSAVE_INTERVAL_MS)
                recorder.snapshot()?.let { store.save(it) }
            }
        }
    }

    private fun refreshSummary(now: Long) {
        lastSummaryMs = now
        recorder.currentSummary()?.let { _summary.value = it }
    }

    private companion object {
        const val SUMMARY_INTERVAL_MS = 1000L
        const val AUTOSAVE_INTERVAL_MS = 5000L
    }
}
