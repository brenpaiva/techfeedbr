package br.com.techfeedbr.ui

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkAdded
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import br.com.techfeedbr.TechFeedUiState
import br.com.techfeedbr.TechFeedViewModel
import br.com.techfeedbr.domain.ContentItem
import br.com.techfeedbr.domain.SavedMode
import br.com.techfeedbr.domain.SortMode
import coil3.compose.AsyncImage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private val Categories = listOf(
    "Inteligência Artificial", "Desenvolvimento", "Segurança", "Dados",
    "Mobile", "Cloud e DevOps", "Carreira",
    "Hardware", "Internet e Apps", "Tecnologia",
)
private val Sources = linkedMapOf(
    "TECNOBLOG" to "Tecnoblog", "OLHARDIGITAL" to "Olhar Digital",
    "TUDOCELULAR" to "TudoCelular", "HARDWARE" to "Hardware.com.br",
    "MUNDOCONECTADO" to "Mundo Conectado", "MACMAGAZINE" to "MacMagazine",
    "ADRENALINE" to "Adrenaline",
    "EBC" to "Agência Brasil", "DEV" to "DEV",
)

private enum class Destination { FEED, SAVED, SETTINGS }

@Composable
fun TechFeedApp(state: TechFeedUiState, viewModel: TechFeedViewModel) {
    if (!state.preferences.onboardingCompleted) {
        OnboardingScreen(viewModel)
    } else {
        MainShell(state, viewModel)
    }
}

@Composable
private fun OnboardingScreen(viewModel: TechFeedViewModel) {
    var categories by remember { mutableStateOf(setOf<String>()) }
    var sources by remember { mutableStateOf(Sources.keys.toSet()) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Text("TechFeed BR", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Tecnologia relevante, organizada em um só lugar.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        item {
            Text("Quais assuntos mais interessam?", style = MaterialTheme.typography.headlineSmall)
            Text("Você pode alterar tudo depois.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            ChipRows(Categories, categories) { value ->
                categories = if (value in categories) categories - value else categories + value
            }
        }
        item {
            Text("Fontes", style = MaterialTheme.typography.titleLarge)
            ChipRows(Sources.values.toList(), sources.mapNotNull { Sources[it] }.toSet()) { label ->
                val code = Sources.entries.first { it.value == label }.key
                sources = if (code in sources) sources - code else sources + code
            }
        }
        item {
            Button(
                onClick = { viewModel.completeOnboarding(categories, sources.ifEmpty { Sources.keys }) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Montar meu feed") }
            TextButton(
                onClick = { viewModel.completeOnboarding(emptySet(), Sources.keys) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Pular e mostrar tudo") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShell(state: TechFeedUiState, viewModel: TechFeedViewModel) {
    var destination by remember { mutableStateOf(Destination.FEED) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    val openFilters = { drawerScope.launch { drawerState.open() }; Unit }
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = destination == Destination.FEED,
        drawerContent = {
            ModalDrawerSheet(Modifier.widthIn(max = 360.dp)) {
                FeedFilterDrawer(state, viewModel, onClose = { drawerScope.launch { drawerState.close() } })
            }
        },
    ) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (destination == Destination.FEED) IconButton(onClick = openFilters) {
                        Icon(Icons.Outlined.Menu, contentDescription = "Abrir filtros do feed")
                    }
                },
                title = {
                    Column {
                        Text("TechFeed BR", fontWeight = FontWeight.Bold)
                        if (state.stale) Text("Algumas fontes estão desatualizadas", style = MaterialTheme.typography.labelSmall)
                    }
                },
                actions = {
                    if (state.loading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    IconButton(onClick = viewModel::refresh, enabled = !state.loading) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Atualizar feed")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = destination == Destination.FEED,
                    onClick = { destination = Destination.FEED },
                    icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
                    label = { Text("Feed") },
                )
                NavigationBarItem(
                    selected = destination == Destination.SAVED,
                    onClick = { destination = Destination.SAVED },
                    icon = { Icon(Icons.Outlined.BookmarkAdded, contentDescription = null) },
                    label = { Text("Salvos") },
                )
                NavigationBarItem(
                    selected = destination == Destination.SETTINGS,
                    onClick = { destination = Destination.SETTINGS },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    label = { Text("Ajustes") },
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (destination) {
                Destination.FEED -> FeedScreen(state, viewModel, onOpenFilters = openFilters)
                Destination.SAVED -> SavedScreen(state, viewModel)
                Destination.SETTINGS -> SettingsScreen(state, viewModel)
            }
        }
    }
    }
}

@Composable
private fun FeedFilterDrawer(state: TechFeedUiState, viewModel: TechFeedViewModel, onClose: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Filtros do feed", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Ajuste a ordem, o idioma e os assuntos das notícias.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = viewModel::clearFeedFilters, modifier = Modifier.weight(1f)) {
                    Text("Limpar filtros")
                }
                Button(onClick = onClose, modifier = Modifier.weight(1f)) { Text("Ver notícias") }
            }
        }
        item { HorizontalDivider() }
        item {
            Text("Ordenação", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.sortMode == SortMode.DATE,
                    onClick = { viewModel.setSortMode(SortMode.DATE) }, label = { Text("Mais recentes") })
                FilterChip(selected = state.sortMode == SortMode.RELEVANCE,
                    onClick = { viewModel.setSortMode(SortMode.RELEVANCE) }, label = { Text("Para você") })
            }
        }
        item {
            Text("Idioma", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.preferences.portugueseOnly,
                    onClick = { viewModel.setPortugueseOnly(true) }, label = { Text("Português") })
                FilterChip(selected = !state.preferences.portugueseOnly,
                    onClick = { viewModel.setPortugueseOnly(false) }, label = { Text("Todos") })
            }
        }
        item {
            FilterChip(selected = state.unreadOnly,
                onClick = { viewModel.setUnreadOnly(!state.unreadOnly) }, label = { Text("Somente não lidos") })
        }
        item { HorizontalDivider() }
        item {
            Text("Assuntos", style = MaterialTheme.typography.titleMedium)
            Text("Sem seleção: todos os assuntos", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Categories.forEach { category ->
                    FilterChip(selected = category in state.selectedCategories,
                        onClick = { viewModel.setCategory(category) }, label = { Text(category) },
                        modifier = Modifier.testTag("topic-filter-$category"))
                }
            }
        }
    }
}

