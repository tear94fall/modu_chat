package com.example.storageservice.api.debug;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class StorageDebugControllerProfileTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(StorageDebugController.class);

    @Test
    void notCreatedInProdProfile() {
        runner.withPropertyValues("spring.profiles.active=prod")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(StorageDebugController.class));
    }

    @Test
    void createdInDevProfile() {
        runner.withPropertyValues("spring.profiles.active=dev")
                .withBean(com.example.storageservice.service.StorageService.class, () -> org.mockito.Mockito.mock(com.example.storageservice.service.StorageService.class))
                .run(ctx -> assertThat(ctx).hasSingleBean(StorageDebugController.class));
    }
}
