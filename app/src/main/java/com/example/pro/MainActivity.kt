package com.example.pro

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import models.Beach
import qr.QrScanActivity
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.ktx.Firebase
import androidx.core.app.TaskStackBuilder


class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var navigationView: NavigationView
    private lateinit var beachRecyclerView: RecyclerView
    private lateinit var beachAdapter: BeachAdapter
    private lateinit var toolbar: Toolbar
    private lateinit var searchView: SearchView
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var beachList: MutableList<Beach>
    private lateinit var btnFilter: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null || !user.isEmailVerified) {
            val intent = Intent(this, LoginWelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        toolbar = findViewById(R.id.toolbar)
        beachRecyclerView = findViewById(R.id.beachRecyclerView)
        searchView = findViewById(R.id.searchView)
        btnFilter = findViewById(R.id.btnFilter)

        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            getBeachesFromFirestore()
            swipeRefreshLayout.isRefreshing = false
        }

        setSupportActionBar(toolbar)
        toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_nav, R.string.close_nav)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        beachRecyclerView.layoutManager = LinearLayoutManager(this)
        beachList = mutableListOf()
        beachAdapter = BeachAdapter(beachList)
        beachRecyclerView.adapter = beachAdapter
        getBeachesFromFirestore()

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val userAuth = FirebaseAuth.getInstance().currentUser ?: return@addOnCompleteListener
                val db = FirebaseFirestore.getInstance()
                val userData = mapOf("token" to token)
                db.collection("users").document(userAuth.uid).set(userData, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d("FCM", "Token guardado correctamente")
                        // 👇 NUEVO: mostrar token en pantalla
                        Toast.makeText(this, "TOKEN ACTUAL:\n$token", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener { Log.e("FCM", "Error al guardar token", it) }
            }
        }


        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_qr_scan -> {
                    startActivity(Intent(this, QrScanActivity::class.java))
                    drawerLayout.closeDrawers(); true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    drawerLayout.closeDrawers(); true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    drawerLayout.closeDrawers(); true
                }
                R.id.nav_logout -> {
                    AlertDialog.Builder(this)
                        .setTitle("Cerrar sesión")
                        .setMessage("¿Estás seguro de que quieres cerrar sesión?")
                        .setPositiveButton("Sí") { _, _ ->
                            FirebaseAuth.getInstance().signOut()
                            val intent = Intent(this, LoginWelcomeActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent); finish()
                        }
                        .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                        .show(); true
                }
                else -> false
            }
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.inicio -> {
                    moveTaskToBack(true)
                    true
                }
                R.id.atras -> { showExitConfirmationDialog(); true }
                else -> false
            }
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterBeachesBySearch(query); return false
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                filterBeachesBySearch(newText); return false
            }
        })

        val regions = arrayOf("Todas", "Norte", "Centro", "Sur")

        val tipoCategorias = listOf(
            "Todos", "Familiar", "Tranquila", "Turística", "Natural", "Cultural",
            "Pesca", "Surf", "Histórica", "Recreativa", "Urbana", "Fiestas"
        )

        btnFilter.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.filter_bottom_sheet, null)
            val dialog = BottomSheetDialog(this)
            dialog.setContentView(dialogView)

            val spinnerRegion = dialogView.findViewById<Spinner>(R.id.spinnerRegion)
            val spinnerTipo = dialogView.findViewById<Spinner>(R.id.spinnerTipo)
            val spinnerRating = dialogView.findViewById<Spinner>(R.id.spinnerRating)

            spinnerRegion.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, regions)
            spinnerTipo.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, tipoCategorias)
            spinnerRating.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Todas", "1", "2", "3", "4", "5"))

            dialogView.findViewById<Button>(R.id.btnApplyFilters).setOnClickListener {
                val selectedRegion = spinnerRegion.selectedItem.toString()
                val selectedTipo = spinnerTipo.selectedItem.toString()
                val selectedRating = spinnerRating.selectedItem.toString().toIntOrNull() ?: 0
                applyCombinedFilters(selectedRegion, selectedTipo, selectedRating)
                dialog.dismiss()
            }

            dialog.show()
        }

        Firebase.dynamicLinks
            .getDynamicLink(intent)
            .addOnSuccessListener(this) { pendingDynamicLinkData ->
                val deepLink: Uri? = pendingDynamicLinkData?.link

                if (deepLink != null) {
                    val beachId = deepLink.getQueryParameter("beachId")
                    if (!beachId.isNullOrEmpty()) {
                        val detailIntent = Intent(this, BeachDetailActivity::class.java)
                        detailIntent.putExtra("id", beachId)

                        val stackBuilder = androidx.core.app.TaskStackBuilder.create(this)
                        stackBuilder.addNextIntentWithParentStack(detailIntent)
                        stackBuilder.startActivities()

                    }
                }
            }
            .addOnFailureListener(this) { e ->
                e.printStackTrace()
            }


    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Notificaciones habilitadas", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permiso de notificación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyCombinedFilters(region: String, tipo: String, minRating: Int) {
        val db = FirebaseFirestore.getInstance()
        var query: Query = db.collection("playas")
        if (region != "Todas") query = query.whereEqualTo("region", region)

        query.get().addOnSuccessListener { result ->
            val filtered = result.mapNotNull { doc ->
                val rating = doc.getDouble("rating")?.toFloat() ?: 0f
                val tipoPlaya = doc.getString("tipo") ?: ""

                val tipoCoincide = when (tipo) {
                    "Todos" -> true
                    else -> tipoPlaya.contains(tipo, ignoreCase = true)
                }

                if (tipoCoincide && rating >= minRating) {
                    Beach(
                        id = doc.id,
                        name = doc.getString("nombre") ?: "",
                        location = doc.getString("ubicacion") ?: "",
                        rating = rating,
                        imageName = doc.getString("imagenNombre") ?: "",
                        type = tipoPlaya,
                        description = doc.getString("descripcion") ?: "",
                        region = doc.getString("region") ?: "",
                        commentCount = doc.getLong("conteo")?.toInt() ?: 0
                    )
                } else null
            }
            beachList.clear(); beachList.addAll(filtered); beachAdapter.notifyDataSetChanged()
        }.addOnFailureListener {
            Log.w("Firestore", "Error al aplicar filtros", it)
        }
    }

    private fun getBeachesFromFirestore() {
        val db = FirebaseFirestore.getInstance()
        db.collection("playas")
            .addSnapshotListener { result, exception ->
                if (exception != null) {
                    Log.w("Firestore", "Error al obtener las playas", exception)
                    return@addSnapshotListener
                }
                beachList.clear()
                for (doc in result!!) {
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

    private fun filterBeachesByRegion(region: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("playas").whereEqualTo("region", region).get()
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
            }.addOnFailureListener {
                Log.w("Firestore", "Error al obtener playas por región", it)
            }
    }

    private fun filterBeachesBySearch(query: String?) {
        if (query.isNullOrEmpty()) {
            beachAdapter.updateList(beachList); return
        }
        val filtered = beachList.filter {
            it.name.contains(query, true) || it.type.contains(query, true) || it.region.contains(query, true)
        }
        beachAdapter.updateList(filtered)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (toggle.onOptionsItemSelected(item)) true else super.onOptionsItemSelected(item)
    }

    @Deprecated("Usar OnBackPressedDispatcher en su lugar")
    override fun onBackPressed() {
        showExitConfirmationDialog()
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("¿Deseas salir?")
            .setMessage("Se cerrará la sesión y volverás al login.")
            .setPositiveButton("Sí") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, LoginWelcomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent); finish()
            }
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}





