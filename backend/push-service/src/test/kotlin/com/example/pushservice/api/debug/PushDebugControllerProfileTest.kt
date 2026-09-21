package com.example.pushservice.api.debug

import com.example.pushservice.fcm.service.FcmService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class PushDebugControllerProfileTest {

    private val runner = ApplicationContextRunner().withUserConfiguration(PushDebugController::class.java)

    @Test
    fun notCreatedInProdProfile() {
        runner.withPropertyValues("spring.profiles.active=prod")
            .run { ctx -> assertThat(ctx).doesNotHaveBean(PushDebugController::class.java) }
    }

    @Test
    fun createdInDevProfile() {
        runner.withPropertyValues("spring.profiles.active=dev", "project.properties.firebase-multicast-message-size=500")
            .withBean(FcmService::class.java, { Mockito.mock(FcmService::class.java) })
            .run { ctx -> assertThat(ctx).hasSingleBean(PushDebugController::class.java) }
    }
}
