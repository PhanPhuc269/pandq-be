package pandq.infrastructure.configurations;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfiguration {

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        // Warning: This assumes the file exists in src/main/resources/firebase-service-account.json
        // In a real production environment, using GOOGLE_APPLICATION_CREDENTIALS env var is often preferred
        // or GoogleCredentials.getApplicationDefault() if running on GCP.
        ClassPathResource resource = new ClassPathResource("firebase-service-account.json");
        
        try (InputStream serviceAccount = resource.getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                return FirebaseApp.initializeApp(options);
            }
            return FirebaseApp.getInstance();
        }
    }
}
