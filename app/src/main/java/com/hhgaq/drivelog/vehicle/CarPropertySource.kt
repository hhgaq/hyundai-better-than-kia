package com.hhgaq.drivelog.vehicle

import android.annotation.SuppressLint
import android.car.Car
import android.car.VehicleGear
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyConfig
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.util.Log
import com.hhgaq.drivelog.log.DriveRecorder

/**
 * Pleos Vehicle SDK에 없는 신호를 Android Automotive 표준 Car API(CarPropertyManager)로 받는다.
 *
 * - PERF_VEHICLE_SPEED: 차량 속도 (android.car.permission.CAR_SPEED, 런타임 권한)
 * - GEAR_SELECTION: 기어 (android.car.permission.CAR_POWERTRAIN). Pleos SDK의 Gear API는
 *   deprecated 이고 이 속성을 쓰라고 안내한다.
 * - ACCELERATOR_PEDAL_COMPRESSION_PERCENTAGE: 가속 페달 (Android 16+ 차량에만 있음).
 *   없으면 속도 변화로 가속을 추정한다.
 */
@SuppressLint("MissingPermission")
class CarPropertySource(
    private val context: Context,
    private val recorder: DriveRecorder,
    private val report: (signal: String, status: String) -> Unit,
) {
    private var car: Car? = null
    private var manager: CarPropertyManager? = null
    private val subscribed = mutableSetOf<Int>()
    /** 실제 값을 한 번이라도 받은 속성. 이때 비로소 "수신 중"으로 표시한다 */
    private val received = java.util.Collections.synchronizedSet(mutableSetOf<Int>())

    private val callback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            if (value.status != CarPropertyValue.STATUS_AVAILABLE) return
            if (received.add(value.propertyId)) nameOf(value.propertyId)?.let { report(it, "수신 중") }
            when (value.propertyId) {
                VehiclePropertyIds.PERF_VEHICLE_SPEED -> recorder.onSpeed(value.value as? Float)
                VehiclePropertyIds.GEAR_SELECTION -> recorder.onGear(gearName(value.value as? Int))
                ACCELERATOR_PEDAL_COMPRESSION_PERCENTAGE -> recorder.onAcceleratorPedal(value.value as? Float)
            }
        }

        override fun onErrorEvent(propertyId: Int, areaId: Int) {
            Log.w(TAG, "property error: $propertyId")
            nameOf(propertyId)?.let { report(it, "오류") }
        }
    }

    /** 속도 권한을 받은 뒤 다시 호출해도 된다. 이미 구독한 속성은 건너뛴다 */
    fun start(hasSpeedPermission: Boolean) {
        val mgr = manager ?: connect() ?: return

        if (hasSpeedPermission) {
            subscribe(mgr, VehiclePropertyIds.PERF_VEHICLE_SPEED, SIGNAL_SPEED)
        } else {
            report(SIGNAL_SPEED, "권한 필요")
        }
        subscribe(mgr, VehiclePropertyIds.GEAR_SELECTION, SIGNAL_GEAR)
        if (!subscribe(mgr, ACCELERATOR_PEDAL_COMPRESSION_PERCENTAGE, SIGNAL_PEDAL)) {
            report(SIGNAL_PEDAL, "미지원 → 속도로 추정")
        }
    }

    fun stop() {
        runCatching { manager?.unregisterCallback(callback) }
        subscribed.clear()
        received.clear()
        runCatching { car?.disconnect() }
        car = null
        manager = null
    }

    private fun connect(): CarPropertyManager? {
        val c = try {
            Car.createCar(context.applicationContext)
        } catch (t: Throwable) {
            Log.e(TAG, "Car.createCar failed", t)
            null
        }
        if (c == null) {
            listOf(SIGNAL_SPEED, SIGNAL_GEAR, SIGNAL_PEDAL).forEach { report(it, "차량 서비스 없음") }
            return null
        }
        car = c
        return (c.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager)?.also { manager = it }
    }

    private fun subscribe(mgr: CarPropertyManager, propertyId: Int, name: String): Boolean {
        if (propertyId in subscribed) return true
        val maybeConfig: CarPropertyConfig<*>? = try {
            mgr.getCarPropertyConfig(propertyId)
        } catch (e: SecurityException) {
            report(name, "권한 없음")
            return false
        } catch (e: Exception) {
            Log.w(TAG, "config failed for $name", e)
            null
        }
        val config = maybeConfig ?: run {
            report(name, "미지원")
            return false
        }

        val rate = if (config.changeMode == CarPropertyConfig.VEHICLE_PROPERTY_CHANGE_MODE_CONTINUOUS) {
            val max = if (config.maxSampleRate > 0f) config.maxSampleRate else 10f
            CONTINUOUS_RATE_HZ.coerceIn(config.minSampleRate, max)
        } else {
            CarPropertyManager.SENSOR_RATE_ONCHANGE
        }

        val ok = try {
            mgr.registerCallback(callback, propertyId, rate)
        } catch (e: Exception) {
            Log.w(TAG, "register failed for $name", e)
            false
        }
        if (ok) {
            subscribed += propertyId
            if (propertyId !in received) report(name, "등록됨 (값 대기)")
        } else {
            report(name, "등록 실패")
        }
        return ok
    }

    private fun nameOf(propertyId: Int): String? = when (propertyId) {
        VehiclePropertyIds.PERF_VEHICLE_SPEED -> SIGNAL_SPEED
        VehiclePropertyIds.GEAR_SELECTION -> SIGNAL_GEAR
        ACCELERATOR_PEDAL_COMPRESSION_PERCENTAGE -> SIGNAL_PEDAL
        else -> null
    }

    companion object {
        private const val TAG = "CarPropertySource"
        const val SIGNAL_SPEED = "속도"
        const val SIGNAL_GEAR = "기어"
        const val SIGNAL_PEDAL = "가속 페달"

        /**
         * VehiclePropertyIds.ACCELERATOR_PEDAL_COMPRESSION_PERCENTAGE (Android 16, API 36).
         * compileSdk 35에는 상수가 없어서 값을 직접 쓴다:
         * 0x030F | SYSTEM(0x10000000) | GLOBAL(0x01000000) | FLOAT(0x00600000)
         */
        const val ACCELERATOR_PEDAL_COMPRESSION_PERCENTAGE = 291504911

        private const val CONTINUOUS_RATE_HZ = 10f

        fun gearName(gear: Int?): String? = when (gear) {
            null -> null
            VehicleGear.GEAR_PARK -> "P"
            VehicleGear.GEAR_REVERSE -> "R"
            VehicleGear.GEAR_NEUTRAL -> "N"
            VehicleGear.GEAR_DRIVE -> "D"
            VehicleGear.GEAR_UNKNOWN -> null
            else -> "기어 $gear"
        }
    }
}
