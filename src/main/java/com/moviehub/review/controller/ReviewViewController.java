package com.moviehub.review.controller;

import com.moviehub.review.dto.MovieResponseDto;
import com.moviehub.review.dto.ReviewRequestDto;
import com.moviehub.review.dto.ReviewResponseDto;
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

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/reviews")
public class ReviewViewController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewViewController.class);

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private MovieService movieService;

    @GetMapping
    public Mono<String> getAllReviews(Model model, Principal principal,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        logger.info("Fetching all reviews - page: {}, size: {}", page, size);

        // Add current user to model if authenticated
        if (principal != null) {
            model.addAttribute("currentUser", principal.getName());
            logger.debug("User {} accessing reviews list", principal.getName());
        }

        return reviewService.getAllReviews()
                .collectList()
                .doOnNext(reviews -> logger.debug("Retrieved {} reviews", reviews.size()))
                .flatMap(reviews -> movieService.getALlMovies()
                        .collectMap(m -> m.getMovieId(), m -> m.getTitle())
                        .defaultIfEmpty(java.util.Collections.emptyMap())
                        .doOnNext(movieTitles -> {
                            logger.debug("Retrieved {} movie titles for mapping", movieTitles.size());

                            // Pagination logic
                            int fromIndex = page * size;
                            int toIndex = Math.min(fromIndex + size, reviews.size());
                            List<ReviewResponseDto> pagedReviews = fromIndex < reviews.size() ?
                                    reviews.subList(fromIndex, toIndex) : java.util.Collections.emptyList();

                            model.addAttribute("reviews", pagedReviews);
                            model.addAttribute("movieTitles", movieTitles);
                            model.addAttribute("currentPage", page);
                            model.addAttribute("totalPages", (int) Math.ceil(reviews.size() / (double) size));
                            model.addAttribute("hasNext", toIndex < reviews.size());
                            model.addAttribute("hasPrevious", page > 0);
                        }))
                .then(Mono.just("reviews/list"))
                .onErrorResume(error -> {
                    logger.error("Error fetching reviews: {}", error.getMessage(), error);
                    model.addAttribute("reviews", java.util.Collections.emptyList());
                    model.addAttribute("movieTitles", java.util.Collections.emptyMap());
                    model.addAttribute("error", "Unable to load reviews at this time.");
                    return Mono.just("reviews/list");
                });
    }

    @GetMapping("/create")
    public Mono<String> showCreateForm(Model model, Principal principal,
                                       @RequestParam(required = false) String movieId,
                                       @RequestParam(required = false) String searchQuery) {
        logger.info("Displaying create review form with search: {}", searchQuery);

        // Check if user is authenticated
        if (principal == null) {
            logger.warn("Unauthorized attempt to access create review form - user not authenticated");
            return Mono.just("redirect:/login");
        }

        model.addAttribute("currentUser", principal.getName());
        logger.debug("User {} accessing create review form", principal.getName());

        // Initialize review object
        ReviewRequestDto review = new ReviewRequestDto();
        if (movieId != null && !movieId.trim().isEmpty()) {
            review.setMovieId(movieId);
        }

        // Auto-set userId from authenticated user
        review.setUserId(principal.getName());

        // Pre-calculate cancel URL
        String cancelUrl = (movieId != null && !movieId.trim().isEmpty()) ?
                "/movie/" + movieId : "/movie/all";

        model.addAttribute("review", review);
        model.addAttribute("cancelUrl", cancelUrl);
        model.addAttribute("selectedMovieId", movieId);
        model.addAttribute("searchQuery", searchQuery);

        // Filter movies based on search query
        return movieService.getALlMovies()
                .collectList()
                .doOnNext(movies -> {
                    List<MovieResponseDto> filteredMovies = movies;

                    // Filter by search query if provided
                    if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                        String query = searchQuery.toLowerCase().trim();
                        filteredMovies = movies.stream()
                                .filter(movie -> movie.getTitle().toLowerCase().contains(query))
                                .collect(Collectors.toList());
                        logger.debug("Filtered {} movies from {} total based on search: '{}'",
                                filteredMovies.size(), movies.size(), searchQuery);
                    }

                    logger.debug("Loaded {} movies for review form dropdown", filteredMovies.size());
                    model.addAttribute("movies", filteredMovies);
                    model.addAttribute("totalMoviesCount", movies.size());
                    model.addAttribute("filteredCount", filteredMovies.size());

                    // Find selected movie title for display
                    if (movieId != null) {
                        movies.stream()
                                .filter(movie -> movieId.equals(movie.getMovieId()))
                                .findFirst()
                                .ifPresent(movie -> model.addAttribute("selectedMovieTitle", movie.getTitle()));
                    }
                })
                .doOnError(error -> logger.error("Error loading movies for create form: {}", error.getMessage(), error))
                .thenReturn("reviews/create");
    }

    @PostMapping("/create")
    public Mono<String> createReview(@Valid @ModelAttribute ReviewRequestDto reviewRequestDto,
                                     BindingResult bindingResult,
                                     Model model, Principal principal,
                                     @RequestParam(required = false) String searchQuery) {
        logger.info("Attempting to create review for movie ID: {}", reviewRequestDto.getMovieId());

        // Check if user is authenticated
        if (principal == null) {
            logger.warn("Unauthorized attempt to create review - user not authenticated");
            return Mono.just("redirect:/login");
        }

        // Add current user to model if authenticated (for error cases)
        model.addAttribute("currentUser", principal.getName());

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors while creating review: {}", bindingResult.getAllErrors());
            model.addAttribute("review", reviewRequestDto);
            model.addAttribute("searchQuery", searchQuery);

            String cancelUrl = (reviewRequestDto.getMovieId() != null && !reviewRequestDto.getMovieId().trim().isEmpty()) ?
                    "/movie/" + reviewRequestDto.getMovieId() : "/movie/all";
            model.addAttribute("cancelUrl", cancelUrl);

            // Re-load and filter movies for dropdown on error
            return movieService.getALlMovies()
                    .collectList()
                    .doOnNext(movies -> {
                        List<MovieResponseDto> filteredMovies = movies;
                        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                            String query = searchQuery.toLowerCase().trim();
                            filteredMovies = movies.stream()
                                    .filter(movie -> movie.getTitle().toLowerCase().contains(query))
                                    .collect(Collectors.toList());
                        }
                        model.addAttribute("movies", filteredMovies);
                        model.addAttribute("totalMoviesCount", movies.size());
                        model.addAttribute("filteredCount", filteredMovies.size());
                    })
                    .thenReturn("reviews/create");
        }

        // Set userId from authenticated user if not already set
        if (reviewRequestDto.getUserId() == null || reviewRequestDto.getUserId().trim().isEmpty()) {
            reviewRequestDto.setUserId(principal.getName());
            logger.debug("Setting userId from authenticated user: {}", principal.getName());
        }

        return reviewService.createReview(reviewRequestDto)
                .doOnSuccess(review -> logger.info("Successfully created review for movie ID: {}", reviewRequestDto.getMovieId()))
                .then(Mono.just("redirect:/reviews"))
                .onErrorResume(error -> {
                    logger.error("Failed to create review for movie {}: {}", reviewRequestDto.getMovieId(), error.getMessage(), error);
                    model.addAttribute("error", "Failed to create review: " + error.getMessage());
                    model.addAttribute("review", reviewRequestDto);
                    model.addAttribute("searchQuery", searchQuery);

                    String cancelUrl = (reviewRequestDto.getMovieId() != null && !reviewRequestDto.getMovieId().trim().isEmpty()) ?
                            "/movie/" + reviewRequestDto.getMovieId() : "/movie/all";
                    model.addAttribute("cancelUrl", cancelUrl);

                    return movieService.getALlMovies()
                            .collectList()
                            .doOnNext(movies -> {
                                List<MovieResponseDto> filteredMovies = movies;
                                if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                                    String query = searchQuery.toLowerCase().trim();
                                    filteredMovies = movies.stream()
                                            .filter(movie -> movie.getTitle().toLowerCase().contains(query))
                                            .collect(Collectors.toList());
                                }
                                model.addAttribute("movies", filteredMovies);
                            })
                            .thenReturn("reviews/create");
                });
    }

    @GetMapping("/edit/{reviewId}")
    public Mono<String> showEditForm(@PathVariable String reviewId, Model model, Principal principal) {
        logger.info("Displaying edit form for review ID: {}", reviewId);

        // Check if user is authenticated
        if (principal == null) {
            logger.warn("Unauthorized attempt to edit review {} - user not authenticated", reviewId);
            return Mono.just("redirect:/login");
        }

        model.addAttribute("currentUser", principal.getName());
        logger.debug("User {} accessing edit form for review {}", principal.getName(), reviewId);

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

    @GetMapping("/{reviewId}")
    public Mono<String> getReviewById(@PathVariable String reviewId, Model model, Principal principal) {
        logger.info("Fetching review details for ID: {}", reviewId);

        // Add current user to model if authenticated
        if (principal != null) {
            model.addAttribute("currentUser", principal.getName());
            logger.debug("User {} accessing review details for {}", principal.getName(), reviewId);
        }

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

    @PostMapping("/update/{reviewId}")
    public Mono<String> updateReview(@PathVariable String reviewId,
                                     @Valid @ModelAttribute ReviewRequestDto requestDto,
                                     BindingResult bindingResult, Model model, Principal principal) {
        logger.info("Attempting to update review ID: {}", reviewId);

        // Check if user is authenticated
        if (principal == null) {
            logger.warn("Unauthorized attempt to update review {} - user not authenticated", reviewId);
            return Mono.just("redirect:/login");
        }

        model.addAttribute("currentUser", principal.getName());

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
    public Mono<String> deleteReview(@PathVariable String reviewId, Principal principal) {
        logger.info("Attempting to delete review ID: {}", reviewId);

        // Check if user is authenticated
        if (principal == null) {
            logger.warn("Unauthorized attempt to delete review {} - user not authenticated", reviewId);
            return Mono.just("redirect:/reviews");
        }

        return reviewService.deleteReview(reviewId)
                .doOnSuccess(unused -> logger.info("Successfully deleted review ID: {}", reviewId))
                .doOnError(error -> logger.error("Failed to delete review {}: {}", reviewId, error.getMessage(), error))
                .then(Mono.just("redirect:/reviews"))
                .onErrorResume(error -> Mono.just("redirect:/reviews"));
    }
}