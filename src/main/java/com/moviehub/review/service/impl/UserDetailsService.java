package com.moviehub.review.service.impl;

import com.moviehub.review.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Service
public class UserDetailsService implements ReactiveUserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserDetailsService.class);

    @Autowired
    private UserService userService;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        logger.debug("Loading user by username: {}", username);

        return userService.findByUsername(username)
                .switchIfEmpty(Mono.defer(() -> {
                    logger.warn("User not found: {}", username);
                    return Mono.error(new UsernameNotFoundException("User not found: " + username));
                }))
                .map(user -> {
                    logger.debug("Found user: {}, role: {}, enabled: {}",
                            user.getUsername(), user.getRole(), user.isEnabled());

                    return User.builder()
                            .username(user.getUsername())
                            .password(user.getPassword())
                            .authorities(Collections.singletonList(new SimpleGrantedAuthority(user.getRole())))
                            .accountExpired(false)
                            .accountLocked(false)
                            .credentialsExpired(false)
                            .disabled(!user.isEnabled())
                            .build();
                })
                .doOnError(error -> logger.error("Error loading user {}: {}", username, error.getMessage()));
    }
}
