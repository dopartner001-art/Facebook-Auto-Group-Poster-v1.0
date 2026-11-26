package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "facebook_groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val isSelected: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastPostedAt: Long? = null
)
