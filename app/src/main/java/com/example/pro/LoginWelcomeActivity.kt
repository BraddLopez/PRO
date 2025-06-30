package com.example.pro

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class LoginWelcomeActivity : AppCompatActivity() {
    private lateinit var registraUsuario: Button
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null && user.isEmailVerified) {
            // El usuario ya está logueado y su correo está verificado
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_login_welcome_activity)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        registraUsuario = findViewById(R.id.botonRegistrar)
        val botonIniciar = findViewById<Button>(R.id.botonIniciar)
        val continuarInvitado = findViewById<TextView>(R.id.Continuar)


        continuarInvitado?.setOnClickListener {
            logOutAndContinueAsGuest()
        }


        registraUsuario.setOnClickListener{
            startActivity(Intent(this, Registrar::class.java))
        }
        botonIniciar.setOnClickListener{
            startActivity(Intent(this, IniciarSesion::class.java))
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
    private fun logOutAndContinueAsGuest() {
        // Cerrar la sesión de Firebase
        FirebaseAuth.getInstance().signOut()

        // Navegar a la pantalla de playas como invitado
        val intent = Intent(this, MainActivity::class.java)  // Aquí vas a la actividad de las playas
        startActivity(intent)
        finish()  // Finalizamos la actividad actual
    }

}