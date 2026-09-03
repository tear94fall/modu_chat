package com.example.pushservice.api.debug;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class PushDebugControllerProfileTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PushDebugController.class);

    @Test
    void notCreatedInProdProfile() {
        runner.withPropertyValues("spring.profiles.active=prod")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(PushDebugController.class));
    }

    @Test
    void createdInDevProfile() {
        runner.withPropertyValues("spring.profiles.active=dev", "project.properties.firebase-multicast-message-size=500")
                .withBean(com.example.pushservice.fcm.service.FcmService.class, () -> org.mockito.Mockito.mock(com.example.pushservice.fcm.service.FcmService.class))
                .run(ctx -> assertThat(ctx).hasSingleBean(PushDebugController.class));
    }
}
