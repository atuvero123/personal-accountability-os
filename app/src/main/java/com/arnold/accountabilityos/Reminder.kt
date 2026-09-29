package com.arnold.accountabilityos

import android.app.*
import android.content.*
import android.os.Build
import androidx.core.app.NotificationCompat
import java.time.LocalDateTime
import java.time.ZoneId

object ReminderScheduler{
    private val reminders=listOf(Triple(630,"Start intentionally","Wake up, move and begin before scrolling."),Triple(725,"Prayer & Bible","Open Spiritual for today's prayer focus and Bible goal."),Triple(800,"Plan the day","Set your Big 3 and protect the morning betslip hour."),Triple(1000,"Morning finance check","Record your current balances."),Triple(2115,"Evening finance check","Record spending and savings progress."),Triple(2125,"Daily review","Review discipline, postponements, phone use and tomorrow."),Triple(2240,"Shutdown","No feeds. Prepare for sleep by 11:00 pm."))
    fun scheduleAll(context:Context){
        val manager=context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        reminders.forEachIndexed{index,(hhmm,title,body)->
            val h=hhmm/100;val m=hhmm%100
            var at=LocalDateTime.now().withHour(h).withMinute(m).withSecond(0).withNano(0)
            if(!at.isAfter(LocalDateTime.now()))at=at.plusDays(1)
            val intent=Intent(context,ReminderReceiver::class.java).putExtra("title",title).putExtra("body",body).putExtra("requestCode",1000+index)
            val pending=PendingIntent.getBroadcast(context,1000+index,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),pending)
        }
    }
}
object NotificationHelper{
    fun notify(context:Context,title:String,body:String,id:Int){
        val manager=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel="accountability_alerts"
        if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(NotificationChannel(channel,"Accountability alerts",NotificationManager.IMPORTANCE_DEFAULT))
        runCatching{manager.notify(id,NotificationCompat.Builder(context,channel).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(body).setAutoCancel(true).build())}
    }
}
class BootReceiver:BroadcastReceiver(){
    override fun onReceive(context:Context,intent:Intent){if(intent.action==Intent.ACTION_BOOT_COMPLETED||intent.action==Intent.ACTION_MY_PACKAGE_REPLACED)ReminderScheduler.scheduleAll(context)}
}
class ReminderReceiver:BroadcastReceiver(){
    override fun onReceive(context:Context,intent:Intent){
        val channel="accountability_reminders";val manager=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(NotificationChannel(channel,"Accountability reminders",NotificationManager.IMPORTANCE_DEFAULT))
        runCatching{manager.notify(intent.getIntExtra("requestCode",999),NotificationCompat.Builder(context,channel).setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle(intent.getStringExtra("title")?:"Accountability OS").setContentText(intent.getStringExtra("body")?:"Check your plan.").setAutoCancel(true).build())}
        ReminderScheduler.scheduleAll(context)
    }
}
