package com.flasskdev.vibe.data.network

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Кэш результатов GIPHY.
 *
 * Зачем: раньше каждое открытие панели (и каждый возврат на вкладку GIF)
 * заново дергало /trending, а любой повтор запроса — /search. Теперь:
 *
 *  1) память  — мгновенная отдача, TTL [MEM_TTL_MS];
 *  2) диск    — SharedPreferences JSON, живёт между запусками, TTL [DISK_TTL_MS];
 *  3) дедупликация — параллельные запросы одного и того же ключа
 *     не превращаются в N сетевых вызовов (Mutex + повторная проверка кэша);
 *  4) сами картинки уже кэшируются singleton-ом ImageLoader (256MB disk cache),
 *     так что после первого показа GIF не перекачивается.
 */
object GifCache {

    private const val PREFS = "gif_cache"
    private const val MEM_TTL_MS = 30 * 60 * 1000L
    private const val DISK_TTL_MS = 24 * 60 * 60 * 1000L
    private const val MAX_MEM_ENTRIES = 40
    private const val MAX_DISK_ENTRIES = 12

    private data class Entry(val ts: Long, val items: List<GifItem>)

    /** LRU: LinkedHashMap с accessOrder = true. */
    private val memory = object : LinkedHashMap<String, Entry>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>?): Boolean =
            size > MAX_MEM_ENTRIES
    }

    private val locks = HashMap<String, Mutex>()
    private val locksGuard = Mutex()

    private fun key(query: String, offset: Int) = "${query.trim().lowercase()}#$offset"

    private suspend fun lockFor(k: String): Mutex = locksGuard.withLock {
        locks.getOrPut(k) { Mutex() }
    }

    /** Мгновенная (синхронная) выдача из памяти — для первого кадра композиции. */
    fun peek(query: String, offset: Int = 0): List<GifItem>? = synchronized(memory) {
        val e = memory[key(query, offset)] ?: return null
        if (System.currentTimeMillis() - e.ts > MEM_TTL_MS) null else e.items
    }

    /**
     * Главная точка входа: отдаёт кэш, если он свежий, иначе идёт в сеть
     * и кладёт результат в память + на диск.
     */
    suspend fun get(
        context: Context,
        query: String,
        offset: Int = 0,
        limit: Int = 27,
        force: Boolean = false
    ): List<GifItem> {
        val k = key(query, offset)

        if (!force) {
            peek(query, offset)?.let { return it }
            loadFromDisk(context, k)?.let { items ->
                synchronized(memory) { memory[k] = Entry(System.currentTimeMillis(), items) }
                return items
            }
        }

        return lockFor(k).withLock {
            // Пока ждали мьютекс, кто-то мог уже всё загрузить.
            if (!force) peek(query, offset)?.let { return@withLock it }

            val fresh = if (query.isBlank()) {
                GiphyApi.trending(offset = offset, limit = limit)
            } else {
                GiphyApi.search(query = query, offset = offset, limit = limit)
            }

            if (fresh.isNotEmpty()) {
                synchronized(memory) { memory[k] = Entry(System.currentTimeMillis(), fresh) }
                saveToDisk(context, k, fresh)
            }
            fresh
        }
    }

    fun clear(context: Context) {
        synchronized(memory) { memory.clear() }
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    /* ------------------------- диск ------------------------- */

    private fun loadFromDisk(context: Context, k: String): List<GifItem>? {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = p.getString(k, null) ?: return null
        return try {
            val obj = JSONObject(raw)
            if (System.currentTimeMillis() - obj.optLong("ts") > DISK_TTL_MS) {
                p.edit().remove(k).apply()
                null
            } else {
                val arr = obj.optJSONArray("items") ?: return null
                val out = ArrayList<GifItem>(arr.length())
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    out.add(
                        GifItem(
                            id = o.optString("id"),
                            previewUrl = o.optString("preview"),
                            fullUrl = o.optString("full"),
                            width = o.optInt("w", 0),
                            height = o.optInt("h", 0)
                        )
                    )
                }
                out.takeIf { it.isNotEmpty() }
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun saveToDisk(context: Context, k: String, items: List<GifItem>) =
        withContext(Dispatchers.IO) {
            try {
                val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                // не даём кэшу распухать
                if (p.all.size >= MAX_DISK_ENTRIES) {
                    val oldest = p.all.keys.firstOrNull()
                    if (oldest != null) p.edit().remove(oldest).apply()
                }
                val arr = JSONArray()
                items.forEach { g ->
                    arr.put(
                        JSONObject()
                            .put("id", g.id)
                            .put("preview", g.previewUrl)
                            .put("full", g.fullUrl)
                            .put("w", g.width)
                            .put("h", g.height)
                    )
                }
                p.edit()
                    .putString(k, JSONObject().put("ts", System.currentTimeMillis()).put("items", arr).toString())
                    .apply()
            } catch (e: Exception) {
                // кэш — не критичный путь, молча игнорируем
            }
        }
}