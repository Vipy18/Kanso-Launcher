package com.kanso.launcher.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface KansoDao {

    @Query("SELECT * FROM home_items ORDER BY pageIndex ASC, gridRow ASC, gridCol ASC, orderIndex ASC, id ASC")
    fun observeHomeItems(): Flow<List<HomeItemEntity>>

    @Query("SELECT * FROM home_items WHERE pageIndex = :pageIndex ORDER BY gridRow ASC, gridCol ASC, orderIndex ASC, id ASC")
    suspend fun getItemsOnPage(pageIndex: Int): List<HomeItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomeItem(item: HomeItemEntity): Long

    @Query("DELETE FROM home_items WHERE id = :id")
    suspend fun deleteHomeItem(id: Long)

    @Query("DELETE FROM home_items WHERE pageIndex = :pageIndex")
    suspend fun clearPageItems(pageIndex: Int)

    @Query("UPDATE home_items SET orderIndex = :orderIndex, pageIndex = :pageIndex WHERE id = :id")
    suspend fun moveHomeItem(id: Long, pageIndex: Int, orderIndex: Int)

    @Query("UPDATE home_items SET gridCol = :gridCol, gridRow = :gridRow WHERE id = :id")
    suspend fun updateHomeItemPosition(id: Long, gridCol: Int, gridRow: Int)

    @Query("UPDATE home_items SET spanColumns = :spanColumns, spanRows = :spanRows WHERE id = :id")
    suspend fun updateHomeItemSpan(id: Long, spanColumns: Int, spanRows: Int)

    @Query("UPDATE home_items SET configJson = :configJson, label = :label WHERE id = :id")
    suspend fun updateHomeItemConfig(id: Long, configJson: String, label: String)

    @Query("SELECT COUNT(*) FROM home_items WHERE pageIndex = :pageIndex")
    suspend fun countItemsOnPage(pageIndex: Int): Int

    @Query("SELECT * FROM dock_slots ORDER BY slotIndex ASC")
    fun observeDockSlots(): Flow<List<DockSlotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDockSlot(slot: DockSlotEntity)

    @Query("DELETE FROM dock_slots WHERE slotIndex = :slotIndex")
    suspend fun deleteDockSlot(slotIndex: Int)

    @Query("SELECT * FROM folders ORDER BY folderId ASC")
    fun observeFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Query("UPDATE folders SET title = :title WHERE folderId = :folderId")
    suspend fun renameFolder(folderId: Long, title: String)

    @Query("DELETE FROM folders WHERE folderId = :folderId")
    suspend fun deleteFolder(folderId: Long)

    @Query("SELECT * FROM folder_apps ORDER BY folderId ASC, orderIndex ASC")
    fun observeFolderApps(): Flow<List<FolderAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolderApp(app: FolderAppEntity)

    @Query("DELETE FROM folder_apps WHERE folderId = :folderId AND packageName = :packageName AND activityName = :activityName")
    suspend fun removeFolderApp(folderId: Long, packageName: String, activityName: String)

    @Query("DELETE FROM folder_apps WHERE folderId = :folderId")
    suspend fun clearFolderApps(folderId: Long)

    @Transaction
    suspend fun createFolderWithApps(
        title: String,
        pageIndex: Int,
        gridCol: Int,
        gridRow: Int,
        apps: List<Pair<String, String>>
    ): Long {
        val folderId = insertFolder(FolderEntity(title = title, pageIndex = pageIndex))
        apps.forEachIndexed { idx, (pkg, act) ->
            insertFolderApp(
                FolderAppEntity(
                    folderId = folderId,
                    packageName = pkg,
                    activityName = act,
                    orderIndex = idx
                )
            )
        }
        val order = countItemsOnPage(pageIndex)
        insertHomeItem(
            HomeItemEntity(
                pageIndex = pageIndex,
                gridCol = gridCol,
                gridRow = gridRow,
                spanColumns = 1,
                spanRows = 1,
                orderIndex = order,
                itemType = HomeItemType.FOLDER,
                label = title,
                folderId = folderId
            )
        )
        return folderId
    }

    @Transaction
    suspend fun deleteFolderAndHomeItem(folderId: Long, homeItemId: Long) {
        clearFolderApps(folderId)
        deleteFolder(folderId)
        deleteHomeItem(homeItemId)
    }

    @Query("SELECT * FROM hidden_apps")
    fun observeHiddenApps(): Flow<List<HiddenAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun hideApp(entity: HiddenAppEntity)

    @Query("DELETE FROM hidden_apps WHERE packageName = :packageName")
    suspend fun unhideApp(packageName: String)

    @Query("SELECT * FROM icon_overrides")
    fun observeIconOverrides(): Flow<List<IconOverrideEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIconOverride(entity: IconOverrideEntity)

    @Query("DELETE FROM icon_overrides WHERE componentKey = :componentKey")
    suspend fun deleteIconOverride(componentKey: String)
}
