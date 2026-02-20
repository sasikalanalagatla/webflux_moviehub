package com.moviehub.review.service;

import com.moviehub.review.dto.UserRegistrationDto;
import com.moviehub.review.dto.UserResponseDto;
import com.moviehub.review.model.User;
import reactor.core.publisher.Mono;

public interface UserService {

    Mono<UserResponseDto> registerUser(UserRegistrationDto userRegistrationDto);
    Mono<User> findByUsername(String username);
    Mono<Boolean> isUsernameExists(String username);
    Mono<Boolean> isEmailExists(String email);
}
