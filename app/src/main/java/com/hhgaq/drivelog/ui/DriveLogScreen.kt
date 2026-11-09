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
fun DriveLogScreen(repository: DriveRepository, onShareCsv: (Long) -> Unit) {
    val live by repository.live.collectAsStateWithLifecycle()
    val events by repository.events.collectAsStateWithLifecycle()
    val summary by repository.summary.collectAsStateWithLifecycle()
    val recording by repository.recording.collectAsStateWithLifecycle()
    val demoRunning by repository.demoRunning.collectAsStateWithLifecycle()
    val signals by repository.signals.collectAsStateWithLifecycle()
    val trips by repository.trips.collectAsStateWithLifecycle()

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Row(Modifier.fillMaxSize().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            LivePanel(
                live = live,
                recording = recording,
                demoRunning = demoRunning,
                signals = signals,
                onToggleRecord = { if (recording) repository.stopTrip() else repository.startTrip() },
                onDemo = { if (demoRunning) repository.stopDemo() else repository.startDemo() },
                modifier = Modifier.weight(0.42f).fillMaxHeight(),
            )
            LogPanel(
                repository = repository,
                events = events,
                summary = summary,
                recording = recording,
                trips = trips,
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
                modifier = Modifier.weight(1f).height(56.dp),
            ) { Text(if (recording) "기록 종료" else "기록 시작", fontSize = 18.sp) }
            OutlinedButton(onClick = onDemo, modifier = Modifier.weight(1f).height(56.dp)) {
                Text(if (demoRunning) "가상 주행 중지" else "가상 주행", fontSize = 18.sp)
            }
        }

        Spacer(Modifier.weight(1f))
        Text("신호 연결", color = DriveColors.Muted, fontSize = 13.sp)
        signals.forEach { (name, status) ->
            Row {
                Text(name, fontSize = 13.sp, modifier = Modifier.width(80.dp))
                Text(
                    status, fontSize = 13.sp,
                    color = if (status == "수신 중") DriveColors.Accel else DriveColors.Muted,
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
        Text(icon, fontSize = 26.sp, color = if (active) Color.White else DriveColors.Muted)
        Text(label, fontSize = 15.sp, color = if (active) Color.White else DriveColors.Muted)
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
    onShareCsv: (Long) -> Unit,
    modifier: Modifier,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var openedTrip by remember { mutableStateOf<TripSummary?>(null) }

    Surface(modifier, shape = RoundedCornerShape(20.dp), color = DriveColors.Panel) {
        Column {
            TabRow(selectedTabIndex = tab, containerColor = DriveColors.Panel) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(if (recording) "● 이번 주행" else "이번 주행") })
                Tab(selected = tab == 1, onClick = { tab = 1; openedTrip = null }, text = { Text("지난 기록 ${trips.size}") })
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
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                    .background(DriveColors.Idle).clickable { openedTrip = trip }.padding(14.dp)
                            ) {
                                Text(dateTime(trip.startMs) + "  ·  " + duration(trip), fontWeight = FontWeight.SemiBold)
                                Text(
                                    "%.1f km · 브레이크 %d · 깜빡이 %d · 급가속 %d · 급감속 %d".format(
                                        trip.distanceKm, trip.brakeCount, trip.leftTurnCount + trip.rightTurnCount,
                                        trip.harshAccelCount, trip.harshBrakeCount,
                                    ),
                                    color = DriveColors.Muted, fontSize = 14.sp,
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
            TextButton(onClick = onBack) { Text("← 목록") }
            Text(dateTime(trip.startMs), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            TextButton(onClick = onShare) { Text("CSV 내보내기") }
            TextButton(onClick = onDelete) { Text("삭제", color = DriveColors.Brake) }
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
        Text(value, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = highlight ?: Color.Unspecified)
        Text(label, fontSize = 12.sp, color = DriveColors.Muted)
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
        Text(time(e.timeMs), color = DriveColors.Muted, fontSize = 14.sp, modifier = Modifier.width(78.dp))
        Box(Modifier.size(10.dp).clip(CircleShape).background(colorOf(e.type)))
        Spacer(Modifier.width(10.dp))
        Text(e.type.label, fontSize = 16.sp, modifier = Modifier.width(120.dp))
        val extra = buildList {
            e.speedKmh?.let { add("%.0f km/h".format(it)) }
            e.durationMs?.let { add("%.1f초 유지".format(it / 1000f)) }
            if (e.detail.isNotBlank()) add(e.detail)
        }.joinToString(" · ")
        Text(extra, color = DriveColors.Muted, fontSize = 14.sp)
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = DriveColors.Muted, textAlign = TextAlign.Center)
    }
}

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
