package models

data class Lugar(
    val nombre: String,
    val direccion: String,
    val descripcion: String,
    val imagenUrl: String? = null,
    val distancia: Double = 0.0,
    val latitud: Double, // <-- AGREGAR
    val longitud: Double // <-- AGREGAR
)
