package com.flasskdev.vibe.ui.components

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.flasskdev.vibe.data.VibeWebSocket
import com.flasskdev.vibe.data.VibeWebSocketListener
import org.json.JSONArray
import org.json.JSONObject

/**
 * Стикеры: паки приходят с сервера (таблица flasskdev_mobilestickerpacks),
 * локальные паки из assets/stickers/<pack>/ поддерживаются как раньше.
 *
 * Формат стикера в сообщении: "sticker:<path>", где path это либо
 * "<pack>/<file>" (assets), либо полный https-URL (серверный пак).
 */
data class StickerItem(
    val id: String,
    /** "<pack>/<file>" для assets или https URL для серверных стикеров. */
    val path: String
)

data class StickerPack(
    val id: String,
    val title: String,
    val stickers: List<StickerItem>,
    val ownerId: Int = 0,
    val isOwner: Boolean = false,
    /** сколько всего стикеров в паке по данным сервера (для "+N" в поиске) */
    val totalCount: Int = stickers.size,
    val isLocal: Boolean = false,
    val isInstalled: Boolean = true
) {
    /** Короткое имя для подписи под иконками паков. */
    fun shortTitle(max: Int = 24): String =
        if (title.length <= max) title else title.take(max - 1).trimEnd() + "…"
}

object StickerRepository {

    private const val ROOT = "stickers"
    private val imageExtensions = setOf("webp", "png", "gif", "jpg", "jpeg")

    @Volatile
    private var localCache: List<StickerPack>? = null

