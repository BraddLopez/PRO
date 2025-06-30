package com.example.pro

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import models.Beach

class RecommendationActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var beachAdapter: BeachAdapter
    private val beachList = mutableListOf<Beach>()
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommendation)

        recyclerView = findViewById(R.id.recommendationRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        beachAdapter = BeachAdapter(beachList)
        recyclerView.adapter = beachAdapter

        cargarRecomendaciones()

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.inicio -> {
                    finishAffinity()
                    true
                }
                R.id.atras -> {
                    onBackPressedDispatcher.onBackPressed()
                    true
                }

                else -> false
            }
        }
    }

    private fun cargarRecomendaciones() {
        val db = FirebaseFirestore.getInstance()
        db.collection("playas")
            .orderBy("rating", com.google.firebase.firestore.Query.Direction.DESCENDING) // 👈 ordena por rating
            .limit(10) // Muestra solo las 10 mejores
            .get()
            .addOnSuccessListener { result ->
                beachList.clear()
                for (doc in result) {
                    val beach = Beach(
                        id = doc.id,
                        name = doc.getString("nombre") ?: "",
                        location = doc.getString("ubicacion") ?: "",
                        rating = doc.getDouble("rating")?.toFloat() ?: 0f,
                        imageName = doc.getString("imagenNombre") ?: "",
                        type = doc.getString("tipo") ?: "",
                        description = doc.getString("descripcion") ?: "",
                        region = doc.getString("region") ?: "",
                        commentCount = doc.getLong("conteo")?.toInt() ?: 0
                    )
                    beachList.add(beach)
                }
                beachAdapter.notifyDataSetChanged()
            }
    }

}
