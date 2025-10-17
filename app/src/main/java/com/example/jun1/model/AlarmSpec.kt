package com.example.jun1.model
import java.util.UUID
data class AlarmSpec(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val enabled: Boolean = true,
    val daysEnabled: Set<Int> = setOf(1,2,3,4,5,6,7),
    val startHour: Int = 9,
    val startMinute: Int = 0,
    val endHour: Int = 18,
    val endMinute: Int = 0,
    val intervalMinutes: Int = 60,
    val ringSeconds: Int = 5,
    val volumePercent: Int = 70,
)
