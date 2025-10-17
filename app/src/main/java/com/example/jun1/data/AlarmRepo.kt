package com.example.jun1.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import com.example.jun1.model.AlarmSpec

private const val FILE_NAME = "alarms.json"

object AlarmRepo {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private fun file(ctx: Context) = File(ctx.filesDir, FILE_NAME)

    fun loadAll(ctx: Context): List<AlarmSpec> {
        val f = file(ctx)
        if (!f.exists()) return emptyList()
        return runCatching { json.decodeFromString<List<AlarmSpec>>(f.readText()) }.getOrElse { emptyList() }
    }

    private fun saveAll(ctx: Context, list: List<AlarmSpec>) {
        file(ctx).writeText(json.encodeToString(list))
    }

    fun upsert(ctx: Context, spec: AlarmSpec) {
        val cur = loadAll(ctx).toMutableList()
        val idx = cur.indexOfFirst { it.id == spec.id }
        if (idx >= 0) cur[idx] = spec else cur += spec
        saveAll(ctx, cur)
    }

    fun toggle(ctx: Context, id: String, enabled: Boolean) {
        val cur = loadAll(ctx).map { if (it.id == id) it.copy(enabled = enabled) else it }
        saveAll(ctx, cur)
    }

    fun delete(ctx: Context, id: String) {
        val cur = loadAll(ctx).filterNot { it.id == id }
        saveAll(ctx, cur)
    }

    fun find(ctx: Context, id: String): AlarmSpec? = loadAll(ctx).firstOrNull { it.id == id }
}
