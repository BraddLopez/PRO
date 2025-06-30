package com.example.pro

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        val layout = findViewById<android.view.View>(R.id.editProfileLayout)
        ViewCompat.setOnApplyWindowInsetsListener(layout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val nombre = findViewById<EditText>(R.id.editTextName)
        val email = findViewById<EditText>(R.id.editTextEmail)
        val password = findViewById<EditText>(R.id.editTextPassword)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarCambios)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            email.setText(currentUser.email)

            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val nombreActual = document.getString("nombre")
                        nombre.setText(nombreActual)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show()
                }
        }

        btnGuardar.setOnClickListener {
            val nuevoNombre = nombre.text.toString()
            val nuevoEmail = email.text.toString()
            val nuevaContrasena = password.text.toString()

            val user = auth.currentUser
            if (user != null) {
                db.collection("users").document(user.uid)
                    .update("nombre", nuevoNombre)
                    .addOnSuccessListener {
                        if (nuevoEmail != user.email) {
                            user.updateEmail(nuevoEmail).addOnCompleteListener { emailTask ->
                                if (!emailTask.isSuccessful) {
                                    Toast.makeText(this, "Error actualizando email", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        if (nuevaContrasena.isNotEmpty()) {
                            user.updatePassword(nuevaContrasena).addOnCompleteListener { passTask ->
                                if (passTask.isSuccessful) {
                                    Toast.makeText(this, "Perfil actualizado con éxito", Toast.LENGTH_SHORT).show()
                                    finish() // ← Cierra esta pantalla
                                } else {
                                    Toast.makeText(this, "Error al cambiar contraseña", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                            finish() // ← Cierra esta pantalla
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al actualizar nombre", Toast.LENGTH_SHORT).show()
                    }
            }
        }


        val profileImage = findViewById<ImageView>(R.id.profileImage)
        val btnEditarFoto = findViewById<ImageButton>(R.id.btnEditarFoto)

        // Leer avatar guardado desde SharedPreferences y mostrarlo
        val avatarId = getSharedPreferences("perfil", MODE_PRIVATE)
            .getInt("avatarSeleccionado", -1)

        if (avatarId != -1) {
            profileImage.setImageResource(avatarId)
        }


// Lista de avatares locales (res/drawable)
        val avatarList = listOf(
            R.drawable.beach_logo,
            R.drawable.avatar1,
            R.drawable.avatar2,
            R.drawable.avatar3
        )

        btnEditarFoto.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Elige tu avatar")

            // Opciones de nombre a mostrar
            val avatarNames = arrayOf("beach_logo", "Avatar 1", "Avatar 2", "Avatar 3")

            builder.setItems(avatarNames) { _, which ->
                profileImage.setImageResource(avatarList[which])

                // Opcional: guardar avatar seleccionado para cargarlo luego
                val sharedPref = getSharedPreferences("perfil", MODE_PRIVATE)
                sharedPref.edit().putInt("avatarSeleccionado", avatarList[which]).apply()
            }

            builder.setNegativeButton("Cancelar", null)
            builder.show()
        }


        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.inicio -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
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

}
