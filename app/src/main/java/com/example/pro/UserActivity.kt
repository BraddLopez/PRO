package com.example.pro

data class UserActivity(
    val beachName: String,  // Nombre de la playa
    val comment: String,    // El comentario del usuario
    val rating: Float,      // La puntuación dada por el usuario (en formato float)
    val date: String        // La fecha en que se hizo el comentario
)

