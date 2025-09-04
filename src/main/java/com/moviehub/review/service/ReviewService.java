package com.moviehub.review.service;

import com.moviehub.review.dto.ReviewRequestDto;
import com.moviehub.review.dto.ReviewResponseDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReviewService {
    Mono<ReviewResponseDto> createReview(ReviewRequestDto reviewRequestDto);
    Mono<ReviewResponseDto> updateReview(ReviewRequestDto requestDto, String reviewId);
    Mono<Void> deleteReview(String reviewId);
    Mono<ReviewResponseDto> getReviewById(String reviewId);
    Flux<ReviewResponseDto> getAllReviews();
    Flux<ReviewResponseDto> getReviewsByMovieId(String movieId); // New method
    Mono<Double> calculateAverageRatingForMovie(String movieId); // New method
}