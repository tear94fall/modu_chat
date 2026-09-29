package modu.chat.schedule_service.api.debug;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ScheduleDebugControllerProfileTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(ScheduleDebugController.class);

    @Test
    void notCreatedInProdProfile() {
        runner.withPropertyValues("spring.profiles.active=prod")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(ScheduleDebugController.class));
    }

    @Test
    void createdInDevProfile() {
        runner.withPropertyValues("spring.profiles.active=dev")
                .withBean(modu.chat.schedule_service.application.usecase.ScheduleUseCase.class, () -> org.mockito.Mockito.mock(modu.chat.schedule_service.application.usecase.ScheduleUseCase.class))
                .run(ctx -> assertThat(ctx).hasSingleBean(ScheduleDebugController.class));
    }
}
