package com.example.pro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import models.Lugar

class LugarAdapter(private val lugares: List<Lugar>) :
    RecyclerView.Adapter<LugarAdapter.LugarViewHolder>() {

    class LugarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombre: TextView = itemView.findViewById(R.id.nombreLugar)
        val direccion: TextView = itemView.findViewById(R.id.direccionLugar)
        val descripcion: TextView = itemView.findViewById(R.id.descripcionLugar)
        val imagen: ImageView = itemView.findViewById(R.id.imagenLugar)
        val progress: ProgressBar = itemView.findViewById(R.id.imageProgress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LugarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lugar, parent, false)
        return LugarViewHolder(view)
    }

    override fun onBindViewHolder(holder: LugarViewHolder, position: Int) {
        val lugar = lugares[position]
        holder.nombre.text = lugar.nombre
        holder.direccion.text = lugar.direccion
        holder.descripcion.text = lugar.descripcion
        Glide.with(holder.itemView.context)
            .load(lugar.imagenUrl)
            .placeholder(R.drawable.placeholder_loading) // ícono mientras carga
            .error(R.drawable.error_image)               // si falla la carga
            .into(holder.imagen)


    }

    override fun getItemCount() = lugares.size
}
