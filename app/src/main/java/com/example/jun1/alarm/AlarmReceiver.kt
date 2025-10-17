package com.example.jun1.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.media.Ringtone
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(AlarmPlanner.EXTRA_ALARM_ID) ?: return

        // 간단한 벨소리 + 진동 (알림/포그라운드 서비스는 이후 확장)
        val uri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ring: Ringtone = RingtoneManager.getRingtone(context, uri)
        ring.play()

        (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(
            VibrationEffect.createOneShot(800, VibrationEffect.DEFAULT_AMPLITUDE)
        )

        // 다음 회차 예약
        AlarmPlanner.onFired(context, id)
    }
}
