package pandq.infrastructure.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class ApplicationConfiguration {
    @Value("${spring.application.url}")
    private String baseUrl;

    @Bean
    public String frontendUrl() {
        return baseUrl;
    }

    @Bean
    public String backendUrl() {
        return baseUrl + "/api";
    }
}