@Composable
private fun FeedScreen(state: TechFeedUiState, viewModel: TechFeedViewModel, onOpenFilters: () -> Unit) {
    var headerCollapsed by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }
    var searchEditing by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val activeCount = state.selectedCategories.size +
        (if (state.unreadOnly) 1 else 0) + (if (!state.preferences.portugueseOnly) 1 else 0) +
        (if (state.sortMode != SortMode.DATE) 1 else 0)
    LaunchedEffect(state.items.isEmpty()) {
        if (state.items.isEmpty()) headerCollapsed = false
    }
    LaunchedEffect(searchExpanded) {
        if (searchExpanded) searchFocusRequester.requestFocus()
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().animateContentSize()) {
            if (!headerCollapsed) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)) {
                    Text("SEU RADAR", style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Text("Tecnologia em foco", style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text("Notícias e ideias de fontes diferentes, em uma leitura só.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        if (state.demoMode) {
            OutlinedCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                Text("Conteúdo de exemplo: nenhuma notícia real está disponível agora. Atualize para tentar novamente.",
                    modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary)
            }
        } else if (state.stale && state.allItems.isNotEmpty()) {
            AssistChip(onClick = viewModel::refresh,
                label = { Text("Algumas fontes falharam. Toque para tentar atualizar.") },
                modifier = Modifier.padding(horizontal = 16.dp))
        }
        if (headerCollapsed && !searchExpanded && !searchEditing) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { searchExpanded = true }) {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(if (state.query.isBlank()) "Buscar notícia" else "Busca: ${state.query.take(16)}",
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                TextButton(onClick = onOpenFilters) {
                    Icon(Icons.Outlined.Menu, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(if (activeCount == 0) "Filtros" else "Filtros $activeCount")
                }
            }
        } else {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                label = { Text("Buscar por palavra-chave") },
                singleLine = true,
                trailingIcon = if (headerCollapsed) {{
                    IconButton(onClick = {
                        focusManager.clearFocus()
                        searchEditing = false
                        searchExpanded = false
                    }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Recolher busca")
                    }
                }} else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.recordSearch() }),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(searchFocusRequester)
                    .onFocusChanged { searchEditing = it.isFocused },
            )
        }
        if (!headerCollapsed) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onOpenFilters) {
                    Icon(Icons.Outlined.Menu, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (activeCount == 0) "Filtrar notícias" else "Filtros ativos: $activeCount")
                }
            }
        }
        }
        state.error?.let { message ->
            AssistChip(onClick = viewModel::refresh, label = { Text(message) }, modifier = Modifier.padding(horizontal = 16.dp))
        }
        if (state.loading && state.items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            ContentList(state.items, viewModel, emptyMessage = "Nenhum conteúdo encontrado com estes filtros.",
                hasMore = state.hasMore && !state.demoMode, loadingMore = state.loadingMore,
                onLoadMore = viewModel::loadNextPage,
                onScrollAwayFromTop = { away ->
                    headerCollapsed = away
                    if (away && !searchEditing) searchExpanded = false
                })
        }
    }
}

