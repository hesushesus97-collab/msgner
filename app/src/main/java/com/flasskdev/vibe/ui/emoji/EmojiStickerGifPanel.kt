package com.flasskdev.vibe.ui.emoji

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import com.flasskdev.vibe.ui.components.VibeSearchField
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import com.flasskdev.vibe.ui.components.EmojiData
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flasskdev.vibe.ui.theme.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/* ============================================================================
 *  ПУНКТЫ 12 И 13 — ПАНЕЛЬ ЭМОДЗИ/СТИКЕРОВ/ГИФОК
 * ============================================================================
 *
 *  ПУНКТ 12: ПОЧЕМУ ЛАГАЛО ПРИ СКРОЛЛЕ (реальные причины из старого кода)
 *  ---------------------------------------------------------------------
 *  1. НЕТ contentType у элементов сетки.
 *     Без него Compose не может переиспользовать поддеревья между
 *     элементами при скролле: заголовок секции и ячейка эмодзи считаются
 *     «одинаковыми» слотами, и на каждый вылетающий/влетающий элемент
 *     заново строится вся композиция. Это главная причина.
 *
 *  2. Ключи собирались строками в горячем цикле:
 *         key = { index, emoji -> "${cat.id}_${index}_$emoji" }
 *     На флинге это тысячи аллокаций String в секунду -> работа GC
 *     ровно в тот момент, когда нужны стабильные 60/120 fps.
 *
 *  3. StickerCell вызывал rememberAnimatedImageLoader() и
 *     ImageRequest.Builder(context)...build() ВНУТРИ КАЖДОЙ ЯЧЕЙКИ.
 *     Новый ImageRequest на каждую рекомпозицию = промах кэша Coil
 *     по equals + новая корутина загрузки. Отсюда моргание стикеров.
 *
 *  4. EmojiCell читал MaterialTheme.* и создавал TextStyle на каждый кадр.
 *     Разметка текста для эмодзи дорогая (составные графемы, ZWJ).
 *
 *  5. RecentsStore держал состояние в SharedPreferences и читал его
 *     синхронно на главном потоке при открытии панели.
 *
 *  6. Лямбда onClick создавалась заново для каждой ячейки -> все ячейки
 *     считались изменившимися и перерисовывались.
 *
 *  ЧТО СДЕЛАНО:
 *   - плоский предвычисленный список элементов (строится ОДИН раз, вне
 *     композиции) с готовыми ключами и contentType;
 *   - contentType проставлен всем элементам -> переиспользование слотов;
 *   - один общий ImageLoader и ОДИН ImageRequest на путь, закэшированный;
 *   - TextStyle вынесен в константу;
 *   - обработчик клика стабилизирован через @Stable-интерфейс;
 *   - недавние читаются из Room через Flow, без блокировки главного потока;
 *   - предзагрузка стикеров на 2 экрана вперёд.
 *
 *  ПУНКТ 13: вкладка «Недавние» удалена полностью.
 *  Недавние стикеры теперь первая секция внутри вкладки стикеров,
 *  ровно как в Telegram, и пустого экрана «тут будут ваши недавние»
 *  больше не существует.
 * ========================================================================== */

/* -------------------------------------------------------------------------- */
/*  Стабильные обработчики: одна ссылка на всю панель                          */
/* -------------------------------------------------------------------------- */

@Stable
interface PanelCallbacks {
    fun onEmoji(emoji: String)
    fun onSticker(path: String)
    fun onGif(url: String)
}

/* -------------------------------------------------------------------------- */
/*  Предвычисленная модель сетки                                               */
/* -------------------------------------------------------------------------- */

@Immutable
sealed interface GridEntry {
    val key: String
    val type: String

    @Immutable
    data class Header(val title: String, override val key: String) : GridEntry {
        override val type get() = TYPE
        companion object { const val TYPE = "hdr" }
    }

    @Immutable
    data class Emoji(val value: String, override val key: String) : GridEntry {
        override val type get() = TYPE
        companion object { const val TYPE = "emj" }
    }

