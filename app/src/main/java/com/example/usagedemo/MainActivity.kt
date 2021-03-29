package com.example.usagedemo

import android.app.AppOpsManager
import android.app.AppOpsManager.OPSTR_GET_USAGE_STATS
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.AppOpsManagerCompat.MODE_ALLOWED
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private var checkPackageNameThread: CheckPackageNameThread? = null
    private var checkPacakgeNameThread1: CheckPackageNameThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPackageNameThread = CheckPackageNameThread()
        checkPackageNameThread?.start()

        val checkPackageNameThread1 = CheckPackageNameThread()
        checkPackageNameThread1.start()


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
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        return mode == MODE_ALLOWED
    }

    fun getPackageName(context: Context) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.YEAR, -1)

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

//        val startTime = GregorianCalendar(2014, 0, 1).timeInMillis
//        val endTime = GregorianCalendar(2016, 0, 1).timeInMillis

        val stats =
            usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                cal.timeInMillis,
                System.currentTimeMillis()
            )

        showUsageStats(stats)
    }

    private fun showUsageStats(stats: List<UsageStats>) {
        val sorted = stats.sortedWith(kotlin.Comparator { right, left ->
            compareValues(
                left.lastTimeUsed,
                right.lastTimeUsed
            )
        })
        sorted.forEach { stat ->
            Log.d(
                TAG,
                "packageName:${stat.packageName} " +
                        "lastTimeUsed:${Date(stat.lastTimeUsed)} " +
                        "foregroundTotalTime:${stat.totalTimeInForeground}"
            )
        }
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
                    getPackageName(applicationContext)
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