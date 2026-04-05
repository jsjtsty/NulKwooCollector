package com.nulstudio.kwoocollector.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

fun Uri.toTempFile(context: Context): File? {
    return try {
        val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")

        context.contentResolver.openInputStream(this)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}