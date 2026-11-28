package com.kanso.launcher

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kanso.launcher.data.db.DockSlotEntity
import com.kanso.launcher.data.db.HiddenAppEntity
import com.kanso.launcher.data.db.HomeItemEntity
import com.kanso.launcher.data.db.HomeItemType
import com.kanso.launcher.data.db.IconOverrideEntity
import com.kanso.launcher.data.db.KansoDao
import com.kanso.launcher.data.db.KansoDatabase
import com.kanso.launcher.data.db.SurfacePage
import com.kanso.launcher.data.model.MechaWidgetTheme
import com.kanso.launcher.data.model.ModularWidgetConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class KansoDatabaseAndGridTest {

    private lateinit var db: KansoDatabase
    private lateinit var dao: KansoDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, KansoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.kansoDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun homeItemCrud_positionSpanAndConfigUpdatesPersist() = runTest {
        val initialCfg = ModularWidgetConfig.encode(ModularWidgetConfig(mechaTheme = MechaWidgetTheme.DEFAULT))
        val id = dao.insertHomeItem(
            HomeItemEntity(
                pageIndex = SurfacePage.WORKSPACE,
                gridCol = 0,
                gridRow = 0,
                spanColumns = 2,
                spanRows = 1,
                orderIndex = 0,
                itemType = HomeItemType.BATTERY_CAPSULE,
                label = "Battery",
                configJson = initialCfg
            )
        )
        assertTrue(id > 0L)
        assertEquals(1, dao.countItemsOnPage(SurfacePage.WORKSPACE))

        dao.updateHomeItemPosition(id, gridCol = 2, gridRow = 3)
        dao.updateHomeItemSpan(id, spanColumns = 4, spanRows = 2)
        val updatedCfg = ModularWidgetConfig.encode(ModularWidgetConfig(mechaTheme = MechaWidgetTheme.DATA_STREAM))
        dao.updateHomeItemConfig(id, updatedCfg, "Battery Cyber")

        val item = dao.getItemsOnPage(SurfacePage.WORKSPACE).single()
        assertEquals(2, item.gridCol)
        assertEquals(3, item.gridRow)
        assertEquals(4, item.spanColumns)
        assertEquals(2, item.spanRows)
        assertEquals("Battery Cyber", item.label)
        assertEquals(MechaWidgetTheme.DATA_STREAM, ModularWidgetConfig.decode(item.configJson).mechaTheme)

        dao.deleteHomeItem(id)
        assertEquals(0, dao.countItemsOnPage(SurfacePage.WORKSPACE))
    }

    @Test
    fun folderTransaction_createsRenamesRemovesAppAndDeletesCleanly() = runTest {
        val folderId = dao.createFolderWithApps(
            title = "Comms",
            pageIndex = SurfacePage.WORKSPACE,
            gridCol = 1,
            gridRow = 2,
            apps = listOf(
                "com.whatsapp" to "com.whatsapp.Main",
                "org.telegram.messenger" to "org.telegram.ui.LaunchActivity"
            )
        )
        assertTrue(folderId > 0L)

        val folders = dao.observeFolders().first()
        assertEquals(1, folders.size)
        assertEquals("Comms", folders.first().title)

        val folderApps = dao.observeFolderApps().first()
        assertEquals(2, folderApps.size)

        val homeItems = dao.getItemsOnPage(SurfacePage.WORKSPACE)
        assertEquals(1, homeItems.size)
        assertEquals(HomeItemType.FOLDER, homeItems.first().itemType)
        assertEquals(folderId, homeItems.first().folderId)

        dao.renameFolder(folderId, "Messaging")
        assertEquals("Messaging", dao.observeFolders().first().single().title)

        dao.removeFolderApp(folderId, "com.whatsapp", "com.whatsapp.Main")
        assertEquals(1, dao.observeFolderApps().first().size)

        dao.deleteFolderAndHomeItem(folderId, homeItems.first().id)
        assertTrue(dao.observeFolders().first().isEmpty())
        assertTrue(dao.observeFolderApps().first().isEmpty())
        assertTrue(dao.getItemsOnPage(SurfacePage.WORKSPACE).isEmpty())
    }

    @Test
    fun dockHiddenAndIconOverrideTables_roundTripProperly() = runTest {
        dao.upsertDockSlot(DockSlotEntity(0, "com.android.dialer", "com.android.dialer.Main", "Phone"))
        dao.upsertDockSlot(DockSlotEntity(1, "com.android.chrome", "com.google.android.apps.chrome.Main", "Chrome"))
        assertEquals(2, dao.observeDockSlots().first().size)

        dao.deleteDockSlot(0)
        assertEquals(1, dao.observeDockSlots().first().size)
        assertEquals("com.android.chrome", dao.observeDockSlots().first().single().packageName)

        dao.hideApp(HiddenAppEntity("com.bloatware.app"))
        assertTrue(dao.observeHiddenApps().first().any { it.packageName == "com.bloatware.app" })
        dao.unhideApp("com.bloatware.app")
        assertFalse(dao.observeHiddenApps().first().any { it.packageName == "com.bloatware.app" })

        val key = "com.spotify.music/com.spotify.music.MainActivity"
        dao.upsertIconOverride(IconOverrideEntity(key, "com.iconpack.kanso", "ic_spotify_amber"))
        val override = dao.observeIconOverrides().first().single()
        assertEquals("ic_spotify_amber", override.drawableName)

        dao.deleteIconOverride(key)
        assertTrue(dao.observeIconOverrides().first().isEmpty())
    }

    @Test
    fun surfacePageIndexMapping_matchesWorkspaceAndLockscreenSlots() {
        assertEquals(SurfacePage.WORKSPACE, SurfacePage.workspacePageIndex(0))
        assertEquals(SurfacePage.WORKSPACE + 1, SurfacePage.workspacePageIndex(1))
        assertEquals(SurfacePage.WORKSPACE + 5, SurfacePage.workspacePageIndex(5))
        assertTrue(SurfacePage.LOCKSCREEN > SurfacePage.workspacePageIndex(5))
    }
}
