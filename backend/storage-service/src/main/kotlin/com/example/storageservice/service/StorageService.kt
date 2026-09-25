package com.example.storageservice.service

import com.example.storageservice.util.Sha256
import io.minio.BucketExistsArgs
import io.minio.GetObjectArgs
import io.minio.ListBucketsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import io.minio.StatObjectArgs
import io.minio.StatObjectResponse
import io.minio.errors.ErrorResponseException
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.InputStreamResource
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.springframework.http.MediaType
import java.nio.file.Path
import java.time.LocalDateTime

@Service
class StorageService(
    private val minioClient: MinioClient,
    @Value("\${minio.bucketImageName}") private val bucket: String,
) {

    private val log = LoggerFactory.getLogger(StorageService::class.java)

    @PostConstruct
    fun init() {
        if (!getBucketNames().contains(bucket)) {
            log.info("Creating bucket {}", bucket)
            createBucket(bucket)
        }
    }

    fun getBucketNames(): List<String> =
        try {
            minioClient.listBuckets(ListBucketsArgs.builder().build()).map { it.name() }
        } catch (e: Exception) {
            log.error(e.message)
            throw RuntimeException("Error fetching bucket list", e)
        }

    fun createBucket(bucketName: String) {
        try {
            val found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build())
                log.info("Bucket '{}' created successfully.", bucketName)
            } else {
                log.info("Bucket '{}' already exists.", bucketName)
            }
        } catch (e: Exception) {
            log.error(e.message)
            throw RuntimeException("Error creating bucket: $bucketName", e)
        }
    }

    /**
     * 저장 이름은 해시라 원본 이름이 사라진다. 채팅의 파일·음성 메시지는 본문이 저장 이름뿐이므로,
     * 원본 이름과 종류를 객체 메타데이터에 함께 둔다(값은 ASCII 여야 해서 URL 인코딩).
     * 내려받기와 [fileInfo] 가 이걸 읽어 원본 이름을 돌려준다.
     */
    fun upload(file: MultipartFile): String =
        try {
            val originalName = requireNotNull(file.originalFilename)
            val fileName = createFileName(originalName)
            val inputStream = file.inputStream
            val metadata = mapOf(META_ORIGINAL_NAME to URLEncoder.encode(originalName, StandardCharsets.UTF_8))
            minioClient.putObject(
                PutObjectArgs.builder().bucket(bucket).`object`(fileName)
                    .stream(inputStream, inputStream.available().toLong(), -1)
                    .contentType(file.contentType ?: MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    .userMetadata(metadata)
                    .build(),
            )
            fileName
        } catch (e: Exception) {
            throw RuntimeException(e.message)
        }

    /** 파일 정보. 메타데이터가 없는 옛 파일은 저장 이름이 곧 원본 이름이다. */
    fun fileInfo(name: String): FileInfo {
        val stat = getMetadata(name)
        val encoded = stat.userMetadata()?.entries?.firstOrNull { it.key.equals(META_ORIGINAL_NAME, ignoreCase = true) }?.value
        val originalName = encoded?.let { URLDecoder.decode(it, StandardCharsets.UTF_8) }?.takeIf { it.isNotBlank() } ?: name
        return FileInfo(
            name = name,
            originalName = originalName,
            size = stat.size(),
            contentType = stat.contentType()?.takeIf { it.isNotBlank() } ?: MediaType.APPLICATION_OCTET_STREAM_VALUE,
        )
    }

    fun upload(filePath: String): String =
        try {
            val fileName = createFileName(filePath)
            val inputStream = FileInputStream(filePath)
            minioClient.putObject(
                PutObjectArgs.builder().bucket(bucket).`object`(fileName).stream(inputStream, inputStream.available().toLong(), -1).build(),
            )
            fileName
        } catch (e: Exception) {
            throw RuntimeException(e.message)
        }

    fun get(name: String): InputStreamResource {
        val path = Path.of(name)
        try {
            val inputStream = minioClient.getObject(GetObjectArgs.builder().bucket(bucket).`object`(path.toString()).build())
            return InputStreamResource(inputStream)
        } catch (e: Exception) {
            throw RuntimeException(e.message)
        }
    }

    fun exist(name: String): Boolean =
        try {
            minioClient.statObject(StatObjectArgs.builder().bucket(bucket).`object`(name).build())
            true
        } catch (e: ErrorResponseException) {
            log.error(e.message)
            false
        } catch (e: Exception) {
            log.error(e.message)
            throw RuntimeException(e.message)
        }

    fun view(name: String): ByteArray =
        try {
            minioClient.getObject(GetObjectArgs.builder().bucket(bucket).`object`(name).build()).readAllBytes()
        } catch (e: Exception) {
            throw RuntimeException(e.message)
        }

    fun delete(name: String) {
        try {
            val path = Path.of(name)
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).`object`(path.toString()).build())
        } catch (e: Exception) {
            throw RuntimeException(e.message)
        }
    }

    fun getMetadata(name: String): StatObjectResponse =
        try {
            val path = Path.of(name)
            minioClient.statObject(StatObjectArgs.builder().bucket(bucket).`object`(path.toString()).build())
        } catch (e: Exception) {
            throw RuntimeException(e.message)
        }

    /** URL 의 파일을 작업 디렉터리에 내려받고 그 경로를 돌려준다. 실패하면 빈 문자열(옛 자바와 같다). */
    fun downloadFromUrl(imgURL: String): String {
        var fileName = imgURL.substring(imgURL.lastIndexOf(SLASH) + 1)
        if (fileName.contains(PARAMETER)) fileName = fileName.substring(0, fileName.indexOf(PARAMETER))

        return try {
            val connection = URL(imgURL).openConnection() as HttpURLConnection
            val file = File(fileName)
            connection.inputStream.use { input -> FileOutputStream(file).use { output -> input.copyTo(output) } }
            file.path
        } catch (e: Exception) {
            log.error("download failed: {}", imgURL, e)
            ""
        }
    }

    fun deleteFile(filePath: String) {
        val file = File(filePath)
        if (file.exists()) file.delete()
    }

    fun createFileName(filename: String): String {
        val name = Path.of(filename).toString()
        val ext = if (name.contains(".")) name.substring(name.lastIndexOf(".")) else ""
        return Sha256.encrypt(name + LocalDateTime.now()) + ext
    }

    companion object {
        /** 업로드 때 원본 파일 이름을 두는 객체 메타데이터 키(x-amz-meta-*). */
        const val META_ORIGINAL_NAME = "original-name"
        private const val SLASH = "/"
        private const val PARAMETER = "?"
    }
}
