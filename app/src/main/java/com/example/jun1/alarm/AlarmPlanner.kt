package com.example.jun1.alarm

import android.content.Context
import com.example.jun1.data.AlarmRepo
import com.example.jun1.model.AlarmMode
import com.example.jun1.model.AlarmSpec

object AlarmPlanner {

    /**
     * 모든 알람을 다시 스케줄링
     */
    fun rescheduleAll(context: Context) {
        val items: List<AlarmSpec> = AlarmRepo.loadAll(context)
        // 리스트로 확실하게 순회 (Map 아님)
        items.forEach { spec ->
            if (spec.enabled) {
                schedule(context, spec)
            } else {
                cancel(context, spec.id)
            }
        }
    }

    /**
     * 단일 알람 스케줄링
     */
    fun schedule(context: Context, spec: AlarmSpec) {
        when (spec.mode) {
            AlarmMode.Range -> {
                // 예: 09:00~18:00 구간에서 intervalMinutes 마다 예약
                // 실제 플랫폼별 예약은 이전에 쓰던 setExactAndAllowWhileIdle(...) 등으로 연결
                // 여기서는 예시 로직만 남겨둡니다.
                // planRange(context, spec) // 필요시 내부 구현으로 분리
            }
            AlarmMode.SingleTimes -> {
                // 예: times 리스트에 들어있는 (hour, minute) 각각 예약
                // planSingleTimes(context, spec)
            }
        }
        // TODO: 실제 AlarmManager/WorkManager 연동 부분은 기존 프로젝트 코드 유지
    }

    fun cancel(context: Context, alarmId: String) {
        // TODO: 기존 cancel 로직 연결
    }
}
