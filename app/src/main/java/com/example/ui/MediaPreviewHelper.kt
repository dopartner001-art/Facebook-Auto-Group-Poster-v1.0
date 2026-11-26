package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaGetContentContract : ActivityResultContracts.GetContent() {
    override fun createIntent(context: Context, input: String): Intent {
        return super.createIntent(context, input).apply {
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}

data class SelectedMediaInfo(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val isVideo: Boolean
)

object MediaPreviewHelper {

    fun resolveMediaInfo(context: Context, uri: Uri): SelectedMediaInfo {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri).orEmpty().ifBlank { "image/*" }
        val isVideo = mimeType.startsWith("video/", ignoreCase = true)
        var displayName = if (isVideo) "Selected Video" else "Selected Photo"

        try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val name = cursor.getString(idx)
                        if (!name.isNullOrBlank()) {
                            displayName = name
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }

        return SelectedMediaInfo(
            uri = uri,
            displayName = displayName,
            mimeType = mimeType,
            isVideo = isVideo
        )
    }

    suspend fun extractVideoThumbnail(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }
}
