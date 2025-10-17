package com.example.jun1.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.example.jun1.data.AlarmRepo
import com.example.jun1.model.AlarmSpec
import com.example.jun1.model.ScheduleMode
import java.util.Calendar
import kotlin.math.max

object AlarmPlanner {
    const val EXTRA_ALARM_ID = "EXTRA_ALARM_ID"

    fun scheduleAll(ctx: Context) {
        cancelAll(ctx)
        AlarmRepo.loadAll(ctx).filter { it.enabled }.forEach { scheduleNext(ctx, it) }
    }

    fun scheduleNext(ctx: Context, alarm: AlarmSpec) {
        val at = nextTriggerUtcMillis(alarm) ?: return
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            at,
            pi(ctx, alarm.id)
        )
    }

    fun onFired(ctx: Context, id: String) {
        // 울렸으면 다음 회차 예약
        AlarmRepo.find(ctx, id)?.let { if (it.enabled) scheduleNext(ctx, it) }
    }

    fun cancelAll(ctx: Context) {
        AlarmRepo.loadAll(ctx).forEach { cancel(ctx, it.id) }
    }

    fun cancel(ctx: Context, id: String) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pi(ctx, id))
    }

    private fun pi(ctx: Context, id: String): PendingIntent {
        val i = Intent(ctx, AlarmReceiver::class.java).putExtra(EXTRA_ALARM_ID, id)
        return PendingIntent.getBroadcast(
            ctx, id.hashCode(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * AlarmSpec 에서 "다음" 트리거 시각(UTC ms) 계산
     */
    private fun nextTriggerUtcMillis(a: AlarmSpec): Long? {
        val now = Calendar.getInstance()
        // 오늘부터 7일 내 범위만 탐색
        repeat(7) { addDays ->
            val day = (now.clone() as Calendar).apply { add(Calendar.DATE, addDays) }
            val dow = ((day.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1 // Calendar -> 1~7(월~일)
            if (dow !in a.daysEnabled) return@repeat

            when (a.scheduleMode) {
                ScheduleMode.RANGE -> {
                    val start = (day.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, a.startHour); set(Calendar.MINUTE, a.startMinute)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    val end = (day.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, a.endHour); set(Calendar.MINUTE, a.endMinute)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    var t = max(start.timeInMillis, now.timeInMillis)
                    while (t <= end.timeInMillis) {
                        val idx = ((t - start.timeInMillis) / (a.intervalMinutes * 60_000L))
                        val snap = start.timeInMillis + idx * a.intervalMinutes * 60_000L
                        if (snap >= now.timeInMillis && snap <= end.timeInMillis) return snap
                        t += a.intervalMinutes * 60_000L
                    }
                }
                ScheduleMode.TIMES -> {
                    val candidates = a.times.sorted().map { minutes ->
                        (day.clone() as Calendar).apply {
                            set(Calendar.HOUR_OF_DAY, minutes / 60)
                            set(Calendar.MINUTE, minutes % 60)
                            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                    }
                    candidates.firstOrNull { it >= now.timeInMillis }?.let { return it }
                }
            }
        }
        return null
    }
}
