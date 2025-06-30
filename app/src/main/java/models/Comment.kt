package models

data class Comment(
    var id: String = "",
    val beachId: String = "",
    val userName: String = "",
    val userId: String = "",
    val date: String = "",
    val text: String = "",
    val rating: Float = 0f,
    var timestamp: Long = 0L,
    var parentId: String? = null

)
