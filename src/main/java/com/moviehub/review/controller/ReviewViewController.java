// First, let's check your ReviewViewController - here's a corrected version

package com.moviehub.review.controller;

import com.moviehub.review.dto.ReviewRequestDto;
import com.moviehub.review.service.impl.ReviewServiceImpl;
import com.moviehub.review.service.impl.MovieServiceImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/reviews")
public class ReviewViewController {

    @Autowired
    private ReviewServiceImpl reviewService;

    @Autowired
    private MovieServiceImpl movieService;

    @GetMapping
    public Mono<String> getAllReviews(Model model) {
        return reviewService.getAllReviews()
                .collectList()
                .doOnNext(reviews -> {
                    model.addAttribute("reviews", reviews);
                    model.addAttribute("newReview", new ReviewRequestDto());
                })
                .then(Mono.just("reviews/list"))
                .onErrorResume(error -> {
                    System.out.println("Error in getAllReviews: " + error.getMessage());
                    model.addAttribute("reviews", java.util.Collections.emptyList());
                    model.addAttribute("newReview", new ReviewRequestDto());
                    return Mono.just("reviews/list");
                });
    }

    @GetMapping("/create")
    public Mono<String> showCreateForm(Model model) {
        model.addAttribute("review", new ReviewRequestDto());
        // Add list of movies for selection
        return movieService.getALlMovies()
                .collectList()
                .doOnNext(movies -> {
                    System.out.println("Found " + movies.size() + " movies for dropdown");
                    model.addAttribute("movies", movies);
                })
                .thenReturn("reviews/create")
                .doOnError(error -> System.out.println("Error loading movies: " + error.getMessage()));
    }

    @PostMapping("/create")
    public Mono<String> createReview(@Valid @ModelAttribute ReviewRequestDto reviewRequestDto,
                                     BindingResult bindingResult, Model model) {

        System.out.println("=== CREATE REVIEW DEBUG ===");
        System.out.println("Movie ID: " + reviewRequestDto.getMovieId());
        System.out.println("Rating: " + reviewRequestDto.getRating());
        System.out.println("Comment: " + reviewRequestDto.getComment());
        System.out.println("User ID: " + reviewRequestDto.getUserId());
        System.out.println("Has validation errors: " + bindingResult.hasErrors());

        if (bindingResult.hasErrors()) {
            System.out.println("Validation errors found:");
            bindingResult.getAllErrors().forEach(error ->
                    System.out.println("- " + error.getDefaultMessage()));

            model.addAttribute("review", reviewRequestDto);
            return movieService.getALlMovies()
                    .collectList()
                    .doOnNext(movies -> model.addAttribute("movies", movies))
                    .thenReturn("reviews/create");
        }

        // Set a default userId if not provided
        if (reviewRequestDto.getUserId() == null || reviewRequestDto.getUserId().trim().isEmpty()) {
            reviewRequestDto.setUserId("anonymous-user");
            System.out.println("Set default user ID: anonymous-user");
        }

        return reviewService.createReview(reviewRequestDto)
                .doOnNext(savedReview -> System.out.println("Review created with ID: " + savedReview.getReviewId()))
                .then(Mono.just("redirect:/reviews"))
                .doOnError(error -> System.out.println("Error creating review: " + error.getMessage()))
                .onErrorResume(error -> {
                    System.out.println("Error in createReview: " + error.getMessage());
                    error.printStackTrace();
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
        return reviewService.getReviewById(reviewId)
                .doOnNext(review -> model.addAttribute("review", review))
                .then(Mono.just("reviews/detail"))
                .onErrorResume(error -> {
                    model.addAttribute("error", "Review not found: " + error.getMessage());
                    return Mono.just("reviews/detail");
                });
    }

    @GetMapping("/edit/{reviewId}")
    public Mono<String> showEditForm(@PathVariable String reviewId, Model model) {
        return reviewService.getReviewById(reviewId)
                .doOnNext(review -> model.addAttribute("review", review))
                .flatMap(review -> movieService.getALlMovies()
                        .collectList()
                        .doOnNext(movies -> model.addAttribute("movies", movies))
                        .thenReturn("reviews/edit"));
    }

    @PostMapping("/update/{reviewId}")
    public Mono<String> updateReview(@PathVariable String reviewId,
                                     @Valid @ModelAttribute ReviewRequestDto requestDto,
                                     BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("review", requestDto);
            return movieService.getALlMovies()
                    .collectList()
                    .doOnNext(movies -> model.addAttribute("movies", movies))
                    .thenReturn("reviews/edit");
        }

        return reviewService.updateReview(requestDto, reviewId)
                .then(Mono.just("redirect:/reviews"))
                .onErrorResume(error -> {
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
        return reviewService.deleteReview(reviewId)
                .then(Mono.just("redirect:/reviews"))
                .onErrorResume(error -> {
                    System.out.println("Error deleting review: " + error.getMessage());
                    return Mono.just("redirect:/reviews");
                });
    }
}
