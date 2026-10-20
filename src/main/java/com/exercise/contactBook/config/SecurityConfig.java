package com.exercise.contactBook.config;

import com.exercise.contactBook.filter.Jwtfilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private Jwtfilter jwtfilter;
    private UserDetailsService userDetailsService;
    private OauthSuccessHandler oauthSuccessHandler;

    public SecurityConfig(Jwtfilter jwtfilter, UserDetailsService userDetailsService, OauthSuccessHandler oauthSuccessHandler){
        this.jwtfilter = jwtfilter;
        this.userDetailsService= userDetailsService;
        this.oauthSuccessHandler=oauthSuccessHandler;
    }

    @Bean
    public PasswordEncoder getPasswordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(){
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(getPasswordEncoder());
        return daoAuthenticationProvider;

    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception{
        return config.getAuthenticationManager();

    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http){
        http.cors(cors -> cors.configurationSource(corsConfigurationSource() ))
                .csrf(AbstractHttpConfigurer::disable);
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) ;
        http.authorizeHttpRequests(         auth->
                        auth.requestMatchers(HttpMethod.OPTIONS,"/**").permitAll()
                                .requestMatchers("/").permitAll()
                                .requestMatchers("/health-check").permitAll()
                                .requestMatchers("/swagger-ui", "/v3/api-docs/**", "/swagger-resources/**").permitAll()
                                .requestMatchers("/api/users/**").authenticated()
                                .requestMatchers("/api/contacts/**").authenticated()
                                .requestMatchers("/api/auth/user/**").permitAll()
                                .requestMatchers("/api/auth/organizations/request", "/api/auth/organizations/verify-otp" , "/api/auth/organizations/resend-otp").permitAll()
                                .anyRequest().authenticated()
        ).exceptionHandling(ex ->
                        ex.authenticationEntryPoint(((request, response, authException) ->
                        {
                            if(request.getRequestURI().startsWith("/api")){
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                         response.setContentType("application/json");
                          response.getWriter().write("""
                                  {
                                  "error" : "unauthorize"
                                  }""");}else{
                                response.sendRedirect("/oauth2/authorization/google");
                            }
                        })))
                .oauth2Login(oauth2 -> oauth2.successHandler(oauthSuccessHandler))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtfilter, UsernamePasswordAuthenticationFilter.class);
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
