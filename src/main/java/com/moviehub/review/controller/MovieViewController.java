package com.moviehub.review.controller;

import com.moviehub.review.dto.MovieRequestDto;
import com.moviehub.review.dto.MovieResponseDto;
import com.moviehub.review.service.MovieService;
import com.moviehub.review.service.ReviewService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/movie")
public class MovieViewController {

    private static final Logger logger = LoggerFactory.getLogger(MovieViewController.class);

    @Autowired
    private MovieService movieService;

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/")
    public Mono<String> home() {
        logger.info("Accessing home page");
        return Mono.just("home");
    }

    @GetMapping("/all")
    public Mono<String> getAllMovies(Model model,
                                     @RequestParam(required = false) String search,
                                     @RequestParam(required = false) String genre,
                                     @RequestParam(required = false) Integer year,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @RequestParam(name = "rpage", defaultValue = "0") int releasedPage,
                                     @RequestParam(name = "upage", defaultValue = "0") int upcomingPage) {

        logger.info("Fetching all movies with filters - search: {}, genre: {}, year: {}, page: {}, size: {}",
                search, genre, year, page, size);

        return movieService.getALlMovies()
                .filter(movie -> matchesFilters(movie, search, genre, year))
                .collectList()
                .doOnNext(movies -> {
                    logger.debug("Retrieved {} movies after filtering", movies.size());

                    List<MovieResponseDto> released = movies.stream()
                            .filter(m -> Boolean.TRUE.equals(m.getReleased()))
                            .collect(Collectors.toList());

                    List<MovieResponseDto> upcoming = movies.stream()
                            .filter(m -> !Boolean.TRUE.equals(m.getReleased()))
                            .collect(Collectors.toList());

                    logger.debug("Movies categorized - released: {}, upcoming: {}", released.size(), upcoming.size());

                    List<MovieResponseDto> allMovies = List.of(upcoming, released)
                            .stream()
                            .flatMap(List::stream)
                            .collect(Collectors.toList());

                    addPaginationAttributes(model, allMovies, page, size, "");
                    addPaginationAttributes(model, released, releasedPage, size, "released");
                    addPaginationAttributes(model, upcoming, upcomingPage, size, "upcoming");

                    model.addAttribute("searchQuery", trimString(search));
                    model.addAttribute("genreFilter", trimString(genre));
                    model.addAttribute("yearFilter", year);
                })
                .doOnError(error -> logger.error("Error fetching movies: {}", error.getMessage(), error))
                .thenReturn("movie-list");
    }

    @GetMapping("/add")
    public Mono<String> showAddForm(Model model) {
        logger.info("Displaying add movie form");
        model.addAttribute("movie", new MovieRequestDto());
        return Mono.just("movie-form");
    }

    @GetMapping("/edit/{movieId}")
    public Mono<String> showEditForm(@PathVariable String movieId, Model model) {
        logger.info("Displaying edit form for movie ID: {}", movieId);

        return movieService.getMovieById(movieId)
                .doOnNext(movie -> {
                    logger.debug("Retrieved movie for editing: {}", movie.getTitle());
                    model.addAttribute("movie", movie);
                })
                .thenReturn("movie-edit-form")
                .onErrorResume(error -> {
                    logger.error("Error retrieving movie {} for editing: {}", movieId, error.getMessage(), error);
                    return handleError(model, "Movie not found: " + error.getMessage());
                });
    }

    @GetMapping("/{movieId}")
    public Mono<String> getMovieById(@PathVariable String movieId, Model model) {
        logger.info("Fetching movie details for ID: {}", movieId);

        return movieService.getMovieById(movieId)
                .doOnNext(movie -> {
                    logger.debug("Retrieved movie: {}", movie.getTitle());
                    model.addAttribute("movie", movie);
                })
                .flatMap(movie -> reviewService.getReviewsByMovieId(movieId)
                        .collectList()
                        .doOnNext(reviews -> {
                            logger.debug("Retrieved {} reviews for movie {}", reviews.size(), movieId);
                            model.addAttribute("reviews", reviews);
                        }))
                .thenReturn("movie-detail")
                .onErrorResume(error -> {
                    logger.error("Error fetching movie {}: {}", movieId, error.getMessage(), error);
                    return handleError(model, "Movie not found: " + error.getMessage());
                });
    }

    @PostMapping("/save")
    public Mono<String> createMovie(@Valid @ModelAttribute MovieRequestDto movieRequestDto,
                                    BindingResult bindingResult, Model model) {
        logger.info("Attempting to create new movie: {}", movieRequestDto.getTitle());

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors while creating movie: {}", bindingResult.getAllErrors());
            model.addAttribute("movie", movieRequestDto);
            return Mono.just("movie-form");
        }

