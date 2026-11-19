package com.example.helpdeskai.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Utility helper to handle camera image file storage and retrieval.
 */
object CameraHelper {

    private const val PHOTO_DIR = "complaint_photos"

    /**
     * Saves a captured Bitmap to app-internal storage and returns the absolute file path.
     */
    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String? {
        val dir = File(context.filesDir, PHOTO_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val fileName = "complaint_${System.currentTimeMillis()}.jpg"
        val file = File(dir, fileName)

        return try {
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            fos.flush()
            fos.close()
            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Loads a Bitmap from the given file path.
     */
    fun loadBitmapFromPath(path: String?): Bitmap? {
        if (path.isNullOrEmpty()) return null
        val file = File(path)
        return if (file.exists()) {
            BitmapFactory.decodeFile(file.absolutePath)
        } else {
            null
        }
    }
}