@Composable
private fun SavedScreen(state: TechFeedUiState, viewModel: TechFeedViewModel) {
    Column(Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = if (state.savedMode == SavedMode.FAVORITES) 0 else 1) {
            Tab(
                selected = state.savedMode == SavedMode.FAVORITES,
                onClick = { viewModel.setSavedMode(SavedMode.FAVORITES) },
                text = { Text("Favoritos") },
            )
            Tab(
                selected = state.savedMode == SavedMode.READ_LATER,
                onClick = { viewModel.setSavedMode(SavedMode.READ_LATER) },
                text = { Text("Ler depois") },
            )
        }
        val saved = state.allItems.filter {
            (!state.preferences.portugueseOnly || it.language.startsWith("pt", ignoreCase = true)) &&
                (if (state.savedMode == SavedMode.FAVORITES) it.favorite else it.readLater)
        }
        ContentList(saved, viewModel, emptyMessage = "Sua lista está vazia.")
    }
}

@Composable
private fun ContentList(
    items: List<ContentItem>, viewModel: TechFeedViewModel, emptyMessage: String,
    hasMore: Boolean = false, loadingMore: Boolean = false, onLoadMore: () -> Unit = {},
    onScrollAwayFromTop: (Boolean) -> Unit = {},
) {
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyMessage, modifier = Modifier.padding(32.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= 700.dp) {
            val gridState = rememberLazyGridState()
            LaunchedEffect(gridState) {
                snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
                    .collect { (index, offset) -> onScrollAwayFromTop(index > 0 || offset > 120) }
            }
            LaunchedEffect(gridState, items.size, hasMore) {
                snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
                    .collect { if (hasMore && it >= items.size - 4) onLoadMore() }
            }
            LazyVerticalGrid(columns = GridCells.Fixed(2), state = gridState,
                modifier = Modifier.testTag("feed-content-list"),
                contentPadding = PaddingValues(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                gridItems(items, key = ContentItem::id) { item ->
                    ContentCard(item, viewModel, featured = item.id == items.first().id)
                }
                if (hasMore || loadingMore) item(span = { GridItemSpan(maxLineSpan) }) {
                    LoadMore(loadingMore, onLoadMore)
                }
            }
        } else {
            val listState = rememberLazyListState()
            LaunchedEffect(listState) {
                snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
                    .collect { (index, offset) -> onScrollAwayFromTop(index > 0 || offset > 120) }
            }
            LaunchedEffect(listState, items.size, hasMore) {
                snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
                    .collect { if (hasMore && it >= items.size - 4) onLoadMore() }
            }
            LazyColumn(state = listState, modifier = Modifier.testTag("feed-content-list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items(items, key = ContentItem::id) { item ->
                    ContentCard(item, viewModel, featured = item.id == items.first().id)
                }
                if (hasMore || loadingMore) item { LoadMore(loadingMore, onLoadMore) }
            }
        }
    }
}

