package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.BookmarkItem
import com.example.data.database.ExtensionItem
import com.example.data.database.HistoryItem
import com.example.data.database.TabItem
import com.example.data.database.WorkspaceItem
import com.example.data.repository.BrowserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URLEncoder

@OptIn(ExperimentalCoroutinesApi::class)
class BrowserViewModel(private val repository: BrowserRepository) : ViewModel() {

    // Active workspace state (default: personal)
    private val _currentWorkspaceId = MutableStateFlow("personal")
    val currentWorkspaceId: StateFlow<String> = _currentWorkspaceId.asStateFlow()

    // Tool bar position layout (default: bottom bar, like Zen or Safari Mobile)
    private val _isBottomToolbar = MutableStateFlow(true)
    val isBottomToolbar: StateFlow<Boolean> = _isBottomToolbar.asStateFlow()

    // Real-time tracking block statistics telemetry
    private val _blockedTrackersCount = MutableStateFlow(0)
    val blockedTrackersCount: StateFlow<Int> = _blockedTrackersCount.asStateFlow()

    // Active Tab in the current workspace
    private val _activeTabId = MutableStateFlow<Int?>(null)
    val activeTabId: StateFlow<Int?> = _activeTabId.asStateFlow()

    // Dynamic workspaces database feed
    val workspaces: StateFlow<List<WorkspaceItem>> = repository.workspaces
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Dynamic Extensions configuration
    val extensions: StateFlow<List<ExtensionItem>> = repository.extensions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Active tabs feed filtered by selected workspace
    val tabs: StateFlow<List<TabItem>> = _currentWorkspaceId
        .flatMapLatest { workspaceId ->
            repository.getTabsForWorkspace(workspaceId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // All tabs feed
    val allTabs: StateFlow<List<TabItem>> = repository.allTabs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Combined active tab details based on activeTabId
    val activeTab: StateFlow<TabItem?> = combine(tabs, _activeTabId) { tabList, activeId ->
        if (activeId != null) {
            tabList.find { it.id == activeId } ?: tabList.firstOrNull()
        } else {
            tabList.firstOrNull()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Bookmarks database feeds
    val bookmarks: StateFlow<List<BookmarkItem>> = repository.bookmarks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Web history feeds
    val history: StateFlow<List<HistoryItem>> = repository.history
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Initialize state configuration once database connects
        viewModelScope.launch {
            // Trigger automatic active state recovery
            val firstWorkspaceTabs = repository.getTabsForWorkspace("personal").first()
            if (firstWorkspaceTabs.isNotEmpty()) {
                val active = firstWorkspaceTabs.find { it.isActive } ?: firstWorkspaceTabs.first()
                _activeTabId.value = active.id
            } else {
                // Pre-populate if missing
                val newId = repository.insertTab(
                    TabItem(
                        title = "DuckDuckGo Güvenli Arama",
                        url = "https://duckduckgo.com",
                        workspaceId = "personal",
                        isActive = true
                    )
                ).toInt()
                _activeTabId.value = newId
            }
        }
    }

    // Toggle bottom search toolbar configuration
    fun toggleToolbarPosition() {
        _isBottomToolbar.value = !_isBottomToolbar.value
    }

    // Workspaces Management
    fun selectWorkspace(workspaceId: String) {
        if (_currentWorkspaceId.value == workspaceId) return
        _currentWorkspaceId.value = workspaceId
        viewModelScope.launch {
            val workspaceTabs = repository.getTabsForWorkspace(workspaceId).first()
            if (workspaceTabs.isNotEmpty()) {
                val active = workspaceTabs.find { it.isActive } ?: workspaceTabs.first()
                _activeTabId.value = active.id
            } else {
                // If workspace has no tabs yet, auto-generate a starter secure search tab
                val starterUrl = when (workspaceId) {
                    "private" -> "https://duckduckgo.com" // Private / anti-tracker duckduckgo
                    "work" -> "https://github.com"
                    "study" -> "https://wikipedia.org"
                    else -> "https://duckduckgo.com"
                }
                val starterTitle = when (workspaceId) {
                    "private" -> "Gizli Oturum"
                    "work" -> "GitHub"
                    "study" -> "Vikipedi"
                    else -> "Zen Arama"
                }
                val newId = repository.insertTab(
                    TabItem(
                        title = starterTitle,
                        url = starterUrl,
                        workspaceId = workspaceId,
                        isActive = true
                    )
                ).toInt()
                _activeTabId.value = newId
            }
        }
    }

    // New Tab Addition
    fun addNewTab(workspaceId: String = _currentWorkspaceId.value, url: String = "https://duckduckgo.com") {
        viewModelScope.launch {
            val oldTabs = repository.getTabsForWorkspace(workspaceId).first()
            // Mark other tabs inactive
            oldTabs.forEach {
                if (it.isActive) {
                    repository.updateTab(it.copy(isActive = false))
                }
            }
            // Insert and focus the new tab
            val newId = repository.insertTab(
                TabItem(
                    title = "Yeni Sekme",
                    url = url,
                    workspaceId = workspaceId,
                    isActive = true
                )
            ).toInt()
            if (workspaceId == _currentWorkspaceId.value) {
                _activeTabId.value = newId
            }
        }
    }

    // Select Active Tab
    fun selectTab(tabId: Int) {
        viewModelScope.launch {
            val workspaceTabs = repository.getTabsForWorkspace(_currentWorkspaceId.value).first()
            workspaceTabs.forEach { tab ->
                val shouldBeActive = tab.id == tabId
                if (tab.isActive != shouldBeActive) {
                    repository.updateTab(tab.copy(isActive = shouldBeActive))
                }
            }
            _activeTabId.value = tabId
        }
    }

    // Close Tab
    fun closeTab(tabId: Int) {
        viewModelScope.launch {
            val currentTabs = repository.getTabsForWorkspace(_currentWorkspaceId.value).first()
            val targetTab = currentTabs.find { it.id == tabId } ?: return@launch
            
            repository.deleteTab(targetTab)

            // If the closed tab was the active tab, find a replacement focus
            if (targetTab.isActive) {
                val remaining = currentTabs.filter { it.id != tabId }
                if (remaining.isNotEmpty()) {
                    val fallback = remaining.last()
                    repository.updateTab(fallback.copy(isActive = true))
                    _activeTabId.value = fallback.id
                } else {
                    // No tabs left! Generate a default empty/search tab
                    val newId = repository.insertTab(
                        TabItem(
                            title = "DuckDuckGo Güvenli Arama",
                            url = "https://duckduckgo.com",
                            workspaceId = _currentWorkspaceId.value,
                            isActive = true
                        )
                    ).toInt()
                    _activeTabId.value = newId
                }
            }
        }
    }

    // Update active URL and Title
    fun updateTabUrlAndTitle(tabId: Int, url: String, title: String) {
        viewModelScope.launch {
            val tabsFeed = repository.allTabs.first()
            val tab = tabsFeed.find { it.id == tabId } ?: return@launch
            // Prevent redundant updates that causes UI loops
            if (tab.url != url || tab.title != title) {
                repository.updateTab(tab.copy(url = url, title = title))
            }
        }
    }

    // Search Engine Selection and AI Assistant States
    private val _searchEngine = MutableStateFlow("Yapay Zeka (AI)")
    val searchEngine: StateFlow<String> = _searchEngine.asStateFlow()

    private val _currentAiSearchQuery = MutableStateFlow<String?>(null)
    val currentAiSearchQuery: StateFlow<String?> = _currentAiSearchQuery.asStateFlow()

    private val _aiSummaryResult = MutableStateFlow<com.example.data.repository.AISummaryResult?>(null)
    val aiSummaryResult: StateFlow<com.example.data.repository.AISummaryResult?> = _aiSummaryResult.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _isAiSelectingEngine = MutableStateFlow(false)
    val isAiSelectingEngine: StateFlow<Boolean> = _isAiSelectingEngine.asStateFlow()

    private val _aiDetectedEngine = MutableStateFlow<String?>(null)
    val aiDetectedEngine: StateFlow<String?> = _aiDetectedEngine.asStateFlow()

    fun setSearchEngine(engine: String) {
        _searchEngine.value = engine
    }

    fun triggerAiSearchCopilot(query: String) {
        _isAiLoading.value = true
        _aiSummaryResult.value = null
        viewModelScope.launch {
            val result = com.example.data.repository.GeminiSearchHelper.getSearchAssistantSummary(query)
            _aiSummaryResult.value = result
            _isAiLoading.value = false
        }
    }

    fun dismissAiSearch() {
        _currentAiSearchQuery.value = null
        _aiSummaryResult.value = null
    }

    fun getSearchUrlForEngine(query: String, engine: String): String {
        val encodedQuery = try {
            URLEncoder.encode(query, "UTF-8")
        } catch (e: Exception) {
            query
        }
        return when (engine) {
            "Google" -> "https://www.google.com/search?q=$encodedQuery"
            "DuckDuckGo" -> "https://duckduckgo.com/?q=$encodedQuery"
            "Bing" -> "https://www.bing.com/search?q=$encodedQuery"
            else -> "https://www.google.com/search?q=$encodedQuery"
        }
    }

    // Navigate Active Tab to a specific url (input parsing with AI smart search detection)
    fun loadUrl(inputUrl: String) {
        val active = activeTab.value ?: return
        val trimmed = inputUrl.trim()
        if (trimmed.isEmpty()) return

        val isDirectUrl = trimmed.startsWith("http://", ignoreCase = true) || 
                          trimmed.startsWith("https://", ignoreCase = true) ||
                          trimmed.startsWith("file://", ignoreCase = true) ||
                          "^(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}(?:/.*)?$".toRegex().matches(trimmed)

        if (isDirectUrl) {
            val targetUrl = if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("file://")) trimmed else "https://$trimmed"
            viewModelScope.launch {
                repository.updateTab(active.copy(url = targetUrl))
            }
            // Clear previous AI search assists so it doesn't stay open on regular links
            _currentAiSearchQuery.value = null
            _aiSummaryResult.value = null
        } else {
            // It's a search! Set current query & launch the summary generator
            _currentAiSearchQuery.value = trimmed
            triggerAiSearchCopilot(trimmed)

            if (_searchEngine.value == "Yapay Zeka (AI)") {
                _isAiSelectingEngine.value = true
                viewModelScope.launch {
                    val recommended = com.example.data.repository.GeminiSearchHelper.recommendSearchEngine(trimmed)
                    val targetEngine = when (recommended) {
                        "duckduckgo" -> "DuckDuckGo"
                        "bing" -> "Bing"
                        else -> "Google"
                    }
                    _aiDetectedEngine.value = targetEngine
                    val targetUrl = getSearchUrlForEngine(trimmed, targetEngine)
                    repository.updateTab(active.copy(url = targetUrl))
                    _isAiSelectingEngine.value = false
                }
            } else {
                val targetUrl = getSearchUrlForEngine(trimmed, _searchEngine.value)
                viewModelScope.launch {
                    repository.updateTab(active.copy(url = targetUrl))
                }
            }
        }
    }

    // Bookmarking Management
    fun toggleBookmark(title: String, url: String) {
        viewModelScope.launch {
            if (repository.isBookmarked(url)) {
                // For simplicity, find bookmark by indexing matched URL items
                val bookmarksList = repository.bookmarks.first()
                val matched = bookmarksList.find { it.url == url }
                if (matched != null) {
                    repository.deleteBookmark(matched)
                }
            } else {
                repository.insertBookmark(BookmarkItem(title = title, url = url))
            }
        }
    }

    fun removeBookmark(id: Int) {
        viewModelScope.launch {
            repository.deleteBookmarkById(id)
        }
    }

    // History logs
    fun visitPage(title: String, url: String) {
        if (url.isEmpty() || url == "about:blank") return
        viewModelScope.launch {
            // Drop search query results from polluting clean browser history where appropriate, or store all
            // To make history interactive, we store everything!
            repository.insertHistory(HistoryItem(title = title, url = url))
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Block telemetry analytics
    fun incrementTrackerCount(amount: Int = 1) {
        viewModelScope.launch {
            _blockedTrackersCount.value += amount
        }
    }

    fun resetTrackerCount() {
        _blockedTrackersCount.value = 0
    }

    // Active Extensions toggles
    fun toggleExtension(extensionId: String) {
        viewModelScope.launch {
            val exList = repository.extensions.first()
            val match = exList.find { it.id == extensionId } ?: return@launch
            val updated = match.copy(isEnabled = !match.isEnabled)
            repository.updateExtension(updated)
            
            // If ublock/shield state resets, reset ticker
            if (extensionId == "ublock" && !updated.isEnabled) {
                resetTrackerCount()
            }
        }
    }

    // Simple state flow to trigger WebView back navigation
    private val _backNavigationTrigger = MutableStateFlow(0)
    val backNavigationTrigger: StateFlow<Int> = _backNavigationTrigger.asStateFlow()

    fun triggerBackNavigation() {
        _backNavigationTrigger.value += 1
    }
}

class BrowserViewModelFactory(private val repository: BrowserRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BrowserViewModel::class.java)) {
            return BrowserViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
