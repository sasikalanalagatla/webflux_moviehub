package com.moviehub.review.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class ReactiveSecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieServerCsrfTokenRepository.withHttpOnlyFalse())
                )
                .cors(cors -> {})
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com; style-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com; img-src 'self' data:; font-src 'self' https://cdnjs.cloudflare.com; frame-ancestors 'self'; object-src 'none'; base-uri 'self'"))
                        .referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.NO_REFERRER))
                        .frameOptions(frame -> frame.mode(org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter.Mode.SAMEORIGIN))
                        .xssProtection(xss -> xss.block(true))
                        .hsts(hsts -> hsts.includeSubDomains(true).maxAge(Duration.ofDays(180)))
                )
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/", "/movie/", "/movie/all", "/movie/{id}").permitAll()
                        .pathMatchers("/css/**", "/js/**", "/images/**").permitAll()
                        .pathMatchers("/auth/login", "/auth/register").permitAll()
                        .pathMatchers("/api/public/**").permitAll()
                        .pathMatchers("/movie/add", "/movie/edit/**", "/movie/delete/**").hasRole("ADMIN")
                        .anyExchange().authenticated()
                )
                .formLogin(formLogin -> formLogin
                        .loginPage("/auth/login")
                        .authenticationFailureHandler((exchange, exception) -> {
                            exchange.getExchange().getResponse().setStatusCode(
                                    org.springframework.http.HttpStatus.FOUND);
                            exchange.getExchange().getResponse().getHeaders()
                                    .add("Location", "/auth/login?error");
                            return reactor.core.publisher.Mono.empty();
                        })
                        .authenticationSuccessHandler((exchange, authentication) -> {
                            exchange.getExchange().getResponse().setStatusCode(
                                    org.springframework.http.HttpStatus.FOUND);
                            exchange.getExchange().getResponse().getHeaders()
                                    .add("Location", "/movie/");
                            return reactor.core.publisher.Mono.empty();
                        })
                )
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessHandler((exchange, authentication) -> {
                            exchange.getExchange().getResponse().setStatusCode(
                                    org.springframework.http.HttpStatus.FOUND);
                            exchange.getExchange().getResponse().getHeaders()
                                    .add("Location", "/movie/");
                            return reactor.core.publisher.Mono.empty();
                        })
                )
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:8080"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type", "X-CSRF-TOKEN"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
