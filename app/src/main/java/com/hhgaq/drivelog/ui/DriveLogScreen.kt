package com.hhgaq.drivelog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hhgaq.drivelog.DriveRepository
import com.hhgaq.drivelog.log.AccelSource
import com.hhgaq.drivelog.log.DriveEvent
import com.hhgaq.drivelog.log.EventType
import com.hhgaq.drivelog.log.LiveState
import com.hhgaq.drivelog.log.Turn
import com.hhgaq.drivelog.log.TripSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DriveLogScreen(
    repository: DriveRepository,
    onShareCsv: (Long) -> Unit,
    onOpenPermissionSettings: () -> Unit,
) {
    val live by repository.live.collectAsStateWithLifecycle()
    val events by repository.events.collectAsStateWithLifecycle()
    val summary by repository.summary.collectAsStateWithLifecycle()
    val recording by repository.recording.collectAsStateWithLifecycle()
    val demoRunning by repository.demoRunning.collectAsStateWithLifecycle()
    val signals by repository.signals.collectAsStateWithLifecycle()
    val trips by repository.trips.collectAsStateWithLifecycle()
    // 주행 중(5 km/h 초과)에는 목록 대신 큰 요약만 보여서 운전 중 주의 분산을 줄인다
    val driving = (live.speedKmh ?: 0f) > DRIVING_KMH

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Row(Modifier.fillMaxSize().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            LivePanel(
                live = live,
                recording = recording,
                demoRunning = demoRunning,
                signals = signals,
                onToggleRecord = { if (recording) repository.stopTrip() else repository.startTrip() },
                onDemo = { if (demoRunning) repository.stopDemo() else repository.startDemo() },
                onSignalClick = { _, status -> if (status == "권한 필요") onOpenPermissionSettings() },
                modifier = Modifier.weight(0.42f).fillMaxHeight(),
            )
            LogPanel(
                repository = repository,
                events = events,
                summary = summary,
                recording = recording,
                trips = trips,
                driving = driving,
                onShareCsv = onShareCsv,
                modifier = Modifier.weight(0.58f).fillMaxHeight(),
            )
        }
    }
}

// ------------------------------------------------------------------ live

@Composable
private fun LivePanel(
    live: LiveState,
    recording: Boolean,
    demoRunning: Boolean,
    signals: Map<String, String>,
    onToggleRecord: () -> Unit,
    onDemo: () -> Unit,
    onSignalClick: (String, String) -> Unit,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                live.speedKmh?.let { "%.0f".format(it) } ?: "--",
                fontSize = 72.sp, fontWeight = FontWeight.Bold,
            )
            Text(" km/h", fontSize = 22.sp, color = DriveColors.Muted, modifier = Modifier.padding(bottom = 14.dp))
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(DriveColors.Idle),
                contentAlignment = Alignment.Center,
            ) { Text(live.gear ?: "-", fontSize = 30.sp, fontWeight = FontWeight.Bold) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SignalTile("◀", "왼쪽", live.turn == Turn.LEFT, DriveColors.Turn, Modifier.weight(1f))
            SignalTile("■", "브레이크", live.brake == true, DriveColors.Brake, Modifier.weight(1f))
            SignalTile("▶", "오른쪽", live.turn == Turn.RIGHT, DriveColors.Turn, Modifier.weight(1f))
        }
        SignalTile(
            icon = "▲",
            label = when (live.accelSource) {
                AccelSource.PEDAL -> "가속 페달 ${live.pedalPercent?.toInt() ?: 0}%"
                AccelSource.SPEED -> "가속 " + (live.accelMs2?.let { "%+.1f m/s²".format(it) } ?: "")
                AccelSource.NONE -> "가속 (신호 없음)"
            },
            active = live.accelerating,
            color = DriveColors.Accel,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onToggleRecord,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (recording) DriveColors.Brake else MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.weight(1f).height(TOUCH_HEIGHT),
            ) { Text(if (recording) "기록 종료" else "기록 시작", fontSize = 22.sp) }
            OutlinedButton(onClick = onDemo, modifier = Modifier.weight(1f).height(TOUCH_HEIGHT)) {
                Text(if (demoRunning) "가상 주행 중지" else "가상 주행", fontSize = 22.sp)
            }
        }

        Spacer(Modifier.weight(1f))
        Text("신호 연결", color = DriveColors.Muted, fontSize = 16.sp)
        signals.forEach { (name, status) ->
            val needsAction = status == "권한 필요"
            Row(
                Modifier.then(if (needsAction) Modifier.clickable { onSignalClick(name, status) } else Modifier)
                    .padding(vertical = 2.dp)
            ) {
                Text(name, fontSize = 16.sp, modifier = Modifier.width(96.dp))
                Text(
                    if (needsAction) "권한 필요 (눌러서 허용)" else status,
                    fontSize = 16.sp,
                    color = when {
                        status == "수신 중" -> DriveColors.Accel
                        needsAction -> DriveColors.Turn
                        else -> DriveColors.Muted
                    },
                )
            }
        }
    }
}

