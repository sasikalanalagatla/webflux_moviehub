package com.moviehub.review.service.impl;

import com.moviehub.review.dto.ReviewRequestDto;
import com.moviehub.review.dto.ReviewResponseDto;
import com.moviehub.review.exception.MovieNotFoundException;
import com.moviehub.review.exception.ReviewNotFoundException;
import com.moviehub.review.mapper.MovieMapper;
import com.moviehub.review.mapper.ReviewMapper;
import com.moviehub.review.model.Movie;
import com.moviehub.review.model.Review;
import com.moviehub.review.repository.MovieRepository;
import com.moviehub.review.repository.ReviewRepository;
import com.moviehub.review.service.MovieService;
import com.moviehub.review.service.ReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewServiceImpl.class);

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private MovieService movieService;

    @Autowired
    private MovieRepository movieRepository;

    @Override
    public Mono<ReviewResponseDto> createReview(ReviewRequestDto reviewRequestDto) {
        logger.info("Creating review for movie: {} with rating: {}", reviewRequestDto.getMovieId(), reviewRequestDto.getRating());

        if (reviewRequestDto.getRating() < 1 || reviewRequestDto.getRating() > 5) {
            logger.warn("Invalid rating provided: {}. Rating must be between 1 and 5", reviewRequestDto.getRating());
            return Mono.error(new IllegalArgumentException("Rating must be between 1 and 5"));
        }

        return resolveMovieId(reviewRequestDto.getMovieId())
                .doOnNext(resolvedId -> logger.debug("Resolved movie ID: {} for review", resolvedId))
                .flatMap(resolvedMovieId -> {
                    reviewRequestDto.setMovieId(resolvedMovieId);
                    return movieRepository.findById(resolvedMovieId)
                            .flatMap(movie -> {
                                logger.debug("Found movie for review: {} (Released: {})", movie.getTitle(), movie.getReleased());
                                if (Boolean.FALSE.equals(movie.getReleased())) {
                                    logger.warn("Attempt to review unreleased movie: {}", movie.getTitle());
                                    return Mono.error(new IllegalStateException("Reviews not allowed before release"));
                                }
                                Review review = ReviewMapper.toEntity(reviewRequestDto);
                                return reviewRepository.save(review)
                                        .doOnSuccess(savedReview -> logger.debug("Successfully saved review with ID: {}", savedReview.getReviewId()))
                                        .map(ReviewMapper::toDto)
                                        .flatMap(savedReview -> {
                                            logger.debug("Calculating new average rating for movie: {}", savedReview.getMovieId());
                                            return calculateAverageRatingForMovie(savedReview.getMovieId())
                                                    .flatMap(avgRating -> {
                                                        logger.debug("New average rating for movie {}: {}", savedReview.getMovieId(), avgRating);
                                                        return movieService.updateMovieRating(savedReview.getMovieId(), avgRating);
                                                    })
                                                    .then(Mono.just(savedReview));
                                        });
                            });
                })
                .doOnSuccess(review -> logger.info("Successfully created review with ID: {} for movie: {}", review.getReviewId(), review.getMovieId()))
                .doOnError(error -> logger.error("Failed to create review for movie {}: {}", reviewRequestDto.getMovieId(), error.getMessage(), error));
    }

    @Override
    public Mono<ReviewResponseDto> updateReview(ReviewRequestDto requestDto, String reviewId) {
        logger.info("Updating review ID: {} for movie: {} with rating: {}", reviewId, requestDto.getMovieId(), requestDto.getRating());

        if (requestDto.getRating() < 1 || requestDto.getRating() > 5) {
            logger.warn("Invalid rating provided for update: {}. Rating must be between 1 and 5", requestDto.getRating());
            return Mono.error(new IllegalArgumentException("Rating must be between 1 and 5"));
        }

        return resolveMovieId(requestDto.getMovieId())
                .doOnNext(resolvedId -> logger.debug("Resolved movie ID: {} for review update", resolvedId))
                .flatMap(resolvedMovieId -> reviewRepository.findById(reviewId)
                        .switchIfEmpty(Mono.error(new ReviewNotFoundException("Review not found with id: " + reviewId)))
                        .doOnNext(existingReview -> logger.debug("Found existing review: {} for movie: {}", reviewId, existingReview.getMovieId()))
                        .flatMap(existingReview -> {
                            existingReview.setMovieId(resolvedMovieId);
                            existingReview.setComment(requestDto.getComment());
                            existingReview.setRating(requestDto.getRating());
                            existingReview.setCreatedAt(Instant.now());
                            return reviewRepository.save(existingReview);
                        })
                        .doOnSuccess(savedReview -> logger.debug("Successfully updated review: {}", reviewId))
                        .map(ReviewMapper::toDto)
                        .flatMap(updatedReview -> {
                            logger.debug("Recalculating average rating for movie: {}", updatedReview.getMovieId());
                            return calculateAverageRatingForMovie(updatedReview.getMovieId())
                                    .flatMap(avgRating -> {
                                        logger.debug("Updated average rating for movie {}: {}", updatedReview.getMovieId(), avgRating);
                                        return movieService.updateMovieRating(updatedReview.getMovieId(), avgRating);
                                    })
                                    .then(Mono.just(updatedReview));
                        }))
                .doOnSuccess(review -> logger.info("Successfully updated review ID: {} for movie: {}", reviewId, review.getMovieId()))
                .doOnError(error -> logger.error("Failed to update review {}: {}", reviewId, error.getMessage(), error));
    }

    @Override
    public Mono<Void> deleteReview(String reviewId) {
        logger.info("Deleting review ID: {}", reviewId);

        return reviewRepository.findById(reviewId)
                .switchIfEmpty(Mono.error(new ReviewNotFoundException("Review not found with reviewId: " + reviewId)))
                .doOnNext(review -> logger.debug("Found review to delete: {} for movie: {}", reviewId, review.getMovieId()))
                .flatMap(review -> reviewRepository.deleteById(reviewId)
                        .doOnSuccess(unused -> logger.debug("Successfully deleted review: {}", reviewId))
                        .then(calculateAverageRatingForMovie(review.getMovieId())
                                .doOnNext(avgRating -> logger.debug("Recalculated average rating after deletion for movie {}: {}", review.getMovieId(), avgRating))
                                .flatMap(avgRating -> movieService.updateMovieRating(review.getMovieId(), avgRating))
                                .then()))
                .doOnSuccess(unused -> logger.info("Successfully deleted review ID: {}", reviewId))
                .doOnError(error -> logger.error("Failed to delete review {}: {}", reviewId, error.getMessage(), error));
    }

    @Override
    public Mono<ReviewResponseDto> getReviewById(String reviewId) {
        logger.info("Fetching review by ID: {}", reviewId);

        return reviewRepository.findById(reviewId)
                .switchIfEmpty(Mono.error(new ReviewNotFoundException("Review not found with reviewId: " + reviewId)))
                .doOnNext(review -> logger.debug("Retrieved review: {} for movie: {}", reviewId, review.getMovieId()))
                .doOnError(error -> logger.error("Error fetching review {}: {}", reviewId, error.getMessage()))
                .map(ReviewMapper::toDto);
    }

    @Override
    public Flux<ReviewResponseDto> getAllReviews() {
        logger.info("Fetching all reviews");

        return reviewRepository.findAll()
                .doOnComplete(() -> logger.debug("Completed fetching all reviews"))
                .doOnError(error -> logger.error("Error fetching all reviews: {}", error.getMessage(), error))
                .map(ReviewMapper::toDto)
                .switchIfEmpty(Flux.empty());
    }

    @Override
    public Flux<ReviewResponseDto> getReviewsByMovieId(String movieId) {
        logger.info("Fetching reviews for movie ID: {}", movieId);

        return reviewRepository.findByMovieId(movieId)
                .doOnComplete(() -> logger.debug("Completed fetching reviews for movie: {}", movieId))
                .doOnError(error -> logger.error("Error fetching reviews for movie {}: {}", movieId, error.getMessage(), error))
                .map(ReviewMapper::toDto);
    }

    @Override
    public Mono<Double> calculateAverageRatingForMovie(String movieId) {
        logger.debug("Calculating average rating for movie ID: {}", movieId);

        return getReviewsByMovieId(movieId)
                .collectList()
                .map(reviews -> {
                    if (reviews.isEmpty()) {
                        logger.debug("No reviews found for movie {}, returning 0.0", movieId);
                        return 0.0;
                    }
                    double average = reviews.stream()
                            .mapToInt(ReviewResponseDto::getRating)
                            .average()
                            .orElse(0.0);
                    logger.debug("Calculated average rating for movie {}: {} (from {} reviews)", movieId, average, reviews.size());
                    return average;
                })
                .doOnError(error -> logger.error("Error calculating average rating for movie {}: {}", movieId, error.getMessage(), error));
    }

    private Mono<String> resolveMovieId(String provided) {
        logger.debug("Resolving movie identifier: {}", provided);

        if (provided == null || provided.trim().isEmpty()) {
            logger.warn("Empty movie identifier provided");
            return Mono.error(new MovieNotFoundException("Movie is required"));
        }

        String candidate = provided.trim();
        Mono<String> byId = movieRepository.findById(candidate)
                .doOnNext(movie -> logger.debug("Found movie by ID: {}", movie.getTitle()))
                .map(Movie::getMovieId);

        Mono<String> byTitle = movieRepository.findByTitleIgnoreCase(candidate)
                .doOnNext(movie -> logger.debug("Found movie by title: {} (ID: {})", movie.getTitle(), movie.getMovieId()))
                .map(Movie::getMovieId);

        return byId.switchIfEmpty(byTitle)
                .switchIfEmpty(Mono.error(new MovieNotFoundException("Movie not found with movieId/title: " + provided)))
                .doOnError(error -> logger.error("Failed to resolve movie identifier '{}': {}", provided, error.getMessage()));
    }
}
