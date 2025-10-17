package com.example.jun1
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel("jun1_alarm","JUN1 알람", NotificationManager.IMPORTANCE_HIGH)
            ch.description = "알람 알림 채널"
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }
}
