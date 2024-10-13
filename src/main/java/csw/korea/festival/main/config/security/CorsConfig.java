package csw.korea.festival.main.config.security;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NotNull CorsRegistry registry) {
                registry.addMapping("/graphql")
                        .allowedOrigins("http://localhost:3001") // Frontend origin
                        .allowedMethods("GET", "POST", "OPTIONS") // HTTP methods
                        .allowedHeaders("*") // Allow all headers
                        .allowCredentials(true) // Allow credentials (cookies, authorization headers, etc.)
                        .maxAge(3600); // Max age for preflight requests in seconds
            }
        };
    }
}