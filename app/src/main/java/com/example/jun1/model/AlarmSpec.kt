package com.example.jun1.model

import java.util.UUID

// 알람이 시간 "범위"인지, "특정시간"들인지
enum class AlarmMode { Range, SingleTimes }

data class AlarmSpec(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "알람",
    val enabled: Boolean = true,

    // 1=월 ... 7=일
    val daysEnabled: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),

    // 알람 방식
    val mode: AlarmMode = AlarmMode.Range,

    // 범위 방식 (예: 09:00 ~ 18:00)
    val startHour: Int = 9,
    val startMinute: Int = 0,
    val endHour: Int = 18,
    val endMinute: Int = 0,

    // 특정시간 방식 (하루에 여러 시각)
    val times: List<Pair<Int, Int>> = emptyList(),

    // 기존 옵션
    val intervalMinutes: Int = 60,
    val volumePercent: Int = 50,
    val ringSeconds: Int = 5,

    // 알람음(벨소리) URI 문자열
    val ringtoneUri: String? = null,
)
