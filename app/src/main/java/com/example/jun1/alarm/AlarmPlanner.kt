package com.example.jun1.alarm

import android.content.Context
import android.util.Log
import com.example.jun1.data.AlarmRepo
import com.example.jun1.model.AlarmSpec

/**
 * 알람 예약/취소/발사 처리를 담당하는 중앙 모듈.
 * 지금은 컴파일 목적의 최소 구현(stub)으로 Log만 남깁니다.
 * 나중에 AlarmManager 연동 로직을 여기에 채워 넣으면 됩니다.
 */
object AlarmPlanner {
    const val EXTRA_ALARM_ID = "com.example.jun1.extra.ALARM_ID"
    private const val TAG = "AlarmPlanner"

    /** 부팅 후 모든 활성 알람 재예약 */
    fun scheduleAll(ctx: Context) {
        runCatching {
            AlarmRepo.loadAll(ctx)
                .filter { it.enabled }
                .forEach { spec -> scheduleOne(ctx, spec) }
        }.onFailure { Log.e(TAG, "scheduleAll failed", it) }
    }

    /** 개별 알람 예약 (stub) */
    fun scheduleOne(ctx: Context, spec: AlarmSpec) {
        // TODO: AlarmManager로 실제 예약 구현
        Log.d(TAG, "scheduleOne: id=${spec.id}, name=${spec.name}")
    }

    /** 개별 알람 취소 (stub) */
    fun cancelOne(ctx: Context, id: String) {
        // TODO: AlarmManager에서 해당 id의 PendingIntent 취소 구현
        Log.d(TAG, "cancelOne: id=$id")
    }

    /** 알람 발사 시 콜백 (stub) */
    fun onFired(ctx: Context, alarmId: String?) {
        Log.d(TAG, "onFired: id=$alarmId")
        // TODO: 다음 스케줄 예약/알림 표시 등 실제 동작 구현
    }
}
