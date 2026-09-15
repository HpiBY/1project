package com.example.ozontracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    var title: String,
    var lastPrice: Long? = null,
    var lastCheckedAt: Long = 0L,
    var lastError: String? = null
)
