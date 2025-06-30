package qr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.pro.BeachDetailActivity
import com.example.pro.R
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.google.zxing.ResultPoint
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.ktx.Firebase


class QrScanActivity : AppCompatActivity() {

    private lateinit var barcodeView: DecoratedBarcodeView
    private var scanned = false
    private val CAMERA_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scan)

        barcodeView = findViewById(R.id.barcodeScannerView)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        } else {
            startCamera()
        }
    }

    private fun startCamera() {
        barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                if (!scanned && result != null) {
                    scanned = true

                    val scannedText = result.text.trim()

                    Firebase.dynamicLinks.getDynamicLink(Uri.parse(scannedText))
                        .addOnSuccessListener { pendingDynamicLinkData ->
                            val deepLink: Uri? = pendingDynamicLinkData?.link

                            if (deepLink != null) {
                                val beachId = deepLink.getQueryParameter("beachId")

                                if (!beachId.isNullOrEmpty()) {
                                    val intent = Intent(this@QrScanActivity, BeachDetailActivity::class.java)
                                    intent.putExtra("id", beachId)
                                    startActivity(intent)
                                } else {
                                    Toast.makeText(this@QrScanActivity, "QR inválido o sin beachId", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(this@QrScanActivity, "QR inválido o sin link", Toast.LENGTH_LONG).show()
                            }

                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this@QrScanActivity, "Error al resolver QR", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                }
            }

            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}
        })

        barcodeView.resume()
    }


    override fun onResume() {
        super.onResume()
        if (::barcodeView.isInitialized) {
            barcodeView.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::barcodeView.isInitialized) {
            barcodeView.pause()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(this, "Se necesita permiso de cámara para escanear", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}
