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
                        // Public access - viewing content
                        .pathMatchers("/register", "/login", "/css/**", "/js/**", "/images/**",
                                "/movie/", "/movie/all", "/movie/*", "/movie/*/detailed",
                                "/reviews", "/reviews/*").permitAll()

                        // User level access - can create reviews
                        .pathMatchers("/reviews/create", "/reviews/update/**").hasAnyRole("USER", "AUTHOR", "ADMIN")

                        // Author level access - can create and manage movies
                        .pathMatchers("/movie/add", "/movie/save", "/movie/edit/**",
                                "/movie/update/**", "/movie/*/enrich").hasAnyRole("AUTHOR", "ADMIN")

                        // Admin level access - can delete content and manage users
                        .pathMatchers("/movie/delete/**", "/reviews/edit/**",
                                "/reviews/delete/**", "/admin/**").hasRole("ADMIN")

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
