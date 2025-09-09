package com.moviehub.review.service.impl;

import com.moviehub.review.dto.UserRegistrationDto;
import com.moviehub.review.dto.UserResponseDto;
import com.moviehub.review.mapper.UserMapper;
import com.moviehub.review.model.User;
import com.moviehub.review.repository.UserRepository;
import com.moviehub.review.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Mono<UserResponseDto> registerUser(UserRegistrationDto userRegistrationDto) {
        logger.info("🚀 Starting registration for user: {}", userRegistrationDto.getUsername());

        return validateUserRegistration(userRegistrationDto)
                .then(userRepository.count()) // Use count() instead of countUsers()
                .doOnNext(count -> logger.info("📊 Current user count: {}", count))
                .flatMap(userCount -> {
                    String role = (userCount == 0) ? "ROLE_ADMIN" : "ROLE_USER";
                    String encodedPassword = passwordEncoder.encode(userRegistrationDto.getPassword());

                    logger.info("🔐 Password encoded, assigning role: {}", role);

                    User user = UserMapper.dtoToUserWithPasswordAndRole(userRegistrationDto, encodedPassword, role);

                    logger.info("👤 Created user entity: username={}, email={}",
                            user.getUsername(), user.getEmail());

                    return userRepository.save(user)
                            .doOnNext(savedUser -> {
                                logger.info("✅ User saved with ID: {}", savedUser.getId());
                            })
                            .doOnError(error -> {
                                logger.error("❌ Failed to save user: {}", error.getMessage(), error);
                            });
                })
                .map(savedUser -> {
                    UserResponseDto dto = UserMapper.userToDto(savedUser);
                    logger.info("🎯 Converted to DTO: {}", dto.getUsername());
                    return dto;
                })
                .doOnSuccess(userDto -> {
                    logger.info("🎉 Registration completed successfully for: {}", userDto.getUsername());
                })
                .doOnError(error -> {
                    logger.error("💥 Registration failed: {}", error.getMessage(), error);
                });
    }

    @Override
    public Mono<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Mono<User> promoteToAuthor(String userId) {
        logger.info("🔄 Promoting user to AUTHOR: {}", userId);

        return userRepository.findById(userId)
                .flatMap(user -> {
                    if ("ROLE_USER".equals(user.getRole())) {
                        user.setRole("ROLE_AUTHOR");
                        return userRepository.save(user)
                                .doOnNext(savedUser -> logger.info("✅ User promoted to AUTHOR: {}", savedUser.getUsername()));
                    }
                    return Mono.just(user);
                });
    }

    @Override
    public Mono<Boolean> isUsernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public Mono<Boolean> isEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public Mono<UserResponseDto> getUserById(String userId) {
        return userRepository.findById(userId)
                .map(UserMapper::userToDto);
    }

    @Override
    public Mono<UserResponseDto> updateUserRole(String userId, String newRole) {
        return userRepository.findById(userId)
                .flatMap(user -> {
                    user.setRole(newRole);
                    return userRepository.save(user);
                })
                .map(UserMapper::userToDto);
    }

    private Mono<Void> validateUserRegistration(UserRegistrationDto userDto) {
        if (!userDto.getPassword().equals(userDto.getConfirmPassword())) {
            return Mono.error(new IllegalArgumentException("Passwords do not match"));
        }

        return isUsernameExists(userDto.getUsername())
                .flatMap(usernameExists -> {
                    if (usernameExists) {
                        return Mono.error(new IllegalArgumentException("Username already exists"));
                    }
                    return isEmailExists(userDto.getEmail());
                })
                .flatMap(emailExists -> {
                    if (emailExists) {
                        return Mono.error(new IllegalArgumentException("Email already exists"));
                    }
                    return Mono.empty();
                });
    }
}
