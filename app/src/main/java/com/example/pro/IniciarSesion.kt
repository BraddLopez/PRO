package com.example.pro

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class IniciarSesion : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var contrasena: EditText
    private lateinit var botonIniciarSesion: Button
    private lateinit var olvidasteContrasena: TextView
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_iniciar_sesion)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main3)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        auth = FirebaseAuth.getInstance()
        olvidasteContrasena = findViewById(R.id.olvidasteContrasena)
        etEmail = findViewById(R.id.etEmail)
        contrasena = findViewById(R.id.contrasena)
        botonIniciarSesion = findViewById(R.id.botonIniciarSesion)

        botonIniciarSesion.setOnClickListener{
            val email = etEmail.text.toString()
            val contrasena = contrasena.text.toString()
            if(email.isEmpty() || contrasena.isEmpty()){
                Toast.makeText(this,  "Por favor, rellene los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, contrasena)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful){
                        val user = auth.currentUser
                        if(user != null && user.isEmailVerified){
                            Toast.makeText(this, "Inicio correctamente", Toast.LENGTH_SHORT).show()

                            val uid = user.uid
                            actualizarTokenFCM(uid)

                            // ✅ Leer si tiene activadas notificaciones y guardar en SharedPreferences
                            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
                            FirebaseFirestore.getInstance().collection("users").document(uid)
                                .get()
                                .addOnSuccessListener { document ->
                                    if (document.exists()) {
                                        val notiEnabled = document.getBoolean("notifications_enabled") ?: true
                                        prefs.edit().putBoolean("notifications_enabled", notiEnabled).apply()
                                    }
                                }

                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }else{
                            Toast.makeText(this, "Por favor, verifica tu correo antes de iniciar sesion", Toast.LENGTH_LONG).show()
                            auth.signOut()
                        }
                    }else{
                        Toast.makeText(this,"Autenticacion fallida: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        olvidasteContrasena.setOnClickListener {
            startActivity(Intent(this, ActividadRecuperar::class.java))

        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.inicio -> {
                    finishAffinity() // Cierra todas las actividades
                    System.exit(0)   // Finaliza la app completamente
                    true
                }
                R.id.atras -> {
                    onBackPressed() // Va a la actividad anterior en el stack
                    true
                }

                else -> false
            }
        }
    }

    private fun actualizarTokenFCM(uid: String) {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val userData = mapOf("token" to token)

                db.collection("users")
                    .document(uid)
                    .set(userData, SetOptions.merge()) // ✅ Crea o actualiza sin borrar otros campos
                    .addOnSuccessListener {
                        android.util.Log.d("FCM", "✅ Token guardado correctamente en Firestore")
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("FCM", "❌ Error al guardar token", e)
                    }
            }
    }
}