package com.moviehub.review.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserResponseDto {
    private String id;
    private String username;
    private String email;
    private List<String> reviewIds;
    private List<String> movieIds;
}