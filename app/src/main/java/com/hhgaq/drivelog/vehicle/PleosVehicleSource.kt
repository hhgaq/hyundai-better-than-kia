package com.hhgaq.drivelog.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener
import ai.pleos.playground.vehicle.listener.TurnSignalStateListener
import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.hhgaq.drivelog.log.DriveRecorder
import com.hhgaq.drivelog.log.Turn

/**
 * Pleos Vehicle SDK로 브레이크 페달(밟음/뗌)과 방향지시등(좌/우/끔)을 구독한다.
 *
 * 권한: pleos.car.permission.CAR_DRIVE(브레이크), pleos.car.permission.CAR_LIGHTS(깜빡이).
 * 둘 다 protectionLevel=normal 이라 매니페스트 선언만으로 충분하다.
 */
@SuppressLint("MissingPermission")
class PleosVehicleSource(
    context: Context,
    private val recorder: DriveRecorder,
    private val report: (signal: String, status: String) -> Unit,
) {
    private val vehicle = Vehicle(context.applicationContext)
    private var started = false

    private val brakeListener = object : BrakePedalStatusListener {
        override fun onBrakePedalStatusUpdated(status: Boolean?) {
            report(SIGNAL_BRAKE, "수신 중")
            recorder.onBrake(status)
        }

        override fun onFailed(e: Exception) {
            Log.w(TAG, "brake listener failed", e)
            report(SIGNAL_BRAKE, "오류: ${e.message}")
        }
    }

    private val turnListener = object : TurnSignalStateListener {
        override fun onTurnSignalStateUpdated(state: TurnSignalValue?) {
            report(SIGNAL_TURN, "수신 중")
            recorder.onTurn(state.toTurn())
        }

        override fun onFailed(e: Exception) {
            Log.w(TAG, "turn signal listener failed", e)
            report(SIGNAL_TURN, "오류: ${e.message}")
        }
    }

    fun start() {
        if (started) return
        started = true
        try {
            vehicle.initialize()
        } catch (e: Exception) {
            Log.e(TAG, "Vehicle SDK initialize failed", e)
            report(SIGNAL_BRAKE, "SDK 연결 실패")
            report(SIGNAL_TURN, "SDK 연결 실패")
            return
        }
        report(SIGNAL_BRAKE, "연결 중")
        report(SIGNAL_TURN, "연결 중")

        runCatching {
            val brake = vehicle.getBrake()
            brake.checkBrakePedalStatusCapabilities(
                onComplete = { Log.d(TAG, "brake capability: $it") },
                onFailed = { report(SIGNAL_BRAKE, "미지원 또는 확인 실패") },
            )
            brake.registerBrakePedalStatus(brakeListener)
            brake.getCurrentBrakePedalStatus(
                onComplete = { recorder.onBrake(it); report(SIGNAL_BRAKE, "수신 중") },
                onFailed = { Log.w(TAG, "brake get failed", it) },
            )
        }.onFailure { report(SIGNAL_BRAKE, "등록 실패: ${it.message}") }

        runCatching {
            val turn = vehicle.getTurnSignal()
            turn.checkTurnSignalStateCapabilities(
                onComplete = { Log.d(TAG, "turn capability: $it") },
                onFailed = { report(SIGNAL_TURN, "미지원 또는 확인 실패") },
            )
            turn.registerTurnSignalState(turnListener)
            turn.getCurrentTurnSignalState(
                onComplete = { recorder.onTurn(it.toTurn()); report(SIGNAL_TURN, "수신 중") },
                onFailed = { Log.w(TAG, "turn get failed", it) },
            )
        }.onFailure { report(SIGNAL_TURN, "등록 실패: ${it.message}") }
    }

    fun stop() {
        if (!started) return
        started = false
        runCatching { vehicle.getBrake().unregisterBrakePedalStatus(brakeListener) }
        runCatching { vehicle.getTurnSignal().unregisterTurnSignalState(turnListener) }
        runCatching { vehicle.release() }
    }

    private fun TurnSignalValue?.toTurn(): Turn? = when (this) {
        TurnSignalValue.LEFT -> Turn.LEFT
        TurnSignalValue.RIGHT -> Turn.RIGHT
        TurnSignalValue.OFF -> Turn.OFF
        null -> null
    }

    companion object {
        private const val TAG = "PleosVehicleSource"
        const val SIGNAL_BRAKE = "브레이크"
        const val SIGNAL_TURN = "깜빡이"
    }
}
