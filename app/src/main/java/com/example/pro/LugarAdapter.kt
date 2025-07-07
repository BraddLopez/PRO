package com.example.pro

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import models.Lugar

class LugarAdapter(val lugares: List<Lugar>) :
    RecyclerView.Adapter<LugarAdapter.LugarViewHolder>() {

    private var selectedLugar: Lugar? = null

    fun seleccionarLugar(lugar: Lugar) {
        selectedLugar = lugar
        notifyDataSetChanged()
    }

    inner class LugarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombre: TextView = itemView.findViewById(R.id.nombreLugar)
        val direccion: TextView = itemView.findViewById(R.id.direccionLugar)
        val descripcion: TextView = itemView.findViewById(R.id.descripcionLugar)
        val imagen: ImageView = itemView.findViewById(R.id.imagenLugar)
        val progress: ProgressBar = itemView.findViewById(R.id.imageProgress)

        fun bind(lugar: Lugar) {
            nombre.text = lugar.nombre
            direccion.text = lugar.direccion
            descripcion.text = lugar.descripcion

            Glide.with(itemView.context)
                .load(lugar.imagenUrl)
                .placeholder(R.drawable.placeholder_loading)
                .error(R.drawable.error_image)
                .into(imagen)

            // ✅ Fondo si está seleccionado
            if (lugar == selectedLugar) {
                itemView.setBackgroundColor(Color.parseColor("#FFF59D")) // Amarillo claro
            } else {
                itemView.setBackgroundColor(Color.WHITE)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LugarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lugar, parent, false)
        return LugarViewHolder(view)
    }

    override fun onBindViewHolder(holder: LugarViewHolder, position: Int) {
        holder.bind(lugares[position])
    }

    override fun getItemCount() = lugares.size
}
