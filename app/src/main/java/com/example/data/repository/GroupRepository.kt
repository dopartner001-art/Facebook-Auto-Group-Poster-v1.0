package com.example.data.repository

import com.example.data.local.GroupDao
import com.example.data.local.GroupEntity
import kotlinx.coroutines.flow.Flow

class GroupRepository(private val groupDao: GroupDao) {

    val allGroups: Flow<List<GroupEntity>> = groupDao.observeAllGroups()

    suspend fun addGroup(rawName: String): Boolean {
        val trimmed = rawName.trim()
        if (trimmed.isEmpty()) return false
        if (groupDao.existsByName(trimmed)) return false
        groupDao.insertGroup(
            GroupEntity(
                name = trimmed,
                isSelected = true
            )
        )
        return true
    }

    suspend fun setGroupSelected(groupId: Long, isSelected: Boolean) {
        groupDao.updateSelection(groupId, isSelected)
    }

    suspend fun setAllSelected(isSelected: Boolean) {
        groupDao.updateAllSelections(isSelected)
    }

    suspend fun deleteGroup(groupId: Long) {
        groupDao.deleteGroupById(groupId)
    }

    suspend fun markGroupPosted(groupName: String, timestamp: Long = System.currentTimeMillis()) {
        groupDao.updateLastPostedAtByName(groupName, timestamp)
    }
}
