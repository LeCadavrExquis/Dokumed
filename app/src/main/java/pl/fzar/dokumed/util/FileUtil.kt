package pl.fzar.dokumed.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object FileUtil {
     fun getFileName(context: Context, uri: Uri): String {
         var name = "unknown_file"
         context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
             if (cursor.moveToFirst()) {
                 val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                 if (nameIndex != -1) {
                     name = cursor.getString(nameIndex)
                 }
             }
         }
         // Fallback if display name is not available
         if (name == "unknown_file") {
             name = uri.lastPathSegment ?: name
         }
         return name
     }

     fun copyFileToInternalStorage(context: Context, sourceUri: Uri, desiredName: String): String? {
         return try {
             val inputStream = context.contentResolver.openInputStream(sourceUri)
             // Use app-specific directory in internal storage
             val destinationDir = File(context.filesDir, "attachments")
             if (!destinationDir.exists()) {
                 destinationDir.mkdirs()
             }
             val destinationFile = File(destinationDir, desiredName)
             val outputStream = FileOutputStream(destinationFile)
             inputStream?.use { input ->
                 outputStream.use { output ->
                     input.copyTo(output)
                 }
             }
             destinationFile.absolutePath // Return the absolute path of the copied file
         } catch (e: Exception) {
             Log.e("FileCopier", "Error copying file: ${e.message}", e)
             null
         }
     }
 }
