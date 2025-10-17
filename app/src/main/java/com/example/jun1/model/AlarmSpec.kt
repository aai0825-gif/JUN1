package com.example.jun1.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class ScheduleMode { RANGE, TIMES }

/**
 * 앱에서 사용하는 알람 데이터 모델
 */
@Serializable
data class AlarmSpec(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "알람",
    val enabled: Boolean = true,

    // 요일: 1=월 ~ 7=일 (Calendar 의 요일과 동일)
    val daysEnabled: Set<Int> = setOf(1,2,3,4,5,6,7),

    // 모드 1: 구간 반복 (start~end 사이 intervalMinutes 마다)
    val startHour: Int = 9,
    val startMinute: Int = 0,
    val endHour: Int = 18,
    val endMinute: Int = 0,

    // 모드 2: 특정 시각(예: 10:00, 14:30 …) – 분 단위의 “하루 분” 값(0~1439)
    val times: List<Int> = emptyList(),

    val scheduleMode: ScheduleMode = ScheduleMode.RANGE,
    val intervalMinutes: Int = 60,

    val volumePercent: Int = 50,
    val ringSeconds: Int = 5,

    // 선택한 알람음(벨소리) URI (없으면 디폴트)
    val ringtoneUri: String? = null
)
