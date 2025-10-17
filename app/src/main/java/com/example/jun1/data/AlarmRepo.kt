package com.example.jun1.data
import android.content.Context
import android.util.Log
import com.example.jun1.model.AlarmSpec
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
object AlarmRepo {
    private const val PREF = "alarms_store"
    private const val KEY = "alarms_json"
    private val json = Json { ignoreUnknownKeys = true }
    fun loadAll(ctx: Context): List<AlarmSpec> {
        val sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val raw = sp.getString(KEY, null) ?: return emptyList()
        return try { json.decodeFromString(raw) } catch (e: Exception) { Log.e("AlarmRepo","decode", e); emptyList() }
    }
    fun saveAll(ctx: Context, list: List<AlarmSpec>) {
        val sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        sp.edit().putString(KEY, json.encodeToString(list)).apply()
    }
    fun upsert(ctx: Context, spec: AlarmSpec) {
        val cur = loadAll(ctx).toMutableList()
        val i = cur.indexOfFirst { it.id == spec.id }
        if (i >= 0) cur[i] = spec else cur += spec
        saveAll(ctx, cur)
    }
    fun delete(ctx: Context, id: String) = saveAll(ctx, loadAll(ctx).filterNot { it.id == id })
    fun toggle(ctx: Context, id: String, enabled: Boolean) =
        saveAll(ctx, loadAll(ctx).map { if (it.id == id) it.copy(enabled = enabled) else it })
}
