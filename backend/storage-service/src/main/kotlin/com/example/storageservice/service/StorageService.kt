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

    fun upload(file: MultipartFile): String =
        try {
            val fileName = createFileName(requireNotNull(file.originalFilename))
            val inputStream = file.inputStream
            minioClient.putObject(
                PutObjectArgs.builder().bucket(bucket).`object`(fileName).stream(inputStream, inputStream.available().toLong(), -1).build(),
            )
            fileName
        } catch (e: Exception) {
            throw RuntimeException(e.message)
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
        private const val SLASH = "/"
        private const val PARAMETER = "?"
    }
}