        return movieService.createMovie(movieRequestDto)
                .doOnSuccess(movie -> logger.info("Successfully created movie: {}", movieRequestDto.getTitle()))
                .then(Mono.just("redirect:/movie/all"))
                .onErrorResume(error -> {
                    logger.error("Failed to create movie {}: {}", movieRequestDto.getTitle(), error.getMessage(), error);
                    model.addAttribute("error", "Failed to create movie: " + error.getMessage());
                    model.addAttribute("movie", movieRequestDto);
                    return Mono.just("movie-form");
                });
    }

    @PostMapping("/update/{movieId}")
    public Mono<String> updateMovie(@PathVariable String movieId,
                                    @Valid @ModelAttribute MovieRequestDto movieRequestDto,
                                    BindingResult bindingResult, Model model) {
        logger.info("Attempting to update movie ID: {}", movieId);

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors while updating movie {}: {}", movieId, bindingResult.getAllErrors());
            return movieService.getMovieById(movieId)
                    .doOnNext(movie -> {
                        model.addAttribute("movie", movie);
                        model.addAttribute("errors", bindingResult.getAllErrors());
                    })
                    .thenReturn("movie-edit-form")
                    .onErrorReturn("movies/error");
        }

        return movieService.updateMovie(movieId, movieRequestDto)
                .doOnSuccess(movie -> logger.info("Successfully updated movie ID: {}", movieId))
                .then(Mono.just("redirect:/movie/all"))
                .onErrorResume(error -> {
                    logger.error("Failed to update movie {}: {}", movieId, error.getMessage(), error);
                    return movieService.getMovieById(movieId)
                            .doOnNext(movie -> {
                                model.addAttribute("movie", movie);
                                model.addAttribute("error", "Failed to update movie: " + error.getMessage());
                            })
                            .thenReturn("movie-edit-form")
                            .onErrorReturn("movies/error");
                });
    }

    @PostMapping("/delete/{movieId}")
    public Mono<String> deleteMovie(@PathVariable String movieId) {
        logger.info("Attempting to delete movie ID: {}", movieId);

        return movieService.deleteMovie(movieId)
                .doOnSuccess(unused -> logger.info("Successfully deleted movie ID: {}", movieId))
                .doOnError(error -> logger.error("Failed to delete movie {}: {}", movieId, error.getMessage(), error))
                .then(Mono.just("redirect:/movie/all"))
                .onErrorReturn("redirect:/movie/all");
    }

    private boolean matchesFilters(MovieResponseDto movie, String search, String genre, Integer year) {
        String trimmedSearch = trimString(search);
        String trimmedGenre = trimString(genre);

        if (trimmedSearch != null) {
            String title = movie.getTitle() == null ? "" : movie.getTitle();
            if (!title.toLowerCase().contains(trimmedSearch.toLowerCase())) {
                return false;
            }
        }

        if (trimmedGenre != null) {
            String genreString = movie.getGenre();
            if (genreString == null || genreString.trim().isEmpty() ||
                    !genreString.toLowerCase().contains(trimmedGenre.toLowerCase())) {
                return false;
            }
        }

        return year == null || year.equals(movie.getReleaseYear());
    }

    private void addPaginationAttributes(Model model, List<MovieResponseDto> movies, int page, int size, String prefix) {
        int total = movies.size();
        int totalPages = (int) Math.ceil(total / (double) size);
        int safePage = Math.max(0, Math.min(page, Math.max(0, totalPages - 1)));

        int from = safePage * size;
        int to = Math.min(total, from + size);
        List<MovieResponseDto> pagedMovies = movies.subList(from, to);

        logger.debug("Pagination for {}: total={}, page={}, size={}, totalPages={}",
                prefix.isEmpty() ? "all" : prefix, total, safePage, size, totalPages);

        model.addAttribute(prefix + "Movies", movies);
        model.addAttribute(prefix + (prefix.isEmpty() ? "pagedMovies" : "Paged"), pagedMovies);
        model.addAttribute(prefix + (prefix.isEmpty() ? "page" : "Page"), safePage);
        model.addAttribute(prefix + (prefix.isEmpty() ? "totalPages" : "TotalPages"), totalPages);

        if (prefix.isEmpty()) {
            model.addAttribute("size", size);
            model.addAttribute("total", total);
        }
    }

    private String trimString(String str) {
        return (str == null || str.trim().isEmpty()) ? null : str.trim();
    }

    private Mono<String> handleError(Model model, String errorMessage) {
        logger.error("Handling error: {}", errorMessage);
        model.addAttribute("error", errorMessage);
        return Mono.just("movies/error");
    }
}
