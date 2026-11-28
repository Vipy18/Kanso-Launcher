package com.kanso.launcher

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.kanso.launcher.receiver.BootReceiver
import com.kanso.launcher.service.KansoDreamService
import com.kanso.launcher.service.KansoLifecycleService
import com.kanso.launcher.service.RecentsSnapshotCache
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class RecentsAndServicesRobolectricTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        RecentsSnapshotCache.clear()
    }

    @After
    fun tearDown() {
        RecentsSnapshotCache.clear()
    }

    @Test
    fun recentsSnapshotCache_closeClearAndRelaunchPersistenceWorksEndToEnd() {
        val bmp = Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565)
        RecentsSnapshotCache.put("com.spotify.music", bmp)
        assertNotNull(RecentsSnapshotCache.get("com.spotify.music"))

        RecentsSnapshotCache.closeRecentTask(context, "com.spotify.music", timestampMs = 50_000L)
        assertNull(RecentsSnapshotCache.get("com.spotify.music"))

        val cutoffs = RecentsSnapshotCache.getDismissedCutoffs(context)
        assertEquals(50_000L, cutoffs["com.spotify.music"])
        assertFalse(RecentsSnapshotCache.shouldIncludeTask("com.spotify.music", 49_999L, cutoffs))
        assertTrue(RecentsSnapshotCache.shouldIncludeTask("com.spotify.music", 50_001L, cutoffs))

        // Relaunch clears dismissal cutoff
        RecentsSnapshotCache.clearDismissedCutoff(context, "com.spotify.music")
        assertFalse(RecentsSnapshotCache.getDismissedCutoffs(context).containsKey("com.spotify.music"))

        // Batch clear persists all packages
        RecentsSnapshotCache.clearAllRecentTasks(
            context = context,
            packages = listOf("com.app.one", "com.app.two"),
            timestampMs = 75_000L
        )
        val batchCutoffs = RecentsSnapshotCache.getDismissedCutoffs(context)
        assertEquals(75_000L, batchCutoffs["com.app.one"])
        assertEquals(75_000L, batchCutoffs["com.app.two"])
    }

    @Test
    fun kansoDreamService_onCreateAttachesSavedStateRegistryWithoutThrowing() {
        val controller = Robolectric.buildService(KansoDreamService::class.java).create()
        val service = controller.get()
        assertEquals(Lifecycle.State.CREATED, service.lifecycle.currentState)
        assertNotNull(service.savedStateRegistry)
        controller.destroy()
        assertEquals(Lifecycle.State.DESTROYED, service.lifecycle.currentState)
    }

    @Test
    fun kansoLifecycleService_startsAndDestroysSafely() {
        val controller = Robolectric.buildService(KansoLifecycleService::class.java).create()
        val startResult = controller.get().onStartCommand(Intent(), 0, 1)
        assertEquals(android.app.Service.START_STICKY, startResult)
        controller.destroy()
    }

    @Test
    fun bootReceiver_handlesBootCompletedAndIgnoresUnrelatedActionsSafely() {
        val receiver = BootReceiver()
        receiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
        receiver.onReceive(context, Intent("com.example.OTHER"))
        receiver.onReceive(null, null)
    }
}
