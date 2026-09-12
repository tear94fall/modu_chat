package com.example.modumessenger.feature.profile

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.modumessenger.core.di.IoDispatcher
import com.example.modumessenger.core.network.ApiConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 프로필 사진 내려받기(부록 A §15).
 *
 * 이미지는 인증이 필요하므로 Hilt 가 주는 Coil [ImageLoader] 로 받아 `MediaStore.Downloads` 에 넣는다.
 * `Downloads` 컬렉션은 API 29 부터라 그 아래에서는 저장하지 않는다(쓰기 권한을 요구하지 않기 위해서).
 */
@Singleton
class ProfileImageDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun download(fileName: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                throw IOException("MediaStore.Downloads 는 API 29 부터다")
            }
            val request = ImageRequest.Builder(context)
                .data(ApiConfig.imageUrl(fileName))
                .allowHardware(false)
                .build()
            val result = imageLoader.execute(request)
            val drawable = (result as? SuccessResult)?.drawable
                ?: throw IOException("사진을 받지 못했다: $fileName")
            saveToDownloads(drawable.toBitmap(), fileName)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToDownloads(bitmap: Bitmap, fileName: String) {
        val base = fileName.substringAfterLast('/').ifBlank { "modu_${System.currentTimeMillis()}" }
        val displayName = if (base.contains('.')) base else "$base.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, MIME_JPEG)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("저장할 자리를 만들지 못했다")

        resolver.openOutputStream(uri).use { output ->
            output ?: throw IOException("저장할 자리를 열지 못했다")
            bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, output)
        }

        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }

    private companion object {
        const val MIME_JPEG = "image/jpeg"
        const val QUALITY = 95
    }
}
