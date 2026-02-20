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
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(auth -> auth
                        .pathMatchers("/register", "/registration", "/login", "/css/**", "/js/**", "/images/**",
                                "/favicon.ico", "/error").permitAll()

                        .pathMatchers("/", "/movie/", "/movie/all", "/movie/*/detailed", "/movie/*",
                                "/reviews", "/reviews/*").permitAll()

                        .pathMatchers("/reviews/create").hasAnyRole("USER", "AUTHOR", "ADMIN")

                        .pathMatchers("/reviews/edit/**", "/reviews/update/**", "/reviews/delete/**")
                        .hasAnyRole("USER", "AUTHOR", "ADMIN")

                        .pathMatchers("/movie/add", "/movie/save").hasAnyRole("AUTHOR", "ADMIN")
                        .pathMatchers("/movie/edit/**", "/movie/update/**", "/movie/*/enrich")
                        .hasAnyRole("AUTHOR", "ADMIN")

                        .pathMatchers("/movie/delete/**", "/admin/**", "/management/**").hasRole("ADMIN")

                        .pathMatchers("/users/**", "/roles/**").hasRole("ADMIN")

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
