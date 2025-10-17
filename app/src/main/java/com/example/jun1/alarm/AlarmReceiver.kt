package com.example.jun1.alarm
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.jun1.R
import com.example.jun1.data.AlarmRepo
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(AlarmPlanner.EXTRA_ALARM_ID) ?: return
        val spec = AlarmRepo.loadAll(context).firstOrNull { it.id == id } ?: return
        AlarmPlanner.onFired(context, id)
        val nm = NotificationManagerCompat.from(context)
        val noti = NotificationCompat.Builder(context, "jun1_alarm")
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(spec.name.ifBlank { "알람" })
            .setContentText("지금 시간입니다")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .build()
        nm.notify(id.hashCode(), noti)
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val ring = RingtoneManager.getRingtone(context, uri)
        ring.play()
        Handler(Looper.getMainLooper()).postDelayed({ try { ring.stop() } catch (_:Throwable){} },
            (spec.ringSeconds * 1000L).coerceAtLeast(3000L))
    }
}
