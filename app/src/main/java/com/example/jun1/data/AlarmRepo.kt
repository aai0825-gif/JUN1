package com.example.jun1.data

import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import com.example.jun1.model.AlarmSpec


@Serializable
data class AlarmSpec(
    val id: String = System.currentTimeMillis().toString(),
    val name: String = "",
    val startHour: Int = 9,
    val startMinute: Int = 0,
    val endHour: Int = 18,
    val endMinute: Int = 0,
    val intervalMinutes: Int = 60,
    val ringSeconds: Int = 5,
    val volumePercent: Int = 50,
    val enabled: Boolean = true,
    val daysEnabled: Set<Int> = setOf(1,2,3,4,5) // 월(1)~일(7)
)

object AlarmRepo {
    private const val TAG = "AlarmRepo"
    private const val FILE_NAME = "alarms.json"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private fun file(ctx: Context): File = File(ctx.filesDir, FILE_NAME)

    /** 손상/구버전 데이터면 빈 리스트로 복구(백업 후 삭제) */
    fun loadAll(ctx: Context): List<AlarmSpec> {
        val f = file(ctx)
        if (!f.exists()) return emptyList()

        return try {
            val text = f.readText()
            if (text.isBlank()) emptyList()
            else json.decodeFromString<List<AlarmSpec>>(text)
        } catch (e: Exception) {
            Log.e(TAG, "loadAll: corrupted/incompatible data. Resetting.", e)
            // 손상 데이터 백업
            try {
                val bak = File(f.parentFile, "$FILE_NAME.bak")
                f.copyTo(bak, overwrite = true)
            } catch (_: Exception) {}
            // 초기화
            try { f.delete() } catch (_: Exception) {}
            emptyList()
        }
    }

    fun upsert(ctx: Context, spec: AlarmSpec) {
        val list = loadAll(ctx).toMutableList()
        val idx = list.indexOfFirst { it.id == spec.id }
        if (idx >= 0) list[idx] = spec else list.add(spec)
        saveAll(ctx, list)
    }

    fun toggle(ctx: Context, id: String, enabled: Boolean) {
        val updated = loadAll(ctx).map { if (it.id == id) it.copy(enabled = enabled) else it }
        saveAll(ctx, updated)
    }

    fun delete(ctx: Context, id: String) {
        val updated = loadAll(ctx).filterNot { it.id == id }
        saveAll(ctx, updated)
    }

    private fun saveAll(ctx: Context, list: List<AlarmSpec>) {
        try {
            file(ctx).writeText(json.encodeToString<List<AlarmSpec>>(list))
        } catch (e: Exception) {
            Log.e(TAG, "saveAll failed", e)
        }
    }
}
