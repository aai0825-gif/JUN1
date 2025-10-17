package com.example.jun1.alarm
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.jun1.data.AlarmRepo
import com.example.jun1.data.AlarmSpec

import java.util.Calendar
import kotlin.math.abs
object AlarmPlanner {
    const val EXTRA_ALARM_ID = "alarm_id"
    fun scheduleAll(ctx: Context) {
        cancelAll(ctx)
        AlarmRepo.loadAll(ctx).filter { it.enabled }.forEach { scheduleNextForSpec(ctx, it) }
    }
    fun scheduleNextForSpec(ctx: Context, spec: AlarmSpec, now: Long = System.currentTimeMillis()) {
        val at = nextTriggerMillisForSpec(spec, now) ?: return
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pending(ctx, spec.id)
        try { am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi) }
        catch (_: Throwable) { am.setExact(AlarmManager.RTC_WAKEUP, at, pi) }
    }
    fun onFired(ctx: Context, alarmId: String) {
        val spec = AlarmRepo.loadAll(ctx).firstOrNull { it.id == alarmId } ?: return
        if (spec.enabled) scheduleNextForSpec(ctx, spec, System.currentTimeMillis() + 1000)
    }
    fun cancelAll(ctx: Context) = AlarmRepo.loadAll(ctx).forEach { cancelSpec(ctx, it.id) }
    fun cancelSpec(ctx: Context, id: String) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pending(ctx, id))
    }
    private fun pending(ctx: Context, id: String): PendingIntent {
        val req = abs(id.hashCode())
        val intent = Intent(ctx, AlarmReceiver::class.java).putExtra(EXTRA_ALARM_ID, id)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(ctx, req, intent, flags)
    }
    private fun nextTriggerMillisForSpec(spec: AlarmSpec, now: Long): Long? {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        repeat(14) {
            val dayOk = toDow(cal) in spec.daysEnabled
            val slots = if (dayOk) slotsForDay(spec, cal) else emptyList()
            val next = slots.firstOrNull { it > now } ?: run {
                cal.add(Calendar.DAY_OF_YEAR, 1); cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0); null }
            if (next != null) return next
        }
        return null
    }
    private fun slotsForDay(spec: AlarmSpec, base: Calendar): List<Long> {
        val day = Calendar.getInstance().apply { timeInMillis = base.timeInMillis; set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val start = Calendar.getInstance().apply { timeInMillis = day.timeInMillis; set(Calendar.HOUR_OF_DAY, spec.startHour); set(Calendar.MINUTE, spec.startMinute) }
        val end = Calendar.getInstance().apply { timeInMillis = day.timeInMillis; set(Calendar.HOUR_OF_DAY, spec.endHour); set(Calendar.MINUTE, spec.endMinute); if (timeInMillis <= start.timeInMillis) add(Calendar.DAY_OF_YEAR, 1) }
        val out = mutableListOf<Long>(); var t = start.timeInMillis; val step = (spec.intervalMinutes * 60_000L).coerceAtLeast(5 * 60_000L)
        while (t <= end.timeInMillis) { out += t; t += step }; return out
    }
    private fun toDow(cal: Calendar): Int = when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 1; Calendar.TUESDAY -> 2; Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4; Calendar.FRIDAY -> 5; Calendar.SATURDAY -> 6
        else -> 7
    }
}
