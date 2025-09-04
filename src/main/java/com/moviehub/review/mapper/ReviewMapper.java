package com.moviehub.review.mapper;

import com.moviehub.review.dto.ReviewRequestDto;
import com.moviehub.review.dto.ReviewResponseDto;
import com.moviehub.review.model.Review;

import java.time.Instant;

public class ReviewMapper {

    public static Review toEntity(ReviewRequestDto dto) {
        Review review = new Review();
        review.setMovieId(dto.getMovieId());
        review.setUserId(dto.getUserId());
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());
        review.setCreatedAt(Instant.now());
        return review;
    }

    public static ReviewResponseDto toDto(Review review) {
        ReviewResponseDto dto = new ReviewResponseDto();
        dto.setReviewId(review.getReviewId());
        dto.setMovieId(review.getMovieId());
        dto.setUserId(review.getUserId());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());
        return dto;
    }
}