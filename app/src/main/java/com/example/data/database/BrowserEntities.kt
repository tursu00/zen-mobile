package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabs")
data class TabItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val workspaceId: String,
    val isActive: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookmarks")
data class BookmarkItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "extensions")
data class ExtensionItem(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val isEnabled: Boolean,
    val iconName: String
)

@Entity(tableName = "workspaces")
data class WorkspaceItem(
    @PrimaryKey val id: String,
    val name: String,
    val iconName: String,
    val colorHex: String
)
