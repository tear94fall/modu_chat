package com.example.storageservice.api.admin

import com.example.storageservice.service.StorageService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.multipart.MultipartFile

@SpringBootTest
@AutoConfigureMockMvc
class StorageAdminControllerTest {

    @Autowired lateinit var mockMvc: MockMvc

    @MockitoBean lateinit var storageService: StorageService

    @Test
    fun withoutToken_is403() {
        mockMvc.perform(get("/api-admin/view/a.jpg")).andExpect(status().isForbidden)
    }

    @Test
    fun withToken_returnsImageBytes() {
        whenever(storageService.view("a.jpg")).thenReturn(byteArrayOf(1, 2, 3))

        mockMvc.perform(get("/api-admin/view/a.jpg").header("X-Internal-Token", "test-internal-token"))
            .andExpect(status().isOk)
            .andExpect(content().bytes(byteArrayOf(1, 2, 3)))
    }

    @Test
    fun missingFile_returns404() {
        whenever(storageService.view("missing.jpg")).thenThrow(RuntimeException("The specified key does not exist."))

        mockMvc.perform(get("/api-admin/view/missing.jpg").header("X-Internal-Token", "test-internal-token"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun upload_withoutToken_is403() {
        val file = MockMultipartFile("file", "a.jpg", "image/jpeg", byteArrayOf(1, 2, 3))
        mockMvc.perform(multipart("/api-admin/upload").file(file)).andExpect(status().isForbidden)
    }

    @Test
    fun upload_withToken_returnsStoredFilename() {
        whenever(storageService.upload(any<MultipartFile>())).thenReturn("stored.jpg")
        val file = MockMultipartFile("file", "a.jpg", "image/jpeg", byteArrayOf(1, 2, 3))

        mockMvc.perform(multipart("/api-admin/upload").file(file).header("X-Internal-Token", "test-internal-token"))
            .andExpect(status().isOk)
            .andExpect(content().string("stored.jpg"))
    }
}