@Composable
private fun LoadMore(loading: Boolean, onLoadMore: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
        if (loading) CircularProgressIndicator(Modifier.size(28.dp))
        else OutlinedButton(onClick = onLoadMore) { Text("Carregar mais notícias") }
    }
}

@Composable
private fun ContentCard(item: ContentItem, viewModel: TechFeedViewModel, featured: Boolean = false) {
    val context = LocalContext.current
    var imageFailed by remember(item.id, item.imageUrl) { mutableStateOf(false) }
    var imageLoaded by remember(item.id, item.imageUrl) { mutableStateOf(false) }
    val openOriginal: () -> Unit = {
        viewModel.viewed(item)
        runCatching { CustomTabsIntent.Builder().build().launchUrl(context, item.canonicalUrl.toUri()) }
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (item.viewedAt == null) 1f else 0.78f)
            .semantics { contentDescription = "${item.title}, fonte ${sourceName(item.primarySource)}" }
            .clickable(enabled = !item.isExample, onClick = openOriginal),
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(if (featured) 220.dp else 150.dp)) {
                TechThumbnail(item, Modifier.fillMaxSize())
                if (item.imageUrl?.startsWith("https://") == true && !imageFailed) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        onSuccess = { imageLoaded = true },
                        onError = { imageFailed = true },
                        modifier = Modifier.fillMaxSize().alpha(if (imageLoaded) 1f else 0f),
                    )
                }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        sourceName(item.primarySource),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (item.isExample) "EXEMPLO" else formatDate(item.publishedAt),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (item.isExample) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant)
                    val languageBadge = when {
                        item.primarySource == "DEV" -> null // Older cached DEV items may still carry the former assumed "en".
                        item.language.startsWith("en") -> "EN"
                        item.language.startsWith("es") -> "ES"
                        else -> null
                    }
                    if (languageBadge != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(languageBadge, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary)
                    }
                    if (item.contentType == "AUDIO") {
                        Spacer(Modifier.width(8.dp))
                        Text("ÁUDIO", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary)
                    }
                }
                Text(item.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                item.summary?.let {
                    Text(it, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    item.categories.take(2).forEach { category ->
                        Text(category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { viewModel.favorite(item) }, enabled = !item.isExample,
                        modifier = Modifier.size(48.dp)) {
                        Icon(
                            if (item.favorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (item.favorite) "Remover dos favoritos" else "Favoritar",
                            tint = if (item.favorite) Color(0xFFE04B5A) else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { viewModel.readLater(item) }, enabled = !item.isExample,
                        modifier = Modifier.size(48.dp)) {
                        Icon(
                            if (item.readLater) Icons.Outlined.BookmarkAdded else Icons.Outlined.BookmarkAdd,
                            contentDescription = if (item.readLater) "Remover de ler depois" else "Ler depois",
                        )
                    }
                    if (!item.isExample) IconButton(onClick = openOriginal, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = "Abrir fonte original")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(state: TechFeedUiState, viewModel: TechFeedViewModel) {
    var categories by remember(state.preferences.categories) { mutableStateOf(state.preferences.categories) }
    var sources by remember(state.preferences.sources) { mutableStateOf(state.preferences.sources) }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Text("Preferências", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            ChipRows(Categories, categories) { value ->
                categories = if (value in categories) categories - value else categories + value
            }
            ChipRows(Sources.values.toList(), sources.mapNotNull { Sources[it] }.toSet()) { label ->
                val code = Sources.entries.first { it.value == label }.key
                sources = if (code in sources) sources - code else sources + code
            }
            Button(onClick = { viewModel.savePreferences(categories, sources) }) { Text("Salvar preferências") }
        }
        item { HorizontalDivider() }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(if (state.preferences.darkTheme) Icons.Outlined.DarkMode else Icons.Outlined.LightMode, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("Tema escuro", modifier = Modifier.weight(1f))
                Switch(checked = state.preferences.darkTheme, onCheckedChange = viewModel::setDarkTheme)
            }
        }
        item {
            Text("Status das fontes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            state.sourceStatuses.filterNot { it.source == "TABNEWS" || it.source == "GNEWS" }
                .forEach { status ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(status.displayName, modifier = Modifier.weight(1f))
                    Text(if (status.refreshing) "ATUALIZANDO" else healthLabel(status.health),
                        color = healthColor(status.health))
                }
                status.lastSuccessAt?.let {
                    Text("Última atualização: ${formatDate(it)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                status.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
        item { EvaluationPanel(state, viewModel) }
        item {
            OutlinedCard(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Text(
                    "Privacidade: favoritos, histórico e métricas permanecem neste aparelho. " +
                            "Nenhum texto pesquisado, nome ou identificador do dispositivo é registrado.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun EvaluationPanel(state: TechFeedUiState, viewModel: TechFeedViewModel) {
    var consent by remember { mutableStateOf(false) }
    var sessionCode by remember { mutableStateOf("") }
    var taskCode by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Modo de avaliação", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        val activeCode = state.evaluationSessionCode
        if (activeCode == null) {
            if (state.allItems.isEmpty()) {
                Text("A avaliação fica disponível quando houver notícias reais no aparelho.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "Ao iniciar uma sessão, o app registra neste aparelho ações como abrir, " +
                    "favoritar e salvar notícias, além do tempo desde o início da tarefa. " +
                    "O texto das buscas não é registrado. Você pode exportar ou excluir " +
                    "os registros em Ajustes.",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(sessionCode, { sessionCode = it }, label = { Text("Código anônimo da sessão") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(taskCode, { taskCode = it }, label = { Text("Código da tarefa") }, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = consent, onCheckedChange = { consent = it })
                Spacer(Modifier.width(8.dp))
                Text("Li as informações acima e concordo com o registro local da sessão.")
            }
            Button(
                onClick = { viewModel.startEvaluation(sessionCode, taskCode, consent) },
                enabled = state.allItems.isNotEmpty() && consent &&
                    sessionCode.isNotBlank() && taskCode.isNotBlank(),
            ) { Text("Iniciar sessão") }
            state.evaluationSessionCodes.forEach { previousCode ->
                Text("Sessão registrada: $previousCode", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.exportEvaluation(previousCode) }) {
                        Icon(Icons.Outlined.SaveAlt, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Exportar")
                    }
                    TextButton(onClick = { viewModel.deleteEvaluation(previousCode) }) {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Excluir dados")
                    }
                }
            }
        } else {
            Text("Sessão ativa: $activeCode", color = MaterialTheme.colorScheme.secondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::completeEvaluationTask) {
                    Icon(Icons.Outlined.TaskAlt, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Concluir tarefa")
                }
                OutlinedButton(onClick = viewModel::stopEvaluation) { Text("Encerrar") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.exportEvaluation(activeCode) }) {
                    Icon(Icons.Outlined.SaveAlt, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Exportar CSV/JSON")
                }
            }
        }
    }
}

@Composable
private fun ChipRows(values: List<String>, selected: Set<String>, onToggle: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        values.forEach { value ->
            FilterChip(selected = value in selected, onClick = { onToggle(value) }, label = { Text(value) })
        }
    }
}

private fun sourceName(code: String) = if (code == "EXAMPLE") "Conteúdo de exemplo" else Sources[code] ?: code

private fun formatDate(value: String): String = runCatching {
    DateTimeFormatter.ofPattern("dd MMM", Locale.forLanguageTag("pt-BR"))
        .withZone(ZoneId.systemDefault()).format(Instant.parse(value))
}.getOrDefault(value.take(10))

@Composable
private fun healthColor(health: String) = when (health) {
    "AVAILABLE" -> MaterialTheme.colorScheme.secondary
    "DISABLED" -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.error
}

private fun healthLabel(health: String) = when (health) {
    "AVAILABLE" -> "DISPONÍVEL"
    "DISABLED" -> "DESABILITADA"
    "UNAVAILABLE" -> "INDISPONÍVEL"
    "STALE" -> "DESATUALIZADA"
    "NEVER_UPDATED" -> "AGUARDANDO"
    else -> health
}
