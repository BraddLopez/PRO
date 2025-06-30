package models

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Beach(
    var id: String = "",

    @get:PropertyName("nombre")
    @set:PropertyName("nombre")
    var name: String = "",

    @get:PropertyName("ubicacion")
    @set:PropertyName("ubicacion")
    var location: String = "",

    val rating: Float = 0f,

    @get:PropertyName("imagenNombre")
    @set:PropertyName("imagenNombre")
    var imageName: String = "",

    @get:PropertyName("tipo")
    @set:PropertyName("tipo")
    var type: String = "",

    @get:PropertyName("descripcion")
    @set:PropertyName("descripcion")
    var description: String = "",

    val region: String = "",
    val commentCount: Int = 0,

    val lugaresTuristicos: String = "",
    val discotecas: String = "",
    val restaurantes: String = "",
    val hoteles: String = "",
    val servicios: String = "",
    val estado: String = "",

    var latitud: Double = -12.0464,   // 🔥 nombre exacto de Firestore
    var longitud: Double = -77.0428   // 🔥 nombre exacto de Firestore
)
