package com.example.storageservice.service

import com.example.storageservice.util.Sha256
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.InputStreamResource
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectResponse
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.time.LocalDateTime

/**
 * S3 호환 오브젝트 스토리지(dev: Ceph RGW)의 파일 버킷을 다루는 서비스. AWS SDK v2 의 [S3Client] 를 쓴다.
 * 저장 이름은 해시(+원래 확장자)이고, 원본 이름은 객체 메타데이터([META_ORIGINAL_NAME])에 둔다.
 */
@Service
class StorageService(
    private val s3Client: S3Client,
    @Value("\${s3.bucket}") private val bucket: String,
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
            s3Client.listBuckets().buckets().map { it.name() }
        } catch (e: Exception) {
            log.error(e.message)
            throw RuntimeException("Error fetching bucket list", e)
        }

    fun createBucket(bucketName: String) {
        try {
            if (!bucketExists(bucketName)) {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build())
                log.info("Bucket '{}' created successfully.", bucketName)
            } else {
                log.info("Bucket '{}' already exists.", bucketName)
            }
        } catch (e: Exception) {
            log.error(e.message)
            throw RuntimeException("Error creating bucket: $bucketName", e)
        }
    }

    private fun bucketExists(bucketName: String): Boolean =
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build())
            true
        } catch (e: NoSuchBucketException) {
            false
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
            val metadata = mapOf(META_ORIGINAL_NAME to URLEncoder.encode(originalName, StandardCharsets.UTF_8))
            val contentType = file.contentType ?: MediaType.APPLICATION_OCTET_STREAM_VALUE
            // http 엔드포인트(dev 의 rgw:80)에선 SigV4 가 본문 해시를 먼저 계산하고 다시 읽어 보내므로 스트림을 두 번 연다 —
            // fromInputStream 은 mark/reset 이 없어 "already read once" 로 실패한다. 멀티파트는 열 때마다 새 스트림을 주므로 provider 로 넘긴다.
            s3Client.putObject(
                PutObjectRequest.builder().bucket(bucket).key(fileName)
                    .contentType(contentType)
                    .metadata(metadata)
                    .build(),
                RequestBody.fromContentProvider({ file.inputStream }, file.size, contentType),
            )
            fileName
        } catch (e: Exception) {
            throw RuntimeException(e.message)
        }

    /** 파일 정보. 메타데이터가 없는 옛 파일은 저장 이름이 곧 원본 이름이다. */
    fun fileInfo(name: String): FileInfo {
        val stat = getMetadata(name)
        val encoded = stat.metadata()?.entries?.firstOrNull { it.key.equals(META_ORIGINAL_NAME, ignoreCase = true) }?.value
        val originalName = encoded?.let { URLDecoder.decode(it, StandardCharsets.UTF_8) }?.takeIf { it.isNotBlank() } ?: name
        return FileInfo(
            name = name,
            originalName = originalName,
            size = stat.contentLength() ?: 0L,
            contentType = stat.contentType()?.takeIf { it.isNotBlank() } ?: MediaType.APPLICATION_OCTET_STREAM_VALUE,
        )
    }

    fun upload(filePath: String): String =
        try {
            val fileName = createFileName(filePath)
            s3Client.putObject(
                PutObjectRequest.builder().bucket(bucket).key(fileName).build(),
                RequestBody.fromFile(Path.of(filePath)),
            )
            fileName
        } catch (e: Exception) {
            throw RuntimeException(e.message)
        }

    fun get(name: String): InputStreamResource {
        val path = Path.of(name)
        try {
            val inputStream = s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(path.toString()).build())
            return InputStreamResource(inputStream)
        } catch (e: Exception) {
            throw RuntimeException(e.message)
        }
    }

    /** 객체가 있으면 true. 없음(404)·접근 거부 같은 S3 오류 응답은 false, 연결 실패 등은 예외. */
    fun exist(name: String): Boolean =
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(name).build())
            true
        } catch (e: NoSuchKeyException) {
            false
        } catch (e: S3Exception) {
            log.error(e.message)
            false
        } catch (e: Exception) {
            log.error(e.message)
            throw RuntimeException(e.message)
        }

    fun view(name: String): ByteArray =
        try {
            s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(name).build()).asByteArray()
        } catch (e: Exception) {
            throw RuntimeException(e.message)
        }

    fun delete(name: String) {
        try {
            val path = Path.of(name)
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(path.toString()).build())
        } catch (e: Exception) {
            throw RuntimeException(e.message)
        }
    }

    fun getMetadata(name: String): HeadObjectResponse =
        try {
            val path = Path.of(name)
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(path.toString()).build())
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
        /**
         * 업로드 때 원본 파일 이름을 두는 객체 메타데이터 키. SDK 가 `x-amz-meta-original-name` 헤더로 보내고,
         * HeadObject 는 접두사를 뗀 소문자 키로 돌려준다 — MinIO SDK 시절 올린 객체도 같은 키라 그대로 읽힌다.
         */
        const val META_ORIGINAL_NAME = "original-name"
        private const val SLASH = "/"
        private const val PARAMETER = "?"
    }
}
