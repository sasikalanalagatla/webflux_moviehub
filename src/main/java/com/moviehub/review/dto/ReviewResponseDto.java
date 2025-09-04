package com.moviehub.review.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class ReviewResponseDto {
    private String reviewId;
    private String movieId;
    private String userId;
    private Integer rating;
    private String comment;
    private Instant createdAt;
}