package com.example.usagedemo

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var checkPackageNameThread: CheckPackageNameThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPackageNameThread = CheckPackageNameThread()
        checkPackageNameThread?.start()


        startButton.setOnClickListener {
            if (!checkPermission()) {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        }
    }

    override fun onStop() {
        super.onStop()
        textView.append("\n\n")
    }

    private fun checkPermission(): Boolean {
        val appOps = applicationContext.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            applicationContext.packageName
        )

        return if (mode == AppOpsManager.MODE_DEFAULT) {
            applicationContext.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }

    fun getPackageName(context: Context): String? {

        // UsageStatsManager 선언
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        var lastRunAppTimeStamp = 0L

        // 얼마만큼의 시간동안 수집한 앱의 이름을 가져오는지 정하기 (begin ~ end 까지의 앱 이름을 수집한다)
        val INTERVAL: Long = 10000
        val end = System.currentTimeMillis()
        // 1 minute ago
        val begin = end - INTERVAL

        val packageNameMap = hashMapOf<Long, String>()

        // 수집한 이벤트들을 담기 위한 UsageEvents
        val usageEvents = usageStatsManager.queryEvents(begin, end)

        // 이벤트가 여러개 있을 경우 (최소 존재는 해야 hasNextEvent가 null이 아니니까)
        while (usageEvents.hasNextEvent()) {

            // 현재 이벤트를 가져오기
            val event = UsageEvents.Event()
            usageEvents.getNextEvent(event)
            if (event.packageName.endsWith("youtube") || event.packageName.endsWith("pointberry")) {
                runOnUiThread {
                    val msg = "${event.packageName} -> ${event.timeStamp}"
                    textView.append(msg + "\n")
                }
            }

            // 현재 이벤트가 포그라운드 상태라면 = 현재 화면에 보이는 앱이라면
            if (isForeGroundEvent(event)) {
                // 해당 앱 이름을 packageNameMap에 넣는다.
                packageNameMap[event.timeStamp] = event.packageName
                // 가장 최근에 실행 된 이벤트에 대한 타임스탬프를 업데이트 해준다.
                if (event.timeStamp > lastRunAppTimeStamp) {
                    lastRunAppTimeStamp = event.timeStamp
                }
            }
        }
        // 가장 마지막까지 있는 앱의 이름을 리턴해준다.
        return packageNameMap[lastRunAppTimeStamp]
    }

    private fun isForeGroundEvent(event: UsageEvents.Event?): Boolean {
        if (event == null) return false
        return if (Build.VERSION.SDK_INT >= 29) {
            event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
        } else {
            event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
        }
    }

    inner class CheckPackageNameThread : Thread() {
        override fun run() {
            while (true) {
                if (!checkPermission()) continue
                Log.d(TAG, "${getPackageName(applicationContext)}")
                try {
                    sleep(2000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
    }

    companion object {
        const val TAG = "UsageDemoDebug"
    }

}