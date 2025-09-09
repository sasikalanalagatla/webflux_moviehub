package com.moviehub.review.controller;

import com.moviehub.review.dto.CastMemberDto;
import com.moviehub.review.dto.MovieRequestDto;
import com.moviehub.review.dto.MovieResponseDto;
import com.moviehub.review.dto.OttPlatformDto;
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

import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    public Mono<String> home(Model model, Principal principal) {
        logger.info("Accessing home page");

        // Add current user to model if authenticated
        if (principal != null) {
            model.addAttribute("currentUser", principal.getName());
            logger.debug("User {} accessing home page", principal.getName());
        } else {
            logger.debug("Anonymous user accessing home page");
        }

        return Mono.just("home");
    }

    @GetMapping("/all")
    public Mono<String> getAllMovies(Model model, Principal principal,
                                     @RequestParam(required = false) String search,
                                     @RequestParam(required = false) String genre,
                                     @RequestParam(required = false) Integer year,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "25") int size,
                                     @RequestParam(name = "rpage", defaultValue = "0") int releasedPage,
                                     @RequestParam(name = "upage", defaultValue = "0") int upcomingPage) {

        logger.info("Fetching all movies with filters - search: {}, genre: {}, year: {}, page: {}, size: {}",
                search, genre, year, page, size);

        return movieService.getALlMovies()
                .collectList()
                .doOnNext(movies -> {
                    // Add current user to model if authenticated
                    if (principal != null) {
                        model.addAttribute("currentUser", principal.getName());
                        logger.debug("User {} accessing movie list", principal.getName());
                    }

                    logger.debug("Retrieved {} total movies from database", movies.size());

                    // **FIX 1: Deduplicate movies by ID first**
                    Map<String, MovieResponseDto> uniqueMovies = new LinkedHashMap<>();
                    movies.forEach(movie -> {
                        if (movie != null && movie.getMovieId() != null) {
                            uniqueMovies.put(movie.getMovieId(), movie);
                        }
                    });

                    List<MovieResponseDto> deduplicatedMovies = new ArrayList<>(uniqueMovies.values());
                    logger.debug("After deduplication: {} unique movies", deduplicatedMovies.size());

                    // **FIX 2: Apply filters on deduplicated list**
                    List<MovieResponseDto> filteredMovies = deduplicatedMovies.stream()
                            .filter(movie -> matchesFilters(movie, search, genre, year))
                            .collect(Collectors.toList());

                    logger.debug("After filtering: {} movies match criteria", filteredMovies.size());

                    // **FIX 3: Split into released and upcoming from filtered list**
                    List<MovieResponseDto> released = filteredMovies.stream()
                            .filter(m -> Boolean.TRUE.equals(m.getReleased()))
                            .sorted((a, b) -> b.getReleaseYear().compareTo(a.getReleaseYear())) // Latest first
                            .collect(Collectors.toList());

                    List<MovieResponseDto> upcoming = filteredMovies.stream()
                            .filter(m -> !Boolean.TRUE.equals(m.getReleased()))
                            .sorted((a, b) -> a.getReleaseYear().compareTo(b.getReleaseYear())) // Earliest first
                            .collect(Collectors.toList());

                    // **FIX 4: Create combined list without duplicates**
                    List<MovieResponseDto> allMoviesForDisplay = new ArrayList<>();
                    allMoviesForDisplay.addAll(upcoming); // Show upcoming first
                    allMoviesForDisplay.addAll(released); // Then released

                    logger.debug("Movies categorized - released: {}, upcoming: {}, total for display: {}",
                            released.size(), upcoming.size(), allMoviesForDisplay.size());

                    // **FIX 5: Use separate pagination for each category**
                    addPaginationAttributes(model, allMoviesForDisplay, page, size, "");
                    addPaginationAttributes(model, released, releasedPage, size, "released");
                    addPaginationAttributes(model, upcoming, upcomingPage, size, "upcoming");

                    // Add filter attributes
                    model.addAttribute("searchQuery", trimString(search));
                    model.addAttribute("genreFilter", trimString(genre));
                    model.addAttribute("yearFilter", year);
                })
                .doOnError(error -> logger.error("Error fetching movies: {}", error.getMessage(), error))
                .thenReturn("movie-list");
    }

    @GetMapping("/add")
    public Mono<String> showAddForm(Model model, Principal principal) {
        logger.info("Displaying add movie form");

        // Add current user to model if authenticated
        if (principal != null) {
            model.addAttribute("currentUser", principal.getName());
            logger.debug("User {} accessing add movie form", principal.getName());
        }

        model.addAttribute("movie", new MovieRequestDto());
        return Mono.just("movie-form");
    }

    @GetMapping("/edit/{movieId}")
    public Mono<String> showEditForm(@PathVariable String movieId, Model model, Principal principal) {
        logger.info("Displaying edit form for movie ID: {}", movieId);

        // Add current user to model if authenticated
        if (principal != null) {
            model.addAttribute("currentUser", principal.getName());
            logger.debug("User {} accessing edit form for movie {}", principal.getName(), movieId);
        }

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
    public Mono<String> getMovieById(@PathVariable String movieId, Model model, Principal principal) {
        logger.info("Fetching movie details for ID: {}", movieId);

        // Add current user to model if authenticated
        if (principal != null) {
            model.addAttribute("currentUser", principal.getName());
            logger.debug("User {} accessing movie details for {}", principal.getName(), movieId);
        }

        return movieService.getMovieById(movieId)
                .doOnNext(movie -> {
                    logger.debug("Retrieved movie: {}", movie.getTitle());
                    model.addAttribute("movie", movie);

                    // **ADD THIS CREW DEBUGGING**
                    if (movie.getCrew() != null) {
                        logger.info("CONTROLLER - Crew data exists for movie: {}", movie.getTitle());
                        logger.info("CONTROLLER - Directors: {}",
                                movie.getCrew().getDirectors() != null ? movie.getCrew().getDirectors().size() : "null");
                        logger.info("CONTROLLER - Producers: {}",
                                movie.getCrew().getProducers() != null ? movie.getCrew().getProducers().size() : "null");
                        logger.info("CONTROLLER - Writers: {}",
                                movie.getCrew().getWriters() != null ? movie.getCrew().getWriters().size() : "null");
                        logger.info("CONTROLLER - Music Directors: {}",
                                movie.getCrew().getMusicDirectors() != null ? movie.getCrew().getMusicDirectors().size() : "null");
                    } else {
                        logger.warn("CONTROLLER - Movie {} has NULL crew data", movie.getTitle());
                    }

                    // Add cast filtering (existing code)
                    if (movie.getCast() != null && !movie.getCast().isEmpty()) {
                        model.addAttribute("heroes", movie.getCast().stream()
                                .filter(c -> "Hero".equals(c.getRole())).collect(Collectors.toList()));
                        model.addAttribute("heroines", movie.getCast().stream()
                                .filter(c -> "Heroine".equals(c.getRole())).collect(Collectors.toList()));
                        model.addAttribute("supportingCast", movie.getCast().stream()
                                .filter(c -> "Supporting".equals(c.getRole())).collect(Collectors.toList()));
                    } else {
                        model.addAttribute("heroes", List.of());
                        model.addAttribute("heroines", List.of());
                        model.addAttribute("supportingCast", List.of());
                    }

                    // Add OTT platform grouping (existing code)
                    if (movie.getOttPlatforms() != null && !movie.getOttPlatforms().isEmpty()) {
                        Map<String, List<OttPlatformDto>> platformsByType = movie.getOttPlatforms().stream()
                                .collect(Collectors.groupingBy(OttPlatformDto::getSubscriptionType));
                        model.addAttribute("ottPlatforms", platformsByType);
                    } else {
                        model.addAttribute("ottPlatforms", Map.of());
                    }
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
                                    BindingResult bindingResult, Model model, Principal principal) {
        logger.info("Attempting to create new movie: {}", movieRequestDto.getTitle());

        // Add current user to model if authenticated (for error cases)
        if (principal != null) {
            model.addAttribute("currentUser", principal.getName());
        }

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
                                    BindingResult bindingResult, Model model, Principal principal) {
        logger.info("Attempting to update movie ID: {}", movieId);

        // Add current user to model if authenticated (for error cases)
        if (principal != null) {
            model.addAttribute("currentUser", principal.getName());
        }

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

    @GetMapping("/{movieId}/detailed")
    public Mono<String> getDetailedMovieById(@PathVariable String movieId, Model model, Principal principal) {
        logger.info("Fetching detailed movie information for ID: {}", movieId);

        // Add current user to model if authenticated
        if (principal != null) {
            model.addAttribute("currentUser", principal.getName());
            logger.debug("User {} accessing detailed movie view for {}", principal.getName(), movieId);
        }

        return movieService.getMovieById(movieId)
                .doOnNext(movie -> {
                    logger.debug("Retrieved movie: {}", movie.getTitle());
                    model.addAttribute("movie", movie);

                    // Add basic cast information if available
                    if (movie.getCast() != null && !movie.getCast().isEmpty()) {
                        List<CastMemberDto> heroes = movie.getCast().stream()
                                .filter(c -> "Hero".equals(c.getRole()))
                                .collect(Collectors.toList());
                        List<CastMemberDto> heroines = movie.getCast().stream()
                                .filter(c -> "Heroine".equals(c.getRole()))
                                .collect(Collectors.toList());
                        List<CastMemberDto> supporting = movie.getCast().stream()
                                .filter(c -> "Supporting".equals(c.getRole()))
                                .collect(Collectors.toList());

                        model.addAttribute("heroes", heroes);
                        model.addAttribute("heroines", heroines);
                        model.addAttribute("supportingCast", supporting);
                    }

                    // Add OTT platform information if available
                    if (movie.getOttPlatforms() != null && !movie.getOttPlatforms().isEmpty()) {
                        Map<String, List<OttPlatformDto>> platformsByType = movie.getOttPlatforms().stream()
                                .collect(Collectors.groupingBy(OttPlatformDto::getSubscriptionType));
                        model.addAttribute("ottPlatforms", platformsByType);
                    }
                })
                .flatMap(movie -> reviewService.getReviewsByMovieId(movieId)
                        .collectList()
                        .doOnNext(reviews -> {
                            logger.debug("Retrieved {} reviews for movie {}", reviews.size(), movieId);
                            model.addAttribute("reviews", reviews);
                        }))
                .then(Mono.just("movie-detailed"))
                .onErrorResume(error -> {
                    logger.error("Error fetching detailed movie {}: {}", movieId, error.getMessage(), error);
                    return handleError(model, "Movie not found: " + error.getMessage());
                });
    }

    @PostMapping("/{movieId}/enrich")
    public Mono<String> enrichMovieWithTmdbData(@PathVariable String movieId) {
        logger.info("Manually enriching movie {} with TMDb data", movieId);

        return movieService.getMovieById(movieId)
                .doOnNext(movie -> logger.debug("Found movie to enrich: {}", movie.getTitle()))
                .flatMap(movie -> {
                    // For now, we'll just update the movie with some mock enriched data
                    // Later you can implement actual TMDb integration
                    return movieService.updateMovieRating(movieId, movie.getAverageRating());
                })
                .doOnSuccess(enrichedMovie -> logger.info("Successfully enriched movie: {}", enrichedMovie.getTitle()))
                .then(Mono.just("redirect:/movie/" + movieId + "/detailed"))
                .onErrorResume(error -> {
                    logger.error("Failed to enrich movie {}: {}", movieId, error.getMessage(), error);
                    return Mono.just("redirect:/movie/all?error=enrichment-failed");
                });
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
            String genreString = String.valueOf(movie.getGenre());
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