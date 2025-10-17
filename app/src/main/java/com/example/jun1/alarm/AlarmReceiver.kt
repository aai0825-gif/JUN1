package com.example.jun1.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(AlarmPlanner.EXTRA_ALARM_ID)
        AlarmPlanner.onFired(context, id ?: "")
    }
}
