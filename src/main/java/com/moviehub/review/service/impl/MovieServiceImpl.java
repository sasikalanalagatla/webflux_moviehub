package com.moviehub.review.service.impl;

import com.moviehub.review.dto.MovieRequestDto;
import com.moviehub.review.dto.MovieResponseDto;
import com.moviehub.review.exception.MovieNotFoundException;
import com.moviehub.review.mapper.MovieMapper;
import com.moviehub.review.model.Movie;
import com.moviehub.review.repository.MovieRepository;
import com.moviehub.review.service.MovieService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class MovieServiceImpl implements MovieService {

    private static final Logger logger = LoggerFactory.getLogger(MovieServiceImpl.class);

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Value("${tmdb.api.base:https://api.themoviedb.org/3}")
    private String tmdbBaseUrl;

    @Value("${tmdb.api.key:}")
    private String tmdbApiKey;

    public Mono<MovieResponseDto> createMovie(MovieRequestDto movieRequestDto) {
        logger.info("Creating movie: {}", movieRequestDto.getTitle());

        Movie movie = MovieMapper.toEntity(movieRequestDto);
        movie.setAverageRating(0.0);

        LocalDate today = LocalDate.now();
        LocalDate releaseDate;

        if (movie.getReleaseDate() == null) {
            releaseDate = LocalDate.of(movieRequestDto.getReleaseYear(), 1, 1);
            movie.setReleaseDate(releaseDate);
            logger.debug("Set default release date for movie {}: {}", movieRequestDto.getTitle(), releaseDate);
        } else {
            releaseDate = movie.getReleaseDate();
        }

        boolean isReleased = releaseDate.isBefore(today) || releaseDate.isEqual(today);
        movie.setReleased(isReleased);
        logger.debug("Movie {} release status: {}", movieRequestDto.getTitle(), isReleased ? "Released" : "Upcoming");

        return movieRepository.save(movie)
                .doOnSuccess(savedMovie -> logger.info("Successfully created movie: {} with ID: {}", savedMovie.getTitle(), savedMovie.getMovieId()))
                .doOnError(error -> logger.error("Failed to create movie {}: {}", movieRequestDto.getTitle(), error.getMessage(), error))
                .map(MovieMapper::toDto);
    }

    @Override
    public Mono<MovieResponseDto> getMovieById(String movieId) {
        logger.info("Fetching movie by ID: {}", movieId);

        return movieRepository.findById(movieId)
                .switchIfEmpty(Mono.error(new MovieNotFoundException("Movie not found with movieId: " + movieId)))
                .doOnNext(movie -> logger.debug("Retrieved movie: {}", movie.getTitle()))
                .doOnError(error -> logger.error("Error fetching movie {}: {}", movieId, error.getMessage()))
                .map(MovieMapper::toDto);
    }

    @Override
    public Flux<MovieResponseDto> getALlMovies() {
        logger.info("Fetching all movies");

        return movieRepository.findAll()
                .doOnComplete(() -> logger.debug("Completed fetching all movies"))
                .doOnError(error -> logger.error("Error fetching all movies: {}", error.getMessage(), error))
                .map(MovieMapper::toDto);
    }

    @Override
    public Mono<MovieResponseDto> updateMovie(String movieId, MovieRequestDto movieRequestDto) {
        logger.info("Updating movie ID: {} with title: {}", movieId, movieRequestDto.getTitle());

        return movieRepository.findById(movieId)
                .switchIfEmpty(Mono.error(new MovieNotFoundException("Movie not found with movieId: " + movieId)))
                .flatMap(existingMovie -> {
                    logger.debug("Found existing movie: {}", existingMovie.getTitle());
                    Movie updatedMovie = MovieMapper.toEntity(movieRequestDto);

                    existingMovie.setTitle(updatedMovie.getTitle());
                    existingMovie.setGenre(updatedMovie.getGenre());
                    existingMovie.setReleaseYear(updatedMovie.getReleaseYear());

                    LocalDate today = LocalDate.now();
                    LocalDate releaseDate = LocalDate.of(movieRequestDto.getReleaseYear(), 1, 1);
                    existingMovie.setReleaseDate(releaseDate);

                    boolean isReleased = releaseDate.isBefore(today) || releaseDate.isEqual(today);
                    existingMovie.setReleased(isReleased);

                    logger.debug("Updated movie {} release status: {}", movieRequestDto.getTitle(), isReleased ? "Released" : "Upcoming");

                    return movieRepository.save(existingMovie);
                })
                .doOnSuccess(movie -> logger.info("Successfully updated movie: {} with ID: {}", movie.getTitle(), movieId))
                .doOnError(error -> logger.error("Failed to update movie {}: {}", movieId, error.getMessage(), error))
                .map(MovieMapper::toDto);
    }

    @Override
    public Mono<Void> deleteMovie(String movieId) {
        logger.info("Deleting movie ID: {}", movieId);

        return movieRepository.findById(movieId)
                .switchIfEmpty(Mono.error(new MovieNotFoundException("Movie not found with movieId: " + movieId)))
                .doOnNext(movie -> logger.debug("Found movie to delete: {}", movie.getTitle()))
                .flatMap(movie -> movieRepository.deleteById(movieId))
                .doOnSuccess(unused -> logger.info("Successfully deleted movie ID: {}", movieId))
                .doOnError(error -> logger.error("Failed to delete movie {}: {}", movieId, error.getMessage(), error));
    }

    @Override
    public Mono<MovieResponseDto> updateMovieRating(String movieId, Double newRating) {
        logger.info("Updating rating for movie ID: {} to {}", movieId, newRating);

        return movieRepository.findById(movieId)
                .switchIfEmpty(Mono.error(new MovieNotFoundException("Movie not found with movieId: " + movieId)))
                .flatMap(movie -> {
                    logger.debug("Updating rating for movie: {} from {} to {}", movie.getTitle(), movie.getAverageRating(), newRating);
                    movie.setAverageRating(newRating);
                    return movieRepository.save(movie);
                })
                .doOnSuccess(movie -> logger.info("Successfully updated rating for movie: {}", movie.getTitle()))
                .doOnError(error -> logger.error("Failed to update rating for movie {}: {}", movieId, error.getMessage(), error))
                .map(MovieMapper::toDto);
    }

    @Override
    public Flux<MovieResponseDto> findMoviesByGenre(String genre) {
        logger.info("Finding movies by genre: {}", genre);

        return movieRepository.findAll()
                .filter(movie -> movie.getGenre() != null &&
                        movie.getGenre().stream().anyMatch(g ->
                                g.toLowerCase().contains(genre.toLowerCase())))
                .doOnComplete(() -> logger.debug("Completed genre search for: {}", genre))
                .doOnError(error -> logger.error("Error finding movies by genre {}: {}", genre, error.getMessage(), error))
                .map(MovieMapper::toDto);
    }

    @Override
    public Mono<MovieResponseDto> createMovieFromTmdbSearch(String query, Integer year) {
        logger.info("Creating movie from TMDB search - query: {}, year: {}", query, year);

        MovieRequestDto dto = new MovieRequestDto();
        dto.setTitle(query);
        dto.setGenre("Telugu");
        dto.setReleaseYear(year != null ? year : LocalDate.now().getYear());

        return createMovie(dto);
    }

    @Scheduled(cron = "0 * * * * *")
    public void syncTeluguMoviesDaily() {
        if (tmdbApiKey == null || tmdbApiKey.isBlank()) {
            logger.warn("TMDb API key not configured, skipping Telugu movie sync");
            return;
        }

        logger.info("Starting comprehensive Telugu movie sync from 1990 to future");

        WebClient client = webClientBuilder.baseUrl(tmdbBaseUrl).build();

        int currentYear = LocalDate.now().getYear();
        int startYear = 1990;
        int endYear = currentYear + 10;

        List<Integer> yearsToSync = new java.util.ArrayList<>();
        for (int year = startYear; year <= endYear; year++) {
            yearsToSync.add(year);
        }

        logger.info("Syncing Telugu movies from {} to {} ({} years)", startYear, endYear, yearsToSync.size());

        int batchSize = 5;
        Flux.range(0, (int) Math.ceil(yearsToSync.size() / (double) batchSize))
                .concatMap(batchIndex -> {
                    int startIndex = batchIndex * batchSize;
                    int endIndex = Math.min(startIndex + batchSize, yearsToSync.size());
                    List<Integer> batchYears = yearsToSync.subList(startIndex, endIndex);

                    logger.debug("Processing batch {}: {}", batchIndex + 1, batchYears);

                    return Flux.fromIterable(batchYears)
                            .concatMap(year -> syncAllTeluguMoviesByYear(client, year)
                                    .delayElement(java.time.Duration.ofMillis(250)))
                            .then();
                })
                .subscribe(
                        null,
                        error -> logger.error("Telugu movie sync error: {}", error.getMessage(), error),
                        () -> logger.info("Complete Telugu movie sync finished! (1990-{})", endYear)
                );
    }

    private Mono<Void> syncAllTeluguMoviesByYear(WebClient client, int year) {
        return fetchTeluguMoviesPage(client, year, 1)
                .flatMap(firstPageResponse -> {
                    int totalPages = (int) firstPageResponse.getOrDefault("total_pages", 1);
                    int totalResults = (int) firstPageResponse.getOrDefault("total_results", 0);

                    logger.debug("Year {}: Found {} Telugu movies across {} pages", year, totalResults, totalPages);

                    if (totalResults == 0) {
                        logger.debug("Year {}: No Telugu movies found, skipping", year);
                        return Mono.empty();
                    }

                    List<Map<String, Object>> firstPageResults =
                            (List<Map<String, Object>>) firstPageResponse.get("results");

                    Flux<Void> firstPageFlux = Flux.fromIterable(firstPageResults != null ? firstPageResults : List.of())
                            .flatMap(this::saveTeluguMovieFromTmdb);

                    Flux<Void> remainingPagesFlux = Flux.range(2, Math.max(0, totalPages - 1))
                            .concatMap(pageNum ->
                                    fetchTeluguMoviesPage(client, year, pageNum)
                                            .delayElement(java.time.Duration.ofMillis(100))
                                            .flatMapMany(response -> {
                                                List<Map<String, Object>> results =
                                                        (List<Map<String, Object>>) response.get("results");
                                                return Flux.fromIterable(results != null ? results : List.of());
                                            })
                                            .flatMap(this::saveTeluguMovieFromTmdb)
                            );

                    return Flux.concat(firstPageFlux, remainingPagesFlux)
                            .then()
                            .doOnSuccess(v -> logger.debug("Year {} completed", year));
                })
                .onErrorResume(error -> {
                    logger.error("Error processing year {}: {}", year, error.getMessage(), error);
                    return Mono.empty();
                });
    }

    private Mono<Map<String, Object>> fetchTeluguMoviesPage(WebClient client, int year, int page) {
        return client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/discover/movie")
                        .queryParam("api_key", tmdbApiKey)
                        .queryParam("with_original_language", "te")
                        .queryParam("region", "IN")
                        .queryParam("primary_release_year", year)
                        .queryParam("sort_by", "popularity.desc")
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(java.time.Duration.ofSeconds(10))
                .doOnNext(response -> {
                    int currentPage = (int) response.getOrDefault("page", page);
                    int totalPages = (int) response.getOrDefault("total_pages", 1);
                    List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
                    int resultsCount = results != null ? results.size() : 0;

                    if (resultsCount > 0) {
                        logger.debug("Year {} - Page {}/{} - {} movies", year, currentPage, totalPages, resultsCount);
                    }
                })
                .onErrorResume(error -> {
                    logger.warn("Error fetching year {} page {}: {}", year, page, error.getMessage());
                    return Mono.just(new java.util.HashMap<>());
                });
    }

    private Mono<Void> saveTeluguMovieFromTmdb(Map<String, Object> movieData) {
        try {
            String title = (String) movieData.get("title");
            String originalTitle = (String) movieData.get("original_title");
            String releaseDate = (String) movieData.get("release_date");

            String movieTitle = originalTitle != null ? originalTitle : title;

            if (movieTitle == null || movieTitle.isBlank()) {
                return Mono.empty();
            }

            return movieRepository.findByTitleIgnoreCase(movieTitle)
                    .hasElement()
                    .flatMap(exists -> {
                        if (exists) {
                            logger.debug("Movie already exists: {}", movieTitle);
                            return Mono.empty();
                        }

                        Movie movie = new Movie();
                        movie.setTitle(movieTitle);
                        movie.setAverageRating(0.0);

                        LocalDate today = LocalDate.now();
                        String decade = "";

                        if (releaseDate != null && releaseDate.length() >= 4) {
                            try {
                                LocalDate parsedReleaseDate = LocalDate.parse(releaseDate);
                                int year = parsedReleaseDate.getYear();

                                movie.setReleaseYear(year);
                                movie.setReleaseDate(parsedReleaseDate);

                                boolean isReleased = parsedReleaseDate.isBefore(today) || parsedReleaseDate.isEqual(today);
                                movie.setReleased(isReleased);

                                decade = (year / 10) * 10 + "s";

                            } catch (Exception e) {
                                logger.warn("Error parsing release date for movie {}: {}", movieTitle, e.getMessage());
                                movie.setReleaseYear(today.getYear());
                                movie.setReleaseDate(today);
                                movie.setReleased(true);
                                decade = "Unknown";
                            }
                        }

                        movie.setGenre(List.of("Telugu", "Indian Cinema"));

                        String status = movie.getReleased() ? "Released" : "Upcoming";
                        logger.debug("Adding {} {} Telugu movie: {}", decade, status, movieTitle);

                        return movieRepository.save(movie)
                                .doOnSuccess(savedMovie -> logger.debug("Successfully saved Telugu movie: {}", movieTitle))
                                .then();
                    });

        } catch (Exception e) {
            logger.error("Error processing Telugu movie: {}", e.getMessage(), e);
            return Mono.empty();
        }
    }
}
