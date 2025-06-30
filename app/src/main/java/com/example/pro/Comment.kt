package com.example.pro

data class Comment(
    val userName: String,
    val date: String,
    val text: String,
    val rating: Float,
    var parentId: String? = null

)