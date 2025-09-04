package com.moviehub.review.mapper;

import com.moviehub.review.dto.UserRequestDto;
import com.moviehub.review.dto.UserResponseDto;
import com.moviehub.review.model.User;

import java.util.ArrayList;

public class UserMapper {

    public static User toEntity(UserRequestDto dto) {
        User user = new User();
        user.setUserName(dto.getUsername());
        user.setEmailId(dto.getEmail());
        user.setReviewIds(new ArrayList<>());
        user.setMovieIds(new ArrayList<>());
        return user;
    }

    public static UserResponseDto toDto(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getUserId());
        dto.setUsername(user.getUserName());
        dto.setEmail(user.getEmailId());
        dto.setReviewIds(user.getReviewIds());
        dto.setMovieIds(user.getMovieIds());
        return dto;
    }
}