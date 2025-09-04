package com.moviehub.review.service.impl;

import com.moviehub.review.dto.ReviewRequestDto;
import com.moviehub.review.dto.ReviewResponseDto;
import com.moviehub.review.exception.MovieNotFoundException;
import com.moviehub.review.exception.ReviewNotFoundException;
import com.moviehub.review.mapper.MovieMapper;
import com.moviehub.review.mapper.ReviewMapper;
import com.moviehub.review.model.Review;
import com.moviehub.review.repository.MovieRepository;
import com.moviehub.review.repository.ReviewRepository;
import com.moviehub.review.service.MovieService;
import com.moviehub.review.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private MovieService movieService;

    @Override
    public Mono<ReviewResponseDto> createReview(ReviewRequestDto reviewRequestDto) {
        if (reviewRequestDto.getRating() < 1 || reviewRequestDto.getRating() > 5) {
            return Mono.error(new IllegalArgumentException("Rating must be between 1 and 5"));
        }

        Review review = ReviewMapper.toEntity(reviewRequestDto);
        return reviewRepository.save(review)
                .map(ReviewMapper::toDto)
                .flatMap(savedReview -> {
                    // Update movie average rating after creating review
                    return calculateAverageRatingForMovie(savedReview.getMovieId())
                            .flatMap(avgRating -> movieService.updateMovieRating(savedReview.getMovieId(), avgRating))
                            .then(Mono.just(savedReview));
                });
    }

    @Override
    public Mono<ReviewResponseDto> updateReview(ReviewRequestDto requestDto, String reviewId) {
        if (requestDto.getRating() < 1 || requestDto.getRating() > 5) {
            return Mono.error(new IllegalArgumentException("Rating must be between 1 and 5"));
        }

        return reviewRepository.findById(reviewId)
                .switchIfEmpty(Mono.error(new ReviewNotFoundException("Review not found with id: " + reviewId)))
                .flatMap(existingReview -> {
                    existingReview.setMovieId(requestDto.getMovieId());
                    existingReview.setComment(requestDto.getComment());
                    existingReview.setRating(requestDto.getRating());
                    existingReview.setCreatedAt(Instant.now());
                    return reviewRepository.save(existingReview);
                })
                .map(ReviewMapper::toDto)
                .flatMap(updatedReview -> {
                    // Update movie average rating after updating review
                    return calculateAverageRatingForMovie(updatedReview.getMovieId())
                            .flatMap(avgRating -> movieService.updateMovieRating(updatedReview.getMovieId(), avgRating))
                            .then(Mono.just(updatedReview));
                });
    }

    @Override
    public Mono<Void> deleteReview(String reviewId) {
        return reviewRepository.findById(reviewId)
                .switchIfEmpty(Mono.error(new ReviewNotFoundException("Review not found with reviewId: " + reviewId)))
                .flatMap(review -> reviewRepository.deleteById(reviewId)
                        .then(calculateAverageRatingForMovie(review.getMovieId())
                                .flatMap(avgRating -> movieService.updateMovieRating(review.getMovieId(), avgRating))
                                .then()));
    }

    @Override
    public Mono<ReviewResponseDto> getReviewById(String reviewId) {
        return reviewRepository.findById(reviewId)
                .switchIfEmpty(Mono.error(new ReviewNotFoundException("Review not found with reviewId: " + reviewId)))
                .map(ReviewMapper::toDto);
    }

    @Override
    public Flux<ReviewResponseDto> getAllReviews() {
        return reviewRepository.findAll()
                .map(ReviewMapper::toDto)
                .switchIfEmpty(Flux.empty());
    }

    @Override
    public Flux<ReviewResponseDto> getReviewsByMovieId(String movieId) {
        return reviewRepository.findAll()
                .filter(review -> movieId.equals(review.getMovieId()))
                .map(ReviewMapper::toDto);
    }

    @Override
    public Mono<Double> calculateAverageRatingForMovie(String movieId) {
        return getReviewsByMovieId(movieId)
                .collectList()
                .map(reviews -> {
                    if (reviews.isEmpty()) {
                        return 0.0;
                    }
                    return reviews.stream()
                            .mapToInt(ReviewResponseDto::getRating)
                            .average()
                            .orElse(0.0);
                });
    }
}