@Composable
private fun SignalTile(icon: String, label: String, active: Boolean, color: Color, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) color else DriveColors.Idle)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(icon, fontSize = 30.sp, color = if (active) Color.White else DriveColors.Muted)
        Text(label, fontSize = 18.sp, color = if (active) Color.White else DriveColors.Muted)
    }
}

// ------------------------------------------------------------------ log

@Composable
private fun LogPanel(
    repository: DriveRepository,
    events: List<DriveEvent>,
    summary: TripSummary?,
    recording: Boolean,
    trips: List<TripSummary>,
    driving: Boolean,
    onShareCsv: (Long) -> Unit,
    modifier: Modifier,
) {
    if (driving) {
        DrivingPanel(summary, modifier)
        return
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var openedTrip by remember { mutableStateOf<TripSummary?>(null) }

    Surface(modifier, shape = RoundedCornerShape(20.dp), color = DriveColors.Panel) {
        Column {
            TabRow(selectedTabIndex = tab, containerColor = DriveColors.Panel) {
                Tab(
                    selected = tab == 0, onClick = { tab = 0 }, modifier = Modifier.height(TOUCH_HEIGHT),
                    text = { Text(if (recording) "● 이번 주행" else "이번 주행", fontSize = 20.sp) },
                )
                Tab(
                    selected = tab == 1, onClick = { tab = 1; openedTrip = null }, modifier = Modifier.height(TOUCH_HEIGHT),
                    text = { Text("지난 기록 ${trips.size}", fontSize = 20.sp) },
                )
            }
            Column(Modifier.padding(16.dp)) {
                when {
                    tab == 0 -> {
                        if (summary == null && events.isEmpty()) {
                            EmptyHint("기록 시작을 누르면 브레이크·가속·깜빡이 입력이 시간순으로 쌓입니다.\n차량 입력이 없으면 가상 주행으로 확인해 보세요.")
                        } else {
                            summary?.let { SummaryRow(it) }
                            Spacer(Modifier.height(12.dp))
                            EventList(events.asReversed())
                        }
                    }
                    openedTrip != null -> TripDetail(
                        trip = openedTrip!!,
                        repository = repository,
                        onBack = { openedTrip = null },
                        onShare = { onShareCsv(openedTrip!!.startMs) },
                        onDelete = { repository.deleteTrip(openedTrip!!.startMs); openedTrip = null },
                    )
                    trips.isEmpty() -> EmptyHint("저장된 기록이 없습니다.")
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(trips, key = { it.startMs }) { trip ->
                            Column(
                                Modifier.fillMaxWidth().heightIn(min = TOUCH_HEIGHT).clip(RoundedCornerShape(12.dp))
                                    .background(DriveColors.Idle).clickable { openedTrip = trip }.padding(16.dp)
                            ) {
                                Text(dateTime(trip.startMs) + "  ·  " + duration(trip), fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                                Text(
                                    "%.1f km · 브레이크 %d · 깜빡이 %d · 급가속 %d · 급감속 %d".format(
                                        trip.distanceKm, trip.brakeCount, trip.leftTurnCount + trip.rightTurnCount,
                                        trip.harshAccelCount, trip.harshBrakeCount,
                                    ),
                                    color = DriveColors.Muted, fontSize = 16.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TripDetail(
    trip: TripSummary,
    repository: DriveRepository,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    var events by remember(trip.startMs) { mutableStateOf<List<DriveEvent>>(emptyList()) }
    LaunchedEffect(trip.startMs) {
        events = withContext(Dispatchers.IO) { repository.tripEvents(trip.startMs) }
    }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack, modifier = Modifier.height(TOUCH_HEIGHT)) { Text("← 목록", fontSize = 18.sp) }
            Text(dateTime(trip.startMs), fontWeight = FontWeight.SemiBold, fontSize = 18.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = onShare, modifier = Modifier.height(TOUCH_HEIGHT)) { Text("CSV 내보내기", fontSize = 18.sp) }
            TextButton(onClick = onDelete, modifier = Modifier.height(TOUCH_HEIGHT)) {
                Text("삭제", color = DriveColors.Brake, fontSize = 18.sp)
            }
        }
        SummaryRow(trip)
        Spacer(Modifier.height(12.dp))
        EventList(events)
    }
}

@Composable
private fun SummaryRow(s: TripSummary) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Stat("거리", "%.1f km".format(s.distanceKm), Modifier.weight(1f))
        Stat("최고", "%.0f km/h".format(s.maxSpeedKmh), Modifier.weight(1f))
        Stat("브레이크", "${s.brakeCount}회", Modifier.weight(1f))
        Stat("깜빡이", "${s.leftTurnCount + s.rightTurnCount}회", Modifier.weight(1f))
        Stat("급가속", "${s.harshAccelCount}", Modifier.weight(1f), if (s.harshAccelCount > 0) DriveColors.Harsh else null)
        Stat("급감속", "${s.harshBrakeCount}", Modifier.weight(1f), if (s.harshBrakeCount > 0) DriveColors.Harsh else null)
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier, highlight: Color? = null) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(DriveColors.Idle).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = highlight ?: Color.Unspecified)
        Text(label, fontSize = 15.sp, color = DriveColors.Muted)
    }
}

@Composable
private fun EventList(events: List<DriveEvent>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(events) { e -> EventRow(e) }
    }
}

@Composable
private fun EventRow(e: DriveEvent) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(time(e.timeMs), color = DriveColors.Muted, fontSize = 17.sp, modifier = Modifier.width(92.dp))
        Box(Modifier.size(12.dp).clip(CircleShape).background(colorOf(e.type)))
        Spacer(Modifier.width(12.dp))
        Text(e.type.label, fontSize = 19.sp, modifier = Modifier.width(150.dp))
        val extra = buildList {
            e.speedKmh?.let { add("%.0f km/h".format(it)) }
            e.durationMs?.let { add("%.1f초 유지".format(it / 1000f)) }
            if (e.detail.isNotBlank()) add(e.detail)
        }.joinToString(" · ")
        Text(extra, color = DriveColors.Muted, fontSize = 17.sp)
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = DriveColors.Muted, textAlign = TextAlign.Center, fontSize = 18.sp)
    }
}

