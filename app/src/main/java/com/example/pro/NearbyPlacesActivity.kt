package com.example.pro

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import models.Lugar
import org.json.JSONArray
import kotlin.math.*


class NearbyPlacesActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private var latitude: Double = -12.0464
    private var longitude: Double = -77.0428
    private var placeType: String = "tourist_attraction" // Por defecto
    private lateinit var lugaresRecyclerView: RecyclerView
    private lateinit var lugarAdapter: LugarAdapter
    private val marcadorPorLugar = mutableMapOf<Lugar, com.google.android.gms.maps.model.Marker>()
    private val lugarPorMarker = mutableMapOf<com.google.android.gms.maps.model.Marker, Lugar>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nearby_places)

        latitude = intent.getDoubleExtra("latitude", latitude)
        longitude = intent.getDoubleExtra("longitude", longitude)
        placeType = intent.getStringExtra("type") ?: "tourist_attraction"

        val tituloTipo = findViewById<TextView>(R.id.tituloTipoLugar)
        tituloTipo.text = when (placeType) {
            "restaurant" -> "Restaurantes cercanos"
            "lodging" -> "Hoteles cercanos"
            "night_club" -> "Discotecas cercanas"
            "tourist_attraction" -> "Lugares turísticos"
            else -> "Lugares cercanos"
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

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

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        val playaLatLng = LatLng(latitude, longitude)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(playaLatLng, 13f))
        googleMap.addMarker(MarkerOptions().position(playaLatLng).title("Playa"))

        buscarLugaresConPlacesAPI()

        googleMap.setOnMarkerClickListener { marker ->
            val lugarSeleccionado = lugarPorMarker[marker]
            if (lugarSeleccionado != null) {
                resaltarLugarEnRecyclerView(lugarSeleccionado)
            }
            false
        }

    }

    private fun buscarLugaresConPlacesAPI() {
        val apiKey = "AIzaSyBtPHL5_kCKaVT7viFfLqkc8NdbTrpk52o" // tu clave real
        val client = OkHttpClient()

        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=$latitude,$longitude" +
                "&radius=1500" +
                "&type=$placeType" +
                "&key=$apiKey"

        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("PlacesAPI", "Error en solicitud: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {

                val body = response.body?.string() ?: return
                val json = JSONObject(body)
                val results = json.getJSONArray("results")
                Log.d("PlacesAPI", "Respuesta completa: $body")
                Log.d("PlacesAPI", "Número de lugares recibidos: ${results.length()}")



                runOnUiThread {
                    if (results.length() == 0) {
                        Log.e("PlacesAPI", "❌ No se encontraron lugares para el tipo: $placeType")
                    }

                    // ✅ Añadir marcadores y generar lista con parseLugaresDesdeAPI()
                    val listaGenerada = parseLugaresDesdeAPI(results)

                    // Marcadores en el mapa
                    for (lugar in listaGenerada) {
                        val marker = googleMap.addMarker(
                            MarkerOptions()
                                .position(LatLng(lugar.latitud, lugar.longitud))
                                .title(lugar.nombre)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                        )

                        if (marker != null) {
                            marcadorPorLugar[lugar] = marker
                            lugarPorMarker[marker] = lugar
                        }
                    }


                    lugaresRecyclerView = findViewById(R.id.lugaresRecyclerView)
                    lugaresRecyclerView.layoutManager = LinearLayoutManager(this@NearbyPlacesActivity)
                    lugarAdapter = LugarAdapter(listaGenerada)
                    lugaresRecyclerView.adapter = lugarAdapter

                }

            }

        })
    }

    private fun parseLugaresDesdeAPI(results: JSONArray): List<Lugar> {
        val lugares = mutableListOf<Lugar>()

        for (i in 0 until results.length()) {
            val lugarJson = results.getJSONObject(i)
            val nombre = lugarJson.optString("name", "Sin nombre")
            val direccion = lugarJson.optString("vicinity", "Dirección no disponible")
            val descripcion = lugarJson.optJSONArray("types")?.join(", ") ?: "Lugar cercano"

            val fotos = lugarJson.optJSONArray("photos")
            val imagen = if (fotos != null && fotos.length() > 0) {
                val ref = fotos.getJSONObject(0).optString("photo_reference")
                "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photoreference=$ref&key=AIzaSyBtPHL5_kCKaVT7viFfLqkc8NdbTrpk52o"
            } else {
                "https://tusitio.com/imagen_por_defecto.png"
            }

            val lat = lugarJson.getJSONObject("geometry").getJSONObject("location").getDouble("lat")
            val lng = lugarJson.getJSONObject("geometry").getJSONObject("location").getDouble("lng")

            // ✅ Calcular distancia desde la playa
            val distancia = calcularDistanciaEnMetros(latitude, longitude, lat, lng)

            val lugar = Lugar(nombre, direccion, descripcion, imagen, distancia, lat, lng)
            lugares.add(lugar)
        }

        // ✅ Ordenar por distancia ascendente y tomar los 5 más cercanos
        return lugares.sortedBy { it.distancia }.take(5)
    }

    private fun calcularDistanciaEnMetros(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val radioTierra = 6371000.0 // en metros
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return radioTierra * c
    }

    private fun resaltarLugarEnRecyclerView(lugar: Lugar) {
        if (!::lugarAdapter.isInitialized) return  // ✅ evita crash si no está listo
        lugarAdapter.seleccionarLugar(lugar)

        // ✅ Mueve el RecyclerView al ítem
        val posicion = lugarAdapter.lugares.indexOf(lugar)
        if (posicion != -1) {
            lugaresRecyclerView.scrollToPosition(posicion)
        }
    }


}

