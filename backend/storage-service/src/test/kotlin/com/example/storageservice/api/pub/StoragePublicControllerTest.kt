package com.example.storageservice.api.pub

import com.example.storageservice.service.FileInfo
import com.example.storageservice.service.StorageService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.InputStreamResource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class StoragePublicControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @MockitoBean lateinit var storageService: StorageService

    private val stored = "43e3c8f381c4def571df53a9da5463c402e94b5a82b2bdc74a4805e87d269bb3.xml"

    @Test
    fun fileInfo_returnsOriginalNameSizeAndType() {
        whenever(storageService.fileInfo(stored)).thenReturn(FileInfo(stored, "회의록.xml", 1234L, "application/xml"))

        mockMvc.perform(get("/api-public/file-info").param("file", stored))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value(stored))
            .andExpect(jsonPath("$.originalName").value("회의록.xml"))
            .andExpect(jsonPath("$.size").value(1234))
            .andExpect(jsonPath("$.contentType").value("application/xml"))
    }

    @Test
    fun download_usesOriginalNameAndContentType() {
        whenever(storageService.fileInfo(stored)).thenReturn(FileInfo(stored, "회의록 v2.xml", 5L, "application/xml"))
        whenever(storageService.get(stored)).thenReturn(InputStreamResource("<a/>\n".byteInputStream()))

        mockMvc.perform(get("/api-public/download").param("file", stored))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "application/xml"))
            .andExpect(header().string("Content-Length", "5"))
            .andExpect(
                header().string(
                    "Content-Disposition",
                    "attachment; filename=\"___ v2.xml\"; filename*=UTF-8''%ED%9A%8C%EC%9D%98%EB%A1%9D%20v2.xml",
                ),
            )
            .andExpect(content().string("<a/>\n"))
    }

    @Test
    fun contentDisposition_keepsAsciiNamesAsIs() {
        assertEquals(
            "attachment; filename=\"report.pdf\"; filename*=UTF-8''report.pdf",
            StoragePublicController.contentDisposition("report.pdf"),
        )
    }
}
