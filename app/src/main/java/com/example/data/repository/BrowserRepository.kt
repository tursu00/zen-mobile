package com.example.data.repository

import com.example.data.database.BookmarkDao
import com.example.data.database.BookmarkItem
import com.example.data.database.ExtensionDao
import com.example.data.database.ExtensionItem
import com.example.data.database.HistoryDao
import com.example.data.database.HistoryItem
import com.example.data.database.TabDao
import com.example.data.database.TabItem
import com.example.data.database.WorkspaceDao
import com.example.data.database.WorkspaceItem
import kotlinx.coroutines.flow.Flow

class BrowserRepository(
    private val tabDao: TabDao,
    private val bookmarkDao: BookmarkDao,
    private val historyDao: HistoryDao,
    private val extensionDao: ExtensionDao,
    private val workspaceDao: WorkspaceDao
) {
    val workspaces: Flow<List<WorkspaceItem>> = workspaceDao.getAllWorkspaces()
    val extensions: Flow<List<ExtensionItem>> = extensionDao.getAllExtensions()
    val allTabs: Flow<List<TabItem>> = tabDao.getAllTabs()
    val bookmarks: Flow<List<BookmarkItem>> = bookmarkDao.getAllBookmarks()
    val history: Flow<List<HistoryItem>> = historyDao.getHistory()

    fun getTabsForWorkspace(workspaceId: String): Flow<List<TabItem>> =
        tabDao.getTabsForWorkspace(workspaceId)

    suspend fun insertTab(tab: TabItem): Long = tabDao.insertTab(tab)

    suspend fun updateTab(tab: TabItem) = tabDao.updateTab(tab)

    suspend fun deleteTab(tab: TabItem) = tabDao.deleteTab(tab)

    suspend fun deleteTabById(id: Int) = tabDao.deleteTabById(id)

    suspend fun clearWorkspaceTabs(workspaceId: String) = tabDao.clearWorkspaceTabs(workspaceId)

    suspend fun clearAllTabs() = tabDao.clearAllTabs()

    suspend fun insertBookmark(bookmark: BookmarkItem) = bookmarkDao.insertBookmark(bookmark)

    suspend fun deleteBookmark(bookmark: BookmarkItem) = bookmarkDao.deleteBookmark(bookmark)

    suspend fun deleteBookmarkById(id: Int) = bookmarkDao.deleteBookmarkById(id)

    suspend fun isBookmarked(url: String): Boolean {
        return bookmarkDao.getBookmarkByUrl(url) != null
    }

    suspend fun insertHistory(history: HistoryItem) = historyDao.insertHistory(history)

    suspend fun deleteHistoryById(id: Int) = historyDao.deleteHistoryById(id)

    suspend fun clearHistory() = historyDao.clearHistory()

    suspend fun updateExtension(extension: ExtensionItem) = extensionDao.updateExtension(extension)
}