    @Immutable
    data class Sticker(val path: String, override val key: String) : GridEntry {
        override val type get() = TYPE
        companion object { const val TYPE = "stk" }
    }
}

/**
 * Плоский список эмодзи строится один раз на процесс.
 * Раньше он пересобирался при каждом открытии панели.
 */
object EmojiGridModel {
    val entries: List<GridEntry> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildList {
            EmojiData.categories.forEach { cat ->
                add(GridEntry.Header(cat.title, "h_${cat.id}"))
                cat.emojis.forEachIndexed { i, e ->
                    // Ключ вычислен ЗАРАНЕЕ: на скролле ни одной аллокации.
                    add(GridEntry.Emoji(e, "${cat.id}#$i"))
                }
            }
        }
    }

    /** categoryId -> позиция заголовка в плоском списке (для быстрой перемотки). */
    val headerIndex: Map<String, Int> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildMap {
            EmojiGridModel.entries.forEachIndexed { index, entry ->
                if (entry is GridEntry.Header) put(entry.key.removePrefix("h_"), index)
            }
        }
    }
}

private val EMOJI_TEXT_STYLE = TextStyle(
    fontSize = 26.sp,
    textAlign = TextAlign.Center,
    // Отключаем всё, что заставляет Compose делать лишнюю работу при разметке.
    lineHeight = 30.sp,
    platformStyle = androidx.compose.ui.text.PlatformTextStyle(
        emojiSupportMatch = androidx.compose.ui.text.EmojiSupportMatch.None
    )
)

private val HEADER_TEXT_STYLE = TextStyle(
    fontSize = 12.sp,
    fontWeight = FontWeight.SemiBold
)

/* -------------------------------------------------------------------------- */
/*  Панель                                                                     */
/* -------------------------------------------------------------------------- */

enum class PanelTab { EMOJI, STICKERS, GIFS }   // <- RECENT удалён (пункт 13)

@Composable
fun EmojiStickerGifPanel(
    callbacks: PanelCallbacks,
    recentStickers: List<String> = emptyList(),
    recentEmojis: List<String> = emptyList(),
    installedPacks: List<StickerPackUi> = emptyList(),
    trendingGifs: List<GifUi> = emptyList(),
    modifier: Modifier = Modifier
) {
    var tab by rememberSaveable { mutableStateOf(PanelTab.EMOJI) }
    val glass = glassStyle()

    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Содержимое ПЕРВЫМ, переключатель снизу: палец уже внизу экрана,
        // тянуться наверх за вкладками неудобно (так же в Telegram).
        Box(Modifier.weight(1f)) {
            when (tab) {
                PanelTab.EMOJI -> EmojiPage(recentEmojis, callbacks)
                PanelTab.STICKERS -> StickerPage(recentStickers, installedPacks, callbacks)
                PanelTab.GIFS -> GifPage(trendingGifs, callbacks)
            }
        }

        PanelTabBar(tab) { tab = it }
    }
}

/* ------------------------------- ЭМОДЗИ ---------------------------------- */

@Composable
private fun EmojiPage(recent: List<String>, callbacks: PanelCallbacks) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val strings = LocalVibeStrings.current

    // Недавние подмешиваем в начало, но список категорий НЕ пересобираем:
    // берём готовый неизменяемый EmojiGridModel.entries и добавляем префикс.
    val recentEntries = remember(recent, strings) {
        if (recent.isEmpty()) emptyList()
        else buildList {
            add(GridEntry.Header(strings.panelRecent, "h_recent"))
            recent.forEachIndexed { i, e -> add(GridEntry.Emoji(e, "r#$i")) }
        }
    }
    val offset = recentEntries.size

    Column(Modifier.fillMaxSize()) {
        CategoryStrip(
            hasRecent = recentEntries.isNotEmpty(),
            onPick = { categoryId ->
                EmojiGridModel.headerIndex[categoryId]?.let { base ->
                    scope.launch { gridState.animateScrollToItem(base + offset) }
                }
            },
            onRecent = { scope.launch { gridState.scrollToItem(0) } }
        )

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(8),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = VibeSpacing.sm, vertical = VibeSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            emojiEntries(recentEntries, callbacks)
            emojiEntries(EmojiGridModel.entries, callbacks)
        }
    }
}

