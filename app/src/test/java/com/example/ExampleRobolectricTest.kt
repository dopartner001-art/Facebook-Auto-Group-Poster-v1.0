package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.GroupRepository
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
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: GroupRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = GroupRepository(database.groupDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read app_name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Auto Group Poster", appName)
    }

    @Test
    fun `add toggle selectAll and delete groups persist properly`() = runTest {
        assertTrue(repository.addGroup("Android Developers BD"))
        assertTrue(repository.addGroup("Kotlin Community"))
        assertFalse(repository.addGroup("android developers bd")) // duplicate check

        var list = repository.allGroups.first()
        assertEquals(2, list.size)
        assertTrue(list.all { it.isSelected })

        repository.setAllSelected(false)
        list = repository.allGroups.first()
        assertTrue(list.none { it.isSelected })

        val target = list.first()
        repository.setGroupSelected(target.id, true)
        list = repository.allGroups.first()
        assertEquals(1, list.count { it.isSelected })

        repository.deleteGroup(target.id)
        list = repository.allGroups.first()
        assertEquals(1, list.size)
    }
}
