package com.hhgaq.drivelog.vehicle

import com.hhgaq.drivelog.log.DriveRecorder
import com.hhgaq.drivelog.log.DriveRecorder.Input.DEMO
import com.hhgaq.drivelog.log.Turn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 실제 차량 입력 없이 기록 기능을 보여주기 위한 가상 주행.
 * 출발 → 가속 → 왼쪽 차선 변경 → 급가속 → 급제동 → 우회전 → 정차 순서로 신호를 넣는다.
 * 실제 신호와 같은 DriveRecorder 경로로 들어가되 DEMO 입력으로 표시되어,
 * 가상 주행 중에는 실제 차량 신호가 섞이지 않는다 (DriveRecorder.beginDemo/endDemo).
 */
class DemoDriver(private val recorder: DriveRecorder) {
    private var job: Job? = null
    val isRunning get() = job?.isActive == true

    fun start(scope: CoroutineScope, onFinished: () -> Unit = {}) {
        if (isRunning) return
        job = scope.launch {
            var kmh = 0f
            suspend fun speedTo(target: Float, seconds: Float) {
                val steps = (seconds * 10).toInt().coerceAtLeast(1)
                val delta = (target - kmh) / steps
                repeat(steps) {
                    if (!isActive) return
                    kmh += delta
                    recorder.onSpeed(kmh / 3.6f, DEMO)
                    delay(100)
                }
            }
            suspend fun hold(seconds: Float) = speedTo(kmh, seconds)

            recorder.onGear("P", DEMO); recorder.onBrake(true, DEMO); hold(1f)
            recorder.onGear("D", DEMO); hold(0.5f)
            recorder.onBrake(false, DEMO); hold(0.5f)
            speedTo(40f, 6f)                       // 부드러운 출발
            hold(2f)
            recorder.onTurn(Turn.LEFT, DEMO); hold(2.5f)  // 왼쪽 차선 변경
            recorder.onTurn(Turn.OFF, DEMO); hold(1.5f)
            speedTo(80f, 3f)                        // 급가속
            hold(2f)
            recorder.onBrake(true, DEMO); speedTo(65f, 1.5f); recorder.onBrake(false, DEMO)  // 가벼운 감속
            hold(2f)
            recorder.onBrake(true, DEMO); speedTo(25f, 2.5f)                          // 급제동
            recorder.onTurn(Turn.RIGHT, DEMO); speedTo(15f, 1.5f)
            recorder.onBrake(false, DEMO); hold(2f)       // 우회전
            recorder.onTurn(Turn.OFF, DEMO)
            speedTo(30f, 3f); hold(1f)
            recorder.onBrake(true, DEMO); speedTo(0f, 4f) // 정차
            recorder.onGear("P", DEMO); hold(0.5f)
            recorder.onBrake(false, DEMO)
            onFinished()
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