/**
 * Ключевой момент по производительности: у каждого элемента задан
 * `contentType`. Compose держит пул переиспользуемых нод отдельно для
 * заголовков и отдельно для ячеек, поэтому при флинге ноды не создаются
 * заново, а переиспользуются. Без этого параметра сетка лагает всегда,
 * какой бы лёгкой ни была ячейка.
 */
private fun androidx.compose.foundation.lazy.grid.LazyGridScope.emojiEntries(
    entries: List<GridEntry>,
    callbacks: PanelCallbacks
) {
    items(
        items = entries,
        key = { it.key },
        contentType = { it.type },
        span = { entry ->
            if (entry is GridEntry.Header) GridItemSpan(maxLineSpan) else GridItemSpan(1)
        }
    ) { entry ->
        when (entry) {
            is GridEntry.Header -> SectionHeader(entry.title)
            is GridEntry.Emoji -> EmojiCell(entry.value, callbacks)
            is GridEntry.Sticker -> Unit
        }
    }
}

@Composable
private fun EmojiCell(emoji: String, callbacks: PanelCallbacks) {
    Box(
        Modifier
            .aspectRatio(1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                // Ripple на ячейке эмодзи — лишний слой отрисовки на каждом касании.
                indication = null
            ) { callbacks.onEmoji(emoji) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            style = EMOJI_TEXT_STYLE,     // константа, а не MaterialTheme.*
            fontFamily = AppleFontProvider.emojiOnlyFamily,
            softWrap = false,
            maxLines = 1
        )
    }
}

/* ------------------------------ СТИКЕРЫ ---------------------------------- */

@Immutable
data class StickerPackUi(val id: Int, val title: String, val stickers: List<String>)

@Composable
private fun StickerPage(
    recent: List<String>,
    packs: List<StickerPackUi>,
    callbacks: PanelCallbacks
) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val strings = LocalVibeStrings.current

    /* ---------------- ПУНКТ 13 ----------------
     * Недавние — первая секция ЭТОГО списка, а не отдельная вкладка.
     * Показываем максимум 16 (2 ряда по 8): больше в Telegram и не бывает,
     * а меньше рядов — меньше работы при первом кадре.
     */
    val entries = remember(recent, packs, strings) {
        buildList {
            val trimmed = recent.take(16)
            if (trimmed.isNotEmpty()) {
                add(GridEntry.Header(strings.panelRecent, "h_recent"))
                trimmed.forEachIndexed { i, p -> add(GridEntry.Sticker(p, "r#$i")) }
            }
            packs.forEach { pack ->
                add(GridEntry.Header(pack.title, "h_p${pack.id}"))
                pack.stickers.forEachIndexed { i, p -> add(GridEntry.Sticker(p, "p${pack.id}#$i")) }
            }
        }
    }

    val headerPositions = remember(entries) {
        entries.withIndex().filter { it.value is GridEntry.Header }.associate { it.value.key to it.index }
    }

    /* Предзагрузка: подтягиваем в кэш Coil стикеры на два экрана вперёд.
     * Без этого при быстром скролле видны пустые квадраты. */
    LaunchedEffect(entries) {
        snapshotFlow { gridState.firstVisibleItemIndex }
            .map { it / 8 }
            .distinctUntilChanged()
            .collect { row ->
                val from = (row + 1) * 8
                entries.asSequence()
                    .drop(from).take(48)
                    .filterIsInstance<GridEntry.Sticker>()
                    .forEach { StickerImageCache.preload(context, it.path) }
            }
    }

    Column(Modifier.fillMaxSize()) {
        StickerPackStrip(
            hasRecent = recent.isNotEmpty(),
            packs = packs,
            onPick = { key -> scope.launch { gridState.scrollToItem(headerPositions[key] ?: 0) } }
        )

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = VibeSpacing.sm, vertical = VibeSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(VibeSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(VibeSpacing.xs)
        ) {
            items(
                items = entries,
                key = { it.key },
                contentType = { it.type },
                span = { if (it is GridEntry.Header) GridItemSpan(maxLineSpan) else GridItemSpan(1) }
            ) { entry ->
                when (entry) {
                    is GridEntry.Header -> SectionHeader(entry.title)
                    is GridEntry.Sticker -> StickerCell(entry.path, callbacks)
                    is GridEntry.Emoji -> Unit
                }
            }
        }
    }
}

