package com.example.modumessenger.common.listener;

import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class AppStartedListener implements ApplicationListener<ApplicationStartingEvent> {

    @Override
    public void onApplicationEvent(ApplicationStartingEvent event) {
        System.out.println("=======================================================");
        System.out.println("          Modu Messenger Application starting          ");
        System.out.println("=======================================================");
    }
}