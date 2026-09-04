package com.example.storageservice.api.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.storageservice.service.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

@SpringBootTest
@AutoConfigureMockMvc
class StorageAdminControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean StorageService storageService;

    @Test
    void withoutToken_is403() throws Exception {
        mockMvc.perform(get("/api-admin/view/a.jpg")).andExpect(status().isForbidden());
    }

    @Test
    void withToken_returnsImageBytes() throws Exception {
        when(storageService.view("a.jpg")).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api-admin/view/a.jpg").header("X-Internal-Token", "test-internal-token"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void missingFile_returns404() throws Exception {
        when(storageService.view("missing.jpg"))
                .thenThrow(new RuntimeException("The specified key does not exist."));

        mockMvc.perform(get("/api-admin/view/missing.jpg").header("X-Internal-Token", "test-internal-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void upload_withoutToken_is403() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api-admin/upload").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    void upload_withToken_returnsStoredFilename() throws Exception {
        when(storageService.upload(any(MultipartFile.class))).thenReturn("stored.jpg");
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api-admin/upload").file(file).header("X-Internal-Token", "test-internal-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("stored.jpg"));
    }
}
