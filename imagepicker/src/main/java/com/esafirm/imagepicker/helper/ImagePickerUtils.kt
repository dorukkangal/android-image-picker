package com.esafirm.imagepicker.helper

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.webkit.MimeTypeMap
import com.esafirm.imagepicker.features.ImagePickerSavePath
import com.esafirm.imagepicker.model.Image
import java.io.File
import java.net.URLConnection
import java.text.SimpleDateFormat
import java.util.*

object ImagePickerUtils {
    private fun createFileInDirectory(savePath: ImagePickerSavePath, context: Context): File? {
        // External sdcard location
        val path = savePath.path
        val mediaStorageDir = if (savePath.isFullPath) {
            File(path)
        } else {
            val parent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.getExternalFilesDir(
                    Environment.DIRECTORY_PICTURES
                )
            } else Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            )
            File(parent, path)
        }

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                IpLogger.d("Oops! Failed create $path")
                return null
            }
        }
        return mediaStorageDir
    }

    fun createImageFile(context: Context, savePath: ImagePickerSavePath): File? {
        val mediaStorageDir = createFileInDirectory(savePath, context) ?: return null

        // Create a media file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
        var result = File(mediaStorageDir, "IMG_$timeStamp.jpg")
        var counter = 0
        while (result.exists()) {
            counter++
            result = File(mediaStorageDir, "IMG_$timeStamp($counter).jpg")
        }
        return result
    }

    fun getNameFromFilePath(path: String): String {
        return if (path.contains(File.separator)) {
            path.substring(path.lastIndexOf(File.separator) + 1)
        } else {
            path
        }
    }

    fun grantAppPermission(context: Context, intent: Intent?, fileUri: Uri?) {
        val resolvedIntentActivities = context.packageManager
            .queryIntentActivities(intent!!, PackageManager.MATCH_DEFAULT_ONLY)
        for (resolvedIntentInfo in resolvedIntentActivities) {
            val packageName = resolvedIntentInfo.activityInfo.packageName
            context.grantUriPermission(
                packageName, fileUri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    fun revokeAppPermission(context: Context, fileUri: Uri?) {
        context.revokeUriPermission(
            fileUri,
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }

    fun isGifFormat(image: Image): Boolean {
        return isGifFormat(image.path)
    }

    fun isGifFormat(path: String): Boolean {
        val extension = getExtension(path)
        return extension.equals("gif", ignoreCase = true)
    }

    fun isVideoFormat(image: Image): Boolean {
        val extension = getExtension(image.path)
        val mimeType = if (extension.isEmpty()) {
            URLConnection.guessContentTypeFromName(image.path)
        } else {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
        return mimeType != null && mimeType.startsWith("video")
    }

    fun getVideoDurationLabel(context: Context?, uri: Uri?): String {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, uri)
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!
            .toLong()
        retriever.release()
        val second = duration / 1000 % 60
        val minute = duration / (1000 * 60) % 60
        val hour = duration / (1000 * 60 * 60) % 24
        return if (hour > 0) {
            String.format("%02d:%02d:%02d", hour, minute, second)
        } else {
            String.format("%02d:%02d", minute, second)
        }
    }

    private fun getExtension(path: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(path)
        if (extension.isNotEmpty()) {
            return extension
        }
        return if (path.contains(".")) {
            path.substring(path.lastIndexOf(".") + 1, path.length)
        } else {
            ""
        }
    }
}
