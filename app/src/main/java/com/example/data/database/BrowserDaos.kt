package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TabDao {
    @Query("SELECT * FROM tabs WHERE workspaceId = :workspaceId ORDER BY timestamp ASC")
    fun getTabsForWorkspace(workspaceId: String): Flow<List<TabItem>>

    @Query("SELECT * FROM tabs ORDER BY timestamp ASC")
    fun getAllTabs(): Flow<List<TabItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: TabItem): Long

    @Update
    suspend fun updateTab(tab: TabItem)

    @Delete
    suspend fun deleteTab(tab: TabItem)

    @Query("DELETE FROM tabs WHERE id = :id")
    suspend fun deleteTabById(id: Int)

    @Query("DELETE FROM tabs WHERE workspaceId = :workspaceId")
    suspend fun clearWorkspaceTabs(workspaceId: String)

    @Query("DELETE FROM tabs")
    suspend fun clearAllTabs()
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkItem)

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkItem)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Int)

    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1")
    suspend fun getBookmarkByUrl(url: String): BookmarkItem?
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT 500")
    fun getHistory(): Flow<List<HistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryItem)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistoryById(id: Int)

    @Query("DELETE FROM history")
    suspend fun clearHistory()
}

@Dao
interface ExtensionDao {
    @Query("SELECT * FROM extensions")
    fun getAllExtensions(): Flow<List<ExtensionItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtensions(extensions: List<ExtensionItem>)

    @Update
    suspend fun updateExtension(extension: ExtensionItem)
}

@Dao
interface WorkspaceDao {
    @Query("SELECT * FROM workspaces")
    fun getAllWorkspaces(): Flow<List<WorkspaceItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspaces(workspaces: List<WorkspaceItem>)
}
