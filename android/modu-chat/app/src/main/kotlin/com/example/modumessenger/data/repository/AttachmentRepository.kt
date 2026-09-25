package com.example.modumessenger.data.repository

import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.example.modumessenger.core.di.IoDispatcher
import com.example.modumessenger.core.model.DownloadedFile
import com.example.modumessenger.core.model.FileInfo
import com.example.modumessenger.core.network.safeCall
import com.example.modumessenger.data.api.StorageApi
import com.example.modumessenger.data.dto.toModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** 채팅의 파일·음성 첨부. 정보 조회, 내려받기, 재생용 캐시. */
interface AttachmentRepository {

    /** 원본 이름·크기·종류. 한 번 받은 것은 기억한다. */
    suspend fun fileInfo(name: String): Result<FileInfo>

    /** Downloads 폴더에 원본 이름으로 저장하고, 연 수 있는 Uri 와 이름을 돌려준다. */
    suspend fun saveToDownloads(name: String): Result<DownloadedFile>

    /** Downloads 에 이미 같은 이름·크기로 받아 둔 파일이 있으면 그것. 없으면 null. */
    suspend fun findDownloaded(info: FileInfo): DownloadedFile?

    /** 다른 앱으로 연다. 열 수 있는 앱이 없으면 false. */
    fun open(file: DownloadedFile, contentType: String): Boolean

    /** 재생을 위해 캐시 디렉터리에 받아 둔다. 이미 있으면 그대로 돌려준다. */
    suspend fun cacheFile(name: String): Result<File>

    /** 음성 길이(ms). 캐시에 받아 둔 파일에서 읽는다. */
    suspend fun audioDuration(name: String): Result<Long>
}

@Singleton
class AttachmentRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storageApi: StorageApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AttachmentRepository {

    private val infoCache = ConcurrentHashMap<String, FileInfo>()
    private val durationCache = ConcurrentHashMap<String, Long>()

    override suspend fun fileInfo(name: String): Result<FileInfo> = withContext(ioDispatcher) {
        infoCache[name]?.let { return@withContext Result.success(it) }
        safeCall { storageApi.fileInfo(name).toModel(name) }
            .onSuccess { infoCache[name] = it }
    }

    override suspend fun saveToDownloads(name: String): Result<DownloadedFile> = withContext(ioDispatcher) {
        safeCall {
            val info = fileInfo(name).getOrElse { FileInfo(name, name, -1L, "application/octet-stream") }
            storageApi.download(name).use { body ->
                body.byteStream().use { input ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        saveViaMediaStore(input, info)
                    } else {
                        saveToPublicDownloads(input, info)
                    }
                }
            }
        }
    }

    override suspend fun findDownloaded(info: FileInfo): DownloadedFile? = withContext(ioDispatcher) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) findInMediaStore(info) else findInPublicDownloads(info)
        }.getOrNull()
    }

    override fun open(file: DownloadedFile, contentType: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(file.uri, contentType.ifBlank { "*/*" })
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    override suspend fun audioDuration(name: String): Result<Long> = withContext(ioDispatcher) {
        durationCache[name]?.let { return@withContext Result.success(it) }
        cacheFile(name).mapCatching { file ->
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.absolutePath)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                    ?: throw IOException("길이를 읽지 못했다: $name")
            } finally {
                retriever.release()
            }
        }.onSuccess { durationCache[name] = it }
    }

    override suspend fun cacheFile(name: String): Result<File> = withContext(ioDispatcher) {
        safeCall {
            val dir = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
            val target = File(dir, name)
            if (target.exists() && target.length() > 0L) return@safeCall target
            val temp = File(dir, "$name.part")
            storageApi.download(name).use { body ->
                body.byteStream().use { input -> temp.outputStream().use { output -> input.copyTo(output) } }
            }
            if (!temp.renameTo(target)) throw IOException("캐시 파일을 만들지 못했다: $name")
            target
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun findInMediaStore(info: FileInfo): DownloadedFile? {
        val projection = arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME, MediaStore.Downloads.SIZE)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ? AND ${MediaStore.Downloads.IS_PENDING} = 0"
        context.contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI, projection, selection, arrayOf(info.originalName), null,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
            while (cursor.moveToNext()) {
                val size = cursor.getLong(sizeCol)
                if (info.sizeBytes >= 0 && size != info.sizeBytes) continue
                val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cursor.getLong(idCol))
                return DownloadedFile(info.originalName, uri)
            }
        }
        return null
    }

    @Suppress("DEPRECATION")
    private fun findInPublicDownloads(info: FileInfo): DownloadedFile? {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), info.originalName)
        if (!file.exists()) return null
        if (info.sizeBytes >= 0 && file.length() != info.sizeBytes) return null
        return DownloadedFile(info.originalName, FileProvider.getUriForFile(context, AUTHORITY, file))
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveViaMediaStore(input: InputStream, info: FileInfo): DownloadedFile {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, info.originalName)
            put(MediaStore.Downloads.MIME_TYPE, info.contentType)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("저장할 자리를 만들지 못했다")
        resolver.openOutputStream(uri).use { output ->
            output ?: throw IOException("저장할 자리를 열지 못했다")
            copy(input, output)
        }
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return DownloadedFile(info.originalName, uri)
    }

    /** API 28. 공용 Downloads 폴더에 직접 쓴다(WRITE_EXTERNAL_STORAGE 가 있어야 한다). */
    @Suppress("DEPRECATION")
    private fun saveToPublicDownloads(input: InputStream, info: FileInfo): DownloadedFile {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).apply { mkdirs() }
        var target = File(dir, info.originalName)
        var suffix = 1
        while (target.exists()) {
            val base = info.originalName.substringBeforeLast('.')
            val ext = info.originalName.substringAfterLast('.', "")
            target = File(dir, if (ext.isEmpty()) "$base ($suffix)" else "$base ($suffix).$ext")
            suffix++
        }
        target.outputStream().use { output -> copy(input, output) }
        return DownloadedFile(target.name, FileProvider.getUriForFile(context, AUTHORITY, target))
    }

    private fun copy(input: InputStream, output: OutputStream) {
        input.copyTo(output)
        output.flush()
    }

    private companion object {
        const val CACHE_DIR = "attachments"
        const val AUTHORITY = "com.example.modumessenger"
    }
}
