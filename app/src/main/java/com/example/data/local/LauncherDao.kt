package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LauncherDao {

    // Desktop Items
    @Query("SELECT * FROM desktop_items ORDER BY orderIndex ASC, id ASC")
    fun observeAllDesktopItems(): Flow<List<DesktopItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDesktopItem(item: DesktopItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDesktopItems(items: List<DesktopItemEntity>)

    @Update
    suspend fun updateDesktopItem(item: DesktopItemEntity)

    @Query("SELECT * FROM desktop_items WHERE id = :id LIMIT 1")
    suspend fun getDesktopItemById(id: Long): DesktopItemEntity?

    @Query("DELETE FROM desktop_items WHERE id = :id OR folderId = :id")
    suspend fun deleteDesktopItemAndChildren(id: Long)

    @Query("SELECT COUNT(*) FROM desktop_items")
    suspend fun getDesktopItemCount(): Int

    // Pinned Apps
    @Query("SELECT * FROM pinned_apps ORDER BY orderIndex ASC")
    fun observePinnedApps(): Flow<List<PinnedAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPinnedApp(pinned: PinnedAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPinnedApps(pinnedList: List<PinnedAppEntity>)

    @Query("DELETE FROM pinned_apps WHERE packageName = :packageName AND target = :target")
    suspend fun removePinnedApp(packageName: String, target: String)

    // Sticky Notes
    @Query("SELECT * FROM sticky_notes ORDER BY updatedAt DESC")
    fun observeStickyNotes(): Flow<List<StickyNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStickyNote(note: StickyNoteEntity): Long

    @Update
    suspend fun updateStickyNote(note: StickyNoteEntity)

    @Query("DELETE FROM sticky_notes WHERE id = :id")
    suspend fun deleteStickyNote(id: Long)

    @Query("SELECT COUNT(*) FROM sticky_notes")
    suspend fun getStickyNoteCount(): Int

    // Recent Apps
    @Query("SELECT * FROM recent_apps ORDER BY lastLaunchedAt DESC LIMIT 12")
    fun observeRecentApps(): Flow<List<RecentAppEntity>>

    @Query("SELECT * FROM recent_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getRecentApp(packageName: String): RecentAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecentApp(recent: RecentAppEntity)
}
