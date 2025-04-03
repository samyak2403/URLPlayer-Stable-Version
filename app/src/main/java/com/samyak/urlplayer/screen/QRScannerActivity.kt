package com.samyak.urlplayer.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.samyak.urlplayer.R
import com.samyak.urlplayer.databinding.ActivityQrScannerBinding
import com.samyak2403.custom_toast.TastyToast
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator

@androidx.camera.core.ExperimentalGetImage
class QRScannerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQrScannerBinding
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraExecutor: ExecutorService
    private var flashEnabled = false
    private var camera: androidx.camera.core.Camera? = null
    
    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
        private const val REQUEST_STORAGE_PERMISSION = 101
        private const val PICK_IMAGE_REQUEST = 102
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupFlashToggle()
        setupGalleryButton()
        
        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        // Check camera permission
        if (hasCameraPermission()) {
            startCamera()
        } else {
            requestCameraPermission()
        }
        
        setupScannerAnimation()
    }
    
    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.scan_qr_code)
        }
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.white))
    }
    
    private fun setupFlashToggle() {
        binding.flashToggleButton.setOnClickListener {
            toggleFlash()
        }
    }
    
    private fun toggleFlash() {
        camera?.let { cam ->
            if (cam.cameraInfo.hasFlashUnit()) {
                flashEnabled = !flashEnabled
                cam.cameraControl.enableTorch(flashEnabled)
                updateFlashIcon()
            } else {
                TastyToast.show(this, "Flash not available on this device", TastyToast.Type.WARNING)
            }
        }
    }
    
    private fun updateFlashIcon() {
        binding.flashToggleButton.setImageResource(
            if (flashEnabled) R.drawable.ic_flash_on else R.drawable.ic_flash_off
        )
    }
    
    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
    
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION
        )
    }
    
    private fun setupGalleryButton() {
        binding.galleryButton.setOnClickListener {
            if (hasStoragePermission()) {
                openGallery()
            } else {
                showStoragePermissionDialog()
            }
        }
    }
    
    private fun hasStoragePermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun requestStoragePermission() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        ActivityCompat.requestPermissions(
            this, arrayOf(permission), REQUEST_STORAGE_PERMISSION
        )
    }
    
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }
    
    private fun showStoragePermissionDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Storage Permission Required")
            .setMessage("To upload QR codes from your gallery, the app needs permission to access your storage. Would you like to grant this permission?")
            .setPositiveButton("Yes") { _, _ ->
                requestStoragePermission()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
                TastyToast.show(this, "Gallery access requires storage permission", TastyToast.Type.INFO)
            }
            .setCancelable(true)
            .show()
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCamera()
                } else {
                    TastyToast.show(this, "Camera permission is required to scan QR codes", TastyToast.Type.WARNING)
                    finish()
                }
            }
            REQUEST_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, 
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            Manifest.permission.READ_MEDIA_IMAGES
                        } else {
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        }
                    )) {
                        // User checked "Don't ask again"
                        showPermissionSettingsDialog()
                    } else {
                        TastyToast.show(this, "Storage permission is required to upload QR codes", TastyToast.Type.WARNING)
                    }
                }
            }
        }
    }
    
    private fun showPermissionSettingsDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("Storage permission is required to upload QR codes from gallery. Please enable it in app settings.")
            .setPositiveButton("Settings") { _, _ ->
                // Open app settings
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = android.net.Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            val imageUri = data.data
            if (imageUri != null) {
                processImageFromGallery(imageUri)
            }
        }
    }
    
    private fun processImageFromGallery(imageUri: Uri) {
        try {
            val image = InputImage.fromFilePath(this, imageUri)
            val scanner = BarcodeScanning.getClient()
            
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isEmpty()) {
                        TastyToast.show(this, "No QR code found in the image", TastyToast.Type.WARNING)
                        return@addOnSuccessListener
                    }
                    
                    for (barcode in barcodes) {
                        if (barcode.valueType == Barcode.TYPE_TEXT) {
                            barcode.rawValue?.let { qrContent ->
                                processQRContent(qrContent)
                                return@addOnSuccessListener
                            }
                        }
                    }
                    
                    TastyToast.show(this, "No valid QR code found in the image", TastyToast.Type.WARNING)
                }
                .addOnFailureListener { e ->
                    TastyToast.show(this, "Failed to process image: ${e.message}", TastyToast.Type.ERROR)
                }
        } catch (e: Exception) {
            TastyToast.show(this, "Failed to process image: ${e.message}", TastyToast.Type.ERROR)
        }
    }
    
    @androidx.camera.core.ExperimentalGetImage
    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            // Setup the preview use case
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            
            // Setup the image analyzer
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, QRCodeAnalyzer { qrContent ->
                        processQRContent(qrContent)
                    })
                }
            
            // Select back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                // Unbind any bound use cases before rebinding
                cameraProvider.unbindAll()
                
                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis
                )
                
                // Check if flash is available
                camera?.let { cam ->
                    if (!cam.cameraInfo.hasFlashUnit()) {
                        binding.flashToggleButton.visibility = View.GONE
                    }
                }
                
            } catch (e: Exception) {
                TastyToast.show(this, "Failed to start camera: ${e.message}", TastyToast.Type.ERROR)
            }
            
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun processQRContent(qrContent: String) {
        // Parse QR content
        try {
            val lines = qrContent.split("\n")
            var channelName = ""
            var channelUrl = ""
            var userAgent: String? = null
            
            for (line in lines) {
                when {
                    line.startsWith("Channel:") -> channelName = line.substringAfter("Channel:").trim()
                    line.startsWith("URL:") -> channelUrl = line.substringAfter("URL:").trim()
                    line.startsWith("User Agent:") -> userAgent = line.substringAfter("User Agent:").trim()
                }
            }
            
            if (channelName.isNotEmpty() && channelUrl.isNotEmpty()) {
                // Save the channel
                saveChannel(channelName, channelUrl, userAgent)
                
                // Show success message and finish
                runOnUiThread {
                    TastyToast.show(this, "Channel added successfully", TastyToast.Type.SUCCESS)
                    setResult(RESULT_OK)
                    finish()
                }
            } else {
                runOnUiThread {
                    TastyToast.show(this, "Invalid QR code format", TastyToast.Type.ERROR)
                }
            }
        } catch (e: Exception) {
            runOnUiThread {
                TastyToast.show(this, "Failed to process QR code: ${e.message}", TastyToast.Type.ERROR)
            }
        }
    }
    
    private fun saveChannel(name: String, url: String, userAgent: String?) {
        try {
            val sharedPreferences = getSharedPreferences("M3U8Links", Context.MODE_PRIVATE)
            val currentLinks = sharedPreferences.getStringSet("links", mutableSetOf()) ?: mutableSetOf()
            
            // Check for duplicate titles
            if (currentLinks.any { it.split("###")[0] == name }) {
                runOnUiThread {
                    TastyToast.show(this, "Channel with this name already exists", TastyToast.Type.WARNING)
                }
                return
            }
            
            // Detect URL type
            val urlType = detectUrlType(url)
            val isLiveStream = urlType == "LIVE" || 
                              urlType == "HLS" || 
                              url.lowercase().contains("live") ||
                              url.lowercase().contains("cricket") ||
                              url.lowercase().contains("match") ||
                              url.lowercase().contains("tv") ||
                              url.lowercase().contains("tata") ||
                              (url.lowercase().contains(".php") && url.lowercase().contains("?"))
            
            val newLinks = currentLinks.toMutableSet()
            
            // Format: title###url###urlType###userAgent###isLiveStream###pin
            val channelData = buildString {
                append("$name###$url###$urlType")
                if (!userAgent.isNullOrEmpty()) {
                    append("###$userAgent")
                } else {
                    append("###")
                }
                append("###$isLiveStream###")  // Empty PIN
            }
            
            newLinks.add(channelData)
            
            // Save to SharedPreferences
            sharedPreferences.edit().apply {
                putStringSet("links", newLinks)
                apply()
            }
        } catch (e: Exception) {
            runOnUiThread {
                TastyToast.show(this, "Failed to save channel: ${e.message}", TastyToast.Type.ERROR)
            }
        }
    }
    
    private fun detectUrlType(url: String): String {
        val lowercaseUrl = url.lowercase()
        return when {
            lowercaseUrl.endsWith(".m3u8") -> "HLS"
            lowercaseUrl.contains(".m3u8?") -> "HLS"
            lowercaseUrl.endsWith(".mp4") -> "MP4"
            lowercaseUrl.endsWith(".avi") -> "AVI"
            lowercaseUrl.endsWith(".mkv") -> "MKV"
            lowercaseUrl.endsWith(".m3u") -> "M3U"
            lowercaseUrl.endsWith(".mpd") -> "DASH"
            lowercaseUrl.endsWith(".ts") -> "TS"
            lowercaseUrl.endsWith(".mov") -> "MOV"
            lowercaseUrl.endsWith(".webm") -> "WEBM"
            lowercaseUrl.startsWith("rtmp://") -> "RTMP"
            lowercaseUrl.startsWith("rtsp://") -> "RTSP"
            lowercaseUrl.startsWith("udp://") -> "UDP"
            lowercaseUrl.startsWith("rtp://") -> "RTP"
            lowercaseUrl.startsWith("mms://") -> "MMS"
            lowercaseUrl.startsWith("srt://") -> "SRT"
            lowercaseUrl.contains(".php") && lowercaseUrl.contains("?") -> "HLS"
            lowercaseUrl.contains("live") || 
            lowercaseUrl.contains("stream") || 
            lowercaseUrl.contains("cricket") || 
            lowercaseUrl.contains("match") ||
            lowercaseUrl.contains("tv") ||
            lowercaseUrl.contains("tata") -> "LIVE"
            else -> "HTTP"
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
    
    // QR Code analyzer class
    @androidx.camera.core.ExperimentalGetImage
    private inner class QRCodeAnalyzer(private val onQRCodeDetected: (String) -> Unit) : ImageAnalysis.Analyzer {
        private val scanner = BarcodeScanning.getClient()
        
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )
                
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            if (barcode.valueType == Barcode.TYPE_TEXT) {
                                barcode.rawValue?.let { qrContent ->
                                    onQRCodeDetected(qrContent)
                                }
                            }
                        }
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }
    
    private fun setupScannerAnimation() {
        val scannerLine = binding.scannerLine
        val animator = ObjectAnimator.ofFloat(
            scannerLine,
            "translationY",
            0f,
            binding.qrCodeOverlay.height.toFloat()
        )
        animator.duration = 2000
        animator.repeatMode = ValueAnimator.REVERSE
        animator.repeatCount = ValueAnimator.INFINITE
        animator.interpolator = LinearInterpolator()
        animator.start()
    }
} 