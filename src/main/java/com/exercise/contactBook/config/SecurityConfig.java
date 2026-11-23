package com.exercise.contactBook.config;

import com.exercise.contactBook.security.AuthErrorEntryPoint;
import com.exercise.contactBook.security.KeycloakJwtConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private final KeycloakJwtConverter keycloakJwtConverter;
    private final AuthErrorEntryPoint authErrorEntryPoint;

    public SecurityConfig(KeycloakJwtConverter keycloakJwtConverter, AuthErrorEntryPoint authErrorEntryPoint) {
        this.keycloakJwtConverter = keycloakJwtConverter;
        this.authErrorEntryPoint = authErrorEntryPoint;
    }

    /** Still needed: OtpService hashes the organization-request OTP with it. Not used for logins any more. */
    @Bean
    public PasswordEncoder getPasswordEncoder(){
        return new BCryptPasswordEncoder();
    }

    /**
     * Verifies Keycloak's signature, expiry and issuer. The signing keys are fetched from Keycloak
     * on the first request (not at startup), so the app can boot before Keycloak is up.
     */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${keycloak.server-url}") String serverUrl,
                                 @Value("${keycloak.internal-url}") String internalUrl,
                                 @Value("${keycloak.realm}") String realm) {
        String issuer = serverUrl + "/realms/" + realm;
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(internalUrl + "/realms/" + realm + "/protocol/openid-connect/certs")
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable);
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.authorizeHttpRequests(auth ->
                        auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                .requestMatchers("/").permitAll()
                                .requestMatchers("/favicon.ico", "/error").permitAll()
                                .requestMatchers("/health-check").permitAll()
                                .requestMatchers("/swagger-ui", "/v3/api-docs/**", "/swagger-resources/**").permitAll()
                                // organization registration is public: the applicant has no account yet
                                .requestMatchers("/api/auth/organizations/request", "/api/auth/organizations/verify-otp", "/api/auth/organizations/resend-otp").permitAll()
                                .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authErrorEntryPoint))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(authErrorEntryPoint)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtConverter)));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(){
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET","POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}