    /** Локальные паки из assets. Кэшируются после первого вызова. */
    fun loadLocalPacks(context: Context): List<StickerPack> {
        localCache?.let { return it }
        val packs = try {
            context.assets.list(ROOT)?.toList().orEmpty().mapNotNull { pack ->
                val files = context.assets.list("$ROOT/$pack")?.toList().orEmpty()
                    .filter { it.substringAfterLast('.', "").lowercase() in imageExtensions }
                    .sorted()
                if (files.isEmpty()) null
                else StickerPack(
                    id = "local:$pack",
                    title = pack.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                    stickers = files.map { StickerItem(id = "$pack/$it", path = "$pack/$it") },
                    isLocal = true
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
        localCache = packs
        return packs
    }

    @Deprecated("используй loadLocalPacks / StickerPacksStore.packs")
    fun loadPacks(context: Context): List<StickerPack> = loadLocalPacks(context)

    /**
     * Превращает путь стикера в то, что умеет грузить Coil.
     * Понимает и https-URL (серверные паки), и assets-путь (локальные).
     */
    fun resolve(pathOrId: String): String = when {
        pathOrId.startsWith("http://") || pathOrId.startsWith("https://") -> pathOrId
        pathOrId.startsWith("file://") || pathOrId.startsWith("content://") -> pathOrId
        else -> "file:///android_asset/$ROOT/$pathOrId"
    }

    /** Оставлено для совместимости со старым кодом (StickerMessage и т.п.). */
    fun assetUri(stickerId: String): String = resolve(stickerId)

    /** "sticker:<path>" -> "<path>" */
    fun idFromContent(content: String): String = content.removePrefix("sticker:")
}

/* ============================ СЕРВЕРНЫЕ ПАКИ + КЭШ ============================ */

/**
 * Кэширующее хранилище стикерпаков.
 *
 * - в памяти: список паков как Compose-observable список (панель открывается мгновенно);
 * - на диске: SharedPreferences JSON, чтобы после перезапуска приложения
 *   не ждать сервер и не мигать пустой панелью;
 * - сеть (WebSocket) дергается не чаще, чем раз в [TTL_MS], либо по force = true.
 */
object StickerPacksStore {

    private const val PREFS = "sticker_packs_cache"
    private const val KEY_PACKS = "packs_json"
    private const val KEY_TS = "packs_ts"
    private const val TTL_MS = 15 * 60 * 1000L      // как часто обновляем список паков
    private const val SEARCH_TTL_MS = 5 * 60 * 1000L

    private val _packs = mutableStateListOf<StickerPack>()
    val packs: List<StickerPack> get() = _packs

    private val _searchResults = mutableStateListOf<StickerPack>()
    val searchResults: List<StickerPack> get() = _searchResults

    var isLoading = mutableStateOf(false)
        private set
    var isSearching = mutableStateOf(false)
        private set

    private var lastFetchTs = 0L
    private var loadedFromDisk = false
    private var attachedTo: VibeWebSocket? = null
    private var appContext: Context? = null

    /** query -> (ts, packs) */
    private val searchCache = LinkedHashMap<String, Pair<Long, List<StickerPack>>>()
    private var lastQuery: String = ""

    /* --------------------- публичное API --------------------- */

    fun ensureLoaded(context: Context) {
        appContext = context.applicationContext
        if (loadedFromDisk) return
        synchronized(this) {
            if (loadedFromDisk) return
            val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val raw = p.getString(KEY_PACKS, null)
            lastFetchTs = p.getLong(KEY_TS, 0L)
            if (!raw.isNullOrBlank()) {
                runCatching { parsePacks(JSONArray(raw)) }.getOrNull()?.let {
                    _packs.clear()
                    _packs.addAll(it)
                }
            }
            loadedFromDisk = true
        }
    }

    /** Запрашивает паки, если кэш устарел. Вызывать при открытии панели. */
    fun refresh(context: Context, ws: VibeWebSocket, userId: Int, force: Boolean = false) {
        ensureLoaded(context)
        attach(ws)
        if (userId <= 0) return
        val stale = System.currentTimeMillis() - lastFetchTs > TTL_MS
        if (!force && !stale && _packs.isNotEmpty()) return
        isLoading.value = _packs.isEmpty()
        ws.getStickerPacks(userId)
    }

    fun search(ws: VibeWebSocket, userId: Int, query: String) {
        attach(ws)
        val q = query.trim()
        lastQuery = q
        val cached = searchCache[q]
        if (cached != null && System.currentTimeMillis() - cached.first < SEARCH_TTL_MS) {
            _searchResults.clear()
            _searchResults.addAll(cached.second)
            isSearching.value = false
            return
        }
        isSearching.value = true
        ws.searchStickerPacks(query = q, userId = userId)
    }

    fun clearSearch() {
        lastQuery = ""
        _searchResults.clear()
        isSearching.value = false
    }

    /** Удалить пак у себя (оптимистично, до ответа сервера). */
    fun remove(ws: VibeWebSocket, userId: Int, packId: String) {
        attach(ws)
        val numeric = packId.toIntOrNull() ?: return
        _packs.removeAll { it.id == packId }
        persist()
        ws.removeStickerPack(userId = userId, packId = numeric)
    }

    /** Добавить пак себе (из поиска). */
    fun add(ws: VibeWebSocket, userId: Int, pack: StickerPack) {
        attach(ws)
        val numeric = pack.id.toIntOrNull() ?: return
        if (_packs.none { it.id == pack.id }) {
            _packs.add(pack.copy(isInstalled = true))
            persist()
        }
        ws.addStickerPack(userId = userId, packId = numeric)
    }

    /* --------------------- WebSocket --------------------- */

    private fun attach(ws: VibeWebSocket) {
        if (attachedTo === ws) return
        attachedTo = ws
        ws.addListener(object : VibeWebSocketListener {
            override fun onStickerPacksResult(packsJson: JSONArray) {
                val parsed = runCatching { parsePacks(packsJson) }.getOrNull() ?: return
                _packs.clear()
                _packs.addAll(parsed)
                lastFetchTs = System.currentTimeMillis()
                isLoading.value = false
                persist()
            }

            override fun onStickerPacksSearchResult(query: String, packsJson: JSONArray) {
                val parsed = runCatching { parsePacks(packsJson) }.getOrNull() ?: return
                searchCache[query.trim()] = System.currentTimeMillis() to parsed
                while (searchCache.size > 30) {
                    searchCache.remove(searchCache.keys.first())
                }
                if (query.trim() == lastQuery) {
                    _searchResults.clear()
                    _searchResults.addAll(parsed)
                    isSearching.value = false
                }
            }

            override fun onStickerPackRemoved(packId: Int) {
                _packs.removeAll { it.id == packId.toString() }
                searchCache.clear()
                persist()
            }

            override fun onStickerPackAdded(packId: Int) {
                searchCache.clear()
                lastFetchTs = 0L
            }
        })
    }

    /* --------------------- parsing / persist --------------------- */

    private fun parsePacks(arr: JSONArray): List<StickerPack> {
        val out = mutableListOf<StickerPack>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val stickersArr = o.optJSONArray("stickers") ?: JSONArray()
            val stickers = mutableListOf<StickerItem>()
            for (j in 0 until stickersArr.length()) {
                when (val raw = stickersArr.opt(j)) {
                    is JSONObject -> {
                        val path = raw.optString("path")
                        if (path.isNotBlank()) {
                            stickers.add(StickerItem(id = raw.optString("id", j.toString()), path = path))
                        }
                    }
                    is String -> if (raw.isNotBlank()) stickers.add(StickerItem(j.toString(), raw))
                }
            }
            out.add(
                StickerPack(
                    id = o.optInt("id", 0).toString(),
                    title = o.optString("name", "Пак"),
                    stickers = stickers,
                    ownerId = o.optInt("owner", 0),
                    isOwner = o.optBoolean("is_owner", false),
                    totalCount = o.optInt("sticker_count", stickers.size),
                    isInstalled = o.optBoolean("is_installed", true)
                )
            )
        }
        return out
    }

    private fun packsToJson(list: List<StickerPack>): JSONArray {
        val arr = JSONArray()
        list.filter { !it.isLocal }.forEach { p ->
            val st = JSONArray()
            p.stickers.forEach { s ->
                st.put(JSONObject().put("id", s.id).put("path", s.path))
            }
            arr.put(
                JSONObject()
                    .put("id", p.id.toIntOrNull() ?: 0)
                    .put("name", p.title)
                    .put("owner", p.ownerId)
                    .put("is_owner", p.isOwner)
                    .put("is_installed", p.isInstalled)
                    .put("sticker_count", p.totalCount)
                    .put("stickers", st)
            )
        }
        return arr
    }

    private fun persist() {
        val ctx = appContext ?: return
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PACKS, packsToJson(_packs).toString())
            .putLong(KEY_TS, lastFetchTs)
            .apply()
    }
}