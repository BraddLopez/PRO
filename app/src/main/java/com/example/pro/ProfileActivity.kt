package com.example.pro

import adapters.ActivityAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import models.Beach
import models.UserActivity

class ProfileActivity : AppCompatActivity() {

    private lateinit var tvNombreUsuario: TextView
    private lateinit var tvCorreoUsuario: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var activityRecyclerView: RecyclerView
    private lateinit var activityAdapter: ActivityAdapter
    private lateinit var toggleActivityButton: Button // El botón para mostrar/ocultar actividades
    private lateinit var activityList: MutableList<UserActivity>
    private lateinit var recommendationRecyclerView: RecyclerView
    private lateinit var recommendationAdapter: BeachAdapter
    private lateinit var recommendationList: MutableList<Beach>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Inicializar vistas
        tvNombreUsuario = findViewById(R.id.tvNombreUsuario)
        tvCorreoUsuario = findViewById(R.id.tvCorreoUsuario)
        toggleActivityButton = findViewById(R.id.showActivityButton)

        // Inicializar autenticación y Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Inicializar lista para el RecyclerView de actividades
        activityList = mutableListOf()

        // Inicializar adaptador
        activityAdapter = ActivityAdapter(activityList)

        // Inicializar RecyclerView
        activityRecyclerView = findViewById(R.id.activityRecyclerView)
        activityRecyclerView.layoutManager = LinearLayoutManager(this)
        activityRecyclerView.adapter = activityAdapter

        // Inicializar la visibilidad del RecyclerView
        activityRecyclerView.visibility = View.GONE  // Ocultar el RecyclerView al principio

        cargarDatosUsuario() // ← Aquí

        // Obtener datos del usuario
        val user = auth.currentUser
        if (user != null) {
            val uid = user.uid

            // Obtener información del usuario de Firestore
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val nombre = document.getString("nombre")
                        val email = document.getString("email")

                        tvNombreUsuario.text = "Nombre: $nombre"
                        tvCorreoUsuario.text = "Correo: $email"
                    } else {
                        Toast.makeText(this, "No se encontraron datos del usuario", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al obtener los datos", Toast.LENGTH_SHORT).show()
                }

            // Cargar la actividad del usuario
            loadUserActivity(uid)


        }

        // Botón de editar perfil
        val editProfileButton = findViewById<Button>(R.id.editProfileButton)
        editProfileButton.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        // Manejo del botón atrás con callback
        val backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish() // o usa finishAffinity() si quieres cerrar todas las actividades
            }
        }
        onBackPressedDispatcher.addCallback(this, backPressedCallback)

        // Configuración del BottomNavigationView
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.inicio -> {
                    finishAffinity()
                    System.exit(0)
                    true
                }
                R.id.atras -> {
                    backPressedCallback.handleOnBackPressed() // ← Usamos el nuevo callback
                    true
                }

                else -> false
            }
        }


        // Configuración del botón para mostrar/ocultar las actividades
        toggleActivityButton.setOnClickListener {
            if (activityRecyclerView.visibility == View.GONE) {
                activityRecyclerView.visibility = View.VISIBLE  // Mostrar el RecyclerView
                toggleActivityButton.text = "Ocultar Actividad"  // Cambiar el texto del botón a "Ocultar"
            } else {
                activityRecyclerView.visibility = View.GONE  // Ocultar el RecyclerView
                toggleActivityButton.text = "Mostrar Actividad"  // Cambiar el texto del botón a "Mostrar"
            }
        }

        findViewById<MaterialButton>(R.id.btnVerRecomendaciones).setOnClickListener {
            val intent = Intent(this, RecommendationActivity::class.java)
            startActivity(intent)
        }


    }

    // Función para cargar la actividad del usuario
    private fun loadUserActivity(userId: String) {
        db.collection("users").document(userId).collection("userActivities")
            .orderBy("date", Query.Direction.DESCENDING)
            .get()

            .addOnSuccessListener { result ->
                activityList.clear()  // Limpiar la lista antes de agregar los nuevos datos
                for (document in result) {
                    // Recuperar los datos del comentario
                    val beachName = document.getString("beachName") ?: "Playa desconocida"
                    val comment = document.getString("commentText") ?: "Comentario desconocido"
                    val rating = document.getDouble("rating")?.toFloat() ?: 0f  // Puntuación
                    val date = document.getString("date") ?: "Fecha desconocida"

                    // Crear un objeto UserActivity y agregarlo a la lista
                    val userActivity = UserActivity(beachName, comment, rating, date)
                    activityList.add(userActivity)
                }
                // Notificar al adaptador que los datos han cambiado
                activityAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error obteniendo la actividad del usuario", e)
            }
    }

    private fun cargarDatosUsuario() {
        val user = auth.currentUser
        if (user != null) {
            val uid = user.uid

            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val nombre = document.getString("nombre")
                        val email = document.getString("email")

                        tvNombreUsuario.text = "Nombre: $nombre"
                        tvCorreoUsuario.text = "Correo: $email"
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al obtener los datos", Toast.LENGTH_SHORT).show()
                }

            // Avatar guardado localmente
            val avatarId = getSharedPreferences("perfil", MODE_PRIVATE)
                .getInt("avatarSeleccionado", -1)

            if (avatarId != -1) {
                findViewById<ImageView>(R.id.profileImage).setImageResource(avatarId)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        cargarDatosUsuario()
    }




    //private fun loadRecommendations() {
    //    db.collection("playas")
    //        .orderBy("conteo", com.google.firebase.firestore.Query.Direction.DESCENDING)
     //       .limit(5)
     //       .get()
       //     .addOnSuccessListener { result ->
         //       recommendationList.clear()
           //     for (document in result) {
             //       val beach = Beach(
               //         id = document.id,
                 //       name = document.getString("nombre") ?: "",
                   //     location = document.getString("ubicacion") ?: "",
                     //   rating = document.getDouble("rating")?.toFloat() ?: 0f,
                       // imageName = document.getString("imagenNombre") ?: "",
                      //  type = document.getString("tipo") ?: "",
                    //    description = document.getString("descripcion") ?: "",
                  //      region = document.getString("region") ?: "",
                     //   commentCount = document.getLong("conteo")?.toInt() ?: 0
                  //  )
                  //  recommendationList.add(beach)
          //      }
          //      recommendationAdapter.notifyDataSetChanged()
          //  }
          //  .addOnFailureListener { e ->
            //    Log.w("Firestore", "Error al cargar recomendaciones", e)
          //  }
   // }

}



