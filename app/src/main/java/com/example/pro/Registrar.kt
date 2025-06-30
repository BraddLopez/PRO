package com.example.pro

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class Registrar : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var nombreCompletos: EditText
    private lateinit var etEmail: EditText
    private lateinit var contrasena: EditText
    private lateinit var confirmarContrasena: EditText
    private lateinit var registrarUsuario: Button

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registrar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.Registrar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        nombreCompletos = findViewById(R.id.nombreCompletos)
        etEmail = findViewById(R.id.etEmail)
        contrasena = findViewById(R.id.contrasena)
        confirmarContrasena = findViewById(R.id.confirmarContrasena)
        registrarUsuario = findViewById(R.id.registarUsuario)

        registrarUsuario.setOnClickListener {
            val nombres = nombreCompletos.text.toString()
            val email = etEmail.text.toString()
            val password = contrasena.text.toString()
            val confirmPassword = confirmarContrasena.text.toString()

            if (nombres.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(nombres)
                            .build()

                        user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                            user.sendEmailVerification().addOnCompleteListener { verifyTask ->
                                if (verifyTask.isSuccessful) {
                                    Toast.makeText(
                                        this,
                                        "Verification email sent. Please check your inbox.",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
                                        if (tokenTask.isSuccessful) {
                                            val token = tokenTask.result

                                            val userData = hashMapOf(
                                                "nombre" to nombres,
                                                "email" to email,
                                                "token" to token, // ✅ Agregamos token
                                                "notifications_enabled" to true // ✅ Guardamos el valor por defecto
                                            )

                                            db.collection("users")
                                                .document(user.uid)
                                                .set(userData)
                                                .addOnSuccessListener {
                                                    Log.d("Registro", "✅ Datos y token guardados")

                                                    auth.signOut()
                                                    startActivity(Intent(this, IniciarSesion::class.java))
                                                    finish()
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.w("Registro", "❌ Error al guardar datos", e)
                                                }
                                        } else {
                                            Log.e("Registro", "❌ No se pudo obtener el token", tokenTask.exception)
                                        }
                                    }

                                    auth.signOut()
                                    startActivity(Intent(this, IniciarSesion::class.java))
                                    finish()
                                } else {
                                    Toast.makeText(
                                        this,
                                        "Failed to send verification email",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "Registration failed: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.inicio -> {
                    finishAffinity()
                    System.exit(0)
                    true
                }
                R.id.atras -> {
                    onBackPressed()
                    true
                }

                else -> false
            }
        }

    }
}
