package com.hhgaq.drivelog.vehicle

import com.hhgaq.drivelog.log.DriveRecorder
import com.hhgaq.drivelog.log.Turn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 실제 차량 입력 없이 기록 기능을 보여주기 위한 가상 주행.
 * 출발 → 가속 → 왼쪽 차선 변경 → 급가속 → 급제동 → 우회전 → 정차 순서로 신호를 넣는다.
 * 실제 신호와 똑같이 DriveRecorder로 들어가므로 이벤트 기록 경로를 그대로 검증한다.
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
                    recorder.onSpeed(kmh / 3.6f)
                    delay(100)
                }
            }
            suspend fun hold(seconds: Float) = speedTo(kmh, seconds)

            recorder.onGear("P"); recorder.onBrake(true); hold(1f)
            recorder.onGear("D"); hold(0.5f)
            recorder.onBrake(false); hold(0.5f)
            speedTo(40f, 6f)                       // 부드러운 출발
            hold(2f)
            recorder.onTurn(Turn.LEFT); hold(2.5f)  // 왼쪽 차선 변경
            recorder.onTurn(Turn.OFF); hold(1.5f)
            speedTo(80f, 3f)                        // 급가속
            hold(2f)
            recorder.onBrake(true); speedTo(65f, 1.5f); recorder.onBrake(false)  // 가벼운 감속
            hold(2f)
            recorder.onBrake(true); speedTo(25f, 2.5f)                          // 급제동
            recorder.onTurn(Turn.RIGHT); speedTo(15f, 1.5f)
            recorder.onBrake(false); hold(2f)       // 우회전
            recorder.onTurn(Turn.OFF)
            speedTo(30f, 3f); hold(1f)
            recorder.onBrake(true); speedTo(0f, 4f) // 정차
            recorder.onGear("P"); hold(0.5f)
            recorder.onBrake(false)
            onFinished()
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
