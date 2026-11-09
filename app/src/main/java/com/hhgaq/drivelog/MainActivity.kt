package com.hhgaq.drivelog

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.hhgaq.drivelog.ui.DriveLogScreen
import com.hhgaq.drivelog.ui.DriveLogTheme

class MainActivity : ComponentActivity() {

    private val repository get() = (application as DriveLogApp).repository

    private val requestSpeed = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        repository.connect(hasSpeedPermission = granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 브레이크·깜빡이는 권한 대화상자 없이 바로 연결한다
        repository.connect(hasSpeedPermission = hasSpeedPermission())
        // 속도 권한은 처음 화면이 만들어질 때 한 번만 묻는다 (회전·재생성 때 반복하지 않음).
        // 거절한 뒤에는 신호 연결의 "권한 필요"를 눌러 설정에서 허용한다.
        if (savedInstanceState == null && !hasSpeedPermission()) {
            requestSpeed.launch(CAR_SPEED)
        }

        setContent {
            DriveLogTheme {
                DriveLogScreen(
                    repository = repository,
                    onShareCsv = ::shareCsv,
                    onOpenPermissionSettings = ::openPermissionSettings,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 설정 화면에서 권한을 허용하고 돌아온 경우 속도 구독을 다시 시도한다
        if (hasSpeedPermission()) repository.connect(hasSpeedPermission = true)
    }

    private fun hasSpeedPermission() =
        ContextCompat.checkSelfPermission(this, CAR_SPEED) == PackageManager.PERMISSION_GRANTED

    private fun openPermissionSettings() {
        if (!hasSpeedPermission() && shouldShowRequestPermissionRationale(CAR_SPEED)) {
            requestSpeed.launch(CAR_SPEED)
            return
        }
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        runCatching { startActivity(intent) }.onFailure {
            Toast.makeText(this, "설정에서 '드라이브 로그'의 차량 속도 권한을 허용해 주세요", Toast.LENGTH_LONG).show()
        }
    }

    /** 공유할 앱이 있으면 공유하고, 없으면(차량에서 흔함) 파일로 저장한 뒤 위치를 알려준다 */
    private fun shareCsv(startMs: Long) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "드라이브 로그 $startMs")
            putExtra(Intent.EXTRA_TEXT, repository.tripCsv(startMs))
        }
        val canShare = send.resolveActivity(packageManager) != null
        val shared = canShare && runCatching { startActivity(Intent.createChooser(send, "기록 내보내기")) }.isSuccess
        if (!shared) {
            val message = runCatching { repository.exportCsvFile(startMs) }
                .fold(
                    onSuccess = { "공유할 앱이 없어 파일로 저장했어요\n${it.absolutePath}" },
                    onFailure = { "CSV 저장에 실패했어요: ${it.message}" },
                )
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val CAR_SPEED = "android.car.permission.CAR_SPEED"
    }
}
