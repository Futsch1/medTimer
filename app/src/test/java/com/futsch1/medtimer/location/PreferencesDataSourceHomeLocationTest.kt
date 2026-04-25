package com.futsch1.medtimer.location

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.futsch1.medtimer.model.HomeLocation
import com.futsch1.medtimer.preferences.PreferencesDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PreferencesDataSourceHomeLocationTest {
    private lateinit var dataSource: PreferencesDataSource

    @Before
    fun setUp() {
        val prefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("test_prefs_home_location", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        dataSource = PreferencesDataSource(prefs, CoroutineScope(Dispatchers.Unconfined))
    }

    @Test
    fun `getHomeLocation returns null when nothing saved`() {
        assertNull(dataSource.getHomeLocation())
    }

    @Test
    fun `saveHomeLocation and getHomeLocation round-trip`() {
        val location = HomeLocation(48.137, 11.575, 200f)
        dataSource.saveHomeLocation(location)
        val loaded = dataSource.getHomeLocation()
        assertNotNull(loaded)
        assertEquals(48.137, loaded!!.latitude, 0.0001)
        assertEquals(11.575, loaded.longitude, 0.0001)
        assertEquals(200f, loaded.radiusMeters, 0.01f)
    }

    @Test
    fun `clearHomeLocation removes saved location`() {
        dataSource.saveHomeLocation(HomeLocation(1.0, 2.0))
        dataSource.clearHomeLocation()
        assertNull(dataSource.getHomeLocation())
    }
}
