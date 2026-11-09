package com.hhgaq.drivelog.log

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 기록을 앱 내부 저장소에 주행 1건당 JSON 파일 하나로 저장한다. */
/**
 * 기록 중에도 주기적으로 같은 파일을 덮어써서(endMs 없음) 앱이 죽어도 그때까지의 기록이 남는다.
 * 다음 실행 때 recoverUnfinished()가 마감 처리한다.
 */
class TripStore(baseDir: File) {
    private val dir = File(baseDir, "trips").apply { mkdirs() }

    @Synchronized
    fun save(trip: DriveRecorder.Trip) {
        val s = trip.summary
        val file = File(dir, "${s.startMs}.json")
        // 늦게 도착한 중간 저장이 이미 마감된 기록을 덮어쓰지 않게 한다
        if (s.endMs == null && file.exists() &&
            runCatching { !JSONObject(file.readText()).getJSONObject("summary").isNull("endMs") }.getOrDefault(false)
        ) return
        val json = JSONObject()
            .put("summary", JSONObject()
                .put("startMs", s.startMs)
                .put("endMs", s.endMs ?: JSONObject.NULL)
                .put("brakeCount", s.brakeCount)
                .put("brakeTotalMs", s.brakeTotalMs)
                .put("leftTurnCount", s.leftTurnCount)
                .put("rightTurnCount", s.rightTurnCount)
                .put("accelCount", s.accelCount)
                .put("harshAccelCount", s.harshAccelCount)
                .put("harshBrakeCount", s.harshBrakeCount)
                .put("maxSpeedKmh", s.maxSpeedKmh.toDouble())
                .put("distanceKm", s.distanceKm))
            .put("events", JSONArray().apply {
                trip.events.forEach { e ->
                    put(JSONObject()
                        .put("t", e.timeMs)
                        .put("type", e.type.name)
                        .put("speed", e.speedKmh?.toDouble() ?: JSONObject.NULL)
                        .put("dur", e.durationMs ?: JSONObject.NULL)
                        .put("detail", e.detail))
                }
            })
        file.writeText(json.toString())
    }

    /** 최근 기록부터 요약 목록 */
    fun list(): List<TripSummary> =
        dir.listFiles { f -> f.extension == "json" }.orEmpty()
            .mapNotNull { runCatching { parseSummary(JSONObject(it.readText()).getJSONObject("summary")) }.getOrNull() }
            .sortedByDescending { it.startMs }

    fun events(startMs: Long): List<DriveEvent> {
        val file = File(dir, "$startMs.json")
        if (!file.exists()) return emptyList()
        val arr = JSONObject(file.readText()).getJSONArray("events")
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val type = runCatching { EventType.valueOf(o.getString("type")) }.getOrNull() ?: return@mapNotNull null
            DriveEvent(
                timeMs = o.getLong("t"),
                type = type,
                speedKmh = if (o.isNull("speed")) null else o.getDouble("speed").toFloat(),
                durationMs = if (o.isNull("dur")) null else o.getLong("dur"),
                detail = o.optString("detail"),
            )
        }
    }

    fun delete(startMs: Long) {
        File(dir, "$startMs.json").delete()
    }

    /**
     * 주행 기록을 CSV 문자열로 (공유·제출용).
     * 엑셀에서 한글이 깨지지 않도록 UTF-8 BOM을 붙이고, 사람이 읽는 시각 열을 함께 넣는다.
     */
    fun toCsv(startMs: Long): String = buildString {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.KOREA)
        append('\uFEFF')
        appendLine("time,time_ms,event,event_ko,speed_kmh,duration_ms,detail")
        events(startMs).forEach { e ->
            appendLine(
                listOf(
                    fmt.format(Date(e.timeMs)),
                    e.timeMs.toString(),
                    e.type.name,
                    e.type.label,
                    e.speedKmh?.let { "%.1f".format(Locale.US, it) } ?: "",
                    e.durationMs?.toString() ?: "",
                    "\"" + e.detail.replace("\"", "'") + "\"",
                ).joinToString(",")
            )
        }
    }

    /** CSV를 앱 전용 외부 저장소에 파일로 저장하고 그 파일을 돌려준다 (공유할 앱이 없을 때) */
    fun exportCsv(startMs: Long, exportDir: File): File {
        exportDir.mkdirs()
        val name = "drive-log-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.KOREA).format(Date(startMs)) + ".csv"
        return File(exportDir, name).apply { writeText(toCsv(startMs)) }
    }

    /**
     * 비정상 종료로 끝나지 못한 기록(endMs 없음)을 마지막 이벤트 시각으로 마감한다.
     * 지금 기록 중인 주행(activeStartMs)은 건드리지 않는다.
     */
    fun recoverUnfinished(activeStartMs: Long?) {
        dir.listFiles { f -> f.extension == "json" }.orEmpty().forEach { file ->
            runCatching {
                val json = JSONObject(file.readText())
                val summary = json.getJSONObject("summary")
                if (!summary.isNull("endMs") || summary.getLong("startMs") == activeStartMs) return@runCatching
                val events = json.getJSONArray("events")
                val lastT = if (events.length() > 0) events.getJSONObject(events.length() - 1).getLong("t")
                else summary.getLong("startMs")
                summary.put("endMs", lastT).put("recovered", true)
                file.writeText(json.toString())
            }
        }
    }

    private fun parseSummary(o: JSONObject) = TripSummary(
        startMs = o.getLong("startMs"),
        endMs = if (o.isNull("endMs")) null else o.getLong("endMs"),
        brakeCount = o.getInt("brakeCount"),
        brakeTotalMs = o.getLong("brakeTotalMs"),
        leftTurnCount = o.getInt("leftTurnCount"),
        rightTurnCount = o.getInt("rightTurnCount"),
        accelCount = o.getInt("accelCount"),
        harshAccelCount = o.getInt("harshAccelCount"),
        harshBrakeCount = o.getInt("harshBrakeCount"),
        maxSpeedKmh = o.getDouble("maxSpeedKmh").toFloat(),
        distanceKm = o.getDouble("distanceKm"),
    )
}
