package com.moviehub.review.config;

import com.moviehub.review.service.impl.UserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class WebSecurityConfig {

    @Autowired
    private UserDetailsService userDetailsService;

    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable) // Disable CSRF for simplicity
                .authorizeExchange(auth -> auth
                        // ===== PUBLIC ACCESS - No authentication required =====
                        .pathMatchers("/register", "/registration", "/login", "/css/**", "/js/**", "/images/**",
                                "/favicon.ico", "/error").permitAll()

                        // Public viewing of content
                        .pathMatchers("/", "/movie/", "/movie/all", "/movie/*/detailed", "/movie/*",
                                "/reviews", "/reviews/*").permitAll()

                        // ===== AUTHENTICATED USERS ONLY - All review operations =====
                        // Users can create reviews (must be logged in)
                        .pathMatchers("/reviews/create").hasAnyRole("USER", "AUTHOR", "ADMIN")

                        // Users can edit/update/delete their own reviews, admins can edit any
                        .pathMatchers("/reviews/edit/**", "/reviews/update/**", "/reviews/delete/**")
                        .hasAnyRole("USER", "AUTHOR", "ADMIN")

                        // ===== AUTHOR ROLE - Can create and manage movies they created =====
                        .pathMatchers("/movie/add", "/movie/save").hasAnyRole("AUTHOR", "ADMIN")
                        .pathMatchers("/movie/edit/**", "/movie/update/**", "/movie/*/enrich")
                        .hasAnyRole("AUTHOR", "ADMIN")

                        // ===== ADMIN ROLE - Full access to everything =====
                        .pathMatchers("/movie/delete/**", "/admin/**", "/management/**").hasRole("ADMIN")

                        // User management (admin only)
                        .pathMatchers("/users/**", "/roles/**").hasRole("ADMIN")

                        // ===== DEFAULT - All other paths require authentication =====
                        .anyExchange().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .authenticationSuccessHandler(new RedirectServerAuthenticationSuccessHandler("/"))
                        .authenticationFailureHandler(new RedirectServerAuthenticationFailureHandler("/login?error=true"))
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(new RedirectServerLogoutSuccessHandler())
                )
                .build();
    }
}
