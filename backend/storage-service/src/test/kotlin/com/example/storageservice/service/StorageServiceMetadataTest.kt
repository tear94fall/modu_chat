package com.example.storageservice.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockMultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectResponse
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse

/**
 * 원본 파일 이름의 메타데이터 왕복. 업로드는 `original-name` 키에 URL 인코딩(ASCII)한 값을 두고,
 * [StorageService.fileInfo] 는 HeadObject 의 메타데이터를 풀어 돌려준다. S3 는 목이라 네트워크 없이 돈다.
 */
class StorageServiceMetadataTest {

    private val s3Client: S3Client = mock()
    private val service = StorageService(s3Client, "file-storage")

    @Test
    fun upload_putsUrlEncodedOriginalName_andFileInfoDecodesIt() {
        val originalName = "회의 녹음 (1).m4a"
        val file = MockMultipartFile("file", originalName, "audio/mp4", ByteArray(12) { it.toByte() })
        whenever(s3Client.putObject(any<PutObjectRequest>(), any<RequestBody>())).thenReturn(PutObjectResponse.builder().build())

        val stored = service.upload(file)

        val request = argumentCaptor<PutObjectRequest>()
        verify(s3Client).putObject(request.capture(), any<RequestBody>())
        val put = request.firstValue
        assertThat(put.bucket()).isEqualTo("file-storage")
        assertThat(put.key()).isEqualTo(stored).endsWith(".m4a")
        assertThat(put.contentType()).isEqualTo("audio/mp4")
        val encoded = put.metadata()[StorageService.META_ORIGINAL_NAME]
        assertThat(encoded).isNotNull.matches("[\\x20-\\x7E]+") // 헤더에 실리므로 ASCII 여야 한다
        assertThat(encoded).isNotEqualTo(originalName)

        whenever(s3Client.headObject(any<HeadObjectRequest>())).thenReturn(
            HeadObjectResponse.builder()
                .metadata(mapOf(StorageService.META_ORIGINAL_NAME to encoded))
                .contentLength(12L)
                .contentType("audio/mp4")
                .build(),
        )

        val info = service.fileInfo(stored)
        assertThat(info).isEqualTo(FileInfo(name = stored, originalName = originalName, size = 12L, contentType = "audio/mp4"))
    }

    @Test
    fun fileInfo_withoutMetadata_fallsBackToStoredName() {
        whenever(s3Client.headObject(any<HeadObjectRequest>())).thenReturn(
            HeadObjectResponse.builder().contentLength(3L).contentType("").build(),
        )

        val info = service.fileInfo("abc123.png")

        assertThat(info.originalName).isEqualTo("abc123.png")
        assertThat(info.contentType).isEqualTo("application/octet-stream")
    }

    @Test
    fun fileInfo_readsMetadataKeyCaseInsensitively() {
        whenever(s3Client.headObject(any<HeadObjectRequest>())).thenReturn(
            HeadObjectResponse.builder().metadata(mapOf("Original-Name" to "photo+1.jpg")).contentLength(1L).build(),
        )

        assertThat(service.fileInfo("x.jpg").originalName).isEqualTo("photo 1.jpg")
    }
}
