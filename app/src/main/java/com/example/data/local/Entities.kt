package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "desktop_items")
data class DesktopItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "SHORTCUT" or "FOLDER"
    val title: String,
    val packageName: String? = null,
    val folderId: Long? = null,
    val orderIndex: Int = 0
)

@Entity(tableName = "pinned_apps", primaryKeys = ["packageName", "target"])
data class PinnedAppEntity(
    val packageName: String,
    val target: String, // "START" or "TASKBAR"
    val orderIndex: Int = 0
)

@Entity(tableName = "sticky_notes")
data class StickyNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val colorKey: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_apps")
data class RecentAppEntity(
    @PrimaryKey val packageName: String,
    val lastLaunchedAt: Long = System.currentTimeMillis(),
    val launchCount: Int = 1
)
