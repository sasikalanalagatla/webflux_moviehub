package com.moviehub.review.mapper;

import com.moviehub.review.dto.UserRegistrationDto;
import com.moviehub.review.dto.UserResponseDto;
import com.moviehub.review.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

public class UserMapper {

    private static final Logger logger = LoggerFactory.getLogger(UserMapper.class);

    public static User dtoToUser(UserRegistrationDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("UserRegistrationDto cannot be null");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    public static UserResponseDto userToDto(User user) {
        if (user == null) {
            logger.error("🚨 Attempting to convert null User to DTO");
            throw new IllegalArgumentException("User cannot be null when converting to DTO");
        }

        logger.debug("🔄 Converting User to DTO: id={}, username={}", user.getId(), user.getUsername());

        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setEnabled(user.isEnabled());
        dto.setCreatedAt(user.getCreatedAt());

        return dto;
    }

    public static User dtoToUserWithPasswordAndRole(UserRegistrationDto dto, String encodedPassword, String role) {
        if (dto == null) {
            throw new IllegalArgumentException("UserRegistrationDto cannot be null");
        }
        if (encodedPassword == null || encodedPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Encoded password cannot be null or empty");
        }
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Role cannot be null or empty");
        }

        logger.debug("🏗️ Creating User from DTO: username={}, role={}", dto.getUsername(), role);

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(encodedPassword);
        user.setRole(role);
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());

        return user;
    }
}