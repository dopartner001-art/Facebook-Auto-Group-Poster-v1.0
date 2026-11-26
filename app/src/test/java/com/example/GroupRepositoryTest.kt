package com.example

import com.example.data.local.GroupDao
import com.example.data.local.GroupEntity
import com.example.data.repository.GroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeGroupDao : GroupDao {
    private val items = MutableStateFlow<List<GroupEntity>>(emptyList())
    private var nextId = 1L

    override fun observeAllGroups(): Flow<List<GroupEntity>> =
        items.map { list -> list.sortedByDescending { it.createdAt } }

    override suspend fun insertGroup(group: GroupEntity): Long {
        val id = nextId++
        items.value = items.value + group.copy(id = id)
        return id
    }

    override suspend fun updateSelection(groupId: Long, isSelected: Boolean) {
        items.value = items.value.map {
            if (it.id == groupId) it.copy(isSelected = isSelected) else it
        }
    }

    override suspend fun updateAllSelections(isSelected: Boolean) {
        items.value = items.value.map { it.copy(isSelected = isSelected) }
    }

    override suspend fun deleteGroupById(groupId: Long) {
        items.value = items.value.filterNot { it.id == groupId }
    }

    override suspend fun updateLastPostedAtByName(groupName: String, timestamp: Long) {
        items.value = items.value.map {
            if (it.name == groupName) it.copy(lastPostedAt = timestamp) else it
        }
    }

    override suspend fun existsByName(name: String): Boolean {
        val trimmed = name.trim().lowercase()
        return items.value.any { it.name.trim().lowercase() == trimmed }
    }
}

class GroupRepositoryTest {

    @Test
    fun `add toggle selectAll and delete groups work properly`() = runTest {
        val repository = GroupRepository(FakeGroupDao())

        assertTrue(repository.addGroup("Android Developers BD"))
        assertTrue(repository.addGroup("Kotlin Community"))
        assertFalse(repository.addGroup("  android developers bd  "))
        assertFalse(repository.addGroup("   "))

        var groups = repository.allGroups.first()
        assertEquals(2, groups.size)
        assertTrue(groups.all { it.isSelected })

        repository.setAllSelected(false)
        groups = repository.allGroups.first()
        assertTrue(groups.none { it.isSelected })

        val firstGroup = groups.first()
        repository.setGroupSelected(firstGroup.id, true)
        groups = repository.allGroups.first()
        assertEquals(1, groups.count { it.isSelected })

        repository.markGroupPosted(firstGroup.name, 123456789L)
        groups = repository.allGroups.first()
        assertEquals(123456789L, groups.first { it.id == firstGroup.id }.lastPostedAt)

        repository.deleteGroup(firstGroup.id)
        groups = repository.allGroups.first()
        assertEquals(1, groups.size)
    }
}
