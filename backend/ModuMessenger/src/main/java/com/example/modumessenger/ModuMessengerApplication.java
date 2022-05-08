package com.example.modumessenger;

import com.example.modumessenger.common.listener.AppStartedListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ModuMessengerApplication {

    public static void main(String[] args) {

        SpringApplication application = new SpringApplication(ModuMessengerApplication.class);
        application.addListeners(new AppStartedListener());
        application.run(args);
    }

}
