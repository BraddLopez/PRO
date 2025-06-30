package com.example.pro

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import models.Beach
import com.google.firebase.firestore.FirebaseFirestore


class BeachAdapter(private var beachList: List<Beach>) : RecyclerView.Adapter<BeachAdapter.BeachViewHolder>() {

    // Vista para cada ítem del RecyclerView
    class BeachViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val beachName: TextView = itemView.findViewById(R.id.nameText)
        val beachLocation: TextView = itemView.findViewById(R.id.locationText)
        val beachRating: RatingBar = itemView.findViewById(R.id.ratingBar)
        val beachImage: ImageView = itemView.findViewById(R.id.imageBeach)
        val commentCountText: TextView = itemView.findViewById(R.id.commentCountText)
        val ratingPercentageText: TextView = itemView.findViewById(R.id.ratingPercentageText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BeachViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.beach_item, parent, false)
        return BeachViewHolder(view)
    }

    override fun onBindViewHolder(holder: BeachViewHolder, position: Int) {
        val beach = beachList[position]
        holder.beachName.text = beach.name
        holder.beachLocation.text = beach.location

        // Imagen
        val imageResId = holder.itemView.context.resources.getIdentifier(
            beach.imageName, "drawable", holder.itemView.context.packageName
        )
        holder.beachImage.setImageResource(imageResId)

        // Comentarios precargados (opcional) Mostrar texto por defecto mientras carga
        holder.commentCountText.text = "Opiniones: cargando..."

        val db = FirebaseFirestore.getInstance()
        db.collection("playas").document(beach.id).collection("comments")
            // Obtener todos los comentarios, sin filtrar por parentId
            .get()
            .addOnSuccessListener { result ->
                // Filtrar solo los comentarios principales (sin parentId) que tengan un rating
                val ratings = result.filter {
                    // Solo comentarios que no son respuestas (sin parentId) y que tienen un rating
                    it.get("parentId") == null && it.contains("rating")
                }.mapNotNull {
                    // Obtener el rating de los comentarios principales
                    it.getDouble("rating")?.toFloat()
                }

                val totalWithRating = ratings.size // Número de comentarios con rating

                // Si hay comentarios con valoraciones, calculamos el promedio
                if (ratings.isNotEmpty()) {
                    val promedio = ratings.sum() / totalWithRating
                    holder.beachRating.rating = promedio
                    holder.ratingPercentageText.text = String.format("%.1f ★", promedio)
                } else {
                    holder.beachRating.rating = 0f
                    holder.ratingPercentageText.text = "0.0 ★"
                }

                // Mostrar el total de comentarios con rating
                holder.commentCountText.text = "($totalWithRating)"
            }
            .addOnFailureListener {
                // Si ocurre un error al recuperar los datos
                holder.beachRating.rating = 0f
                holder.ratingPercentageText.text = "0.0 ★"
                holder.commentCountText.text = ""
            }

        // Clic para ir al detalle de la playa
        holder.itemView.setOnClickListener {
            val context = it.context
            val intent = Intent(context, BeachDetailActivity::class.java).apply {
                putExtra("id", beach.id)
                putExtra("name", beach.name)
                putExtra("location", beach.location)
                putExtra("rating", beach.rating)
                putExtra("imageResId", beach.imageName)
                putExtra("type", beach.type)
                putExtra("description", beach.description)
                putExtra("region", beach.region)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = beachList.size

    fun updateList(newList: List<Beach>) {
        beachList = newList
        notifyDataSetChanged()
    }
}








