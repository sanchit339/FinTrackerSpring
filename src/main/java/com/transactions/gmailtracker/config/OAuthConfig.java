package com.transactions.gmailtracker.config;

import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class OAuthConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
        http
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(
                        a -> a.requestMatchers("/error", "/webjars/**", "/favicon.ico").permitAll()
                                .requestMatchers(PathRequest.toH2Console()).permitAll()
                                .anyRequest().authenticated()
                )
                .oauth2Login(Customizer.withDefaults())
                .logout(l -> l.logoutSuccessUrl("/logout").permitAll())
                .csrf(c -> c
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        // H2 console uses its own forms and is not CSRF-aware
                        .ignoringRequestMatchers(PathRequest.toH2Console())
                )
                // H2 console UI is rendered in frames; default DENY blocks it
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }
}