@Composable
private fun StickerCell(path: String, callbacks: PanelCallbacks) {
    val context = LocalContext.current
    // ImageRequest кэшируется по пути: один и тот же объект между
    // рекомпозициями -> Coil попадает в память-кэш, а не перезапускает загрузку.
    val animate = VibeEffects.animatedPreviews
    val request = remember(path, animate) { if (animate) StickerImageCache.request(context, path) else StickerImageCache.request(context, path).newBuilder(context).memoryCacheKey("static-preview:$path").decoderFactory(coil.decode.BitmapFactoryDecoder.Factory()).build() }

    Box(
        Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(VibeRadius.sm))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { callbacks.onSticker(path) },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = request,
            contentDescription = null,
            imageLoader = StickerImageCache.loader(context),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(4.dp)
        )
    }
}

/* -------------------------------- ГИФКИ ---------------------------------- */

@Immutable
data class GifUi(val id: String, val previewUrl: String, val fullUrl: String, val aspect: Float)

@Composable
private fun GifPage(gifs: List<GifUi>, callbacks: PanelCallbacks) {
    val context = LocalContext.current
    val strings = LocalVibeStrings.current
    var query by rememberSaveable { mutableStateOf("") }
    var loaded by remember { mutableStateOf(gifs) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(query, retry, gifs) {
        if (query.isBlank() && gifs.isNotEmpty()) { loaded = gifs; return@LaunchedEffect }
        loading = true; error = null
        try {
            kotlinx.coroutines.delay(300)
            val result = com.flasskdev.vibe.data.network.GiphyApi.search(query)
            loaded = result.distinctBy { it.id }.map {
                GifUi(it.id, it.previewUrl, it.fullUrl, if (it.height > 0 && it.width > 0) it.width.toFloat() / it.height else 1f)
            }
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { loaded = emptyList(); error = e.message ?: strings.gifLoadFailed }
        finally { loading = false }
    }
    Column(Modifier.fillMaxSize()) {
        // Поле поиска в общем стиле вместо материального OutlinedTextField с плавающим label.
        VibeSearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = strings.gifSearchPlaceholder,
            clearContentDescription = strings.a11yClearField,
            modifier = Modifier.padding(horizontal = VibeSpacing.sm, vertical = VibeSpacing.sm)
        )
        AnimatedVisibility(
            visible = loading,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VibeSpacing.sm)
                    .height(2.dp)
                    .clip(RoundedCornerShape(VibeRadius.pill)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )
        }
        if (!loading && loaded.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VibeSpacing.lg, vertical = VibeSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = error ?: strings.gifNotFound,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = VibeSpacing.md)
                )
                if (error != null) {
                    TextButton(onClick = { retry++ }) { Text(strings.toastActionRetry) }
                }
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2), modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(VibeSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(VibeSpacing.xs), verticalArrangement = Arrangement.spacedBy(VibeSpacing.xs)
        ) {
            items(items = loaded, key = { it.id }, contentType = { "gif" }) { gif ->
                val animate = VibeEffects.animatedPreviews
                val request = remember(gif.previewUrl, animate) {
                    ImageRequest.Builder(context).data(gif.previewUrl).memoryCacheKey("gif-preview:${gif.previewUrl}:$animate")
                        .apply { if (!animate) decoderFactory(coil.decode.BitmapFactoryDecoder.Factory()) }
                        .build()
                }
                AsyncImage(
                    model = request, contentDescription = strings.gifSendCd,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(gif.aspect.coerceIn(0.6f, 1.8f))
                        .clip(RoundedCornerShape(VibeRadius.sm)).clickable { callbacks.onGif(gif.fullUrl) }
                )
            }
        }
    }
}

