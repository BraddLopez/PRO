package com.example.pro

data class Beach(
    val id: String,
    val name: String,
    val location: String,
    val rating: Float,
    val imageName: String,
    val type: String,
    val description: String,
    val region: String,
    val commentCount: Int = 0
)