/** 주행 중 오른쪽 화면: 읽을 거리를 줄이고 큰 숫자만 보여준다 */
@Composable
private fun DrivingPanel(summary: TripSummary?, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(20.dp), color = DriveColors.Panel) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(if (summary != null) "● 기록 중" else "주행 중", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            if (summary != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    BigStat("브레이크", "${summary.brakeCount}", Modifier.weight(1f))
                    BigStat("깜빡이", "${summary.leftTurnCount + summary.rightTurnCount}", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    BigStat("급가속", "${summary.harshAccelCount}", Modifier.weight(1f),
                        if (summary.harshAccelCount > 0) DriveColors.Harsh else null)
                    BigStat("급감속", "${summary.harshBrakeCount}", Modifier.weight(1f),
                        if (summary.harshBrakeCount > 0) DriveColors.Harsh else null)
                }
            }
            Spacer(Modifier.weight(1f))
            Text("정차하면 상세 기록이 보여요", color = DriveColors.Muted, fontSize = 18.sp)
        }
    }
}

@Composable
private fun BigStat(label: String, value: String, modifier: Modifier, highlight: Color? = null) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(DriveColors.Idle).padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 44.sp, color = highlight ?: Color.Unspecified)
        Text(label, fontSize = 18.sp, color = DriveColors.Muted)
    }
}

/** 차량 UI 권장 최소 터치 높이 */
private val TOUCH_HEIGHT = 76.dp
private const val DRIVING_KMH = 5f

private fun colorOf(type: EventType): Color = when (type) {
    EventType.BRAKE_ON, EventType.BRAKE_OFF -> DriveColors.Brake
    EventType.TURN_LEFT, EventType.TURN_RIGHT, EventType.TURN_OFF -> DriveColors.Turn
    EventType.ACCEL_ON, EventType.ACCEL_OFF -> DriveColors.Accel
    EventType.HARSH_ACCEL, EventType.HARSH_BRAKE -> DriveColors.Harsh
    else -> DriveColors.Muted
}

private fun time(ms: Long) = SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(Date(ms))
private fun dateTime(ms: Long) = SimpleDateFormat("M월 d일 HH:mm", Locale.KOREA).format(Date(ms))
private fun duration(t: TripSummary): String {
    val sec = ((t.endMs ?: t.startMs) - t.startMs) / 1000
    return if (sec >= 60) "${sec / 60}분 ${sec % 60}초" else "${sec}초"
}
