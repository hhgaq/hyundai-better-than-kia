package com.hhgaq.drivelog.log

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** 끝난 기록을 앱 내부 저장소에 주행 1건당 JSON 파일 하나로 저장한다 */
class TripStore(baseDir: File) {
    private val dir = File(baseDir, "trips").apply { mkdirs() }

    fun save(trip: DriveRecorder.Trip) {
        val s = trip.summary
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
        File(dir, "${s.startMs}.json").writeText(json.toString())
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

    /** 주행 기록을 CSV 문자열로 (공유·제출용) */
    fun toCsv(startMs: Long): String = buildString {
        appendLine("time_ms,event,speed_kmh,duration_ms,detail")
        events(startMs).forEach { e ->
            appendLine("${e.timeMs},${e.type.name},${e.speedKmh ?: ""},${e.durationMs ?: ""},\"${e.detail.replace("\"", "'")}\"")
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
