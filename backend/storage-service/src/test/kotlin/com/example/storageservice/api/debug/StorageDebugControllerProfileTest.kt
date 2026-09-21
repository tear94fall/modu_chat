package com.example.storageservice.api.debug

import com.example.storageservice.service.StorageService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class StorageDebugControllerProfileTest {

    private val runner = ApplicationContextRunner().withUserConfiguration(StorageDebugController::class.java)

    @Test
    fun notCreatedInProdProfile() {
        runner.withPropertyValues("spring.profiles.active=prod")
            .run { ctx -> assertThat(ctx).doesNotHaveBean(StorageDebugController::class.java) }
    }

    @Test
    fun createdInDevProfile() {
        runner.withPropertyValues("spring.profiles.active=dev")
            .withBean(StorageService::class.java, { Mockito.mock(StorageService::class.java) })
            .run { ctx -> assertThat(ctx).hasSingleBean(StorageDebugController::class.java) }
    }
}
