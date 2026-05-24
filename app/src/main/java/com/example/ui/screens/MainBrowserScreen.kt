package com.example.ui.screens

import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.BookmarkItem
import com.example.data.database.ExtensionItem
import com.example.data.database.HistoryItem
import com.example.data.database.TabItem
import com.example.data.database.WorkspaceItem
import com.example.ui.BrowserViewModel
import com.example.ui.components.WebViewContainer
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainBrowserScreen(viewModel: BrowserViewModel) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // State bindings
    val currentWorkspaceId by viewModel.currentWorkspaceId.collectAsStateWithLifecycle()
    val isBottomToolbar by viewModel.isBottomToolbar.collectAsStateWithLifecycle()
    val blockedTrackersCount by viewModel.blockedTrackersCount.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val extensions by viewModel.extensions.collectAsStateWithLifecycle()
    val workspaces by viewModel.workspaces.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    // AI and search engine preferences state collections
    val selectedSearchEngine by viewModel.searchEngine.collectAsStateWithLifecycle()
    val currentAiSearchQuery by viewModel.currentAiSearchQuery.collectAsStateWithLifecycle()
    val aiSummaryResult by viewModel.aiSummaryResult.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val isAiSelectingEngine by viewModel.isAiSelectingEngine.collectAsStateWithLifecycle()
    val aiDetectedEngine by viewModel.aiDetectedEngine.collectAsStateWithLifecycle()

    // Screen-level navigation overlay states
    var showTabsSheet by remember { mutableStateOf(false) }
    var showExtensionsSheet by remember { mutableStateOf(false) }
    var showBookmarksHistorySheet by remember { mutableStateOf(false) }
    var showPrivacyArmorSheet by remember { mutableStateOf(false) }

    // Loading & progress states
    var loadingProgress by remember { mutableStateOf(0) }
    var isPageLoading by remember { mutableStateOf(false) }

    // Address bar query input state
    var addressInputText by remember { mutableStateOf("") }

    // Keep address bar synced with active tab's real-time loaded url
    LaunchedEffect(activeTab?.url) {
        activeTab?.url?.let {
            if (it == "https://duckduckgo.com") {
                addressInputText = ""
            } else {
                addressInputText = it
            }
        }
    }

    // Modern Drawer state
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = ZenDarkBG,
                drawerTonalElevation = 8.dp,
                modifier = Modifier.width(280.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Drawer Header
                    Text(
                        text = "Zen Workspace",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZenPurpleLight,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Zen Browser ile sekmelerinizi farklı odak alanlarında gruplayın.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    HorizontalDivider(color = ZenOutlines, thickness = 1.dp, modifier = Modifier.padding(bottom = 16.dp))

                    // Workspace Item List
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(workspaces) { ws ->
                            val isSelected = ws.id == currentWorkspaceId
                            val wsColor = Color(android.graphics.Color.parseColor(ws.colorHex))
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) ZenCardBG else Color.Transparent)
                                    .clickable {
                                        viewModel.selectWorkspace(ws.id)
                                        scope.launch { drawerState.close() }
                                    }
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) wsColor.copy(alpha = 0.5f) else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Dynamic theme icon simulation
                                Icon(
                                    imageVector = when(ws.iconName) {
                                        "home" -> Icons.Default.Home
                                        "work" -> Icons.Default.Build
                                        "school" -> Icons.Default.Info
                                        "visibility_off" -> Icons.Default.Check
                                        else -> Icons.AutoMirrored.Filled.List
                                    },
                                    contentDescription = ws.name,
                                    tint = wsColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = ws.name,
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color.LightGray
                                    )
                                    if (isSelected) {
                                        Text(
                                            text = "Aktif Alan",
                                            fontSize = 10.sp,
                                            color = wsColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1.0f))

                    // Sidebar lower telemetry
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ZenCardBG),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Shield",
                                    tint = ZenTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Güvenlik Raporu", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Engellenen İzleyici: $blockedTrackersCount", fontSize = 12.sp, color = ZenTeal, fontWeight = FontWeight.Bold)
                            Text("Zen koruması aktif. Kişisel verileriniz Firefox Gecko güvencesinde saklanır.", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = ZenDarkBG,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (isBottomToolbar) {
                    BrowserCommandLine(
                        url = addressInputText,
                        onUrlChange = { addressInputText = it },
                        activeTab = activeTab,
                        tabsCount = tabs.size,
                        blockedCount = blockedTrackersCount,
                        isPageLoading = isPageLoading,
                        onBackPressed = { viewModel.triggerBackNavigation() },
                        onInputFocused = { /* Keep tracking */ },
                        onGoClick = {
                            viewModel.loadUrl(addressInputText)
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        },
                        onTabsClick = { showTabsSheet = true },
                        onMenuClick = { showExtensionsSheet = true },
                        onWorkspaceClick = { scope.launch { drawerState.open() } },
                        onHeartClick = { showBookmarksHistorySheet = true },
                        onShieldClick = { showPrivacyArmorSheet = true },
                        modifier = Modifier.navigationBarsPadding()
                    )
                }
            },
            topBar = {
                if (!isBottomToolbar) {
                    BrowserCommandLine(
                        url = addressInputText,
                        onUrlChange = { addressInputText = it },
                        activeTab = activeTab,
                        tabsCount = tabs.size,
                        blockedCount = blockedTrackersCount,
                        isPageLoading = isPageLoading,
                        onBackPressed = { viewModel.triggerBackNavigation() },
                        onInputFocused = { /* Tracking */ },
                        onGoClick = {
                            viewModel.loadUrl(addressInputText)
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        },
                        onTabsClick = { showTabsSheet = true },
                        onMenuClick = { showExtensionsSheet = true },
                        onWorkspaceClick = { scope.launch { drawerState.open() } },
                        onHeartClick = { showBookmarksHistorySheet = true },
                        onShieldClick = { showPrivacyArmorSheet = true },
                        modifier = Modifier.statusBarsPadding()
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Smooth load progress indicator
                AnimatedVisibility(
                    visible = isPageLoading,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    LinearProgressIndicator(
                        progress = { loadingProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = ZenTeal,
                        trackColor = ZenDarkBG
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    // Check if there is an active loading URL OR if we are on empty speed-dial
                    val isSpeedDial = activeTab == null || activeTab?.url == "https://duckduckgo.com"
                    
                    if (isSpeedDial) {
                        ZenSpeedDialHome(
                            blockedTrackers = blockedTrackersCount,
                            onSearchLaunch = { query ->
                                addressInputText = query
                                viewModel.loadUrl(query)
                            },
                            quickBookmarks = bookmarks,
                            selectedSearchEngine = selectedSearchEngine,
                            onSearchEngineSelected = { viewModel.setSearchEngine(it) }
                        )
                    } else {
                        // Render full customized web container
                        activeTab?.let { tab ->
                            WebViewContainer(
                                tab = tab,
                                viewModel = viewModel,
                                extensions = extensions,
                                onProgressChanged = { loadingProgress = it },
                                onLoadingStateChanged = { isPageLoading = it },
                                modifier = Modifier.fillMaxSize()
                            )
                        } ?: Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Bir sekme açın veya aramaya başlayın")
                        }
                    }

                    // --- AI Powered Engines Routing status indicator ---
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isAiSelectingEngine,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = ZenCardBG),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .width(280.dp)
                                    .padding(16.dp)
                                    .border(1.dp, ZenPurpleLight, RoundedCornerShape(16.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        color = ZenPurpleLight,
                                        strokeWidth = 3.dp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Akıllı Yapay Zeka Arama",
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Sorgunuz analiz ediliyor ve en ideal sonuç üreten arama motoru seçiliyor...",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // --- AI Search Copilot Slideout Assistant Panel ---
                    androidx.compose.animation.AnimatedVisibility(
                        visible = currentAiSearchQuery != null,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .zIndex(100f)
                    ) {
                        var isExpanded by remember { mutableStateOf(true) }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = ZenDarkBG.copy(alpha = 0.98f)),
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .border(
                                    width = 1.5.dp,
                                    color = ZenPurpleLight.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp)
                            ) {
                                // Header row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "AI",
                                            tint = ZenPurpleLight,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Zen Yapay Zeka Arama Desteği",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { isExpanded = !isExpanded }) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Genişlet",
                                                tint = Color.LightGray,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        IconButton(onClick = { viewModel.dismissAiSearch() }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Kapat",
                                                tint = Color.LightGray,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                if (isExpanded) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Sorgu: \"$currentAiSearchQuery\"",
                                        fontSize = 12.sp,
                                        color = ZenTeal,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (isAiLoading) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(140.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                CircularProgressIndicator(
                                                    color = ZenPurpleLight,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    text = "Gemini aramayı detaylandırıyor...",
                                                    fontSize = 12.sp,
                                                    color = Color.LightGray
                                                )
                                            }
                                        }
                                    } else {
                                        aiSummaryResult?.let { result ->
                                            // Real summary result display
                                            Text(
                                                text = result.summary,
                                                fontSize = 13.sp,
                                                color = Color.White,
                                                lineHeight = 20.sp,
                                                modifier = Modifier.padding(bottom = 12.dp)
                                            )

                                            // Hap Bilgiler List
                                            if (result.quickFacts.isNotEmpty()) {
                                                Text(
                                                    text = "Önemli Hap Bilgiler",
                                                    fontSize = 11.sp,
                                                    color = ZenPurpleLight,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(bottom = 6.dp)
                                                )
                                                Column(
                                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.padding(bottom = 12.dp)
                                                ) {
                                                    result.quickFacts.forEach { fact ->
                                                        Row(verticalAlignment = Alignment.Top) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = "Bullet",
                                                                tint = ZenTeal,
                                                                modifier = Modifier
                                                                    .size(14.dp)
                                                                    .padding(top = 2.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = fact,
                                                                fontSize = 11.sp,
                                                                color = Color.LightGray
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // Suggested Followups tips
                                            if (result.suggestedTips.isNotEmpty()) {
                                                Text(
                                                    text = "İlgili Yapay Zeka Önerileri",
                                                    fontSize = 11.sp,
                                                    color = ZenPurpleLight,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(bottom = 6.dp)
                                                )
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    result.suggestedTips.take(2).forEach { tip ->
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .background(ZenCardBG)
                                                                .border(0.5.dp, ZenOutlines, RoundedCornerShape(10.dp))
                                                                .clickable {
                                                                    addressInputText = tip
                                                                    viewModel.loadUrl(tip)
                                                                }
                                                                .padding(10.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = tip,
                                                                fontSize = 10.sp,
                                                                color = Color.White,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                textAlign = TextAlign.Center
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        } ?: Text(
                                            text = "Arama asistanı yanıtı yüklenemedi.",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                } else {
                                    // Collapsed helper indicator
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Yapay zeka asistan özeti hazır. Görmek için oka dokunarak genişletin.",
                                        fontSize = 11.sp,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1. Sleek Tabs Manager Bottom Sheet
        if (showTabsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showTabsSheet = false },
                containerColor = ZenDarkBG,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                TabsManagerSheetContent(
                    tabs = tabs,
                    allWorkspaces = workspaces,
                    currentWorkspaceId = currentWorkspaceId,
                    onSelectTab = { tabId ->
                        viewModel.selectTab(tabId)
                        showTabsSheet = false
                    },
                    onCloseTab = { tabId ->
                        viewModel.closeTab(tabId)
                    },
                    onNewTab = {
                        viewModel.addNewTab()
                        showTabsSheet = false
                    },
                    onWorkspaceSwitch = { wsId ->
                        viewModel.selectWorkspace(wsId)
                    }
                )
            }
        }

        // 2. Extensions Manager Custom Tool Sheet
        if (showExtensionsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showExtensionsSheet = false },
                containerColor = ZenDarkBG,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                ExtensionsSheetContent(
                    extensions = extensions,
                    isBottomToolbar = isBottomToolbar,
                    onToggleExtension = { id ->
                        viewModel.toggleExtension(id)
                    },
                    onToggleToolbarPos = {
                        viewModel.toggleToolbarPosition()
                    }
                )
            }
        }

        // 3. Bookmarks & History Bottom Sheet
        if (showBookmarksHistorySheet) {
            ModalBottomSheet(
                onDismissRequest = { showBookmarksHistorySheet = false },
                containerColor = ZenDarkBG,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                BookmarksHistoryContent(
                    bookmarks = bookmarks,
                    history = history,
                    isCurrentlyBookmarked = activeTab?.let { tab -> bookmarks.any { it.url == tab.url } } ?: false,
                    onAddBookmark = {
                        activeTab?.let { tab ->
                            viewModel.toggleBookmark(tab.title, tab.url)
                        }
                    },
                    onLaunchUrl = { url ->
                        addressInputText = url
                        viewModel.loadUrl(url)
                        showBookmarksHistorySheet = false
                    },
                    onDeleteBookmark = { id ->
                        viewModel.removeBookmark(id)
                    },
                    onClearHistory = {
                        viewModel.clearHistory()
                    }
                )
            }
        }

        // 4. Privacy Shield Hub
        if (showPrivacyArmorSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPrivacyArmorSheet = false },
                containerColor = ZenDarkBG,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                PrivacyArmorContent(
                    blockedCount = blockedTrackersCount,
                    extensions = extensions,
                    onResetCount = { viewModel.resetTrackerCount() }
                )
            }
        }
    }
}

// Custom Toolbar Layout for command and Navigation
@Composable
fun BrowserCommandLine(
    url: String,
    onUrlChange: (String) -> Unit,
    activeTab: TabItem?,
    tabsCount: Int,
    blockedCount: Int,
    isPageLoading: Boolean,
    onBackPressed: () -> Unit,
    onInputFocused: () -> Unit,
    onGoClick: () -> Unit,
    onTabsClick: () -> Unit,
    onMenuClick: () -> Unit,
    onWorkspaceClick: () -> Unit,
    onHeartClick: () -> Unit,
    onShieldClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ZenDarkBG, ZenCardBG.copy(alpha = 0.98f))
                )
            )
            .border(width = 1.dp, color = ZenOutlines, shape = RoundedCornerShape(0.dp))
            .padding(top = 10.dp, bottom = 10.dp, start = 14.dp, end = 14.dp)
    ) {
        // Upper line containing Address Bar input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Workspace selector button
            IconButton(
                onClick = onWorkspaceClick,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ZenCardBG)
                    .border(width = 1.dp, color = ZenOutlines, shape = RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = "Workspaces",
                    tint = ZenPurpleLight,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Address Card Input
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(ZenCardBG.copy(alpha = 0.85f))
                    .border(
                        width = 1.dp,
                        color = if (isPageLoading) ZenTeal else ZenOutlines,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable { onInputFocused() }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Safe Lock / Shield icon representing HTTPS State
                val isSecure = url.startsWith("https://")
                Icon(
                    imageVector = if (isSecure) Icons.Default.Lock else Icons.Default.Warning,
                    contentDescription = "Secure Status",
                    tint = if (isSecure) ZenTeal else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Custom search/url text field
                TextField(
                    value = url,
                    onValueChange = onUrlChange,
                    placeholder = {
                        Text(
                            text = "Arama yapın veya URL girin",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray
                    ),
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight(),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 13.sp),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = { onGoClick() }
                    )
                )

                // Privacy Shield glowing count marker!
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (blockedCount > 0) ZenTeal.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { onShieldClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Privacy Shield",
                            tint = if (blockedCount > 0) ZenTeal else Color.LightGray,
                            modifier = Modifier.size(14.dp)
                        )
                        if (blockedCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = blockedCount.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ZenTeal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Favorites and search launching buttons
            IconButton(
                onClick = onGoClick,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(ZenPurple, ZenPurpleLight)
                        )
                    )
                    .border(1.dp, ZenPurpleLight.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Go",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Lower Navigation Line
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackPressed) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = Color.LightGray)
            }
            IconButton(onClick = onShieldClick) {
                Icon(Icons.Default.Info, contentDescription = "Kalkan", tint = ZenTeal)
            }
            IconButton(onClick = onHeartClick) {
                Icon(Icons.Default.Favorite, contentDescription = "Yer imleri", tint = Color.LightGray)
            }
            
            // Modern Tab Counter Box
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ZenPurple.copy(alpha = 0.15f))
                    .border(width = 1.2.dp, color = ZenPurpleLight, shape = RoundedCornerShape(8.dp))
                    .clickable { onTabsClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tabsCount.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp
                )
            }

            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.MoreVert, contentDescription = "Eklentiler", tint = Color.LightGray)
            }
        }
    }
}

