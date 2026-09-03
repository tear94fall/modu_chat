package com.example.pushservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {
    private final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${project.properties.firebase-sdk-path}")
    private String firebaseSdkPath;

    @PostConstruct
    public void initialize() {
        try {
            // FirebaseApp 의 기본("[DEFAULT]") 앱은 JVM 전역 싱글턴이다. 같은 JVM 에서
            // 서로 다르게 구성된 Spring 컨텍스트가 두 번째로 뜨면(@AutoConfigureMockMvc 가
            // 붙은 테스트 컨텍스트 등) 다시 initializeApp 을 부르게 되어
            // "FirebaseApp name [DEFAULT] already exists!" 로 기동이 실패한다. 이미 있으면 건너뛴다.
            if (!FirebaseApp.getApps().isEmpty()) {
                return;
            }

            ClassPathResource resource = new ClassPathResource(firebaseSdkPath);
            InputStream serviceAccount = resource.getInputStream();

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
        } catch (FileNotFoundException e) {
            logger.error("Firebase ServiceAccountKey FileNotFoundException" + e.getMessage());
        } catch (IOException e) {
            logger.error("FirebaseOptions IOException" + e.getMessage());
        }
    }
}
