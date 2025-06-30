package com.example.pro

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settingsLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializar SharedPreferences
        prefs = getSharedPreferences("settings", MODE_PRIVATE)

        // Referencias a los switches
        val switchNotifications = findViewById<SwitchCompat>(R.id.switch_notifications)
        val switchDarkMode = findViewById<SwitchCompat>(R.id.switch_dark_mode)

        // Cargar estados guardados
        switchNotifications.isChecked = prefs.getBoolean("notifications_enabled", true)
        switchDarkMode.isChecked = prefs.getBoolean("dark_mode_enabled", false)

        // Listener para Notificaciones
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notifications_enabled", isChecked).apply()

            if (isChecked) {
                checkNotificationPermission() // 👈 Verifica el permiso del sistema
            }

            val msg = if (isChecked) "Notificaciones activadas" else "Notificaciones desactivadas"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }



        // Listener para Modo Oscuro
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode_enabled", isChecked).apply()
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        val tvPrivacidad = findViewById<TextView>(R.id.tvPrivacidad)
        val tvTerminos = findViewById<TextView>(R.id.tvTerminos)
        val tvAyuda = findViewById<TextView>(R.id.tvAyuda)

        tvPrivacidad.setOnClickListener {
            mostrarDialogo("Política de privacidad", "Tu información personal será utilizada solo para fines de mejora de la experiencia en la app. No será compartida con terceros.")
        }

        tvTerminos.setOnClickListener {
            mostrarDialogo("Términos y condiciones", "Al usar esta aplicación aceptas nuestras condiciones de uso. No está permitido el uso indebido ni la distribución del contenido.")
        }

        tvAyuda.setOnClickListener {
            mostrarDialogo("Ayuda", "Para soporte técnico o consultas, contacta a: soporte@playasperu.com o visita la sección de preguntas frecuentes.")
        }

        val logoutButton = findViewById<Button>(R.id.logoutButton)
        logoutButton.setOnClickListener {
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginWelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // Navegación inferior
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.inicio -> {
                    finishAffinity() // Cierra todas las actividades
                    System.exit(0)   // Finaliza la app completamente
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

    fun checkNotificationPermission() {
        val areEnabled = NotificationManagerCompat.from(this).areNotificationsEnabled()
        if (!areEnabled) {
            Toast.makeText(this, "Las notificaciones están desactivadas desde el sistema", Toast.LENGTH_LONG).show()

            // Abrir los ajustes de notificaciones de esta app
            val intent = Intent().apply {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
            startActivity(intent)
        }
    }

    private fun mostrarDialogo(titulo: String, mensaje: String) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle(titulo)
        builder.setMessage(mensaje)
        builder.setPositiveButton("OK", null)
        builder.show()
    }


}
