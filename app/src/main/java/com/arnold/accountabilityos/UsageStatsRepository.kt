package com.arnold.accountabilityos

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import java.time.LocalDate
import java.time.ZoneId

class UsageStatsRepository(private val context:Context){
    fun hasUsageAccess():Boolean{
        val ops=context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,Process.myUid(),context.packageName)==AppOpsManager.MODE_ALLOWED
    }
    fun openUsageAccessSettings(){context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}
    fun todayUsage():List<UsageRow>{
        if(!hasUsageAccess())return emptyList()
        val manager=context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val start=LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val stats=manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,start,System.currentTimeMillis()).orEmpty()
        val pm=context.packageManager
        return stats.filter{it.totalTimeInForeground>=60000}.map{
            val label=runCatching{pm.getApplicationLabel(pm.getApplicationInfo(it.packageName,0)).toString()}.getOrElse{it.packageName}
            UsageRow(it.packageName,label,it.totalTimeInForeground/60000)
        }.sortedByDescending{it.minutes}
    }
    fun likelySocialMinutes(rows:List<UsageRow>):Long{
        val packages=setOf("com.zhiliaoapp.musically","com.google.android.youtube","com.instagram.android","com.facebook.katana","com.twitter.android","com.reddit.frontpage","com.snapchat.android")
        return rows.filter{it.packageName in packages}.sumOf{it.minutes}
    }
}
