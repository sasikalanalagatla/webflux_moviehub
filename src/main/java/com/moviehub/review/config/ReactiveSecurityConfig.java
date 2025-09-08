package com.moviehub.review.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

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
                .authorizeExchange(exchanges -> exchanges
                        // Public access
                        .pathMatchers("/", "/movie/", "/movie/all", "/movie/{id}").permitAll()
                        .pathMatchers("/css/**", "/js/**", "/images/**").permitAll()
                        .pathMatchers("/auth/login", "/auth/register").permitAll()

                        // Admin-only paths
                        .pathMatchers("/movie/add", "/movie/edit/**", "/movie/delete/**").hasRole("ADMIN")

                        // All other requests need authentication
                        .anyExchange().authenticated()
                )
                .formLogin(formLogin -> formLogin
                        .loginPage("/auth/login")
                        .authenticationSuccessHandler((exchange, authentication) -> {
                            // Redirect to home page after successful login
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
                            // Redirect to home page after logout
                            exchange.getExchange().getResponse().setStatusCode(
                                    org.springframework.http.HttpStatus.FOUND);
                            exchange.getExchange().getResponse().getHeaders()
                                    .add("Location", "/movie/");
                            return reactor.core.publisher.Mono.empty();
                        })
                )
                .csrf(csrf -> csrf.disable()) // Disable CSRF for simplicity
                .build();
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .roles("ADMIN", "USER")
                .build();

        UserDetails user = User.builder()
                .username("user")
                .password(passwordEncoder.encode("user123"))
                .roles("USER")
                .build();

        return new MapReactiveUserDetailsService(admin, user);
    }
}
