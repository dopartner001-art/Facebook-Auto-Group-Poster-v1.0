package com.example.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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

    /**
     * On Android 10+ (API 29+), creates a fresh MediaStore entry with current timestamp
     * (requiring zero storage permissions) so the selected photo/video appears first
     * in Facebook's gallery picker.
     */
    suspend fun stageMediaToGalleryTopIfSupported(
        context: Context,
        media: SelectedMediaInfo
    ): Uri = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return@withContext media.uri
        }

        val resolver = context.contentResolver
        val collection = if (media.isVideo) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }

        val relativePath = if (media.isVideo) {
            "${Environment.DIRECTORY_MOVIES}/AutoGroupPoster"
        } else {
            "${Environment.DIRECTORY_PICTURES}/AutoGroupPoster"
        }

        val safeName = "agp_${System.currentTimeMillis()}_${media.displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")}"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, safeName)
            put(MediaStore.MediaColumns.MIME_TYPE, media.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val insertedUri = resolver.insert(collection, values) ?: return@withContext media.uri
        try {
            resolver.openInputStream(media.uri)?.use { input ->
                resolver.openOutputStream(insertedUri)?.use { output ->
                    input.copyTo(output)
                }
            }
            val doneValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            resolver.update(insertedUri, doneValues, null, null)
            insertedUri
        } catch (_: Exception) {
            try {
                resolver.delete(insertedUri, null, null)
            } catch (_: Exception) {
            }
            media.uri
        }
    }
}
