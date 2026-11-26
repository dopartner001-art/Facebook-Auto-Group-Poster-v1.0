package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Query("SELECT * FROM facebook_groups ORDER BY createdAt DESC")
    fun observeAllGroups(): Flow<List<GroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity): Long

    @Query("UPDATE facebook_groups SET isSelected = :isSelected WHERE id = :groupId")
    suspend fun updateSelection(groupId: Long, isSelected: Boolean)

    @Query("UPDATE facebook_groups SET isSelected = :isSelected")
    suspend fun updateAllSelections(isSelected: Boolean)

    @Query("DELETE FROM facebook_groups WHERE id = :groupId")
    suspend fun deleteGroupById(groupId: Long)

    @Query("UPDATE facebook_groups SET lastPostedAt = :timestamp WHERE name = :groupName")
    suspend fun updateLastPostedAtByName(groupName: String, timestamp: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM facebook_groups WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)))")
    suspend fun existsByName(name: String): Boolean
}
