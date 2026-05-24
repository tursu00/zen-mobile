package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TabItem::class, BookmarkItem::class, HistoryItem::class, ExtensionItem::class, WorkspaceItem::class],
    version = 1,
    exportSchema = false
)
abstract class BrowserDatabase : RoomDatabase() {
    abstract fun tabDao(): TabDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun extensionDao(): ExtensionDao
    abstract fun workspaceDao(): WorkspaceDao

    companion object {
        @Volatile
        private var INSTANCE: BrowserDatabase? = null

        fun getDatabase(context: Context): BrowserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BrowserDatabase::class.java,
                    "zen_browser_database"
                )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Prepopulate default workspaces
                        db.execSQL("INSERT OR REPLACE INTO workspaces (id, name, iconName, colorHex) VALUES ('personal', 'Kişisel', 'home', '#9D4EDD')")
                        db.execSQL("INSERT OR REPLACE INTO workspaces (id, name, iconName, colorHex) VALUES ('work', 'İş', 'work', '#3D5A80')")
                        db.execSQL("INSERT OR REPLACE INTO workspaces (id, name, iconName, colorHex) VALUES ('study', 'Araştırma', 'school', '#2EC4B6')")
                        db.execSQL("INSERT OR REPLACE INTO workspaces (id, name, iconName, colorHex) VALUES ('private', 'Gizli Oturum', 'visibility_off', '#FF0055')")

                        // Prepopulate default mock/interactive Extensions
                        db.execSQL("INSERT OR REPLACE INTO extensions (id, name, description, isEnabled, iconName) VALUES ('ublock', 'uBlock Origin Pro', 'Blocks analytical trackers, ads, analytics scripts, and telemetry hosts.', 1, 'shield')")
                        db.execSQL("INSERT OR REPLACE INTO extensions (id, name, description, isEnabled, iconName) VALUES ('darkreader', 'Dark Reader Mobile', 'Forces a beautiful night stylesheet on loaded pages for protection.', 0, 'dark_mode')")
                        db.execSQL("INSERT OR REPLACE INTO extensions (id, name, description, isEnabled, iconName) VALUES ('https_only', 'HTTPS Security Guard', 'Forces upgrade of unsafe HTTP frames into HTTPS URLs instantly.', 1, 'lock')")
                        db.execSQL("INSERT OR REPLACE INTO extensions (id, name, description, isEnabled, iconName) VALUES ('private_shield', 'Anti-Fingerprint Shield', 'Fakes device dimensions, cookies, and prevents identity tracking.', 1, 'security')")
                        db.execSQL("INSERT OR REPLACE INTO extensions (id, name, description, isEnabled, iconName) VALUES ('user_agent_switcher', 'Gecko User-Agent Spoof', 'Blends in client signature as standard Firefox Gecko Mobile rv:119.', 1, 'dns')")

                        // Prepopulate a basic bookmark and default starter Tab
                        db.execSQL("INSERT OR REPLACE INTO bookmarks (id, title, url, timestamp) VALUES (1, 'Zen Browser Official', 'https://zen-browser.app', 1716474968000)")
                        db.execSQL("INSERT OR REPLACE INTO bookmarks (id, title, url, timestamp) VALUES (2, 'Firefox Support Mobile', 'https://support.mozilla.org', 1716474968000)")
                        db.execSQL("INSERT OR REPLACE INTO bookmarks (id, title, url, timestamp) VALUES (3, 'DuckDuckGo Privacy', 'https://duckduckgo.com', 1716474968000)")
                        
                        db.execSQL("INSERT OR REPLACE INTO tabs (id, title, url, workspaceId, isActive, timestamp) VALUES (1, 'Zen Başlangıç', 'https://duckduckgo.com', 'personal', 1, 1716474968000)")
                    }
                })
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
