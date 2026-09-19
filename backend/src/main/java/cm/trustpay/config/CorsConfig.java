package cm.trustpay.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS : par défaut, un navigateur interdit à index.html de parler à un autre serveur.
 * Ici on l'autorise pour /api/**, uniquement pour les sites listés dans CORS_ORIGINS
 * (séparés par des virgules). Par défaut "*" = tout le monde (pratique en local seulement).
 */
@Configuration
public class CorsConfig {

    @Value("${trustpay.cors:*}")               // lit la valeur définie dans application.properties
    private String origines;

    @Bean
    public WebMvcConfigurer cors() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(origines.split(","))   // "a,b" devient ["a","b"]
                        .allowedMethods("GET", "POST");
            }
        };
    }
}
