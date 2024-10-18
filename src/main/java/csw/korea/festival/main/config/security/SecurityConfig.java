package csw.korea.festival.main.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Enable CORS
                .cors(withDefaults())
                // Disable CSRF for simplicity (enable in production)
                .csrf(AbstractHttpConfigurer::disable)
                // Configure session management
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Configure authorization requests
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/graphql").permitAll()
                        .anyRequest().permitAll()
                )
                // Configure OAuth2 Login
                .oauth2Login(withDefaults());

        return http.build();
    }
}
