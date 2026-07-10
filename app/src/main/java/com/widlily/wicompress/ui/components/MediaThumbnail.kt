package com.widlily.wicompress.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun MediaThumbnail(
    modifier: Modifier = Modifier,
    path: String? = null,
    uri: Uri? = null,
    placeholder: String = "🎬"
) {
    val context = LocalContext.current
    var bitmap by remember(path, uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(path, uri) {
        withContext(Dispatchers.IO) {
            bitmap = loadThumbnailCompat(context, path, uri)
        }
    }

    Box(
        modifier = modifier.background(Color(0xFF1E1F29)),
        contentAlignment = Alignment.Center
    ) {
        val currentBitmap = bitmap
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = "Media Thumbnail",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = placeholder,
                fontSize = 18.sp
            )
        }
    }
}

/**
 * Robust utility method that returns a video thumbnail, supporting both Q+ ContentResolver API
 * and older compatibility pathways.
 */
private fun loadThumbnailCompat(context: Context, path: String?, uri: Uri?): Bitmap? {
    try {
        if (uri != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return context.contentResolver.loadThumbnail(uri, Size(128, 128), null)
            } else {
                val id = uri.lastPathSegment?.toLongOrNull()
                if (id != null) {
                    @Suppress("DEPRECATION")
                    return MediaStore.Video.Thumbnails.getThumbnail(
                        context.contentResolver,
                        id,
                        MediaStore.Video.Thumbnails.MINI_KIND,
                        null
                    )
                }
            }
        }
        
        if (!path.isNullOrEmpty()) {
            val file = File(path)
            if (file.exists() && file.length() > 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    return ThumbnailUtils.createVideoThumbnail(file, Size(128, 128), null)
                } else {
                    @Suppress("DEPRECATION")
                    return ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.MINI_KIND)
                }
            }
        }
    } catch (e: Exception) {
        // Suppress and fallback to null
    }
    return null
}
