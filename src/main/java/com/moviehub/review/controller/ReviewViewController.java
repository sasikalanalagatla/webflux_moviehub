package com.moviehub.review.controller;

import com.moviehub.review.dto.ReviewRequestDto;
import com.moviehub.review.service.ReviewService;
import com.moviehub.review.service.MovieService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/reviews")
public class ReviewViewController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewViewController.class);

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private MovieService movieService;

    @GetMapping
    public Mono<String> getAllReviews(Model model) {
        logger.info("Fetching all reviews");

        return reviewService.getAllReviews()
                .collectList()
                .doOnNext(reviews -> logger.debug("Retrieved {} reviews", reviews.size()))
                .flatMap(reviews -> movieService.getALlMovies()
                        .collectMap(m -> m.getMovieId(), m -> m.getTitle())
                        .defaultIfEmpty(java.util.Collections.emptyMap())
                        .doOnNext(movieTitles -> {
                            logger.debug("Retrieved {} movie titles for mapping", movieTitles.size());
                            model.addAttribute("reviews", reviews);
                            model.addAttribute("movieTitles", movieTitles);
                            model.addAttribute("newReview", new ReviewRequestDto());
                        }))
                .then(Mono.just("reviews/list"))
                .onErrorResume(error -> {
                    logger.error("Error fetching reviews: {}", error.getMessage(), error);
                    model.addAttribute("reviews", java.util.Collections.emptyList());
                    model.addAttribute("movieTitles", java.util.Collections.emptyMap());
                    model.addAttribute("newReview", new ReviewRequestDto());
                    return Mono.just("reviews/list");
                });
    }

    @GetMapping("/create")
    public Mono<String> showCreateForm(Model model) {
        logger.info("Displaying create review form");

        model.addAttribute("review", new ReviewRequestDto());
        return movieService.getALlMovies()
                .collectList()
                .doOnNext(movies -> {
                    logger.debug("Loaded {} movies for review form", movies.size());
                    model.addAttribute("movies", movies);
                })
                .doOnError(error -> logger.error("Error loading movies for create form: {}", error.getMessage(), error))
                .thenReturn("reviews/create");
    }

    @PostMapping("/create")
    public Mono<String> createReview(@Valid @ModelAttribute ReviewRequestDto reviewRequestDto,
                                     BindingResult bindingResult, Model model) {
        logger.info("Attempting to create review for movie ID: {}", reviewRequestDto.getMovieId());

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors while creating review: {}", bindingResult.getAllErrors());
            model.addAttribute("review", reviewRequestDto);
            return movieService.getALlMovies()
                    .collectList()
                    .doOnNext(movies -> model.addAttribute("movies", movies))
                    .thenReturn("reviews/create");
        }

        if (reviewRequestDto.getUserId() == null || reviewRequestDto.getUserId().trim().isEmpty()) {
            logger.debug("Setting default userId for review");
            reviewRequestDto.setUserId("anonymous-user");
        }

        return reviewService.createReview(reviewRequestDto)
                .doOnSuccess(review -> logger.info("Successfully created review for movie ID: {}", reviewRequestDto.getMovieId()))
                .then(Mono.just("redirect:/reviews"))
                .onErrorResume(error -> {
                    logger.error("Failed to create review for movie {}: {}", reviewRequestDto.getMovieId(), error.getMessage(), error);
                    model.addAttribute("error", "Failed to create review: " + error.getMessage());
                    model.addAttribute("review", reviewRequestDto);
                    return movieService.getALlMovies()
                            .collectList()
                            .doOnNext(movies -> model.addAttribute("movies", movies))
                            .thenReturn("reviews/create");
                });
    }

    @GetMapping("/{reviewId}")
    public Mono<String> getReviewById(@PathVariable String reviewId, Model model) {
        logger.info("Fetching review details for ID: {}", reviewId);

        return reviewService.getReviewById(reviewId)
                .doOnNext(review -> {
                    logger.debug("Retrieved review: {} for movie ID: {}", reviewId, review.getMovieId());
                    model.addAttribute("review", review);
                })
                .flatMap(review -> movieService.getMovieById(review.getMovieId())
                        .doOnNext(movie -> {
                            logger.debug("Retrieved movie title: {} for review {}", movie.getTitle(), reviewId);
                            model.addAttribute("movieTitle", movie.getTitle());
                        })
                        .thenReturn(review))
                .then(Mono.just("reviews/detail"))
                .onErrorResume(error -> {
                    logger.error("Error fetching review {}: {}", reviewId, error.getMessage(), error);
                    model.addAttribute("error", "Review not found: " + error.getMessage());
                    return Mono.just("reviews/detail");
                });
    }

    @GetMapping("/edit/{reviewId}")
    public Mono<String> showEditForm(@PathVariable String reviewId, Model model) {
        logger.info("Displaying edit form for review ID: {}", reviewId);

        return reviewService.getReviewById(reviewId)
                .doOnNext(review -> {
                    logger.debug("Retrieved review for editing: {}", reviewId);
                    model.addAttribute("review", review);
                })
                .flatMap(review -> movieService.getALlMovies()
                        .collectList()
                        .doOnNext(movies -> {
                            logger.debug("Loaded {} movies for edit form", movies.size());
                            model.addAttribute("movies", movies);
                        })
                        .thenReturn("reviews/edit"))
                .onErrorResume(error -> {
                    logger.error("Error loading review {} for editing: {}", reviewId, error.getMessage(), error);
                    return Mono.just("reviews/error");
                });
    }

    @PostMapping("/update/{reviewId}")
    public Mono<String> updateReview(@PathVariable String reviewId,
                                     @Valid @ModelAttribute ReviewRequestDto requestDto,
                                     BindingResult bindingResult, Model model) {
        logger.info("Attempting to update review ID: {}", reviewId);

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors while updating review {}: {}", reviewId, bindingResult.getAllErrors());
            model.addAttribute("review", requestDto);
            return movieService.getALlMovies()
                    .collectList()
                    .doOnNext(movies -> model.addAttribute("movies", movies))
                    .thenReturn("reviews/edit");
        }

        return reviewService.updateReview(requestDto, reviewId)
                .doOnSuccess(review -> logger.info("Successfully updated review ID: {}", reviewId))
                .then(Mono.just("redirect:/reviews"))
                .onErrorResume(error -> {
                    logger.error("Failed to update review {}: {}", reviewId, error.getMessage(), error);
                    model.addAttribute("error", "Failed to update review: " + error.getMessage());
                    return reviewService.getReviewById(reviewId)
                            .doOnNext(review -> model.addAttribute("review", review))
                            .flatMap(review -> movieService.getALlMovies()
                                    .collectList()
                                    .doOnNext(movies -> model.addAttribute("movies", movies))
                                    .thenReturn("reviews/edit"));
                });
    }

    @GetMapping("/delete/{reviewId}")
    public Mono<String> deleteReview(@PathVariable String reviewId) {
        logger.info("Attempting to delete review ID: {}", reviewId);

        return reviewService.deleteReview(reviewId)
                .doOnSuccess(unused -> logger.info("Successfully deleted review ID: {}", reviewId))
                .doOnError(error -> logger.error("Failed to delete review {}: {}", reviewId, error.getMessage(), error))
                .then(Mono.just("redirect:/reviews"))
                .onErrorResume(error -> Mono.just("redirect:/reviews"));
    }
}