/* ------------------------------ ОБЩЕЕ ------------------------------------ */

@Composable
private fun SectionHeader(title: String) {
    val strings = LocalVibeStrings.current
    val localizedTitle = when (title) {
        "Смайлики", "smileys" -> strings.emojiCategorySmileys
        "Жесты и люди", "gestures" -> strings.emojiCategoryGestures
        "Сердца и символы", "hearts" -> strings.emojiCategoryHearts
        "Животные и природа", "animals" -> strings.emojiCategoryAnimals
        "Еда и напитки", "food" -> strings.emojiCategoryFood
        "Активности", "activities" -> strings.emojiCategoryActivities
        "Путешествия", "travel" -> strings.emojiCategoryTravel
        "Объекты", "objects" -> strings.emojiCategoryObjects
        else -> title
    }
    Text(
        text = localizedTitle,
        style = HEADER_TEXT_STYLE,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = VibeSpacing.xs, top = VibeSpacing.md, bottom = VibeSpacing.xs)
    )
}

@Composable
private fun CategoryStrip(hasRecent: Boolean, onPick: (String) -> Unit, onRecent: () -> Unit) {
    LazyRow(
        Modifier.fillMaxWidth().height(44.dp),
        contentPadding = PaddingValues(horizontal = VibeSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(VibeSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasRecent) {
            item(key = "recent", contentType = "chip") {
                StripChip(emoji = "🕘", onClick = onRecent)
            }
        }
        items(
            items = EmojiData.categories,
            key = { it.id },
            contentType = { "chip" }
        ) { cat ->
            StripChip(emoji = cat.icon) { onPick(cat.id) }
        }
    }
}

@Composable
private fun StickerPackStrip(hasRecent: Boolean, packs: List<StickerPackUi>, onPick: (String) -> Unit) {
    val context = LocalContext.current
    LazyRow(
        Modifier.fillMaxWidth().height(48.dp),
        contentPadding = PaddingValues(horizontal = VibeSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(VibeSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasRecent) {
            item(key = "h_recent", contentType = "chip") {
                StripChip(emoji = "🕘") { onPick("h_recent") }
            }
        }
        items(items = packs, key = { it.id }, contentType = { "packchip" }) { pack ->
            val first = pack.stickers.firstOrNull()
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(VibeRadius.xs))
                    .clickable { onPick("h_p${pack.id}") },
                contentAlignment = Alignment.Center
            ) {
                if (first != null) {
                    AsyncImage(
                        model = remember(first) { StickerImageCache.request(context, first) },
                        contentDescription = pack.title,
                        imageLoader = StickerImageCache.loader(context),
                        modifier = Modifier.fillMaxSize().padding(4.dp)
                    )
                } else {
                    Text(pack.title.take(1), style = HEADER_TEXT_STYLE)
                }
            }
        }
    }
}

@Composable
private fun StripChip(emoji: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = 19.sp,
            fontFamily = AppleFontProvider.emojiOnlyFamily,
            style = EMOJI_TEXT_STYLE.copy(fontSize = 19.sp)
        )
    }
}

@Composable
private fun PanelTabBar(selected: PanelTab, onSelect: (PanelTab) -> Unit) {
    val strings = LocalVibeStrings.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Вкладка «Недавние» удалена: см. пункт 13.
        TabButton(strings.panelTabEmoji, selected == PanelTab.EMOJI) { onSelect(PanelTab.EMOJI) }
        TabButton(strings.panelTabStickers, selected == PanelTab.STICKERS) { onSelect(PanelTab.STICKERS) }
        TabButton(strings.panelTabGifs, selected == PanelTab.GIFS) { onSelect(PanelTab.GIFS) }
    }
}

@Composable
private fun TabButton(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(VibeRadius.pill))
            .clickable(onClick = onClick)
            .background(if (active) VibeViolet.copy(alpha = 0.14f) else Color.Transparent)
            .padding(horizontal = VibeSpacing.lg, vertical = VibeSpacing.sm)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (active) VibeViolet else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}