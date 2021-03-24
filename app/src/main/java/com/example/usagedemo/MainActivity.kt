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

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        var lastRunAppTimeStamp = 0L

        val INTERVAL: Long = POLL_INTERVAL + 1000
        val end = System.currentTimeMillis()
        val begin = end - INTERVAL

        val packageNameMap = hashMapOf<Long, String>()

        val usageEvents = usageStatsManager.queryEvents(begin, end)

        while (usageEvents.hasNextEvent()) {

            val event = UsageEvents.Event()
            usageEvents.getNextEvent(event)

            if (isForeGroundEvent(event)) {
                packageNameMap[event.timeStamp] = event.packageName
                if (event.timeStamp > lastRunAppTimeStamp) {
                    lastRunAppTimeStamp = event.timeStamp
                }
            }
        }

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
                if (!checkPermission()) {
                    continue
                }

                try {
                    val packageName = getPackageName(applicationContext)
                    packageName?.let {
                        if (it.endsWith("youtube") || it.endsWith("pointberry")) {
                            runOnUiThread {
                                textView.append("$it \n")
                            }
                        }
                    }

                    sleep(POLL_INTERVAL)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
    }

    companion object {
        const val TAG = "UsageDemoDebug"
        const val POLL_INTERVAL = 2000L
    }

}