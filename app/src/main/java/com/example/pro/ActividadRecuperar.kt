package com.example.pro

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class ActividadRecuperar : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var restablecerContrasena: Button

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_actividad_recuperar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main5)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets

        }

        auth = FirebaseAuth.getInstance()
        etEmail = findViewById(R.id.etEmail)
        restablecerContrasena = findViewById(R.id.btnRestablecerContraseña)


        restablecerContrasena.setOnClickListener {
            val correo = etEmail.text.toString()

            if (correo.isEmpty()) {
                Toast.makeText(this, "Por favor ingrese su correo electrónico", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(correo)
                .addOnCompleteListener { resetTask ->
                    if (resetTask.isSuccessful) {
                        Toast.makeText(
                            this,
                            "Correo de restablecimiento enviado. Revisa tu bandeja de entrada.",
                            Toast.LENGTH_LONG
                        ).show()
                        startActivity(Intent(this, IniciarSesion::class.java))
                        finish()
                    } else {
                        val exceptionMessage = resetTask.exception?.message ?: "Error desconocido"
                        Toast.makeText(
                            this,
                            "Error al enviar el correo: $exceptionMessage",
                            Toast.LENGTH_SHORT
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
