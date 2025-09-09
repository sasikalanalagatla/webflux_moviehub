package com.moviehub.review.service;

import com.moviehub.review.dto.UserRegistrationDto;
import com.moviehub.review.dto.UserResponseDto;
import com.moviehub.review.model.User;
import reactor.core.publisher.Mono;

public interface UserService {

    Mono<UserResponseDto> registerUser(UserRegistrationDto userRegistrationDto);

    Mono<User> findByUsername(String username);

    Mono<User> promoteToAuthor(String userId);

    Mono<Boolean> isUsernameExists(String username);

    Mono<Boolean> isEmailExists(String email);

    Mono<UserResponseDto> getUserById(String userId);

    Mono<UserResponseDto> updateUserRole(String userId, String newRole);
}
