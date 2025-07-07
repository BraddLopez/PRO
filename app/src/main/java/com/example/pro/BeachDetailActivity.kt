package com.example.pro

import adapters.CommentAdapter
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.dynamiclinks.ktx.androidParameters
import com.google.firebase.dynamiclinks.ktx.dynamicLink
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.google.firebase.dynamiclinks.ktx.*
import models.Beach
import models.Comment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BeachDetailActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var nameText: TextView
    private lateinit var locationText: TextView
    private lateinit var mapButton: ImageButton
    private lateinit var shareButton: ImageButton
    private lateinit var commentRecyclerView: RecyclerView
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var commentList: MutableList<Comment>
    private lateinit var beachId: String
    private lateinit var submitCommentBtn: Button
    private lateinit var commentInput: EditText
    private lateinit var commentsRecyclerView: RecyclerView
    private lateinit var progressBar5: ProgressBar
    private lateinit var progressBar4: ProgressBar
    private lateinit var progressBar3: ProgressBar
    private lateinit var progressBar2: ProgressBar
    private lateinit var progressBar1: ProgressBar
    private lateinit var textRatingPromedio: TextView
    private lateinit var textUserRating: TextView

    private lateinit var beach: Beach
    private var destacadoCommentId: String? = null
    private lateinit var mapView: MapView
    private var latitude: Double = -12.0464
    private var longitude: Double = -77.0428


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser
        val vieneDeQR = intent.getBooleanExtra("fromQR", false)
        Log.d("DEBUG", "¿Abierto desde QR?: $vieneDeQR")

        if (user == null) {
            if (vieneDeQR) {
                // Si viene del QR, iniciar sesión anónima automáticamente
                FirebaseAuth.getInstance().signInAnonymously()
                    .addOnSuccessListener {
                        MainActivity.isAnonimo = true
                        recreate() // Reinicia la actividad ya como invitado
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al iniciar como invitado", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                return // 👈 evita que siga hasta que se complete
            } else {
                val intent = Intent(this, LoginWelcomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                return
            }
        } else {
            MainActivity.isAnonimo = user.isAnonymous
        }

        setContentView(R.layout.activity_beach_detail)

        textRatingPromedio = findViewById(R.id.textRatingPromedio)
        textRatingPromedio.text = "Cargando..."

        progressBar5 = findViewById(R.id.progressBar5)
        progressBar4 = findViewById(R.id.progressBar4)
        progressBar3 = findViewById(R.id.progressBar3)
        progressBar2 = findViewById(R.id.progressBar2)
        progressBar1 = findViewById(R.id.progressBar1)

        commentsRecyclerView = findViewById(R.id.commentsRecyclerView)

        destacadoCommentId = intent.getStringExtra("commentId")

        // ✅ Obtener el beachId correctamente primero
        beachId = intent.getStringExtra("id") ?: ""
        if (beachId.isEmpty()) {
            Toast.makeText(this, "ID de playa inválido", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        calcularYActualizarPromedio(beachId)

        // Inicialización de vistas
        val imageBeach = findViewById<ImageView>(R.id.detailImage)
        nameText = findViewById(R.id.detailName)
        locationText = findViewById(R.id.detailLocation)

        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)

        textUserRating = findViewById(R.id.textUserRating)
        verificarValoracionUsuarioActual(beachId)

        val typeText = findViewById<TextView>(R.id.detailType)
        val storyText = findViewById<TextView>(R.id.storyText)
        val btnTouristic = findViewById<Button>(R.id.btnTouristic)
        val btnRestaurants = findViewById<Button>(R.id.btnRestaurants)
        val btnHotels = findViewById<Button>(R.id.btnHotels)
        val btnNightlife = findViewById<Button>(R.id.btnNightlife)
        mapButton = findViewById(R.id.mapButton)
        shareButton = findViewById(R.id.shareButton)
        commentRecyclerView = findViewById(R.id.commentsRecyclerView)

        // Intentar obtener el nombre para saber si vino desde QR o desde otra pantalla
        cargarPlayaDesdeFirestore(beachId)

        // Rating
        val btnDarOpinion = findViewById<Button>(R.id.btnDarOpinion)
        btnDarOpinion.setOnClickListener {
            if (MainActivity.isAnonimo) {
                AlertDialog.Builder(this)
                    .setTitle("🔒 Inicia sesión")
                    .setMessage("Necesitas iniciar sesión para valorar esta playa.")
                    .setPositiveButton("Iniciar sesión") { dialog, _ ->
                        startActivity(Intent(this, LoginWelcomeActivity::class.java))
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancelar") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            } else {
                mostrarDialogoOpinion(beachId)
            }
        }




        mapButton.setOnClickListener {
            abrirUbicacion(beach.name)
        }
        shareButton.setOnClickListener {
            compartirPlaya(beach.name, beach.location)
        }

        btnTouristic.setOnClickListener {
            abrirLugaresCercanos("tourist_attraction")
        }
        btnRestaurants.setOnClickListener {
            abrirLugaresCercanos("restaurant")
        }
        btnHotels.setOnClickListener {
            abrirLugaresCercanos("lodging")
        }
        btnNightlife.setOnClickListener {
            abrirLugaresCercanos("night_club")
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

        commentList = mutableListOf()
        commentAdapter = CommentAdapter(
            this, // ✅ Pasar contexto
            commentList,
            onEditClick = { comment -> mostrarDialogoEditar(comment) },
            onDeleteClick = { comment -> eliminarComentario(comment) },
            onReplyClick = { comment -> showReplyDialog(comment) },
            beachId
        )
        commentRecyclerView = findViewById(R.id.commentsRecyclerView)
        commentRecyclerView.layoutManager = LinearLayoutManager(this)
        commentRecyclerView.adapter = commentAdapter
        loadCommentsFromFirebase(beachId, destacadoCommentId)



        val btnCuidar = findViewById<Button>(R.id.btnCuidarPlaya)
        val blinkAnimation = AnimationUtils.loadAnimation(this, R.anim.blink)
        btnCuidar.startAnimation(blinkAnimation)

        btnCuidar.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("🌊 Protejamos nuestras playas")
                .setMessage("Cuidar la playa es cuidar la vida. No dejes basura, respeta la fauna marina y comparte este mensaje 🌱💚")
                .setPositiveButton("¡Entendido!") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    //*****************FIRESTORE*******************
    private fun cargarPlayaDesdeFirestore(beachId: String) {
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("playas").document(beachId)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val playa = document.toObject(Beach::class.java)
                    Log.d("DEBUG_PLAYA", "Playa cargada: ${playa?.name} | Lat: ${playa?.latitud}, Lng: ${playa?.longitud}")
                    if (playa != null) {
                        playa.id = beachId
                        beach = playa
                        latitude = playa.latitud
                        longitude = playa.longitud
                        Log.d("FIRESTORE", "Latitude: $latitude, Longitude: $longitude") // verifica que llegan bien
                        inicializarVistasConPlaya(playa)
                        mapView.getMapAsync(this) // <- Agrega esto para que actualice el mapa cuando ya tenga lat/lng
                } else {
                        Toast.makeText(this, "Error al convertir la playa", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "No se encontró la playa", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
    }
    private fun inicializarVistasConPlaya(beach: Beach) {
        nameText.text = beach.name
        locationText.text = beach.location
        findViewById<TextView>(R.id.detailType).text = getString(R.string.beach_type, beach.type)
        findViewById<TextView>(R.id.storyText).text = beach.description
        val imageBeach = findViewById<ImageView>(R.id.detailImage)
        if (beach.imageName.isNotEmpty()) {
            val imageResource = resources.getIdentifier(beach.imageName, "drawable", packageName)
            if (imageResource != 0) {
                imageBeach.setImageResource(imageResource)
            } else {
                Toast.makeText(this, "Imagen '${beach.imageName}' no encontrada", Toast.LENGTH_SHORT).show()
            }
        }
        // Botones y QR
        findViewById<Button>(R.id.btnDarOpinion).setOnClickListener {
            mostrarDialogoOpinion(beach.id)
        }
        mapButton.setOnClickListener {
            abrirUbicacion(beach.name)
        }
        shareButton.setOnClickListener {
            compartirPlaya(beach.name, beach.location)
        }

        findViewById<ImageButton>(R.id.btnQr).setOnClickListener {
            generarQrConDynamicLink(beach.id) { qrBitmap ->
                qrBitmap?.let {
                    mostrarDialogoQr(it, beach)
                }
            }
        }
    }
    private fun saveUserActivity(userId: String, commentText: String, beachId: String, beachName: String, rating: Float?) {
        val db = FirebaseFirestore.getInstance()
        // Crear un documento con el historial de actividades del usuario
        val activity = hashMapOf(
            "commentText" to commentText,
            "beachId" to beachId,      // Guardamos el ID de la playa
            "beachName" to beachName,  // Guardamos el nombre de la playa
            "date" to getCurrentDate(),
            "userName" to getUserName(),
            "rating" to rating  // Guardamos el rating
        )
        // Guardar la actividad del usuario en la colección "userActivities"
        db.collection("users").document(userId).collection("userActivities")
            .add(activity)
            .addOnSuccessListener {
                Log.d("Firebase", "Actividad guardada correctamente")
            }
            .addOnFailureListener { e ->
                Log.w("Firebase", "Error guardando la actividad del usuario", e)
            }
    }

    //******************DATOS DE QRR************************
    fun generarQrConDynamicLink(beachId: String, callback: (Bitmap?) -> Unit) {
        val deepLink = Uri.parse("https://guiaperu.com/playa?beachId=$beachId")
        val dynamicLink = Firebase.dynamicLinks.dynamicLink {
            link = deepLink
            domainUriPrefix = "https://guiaperuplayas.page.link"
            androidParameters("com.example.pro") {
                fallbackUrl = Uri.parse("https://drive.google.com/drive/folders/1pG7ApcFFMekfcKHYARiNmrtu7ShtvXcS?usp=sharing")
            }
        }
        val dynamicLinkUri = dynamicLink.uri.toString()
        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(
                dynamicLinkUri,
                BarcodeFormat.QR_CODE,
                400,
                400
            )
            callback(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            callback(null)
        }
    }
    private fun mostrarDialogoQr(qrBitmap: Bitmap, beach: Beach) {
        val imageView = ImageView(this)
        imageView.setImageBitmap(qrBitmap)
        AlertDialog.Builder(this)
            .setTitle("Código QR de la playa")
            .setView(imageView)
            .setPositiveButton("Cerrar", null)
            .setNeutralButton("Descargar PDF") { _, _ ->
                val nombreSeguro = beach.name.replace(" ", "_").replace(Regex("[^A-Za-z0-9_]"), "")
                guardarQrComoPdf(qrBitmap, "qr_${nombreSeguro}.pdf")
            }
            .show()
    }
    private fun guardarQrComoPdf(bitmap: Bitmap, nombreArchivo: String) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        pdfDocument.finishPage(page)
        val file = File(getExternalFilesDir(null), nombreArchivo)
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(this, "QR guardado en PDF: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error al guardar PDF", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    //*************DIALOG******************
    fun mostrarDialogoOpinion(beachId: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_opinion, null)
        val commentInput = dialogView.findViewById<EditText>(R.id.commentInput)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)
        val submitBtn = dialogView.findViewById<Button>(R.id.submitCommentBtn)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .setNegativeButton("Cancelar") { dialogInterface, _ -> dialogInterface.dismiss() }
            .create()

        val user = FirebaseAuth.getInstance().currentUser

        if (user == null || user.isAnonymous || MainActivity.isAnonimo) {
            // Bloquear el diálogo por completo y pedir login
            dialog.dismiss()
            AlertDialog.Builder(this)
                .setTitle("🔒 Inicia sesión")
                .setMessage("Debes iniciar sesión para comentar y valorar.")
                .setPositiveButton("Iniciar sesión") { _, _ ->
                    startActivity(Intent(this, LoginWelcomeActivity::class.java))
                }
                .setNegativeButton("Cancelar", null)
                .show()
            return
        }
        // Si el usuario no es anónimo, permitir la interacción
        val db = FirebaseFirestore.getInstance()
        db.collection("playas").document(beachId)
            .collection("comments")
            .whereEqualTo("userId", user.uid)
            .whereGreaterThan("rating", 0)
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    ratingBar.isEnabled = false
                    ratingBar.alpha = 0.5f
                    ratingBar.rating = result.documents.first().getDouble("rating")?.toFloat() ?: 0f
                    Toast.makeText(this, "Ya calificaste. Puedes seguir comentando sin puntuar.", Toast.LENGTH_SHORT).show()
                }
            }
        submitBtn.setOnClickListener {
            val commentText = commentInput.text.toString().trim()
            val rating = if (ratingBar.isEnabled) ratingBar.rating else 0f

            if (commentText.isNotEmpty()) {
                guardarComentario(beachId, commentText, rating)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Escribe un comentario", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun showReplyDialog(parentComment: Comment) {
        val input = EditText(this)
        input.hint = "Escribe tu respuesta..."
        AlertDialog.Builder(this)
            .setTitle("Responder a ${parentComment.userName}")
            .setView(input)
            .setPositiveButton("Enviar") { _, _ ->
                val replyText = input.text.toString()
                if (replyText.isNotBlank()) {
                    // Usa tu función existente
                    guardarComentario(parentComment.beachId, replyText, 0f, parentComment.id)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    private fun mostrarDialogoEditar(comment: Comment) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_opinion, null)
        val editTextComentario = dialogView.findViewById<EditText>(R.id.commentInput)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)
        val btnEnviar = dialogView.findViewById<Button>(R.id.submitCommentBtn)
        val btnGuardar = dialogView.findViewById<Button>(R.id.btnGuardar)
        val btnCancelar = dialogView.findViewById<Button>(R.id.btnCancelar)
        // Prellenar campos
        editTextComentario.setText(comment.text)
        ratingBar.rating = comment.rating // Permitir la edición del rating
        // Mostrar el botón de enviar (si corresponde en tu flujo)
        btnEnviar.visibility = View.GONE
        // Mostrar botones de guardar/cancelar
        btnGuardar.visibility = View.VISIBLE
        btnCancelar.visibility = View.VISIBLE
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        btnGuardar.setOnClickListener {
            val nuevoComentario = editTextComentario.text.toString().trim()
            if (nuevoComentario.isEmpty()) {
                Toast.makeText(this, "Ingresa un comentario", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Obtener la nueva calificación desde el ratingBar (editable ahora)
            val nuevaCalificacion = ratingBar.rating
            // ✅ Usamos la nueva calificación y el comentario actualizado
            actualizarComentarioEnFirestore(comment.beachId, comment.id, nuevoComentario, nuevaCalificacion)
            alertDialog.dismiss()
        }
        btnCancelar.setOnClickListener {
            alertDialog.dismiss()
        }
        alertDialog.show()
    }
    private fun mostrarDialogoEliminarComentario(comment: Comment, onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar comentario")
            .setMessage("¿Estás seguro de que deseas eliminar este comentario?")
            .setPositiveButton("Sí") { _, _ ->
                onConfirm()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    private fun eliminarComentario(comment: Comment) {
        val db = FirebaseFirestore.getInstance()
        mostrarDialogoEliminarComentario(comment) {
            // Paso 1: Buscar respuestas si es un comentario raíz
            db.collection("playas").document(comment.beachId)
                .collection("comments")
                .whereEqualTo("parentId", comment.id)
                .get()
                .addOnSuccessListener { repliesSnapshot ->
                    val batch = db.batch()
                    // Eliminar respuestas
                    for (reply in repliesSnapshot.documents) {
                        val replyRef = db.collection("playas").document(comment.beachId)
                            .collection("comments").document(reply.id)
                        batch.delete(replyRef)
                        // También eliminar de colección raíz si usas una colección global
                        val globalReplyRef = db.collection("comments").document(reply.id)
                        batch.delete(globalReplyRef)
                    }
                    // Eliminar el comentario raíz
                    val commentRef = db.collection("playas").document(comment.beachId)
                        .collection("comments").document(comment.id)
                    val globalCommentRef = db.collection("comments").document(comment.id)
                    batch.delete(commentRef)
                    batch.delete(globalCommentRef)
                    batch.commit().addOnSuccessListener {
                        Toast.makeText(this, "Comentario y respuestas eliminadas", Toast.LENGTH_SHORT).show()
                        // Actualizar lista local
                        commentList.remove(comment)
                        commentAdapter.notifyDataSetChanged()
                        Handler(Looper.getMainLooper()).postDelayed({
                            calcularPromedioYActualizarUI(comment.beachId)
                            verificarValoracionUsuarioActual(comment.beachId)
                        }, 500)
                    }.addOnFailureListener {
                        Toast.makeText(this, "Error al eliminar comentarios", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al buscar respuestas", Toast.LENGTH_SHORT).show()
                }
        }
    }

    //*******************COMENTARIOS************
    // Guardar comentario del usuario en Firestore
    private fun guardarComentario(beachId: String, commentText: String, rating: Float, parentId: String? = null) {
        val db = FirebaseFirestore.getInstance()
        val user = FirebaseAuth.getInstance().currentUser ?: return
        if (beachId.isBlank()) {
            Toast.makeText(this, "ID de playa no válido", Toast.LENGTH_SHORT).show()
            return
        }
        val userId = user.uid
        val userName = user.displayName ?: "Anónimo"
        val timestamp = System.currentTimeMillis()
        val commentDocRef = db.collection("comments").document()
        val commentId = commentDocRef.id
        val commentData = mutableMapOf<String, Any?>(
            "id" to commentId,
            "userId" to userId,
            "userName" to userName,
            "beachId" to beachId,
            "text" to commentText,
            "rating" to rating,
            "date" to obtenerFechaFormateada(timestamp),
            "timestamp" to timestamp,
            "parentId" to parentId // ← si es null, se guarda como null
        )
        // Guardar en colección raíz
        commentDocRef.set(commentData)
        // Guardar también en subcolección de la playa
        db.collection("playas").document(beachId)
            .collection("comments")
            .document(commentId)
            .set(commentData)
            .addOnSuccessListener {
                Toast.makeText(this, "Comentario guardado", Toast.LENGTH_SHORT).show()
                loadCommentsFromFirebase(beachId)
                updateCommentCount(beachId)
                calcularYActualizarPromedio(beachId)
                verificarValoracionUsuarioActual(beachId)
                db.collection("playas").document(beachId).get()
                    .addOnSuccessListener { document ->
                        val beachName = document.getString("nombre") ?: "Playa desconocida"
                        saveUserActivity(userId, commentText, beachId, beachName, rating)
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al guardar comentario", Toast.LENGTH_SHORT).show()
            }
        if (commentText.contains("@destacar", ignoreCase = true)) {
            notificarUsuariosDeLaPlaya(beachId, userId, userName)
        }
        if (!parentId.isNullOrEmpty()) {
            notificarRespuestaComentario(beachId, userId, userName, parentId)
        }

    }
    private fun loadCommentsFromFirebase(beachId: String, commentIdDestacado: String? = null) {
        val db = FirebaseFirestore.getInstance()
        db.collection("playas").document(beachId).collection("comments")
            .orderBy("timestamp", Query.Direction.DESCENDING) // Orden por fecha
            .get()
            .addOnSuccessListener { result ->
                // Limpia listas
                commentList.clear()
                val allComments = mutableListOf<Comment>()
                val repliesMap = mutableMapOf<String, MutableList<Comment>>()
                val userNameMap = mutableMapOf<String, String>() // id -> userName
                for (document in result) {
                    val comment = document.toObject(Comment::class.java)
                    comment.id = document.id
                    allComments.add(comment)
                    userNameMap[comment.id] = comment.userName ?: "Anónimo"
                }
                // Separar en raíz y respuestas
                for (comment in allComments) {
                    val parentId = comment.parentId
                    if (parentId.isNullOrEmpty()) {
                        commentList.add(comment) // raíz
                    } else {
                        val list = repliesMap.getOrPut(parentId) { mutableListOf() }
                        list.add(comment)
                    }
                }
                // Enviar mapas al adapter
                commentAdapter.setCommentIdToUserNameMap(userNameMap)
                commentAdapter.setRepliesMap(repliesMap)
                commentAdapter.notifyDataSetChanged()
                // Destacar comentario si aplica
                if (!commentIdDestacado.isNullOrEmpty()) {
                    commentAdapter.setComentarioDestacado(commentIdDestacado)

                    // Si está en la lista principal, hacer scroll
                    val index = commentList.indexOfFirst { it.id == commentIdDestacado }
                    if (index != -1) {
                        commentRecyclerView.scrollToPosition(index)
                    }
                }

            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar comentarios", Toast.LENGTH_SHORT).show()
            }
    }
    private fun updateCommentCount(beachId: String) {
        val db = FirebaseFirestore.getInstance()
        // Contar los comentarios en la colección de comentarios de la playa
        db.collection("playas").document(beachId).collection("comments")
            .get()
            .addOnSuccessListener { result ->
                val commentCount = result.size() // Contamos el número de comentarios
                // Crear el mapa con el tipo adecuado para Firebase
                val updatedData: MutableMap<String, Any> = hashMapOf(
                    "conteo" to commentCount  // Usamos Any en lugar de Int
                )
                // Actualizar el campo commentCount en el documento de la playa
                db.collection("playas").document(beachId)
                    .update(updatedData)
                    .addOnSuccessListener {
                        Log.d("Firebase", "Conteo de comentarios actualizado correctamente")
                    }
                    .addOnFailureListener { e ->
                        Log.w("Firebase", "Error actualizando el conteo de comentarios", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.w("Firebase", "Error contando los comentarios", e)
            }
    }
    private fun actualizarComentarioEnFirestore(beachId: String, commentId: String, nuevoTexto: String, nuevaRating: Float) {
        val db = FirebaseFirestore.getInstance()
        val nuevosDatos = mapOf(
            "text" to nuevoTexto,
            "rating" to nuevaRating,
            "date" to obtenerFechaFormateada(System.currentTimeMillis()),
            "timestamp" to System.currentTimeMillis()
        )
        // 🔁 Actualizar en /comments
        db.collection("comments").document(commentId)
            .update(nuevosDatos)
            .addOnSuccessListener {
                // 🔁 También actualizar en /playas/{beachId}/comments/{commentId}
                db.collection("playas").document(beachId)
                    .collection("comments").document(commentId)
                    .update(nuevosDatos)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Comentario actualizado", Toast.LENGTH_SHORT).show()
                        // ✅ Recargar la lista
                        loadCommentsFromFirebase(beachId)
                        // ✅ Recalcular el promedio con pequeño delay
                        Handler(Looper.getMainLooper()).postDelayed({
                            calcularPromedioYActualizarUI(beachId)
                            calcularYActualizarPromedio(beachId)
                        }, 500)
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "⚠️ Se actualizó solo la raíz", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar comentario", Toast.LENGTH_SHORT).show()
            }
    }

    //************************VALORACIONES*********************
    private fun calcularYActualizarPromedio(beachId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("playas").document(beachId).collection("comments")
            .whereEqualTo("parentId", null)
            .get()
            .addOnSuccessListener { result ->
                val ratings = result.mapNotNull { it.getDouble("rating")?.toFloat() }
                if (ratings.isNotEmpty()) {
                    val total = ratings.size
                    val promedio = ratings.sum() / total
                    val cinco = ratings.count { it == 5f }
                    val cuatro = ratings.count { it == 4f }
                    val tres = ratings.count { it == 3f }
                    val dos = ratings.count { it == 2f }
                    val uno = ratings.count { it == 1f }
                    // ✅ Actualiza el promedio en Firestore también
                    db.collection("playas").document(beachId)
                        .update("rating", promedio)
                        .addOnSuccessListener {
                            Log.d("Firebase", "Rating actualizado correctamente a $promedio")
                        }
                        .addOnFailureListener {
                            Log.w("Firebase", "Error actualizando rating", it)
                        }
                    // UI (solo visual)
                    textRatingPromedio.text = "Promedio: %.1f ★".format(promedio)
                    progressBar5.progress = (cinco * 100 / total)
                    progressBar4.progress = (cuatro * 100 / total)
                    progressBar3.progress = (tres * 100 / total)
                    progressBar2.progress = (dos * 100 / total)
                    progressBar1.progress = (uno * 100 / total)
                } else {
                    textRatingPromedio.text = "Sin valoraciones aún"
                }
            }
            .addOnFailureListener {
                textRatingPromedio.text = "Error al cargar ratings"
            }
    }
    // Calcular promedio de valoraciones y actualizar UI
    private fun calcularPromedioYActualizarUI(beachId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("comments")
            .whereEqualTo("beachId", beachId)
            .get()
            .addOnSuccessListener { snapshot ->
                val conteos = IntArray(5)
                var suma = 0f
                var total = 0
                for (doc in snapshot) {
                    val rating = doc.getDouble("rating")?.toInt() ?: 0
                    if (rating in 1..5) {
                        conteos[rating - 1]++
                        suma += rating
                        total++
                    }
                }
                val promedio = if (total > 0) suma / total else 0f
                textRatingPromedio.text = String.format("%.1f ★ (%d opiniones)", promedio, total)
                if (total > 0) {
                    progressBar5.progress = conteos[4] * 100 / total
                    progressBar4.progress = conteos[3] * 100 / total
                    progressBar3.progress = conteos[2] * 100 / total
                    progressBar2.progress = conteos[1] * 100 / total
                    progressBar1.progress = conteos[0] * 100 / total
                }
            }
    }
    private fun verificarValoracionUsuarioActual(beachId: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        val btnOpinion = findViewById<Button>(R.id.btnDarOpinion)
        // Agregar un Log para ver si el usuario está autenticado
        Log.d("VERIFICACION", "Usuario autenticado: ${user.uid}")
        // Consulta para verificar si el usuario ya ha calificado
        db.collection("playas").document(beachId)
            .collection("comments")
            .whereEqualTo("userId", user.uid) // Busca por el userId
            .whereGreaterThan("rating", 0.0)  // Solo comentarios con una calificación
            .get()
            .addOnSuccessListener { result ->
                // Agregar Log para ver si encontramos comentarios
                Log.d("VERIFICACION", "Comentarios encontrados: ${result.size()}")
                if (result.isEmpty) {
                    // Si no hay comentarios con calificación, el usuario puede dar una nueva
                    Log.d("VERIFICACION", "No se encontró calificación previa del usuario.")
                    btnOpinion.isEnabled = true
                    btnOpinion.text = "Dar mi opinión"
                    textUserRating.visibility = View.GONE
                } else {
                    // Si el usuario ya ha calificado
                    val comentarioRaiz = result.documents.firstOrNull { doc ->
                        // Agregar Log para verificar cada documento
                        Log.d("VERIFICACION", "Documento: ${doc.id}, Rating: ${doc.getDouble("rating")}")

                        // Verificar si tiene parentId nulo o no tiene parentId
                        !doc.contains("parentId") || doc.get("parentId") == null
                    }
                    if (comentarioRaiz != null) {
                        val rating = comentarioRaiz.getDouble("rating") ?: 0.0
                        // Mostrar la calificación del usuario
                        textUserRating.text = "Tu calificación: %.1f ★".format(rating)
                        textUserRating.visibility = View.VISIBLE
                        btnOpinion.isEnabled = false
                        btnOpinion.text = "Ya calificaste"
                        Log.d("VERIFICACION", "Usuario ya calificó. Calificación: $rating")
                    } else {
                        // Si no se encontró un comentario raíz, pero hay comentarios con calificación, podemos permitir que el usuario vuelva a comentar
                        Log.d("VERIFICACION", "Se encontró un comentario con rating, pero no es comentario raíz.")
                        btnOpinion.isEnabled = true
                        btnOpinion.text = "Dar mi opinión"
                        textUserRating.visibility = View.GONE
                    }
                }
            }
            .addOnFailureListener {
                Log.e("VERIFICACION", "Error al verificar la calificación: ${it.message}")
                Toast.makeText(this, "Error al verificar tu calificación", Toast.LENGTH_SHORT).show()
            }
    }


    //***********************MAPA*************************
    private fun abrirUbicacion(name: String) {
        val query = Uri.encode(name)
        val geoUri = Uri.parse("geo:0,0?q=$query")
        val intent = Intent(Intent.ACTION_VIEW, geoUri)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            val url = "https://maps.google.com/?q=$query"
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(webIntent)
        }
    }
    private fun compartirPlaya(name: String, location: String) {
        val query = Uri.encode("$name, $location")
        val mapsUrl = "https://www.google.com/maps/search/?api=1&query=$query"
        val shareText = "Mira esta playa: $name, ubicada en $location.\nUbícala en el mapa: $mapsUrl"
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Compartir con"))
    }
    override fun onMapReady(googleMap: GoogleMap) {
        Log.d("MAPA", "Map listo con lat=$latitude, lng=$longitude")
        val beachLocation = LatLng(latitude, longitude)
        googleMap.addMarker(MarkerOptions().position(beachLocation).title("Ubicación de la playa"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(beachLocation, 15f))

    }

    private fun abrirLugaresCercanos(googleType: String) {
        val intent = Intent(this, NearbyPlacesActivity::class.java)
        intent.putExtra("latitude", latitude)
        intent.putExtra("longitude", longitude)
        intent.putExtra("type", googleType)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }
    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
    override fun onDestroy() {
        super.onDestroy()
        if (::mapView.isInitialized) {
            mapView.onDestroy()
        }
    }
    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    //********************NOTIFICACIONES**********************
    private fun notificarUsuariosDeLaPlaya(beachId: String, userIdActual: String, nombreUsuario: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("playas").document(beachId).collection("comments")
            .get()
            .addOnSuccessListener { result ->
                val usuariosAVisitar = mutableSetOf<String>()
                for (doc in result) {
                    val userId = doc.getString("userId")
                    if (userId != null && userId != userIdActual) {
                        usuariosAVisitar.add(userId)
                    }
                }
                for (user in usuariosAVisitar) {
                    // Aquí simulamos notificación local (solo si el usuario está usando la app)
                    enviarNotificacionLocal("$nombreUsuario destacó un comentario en una playa que tú comentaste.")
                }
            }
            .addOnFailureListener {
                Log.e("NOTIF", "Error al buscar comentarios: ${it.message}")
            }
    }
    private fun enviarNotificacionLocal(mensaje: String) {
        val canalId = "destacar_canal"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(canalId, "Notificaciones de Comentarios", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(canal)
        }
        val notificacion = NotificationCompat.Builder(this, canalId)
            .setSmallIcon(R.drawable.uc_user) // Usa un ícono válido
            .setContentTitle("Comentario Destacado")
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        manager.notify(System.currentTimeMillis().toInt(), notificacion)
    }
    private fun obtenerFechaFormateada(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    private fun notificarRespuestaComentario(beachId: String, userIdActual: String, nombreUsuario: String, parentId: String) {
        val db = FirebaseFirestore.getInstance()
        // Buscar el comentario original (padre) al que se está respondiendo
        db.collection("comments").document(parentId)
            .get()
            .addOnSuccessListener { doc ->
                val userIdOriginal = doc.getString("userId")

                // Verificar si el comentario original pertenece a otro usuario
                if (userIdOriginal != null && userIdOriginal != userIdActual) {
                    // Enviar notificación a la persona que fue respondida
                    enviarNotificacionLocal("$nombreUsuario te respondió en un comentario sobre la playa.")
                }
            }
            .addOnFailureListener {
                Log.e("NOTIF", "Error al buscar el comentario original: ${it.message}")
            }
    }

    //*************GET***********
    private fun getUserName(): String {
        val user = FirebaseAuth.getInstance().currentUser
        return user?.displayName ?: "Desconocido"  // Si no está autenticado, usamos "Desconocido"
    }

    private fun getCurrentDate(): String {
        val format = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
        return format.format(Date())
    }
}