// Gorgeous Speed Dial Start Layout
@Composable
fun ZenSpeedDialHome(
    blockedTrackers: Int,
    onSearchLaunch: (String) -> Unit,
    quickBookmarks: List<BookmarkItem>,
    selectedSearchEngine: String,
    onSearchEngineSelected: (String) -> Unit
) {
    var queryText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val preConfiguredSites = listOf(
        ShortcutItem("Zen", "https://zen-browser.app", Icons.Default.Build, ZenPurple),
        ShortcutItem("DuckGo", "https://duckduckgo.com", Icons.Default.Info, ZenTeal),
        ShortcutItem("GitHub", "https://github.com", Icons.Default.Star, Color.White),
        ShortcutItem("Wikipedia", "https://wikipedia.org", Icons.Default.Info, Color.LightGray),
        ShortcutItem("Reddit", "https://reddit.com", Icons.Default.Favorite, Color(0xFFFF4500)),
        ShortcutItem("YouTube", "https://youtube.com", Icons.Default.PlayArrow, Color(0xFFFF0000)),
        ShortcutItem("Google", "https://google.com", Icons.Default.Search, Color(0xFF4285F4)),
        ShortcutItem("Portal", "https://yahoo.com", Icons.AutoMirrored.Filled.List, ZenPurpleLight)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ZenDarkBG, Color(0xFF131322))
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Elegant Space Top Cap
        item {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Zen Floating Logo Glowing Concept
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(ZenPurple, ZenPurple.copy(alpha = 0.2f))
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(colors = listOf(ZenPurpleLight, ZenTeal)),
                        shape = RoundedCornerShape(26.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Zen Browser Logo",
                    tint = Color.White,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            val titleGradient = Brush.linearGradient(
                colors = listOf(Color.White, ZenPurpleLight, ZenTeal)
            )
            Text(
                text = "Zen Mobile",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                style = TextStyle(
                    brush = titleGradient
                ),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Tamamen Şifreli • Firefox Gecko Tabanlı Sezgisel Sınırsız Güç",
                fontSize = 11.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
            )
        }

        // --- Active Search Engine Selection Header ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ZenDarkBG),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = ZenOutlines, shape = RoundedCornerShape(16.dp))
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "AKTİF ARAMA MOTORU",
                        fontSize = 10.sp,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZenPurpleLight,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Yapay Zeka (AI)", "Google", "Firefox (DDG)", "Bing").forEach { engine ->
                            val isSelected = engine == selectedSearchEngine
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) ZenPurple.copy(alpha = 0.25f) else ZenCardBG)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) ZenPurpleLight else ZenOutlines,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onSearchEngineSelected(engine) }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = when (engine) {
                                            "Yapay Zeka (AI)" -> Icons.Default.Star
                                            "Google" -> Icons.Default.Search
                                            "Firefox (DDG)" -> Icons.Default.Info
                                            else -> Icons.Default.PlayArrow
                                        },
                                        contentDescription = engine,
                                        tint = if (isSelected) ZenPurpleLight else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = engine,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Mid Stats Analytics Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ZenCardBG.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(colors = listOf(ZenOutlines, ZenTeal.copy(alpha = 0.4f))),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ZEN KALKANI RAPORU",
                            fontSize = 10.sp,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold,
                            color = ZenPurpleLight
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Korumaların Engellediği",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$blockedTrackers",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = ZenTeal
                        )
                        Text(
                            text = "izleyici & reklam",
                            fontSize = 9.sp,
                            color = Color.LightGray
                        )
                    }
                }
            }
        }

        // Main Speed Dial Grid Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hızlı Erişim Portalları",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                preConfiguredSites.chunked(4).forEach { rowSites ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        rowSites.forEach { site ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onSearchLaunch(site.url) }
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(site.color.copy(alpha = 0.15f), ZenCardBG)
                                            )
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = site.color.copy(alpha = 0.25f),
                                            shape = RoundedCornerShape(16.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = site.icon,
                                        contentDescription = site.name,
                                        tint = site.color,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = site.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.LightGray,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (rowSites.size < 4) {
                            repeat(4 - rowSites.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Show Bookmarks on dashboard if available
        if (quickBookmarks.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Favorilerim",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            items(quickBookmarks) { bookmark ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ZenCardBG)
                        .border(width = 0.8.dp, color = ZenOutlines, shape = RoundedCornerShape(14.dp))
                        .clickable { onSearchLaunch(bookmark.url) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Favorite, contentDescription = "Yer imi", tint = ZenPurpleLight, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(bookmark.title, fontSize = 13.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(bookmark.url, fontSize = 10.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// 1. Tabs Manager Sheet Layout
@Composable
fun TabsManagerSheetContent(
    tabs: List<TabItem>,
    allWorkspaces: List<WorkspaceItem>,
    currentWorkspaceId: String,
    onSelectTab: (Int) -> Unit,
    onCloseTab: (Int) -> Unit,
    onNewTab: () -> Unit,
    onWorkspaceSwitch: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(550.dp)
            .padding(20.dp)
    ) {
        // Switch workspaces internally inside Sheet!
        Text(
            text = "Eş zamanlı Alan Segmanları",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            allWorkspaces.forEach { ws ->
                val isSelected = ws.id == currentWorkspaceId
                val wsColor = Color(android.graphics.Color.parseColor(ws.colorHex))
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) wsColor.copy(alpha = 0.25f) else ZenCardBG)
                        .border(
                            width = 1.2.dp,
                            color = if (isSelected) wsColor else ZenOutlines,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onWorkspaceSwitch(ws.id) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ws.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Açık Sekmeler (${tabs.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            IconButton(
                onClick = onNewTab,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(ZenPurple)
                    .size(36.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Yeni Sekme", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (tabs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Hiç açık sekme bulunmuyor", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(tabs) { tab ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (tab.isActive) ZenPurple.copy(alpha = 0.15f) else ZenCardBG)
                            .border(
                                width = 1.dp,
                                color = if (tab.isActive) ZenPurpleLight else ZenOutlines,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectTab(tab.id) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon simulation using dynamic letters
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ZenDarkBG),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (tab.title.isNotEmpty()) tab.title.take(1).uppercase() else "Z",
                                color = ZenTeal,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (tab.title.isNotEmpty()) tab.title else "Yükleniyor...",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = if (tab.isActive) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tab.url,
                                color = Color.Gray,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(onClick = { onCloseTab(tab.id) }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = Color.LightGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// 2. Extensions Settings Sheet
@Composable
fun ExtensionsSheetContent(
    extensions: List<ExtensionItem>,
    isBottomToolbar: Boolean,
    onToggleExtension: (String) -> Unit,
    onToggleToolbarPos: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(550.dp)
            .padding(20.dp)
    ) {
        Text(
            text = "Zen Eklentiler & Ayarlar",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Tamamen entegre Firefox eklenti yetenekleri",
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        HorizontalDivider(color = ZenOutlines, thickness = 1.dp, modifier = Modifier.padding(bottom = 16.dp))

        // AdBloker eklenti simülatörü
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ZenCardBG)
                .clickable { onToggleToolbarPos() }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Toolbar", tint = ZenPurpleLight)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Adres Çubuğu Konumu", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = if (isBottomToolbar) "Aşağıda odaklı (Zen Klasik/Mobil)" else "Yukarıda odaklı",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
            Text("Değiştir", color = ZenTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Eklenti Mağazası (Aktif Entegrasyonlar)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(extensions) { ext ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ZenCardBG)
                        .border(
                            width = 1.dp,
                            color = if (ext.isEnabled) ZenPurple.copy(alpha = 0.5f) else ZenOutlines,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ZenDarkBG),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when(ext.iconName) {
                                "shield" -> Icons.Default.Star
                                "dark_mode" -> Icons.Default.Info
                                "lock" -> Icons.Default.Lock
                                "security" -> Icons.Default.Favorite
                                else -> Icons.Default.Build
                            },
                            contentDescription = ext.name,
                            tint = if (ext.isEnabled) ZenTeal else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ext.name,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = ext.description,
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }

                    Switch(
                        checked = ext.isEnabled,
                        onCheckedChange = { onToggleExtension(ext.id) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ZenTeal,
                            checkedTrackColor = ZenTeal.copy(alpha = 0.4f),
                            uncheckedThumbColor = Color.LightGray,
                            uncheckedTrackColor = ZenOutlines
                        )
                    )
                }
            }
        }
    }
}

// 3. Bookmarks and History Sheets Combined
@Composable
fun BookmarksHistoryContent(
    bookmarks: List<BookmarkItem>,
    history: List<HistoryItem>,
    isCurrentlyBookmarked: Boolean,
    onAddBookmark: () -> Unit,
    onLaunchUrl: (String) -> Unit,
    onDeleteBookmark: (Int) -> Unit,
    onClearHistory: () -> Unit
) {
    var activeTabIdx by remember { mutableStateOf(0) } // 0: Yer İmleri, 1: Geçmiş

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(550.dp)
            .padding(20.dp)
    ) {
        // Upper controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Kütüphane & Yer İmleri",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // Dynamic Bookmark Adder for active website!
            Button(
                onClick = onAddBookmark,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCurrentlyBookmarked) ZenPurple.copy(alpha = 0.2f) else ZenPurple
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (isCurrentlyBookmarked) Icons.Default.Star else Icons.Default.Star,
                    contentDescription = "Bookmark",
                    tint = if (isCurrentlyBookmarked) ZenTeal else Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isCurrentlyBookmarked) "Favorilerden Çıkar" else "Favoriye Ekle",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Selectors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ZenCardBG, RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (activeTabIdx == 0) ZenDarkBG else Color.Transparent)
                    .clickable { activeTabIdx = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Favoriler",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (activeTabIdx == 0) Color.White else Color.Gray
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (activeTabIdx == 1) ZenDarkBG else Color.Transparent)
                    .clickable { activeTabIdx = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Göz Atma Geçmişi",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (activeTabIdx == 1) Color.White else Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // List drawing
        if (activeTabIdx == 0) {
            // Bookmarks tab
            if (bookmarks.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Favorileriniz henüz boş", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(bookmarks) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ZenCardBG)
                                .clickable { onLaunchUrl(item.url) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = "Star", tint = ZenPurpleLight, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(item.url, color = Color.Gray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { onDeleteBookmark(item.id) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Sil", tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        } else {
            // History tab
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Geçmişi Temizle",
                    fontSize = 11.sp,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onClearHistory() }
                )
            }

            if (history.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Arama geçmişi boş", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(history) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ZenCardBG)
                                .clickable { onLaunchUrl(item.url) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.List, contentDescription = "History", tint = ZenTeal, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(item.url, color = Color.Gray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 4. Privacy Shield detailed explanation sheet
@Composable
fun PrivacyArmorContent(
    blockedCount: Int,
    extensions: List<ExtensionItem>,
    onResetCount: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(500.dp)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Zen Kalkanı Koruması",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Sıfırla",
                color = ZenPurpleLight,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onResetCount() }
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Kişisel verilerinizin sızdırılması uBlock entegrasyonu ile tamamen engellenir.",
            fontSize = 11.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Large graphics numeric layout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(ZenCardBG)
                .border(2.dp, ZenTeal.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = blockedCount.toString(),
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Black,
                    color = ZenTeal
                )
                Text(
                    text = "REKLAM & İZLEYİCİ ENGELLENDİ",
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold,
                    color = ZenPurpleLight
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Aktif Kalkan Parametreleri",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        // List of safety features
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                SafetyFeatureItem("uBlock Tracker Blokajı", "Sitelerdeki görünmeyen analitik botlar durduruluyor.", extensions.any { it.id == "ublock" && it.isEnabled })
            }
            item {
                SafetyFeatureItem("Güvenli HTTPS Yükseltmesi", "Tüm HTTP bağlantıları güvenli TLS şifrelemesine yükseltiliyor.", extensions.any { it.id == "https_only" && it.isEnabled })
            }
            item {
                SafetyFeatureItem("Fingerprint Kimlik Gizleme", "Cihaz bilgileri Firefox Gecko Mobile arkasına maskeleniyor.", extensions.any { it.id == "user_agent_switcher" && extActive(extensions, "user_agent_switcher") })
            }
        }
    }
}

// Utility to fetch extension
fun extActive(extensions: List<ExtensionItem>, id: String): Boolean {
    return extensions.find { it.id == id }?.isEnabled == true
}

@Composable
fun SafetyFeatureItem(name: String, desc: String, isActive: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ZenCardBG)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isActive) Icons.Default.Check else Icons.Default.Close,
            contentDescription = "Status",
            tint = if (isActive) ZenTeal else Color.Gray,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(desc, color = Color.Gray, fontSize = 10.sp)
        }
    }
}

// Simple Holder Data model
data class ShortcutItem(
    val name: String,
    val url: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)
