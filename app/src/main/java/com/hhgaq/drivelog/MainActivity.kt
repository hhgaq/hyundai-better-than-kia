package com.hhgaq.drivelog

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.hhgaq.drivelog.ui.DriveLogScreen
import com.hhgaq.drivelog.ui.DriveLogTheme

class MainActivity : ComponentActivity() {

    private val repository get() = (application as DriveLogApp).repository

    private val speedPermission = ActivityResultContracts.RequestPermission()
    private val requestSpeed = registerForActivityResult(speedPermission) { granted ->
        repository.connect(hasSpeedPermission = granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (hasSpeedPermission()) {
            repository.connect(hasSpeedPermission = true)
        } else {
            // 브레이크·깜빡이는 권한 대화상자 없이 바로 연결하고, 속도는 허용 후 연결한다
            repository.connect(hasSpeedPermission = false)
            requestSpeed.launch(CAR_SPEED)
        }

        setContent {
            DriveLogTheme {
                DriveLogScreen(repository = repository, onShareCsv = ::shareCsv)
            }
        }
    }

    private fun hasSpeedPermission() =
        ContextCompat.checkSelfPermission(this, CAR_SPEED) == PackageManager.PERMISSION_GRANTED

    private fun shareCsv(startMs: Long) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "드라이브 로그 $startMs")
            putExtra(Intent.EXTRA_TEXT, repository.tripCsv(startMs))
        }
        runCatching { startActivity(Intent.createChooser(send, "기록 내보내기")) }
    }

    companion object {
        private const val CAR_SPEED = "android.car.permission.CAR_SPEED"
    }
}
