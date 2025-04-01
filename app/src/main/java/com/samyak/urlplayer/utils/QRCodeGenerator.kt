package com.samyak.urlplayer.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.samyak.urlplayer.models.Videos

object QRCodeGenerator {
    
    @Throws(WriterException::class)
    fun generateQRCode(video: Videos, width: Int = 300, height: Int = 300): Bitmap {
        // Create QR code content with channel data
        val qrContent = buildString {
            append("Channel: ${video.name}\n")
            append("URL: ${video.url}\n")
            if (!video.userAgent.isNullOrEmpty()) {
                append("User Agent: ${video.userAgent}")
            }
        }

        // Generate QR code bitmap
        val bitMatrix = MultiFormatWriter().encode(
            qrContent,
            BarcodeFormat.QR_CODE,
            width,
            height
        )
        
        return createBitmap(bitMatrix)
    }
    
    private fun createBitmap(bitMatrix: BitMatrix): Bitmap {
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        // Convert bit matrix to bitmap
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        
        return bitmap
    }
} 