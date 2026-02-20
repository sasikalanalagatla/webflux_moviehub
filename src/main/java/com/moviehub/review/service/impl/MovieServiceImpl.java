package com.moviehub.review.service.impl;

import com.moviehub.review.dto.MovieRequestDto;
import com.moviehub.review.dto.MovieResponseDto;
import com.moviehub.review.exception.MovieNotFoundException;
import com.moviehub.review.mapper.MovieMapper;
import com.moviehub.review.model.*;
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
import reactor.util.retry.Retry;

import java.io.IOException;
import java.net.SocketException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

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

    @Override
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

        if (movie.getCrew() == null) {
            movie.setCrew(new CrewInfo());
        }

        logger.debug("Movie {} release status: {}", movieRequestDto.getTitle(), isReleased ? "Released" : "Upcoming");

        return movieRepository.save(movie)
                .doOnSuccess(savedMovie -> logger.info("Successfully created movie: {} with ID: {}",
                        savedMovie.getTitle(), savedMovie.getMovieId()))
                .doOnError(error -> logger.error("Failed to create movie {}: {}",
                        movieRequestDto.getTitle(), error.getMessage(), error))
                .map(MovieMapper::toDto);
    }

    @Override
    public Mono<MovieResponseDto> getMovieById(String movieId) {
        logger.info("Fetching movie by ID: {}", movieId);

        return movieRepository.findById(movieId)
                .switchIfEmpty(Mono.error(new MovieNotFoundException("Movie not found with movieId: " + movieId)))
                .doOnNext(movie -> logger.debug("Retrieved movie: {}", movie.getTitle()))
                .doOnError(error -> logger.error("Error fetching movie {}: {}", movieId, error.getMessage()))
                .map(this::ensureCrewInfoExists)
                .map(MovieMapper::toDto);
    }

    @Override
    public Flux<MovieResponseDto> getALlMovies() {
        logger.info("Fetching all movies");

        return movieRepository.findAll()
                .doOnComplete(() -> logger.debug("Completed fetching all movies"))
                .doOnError(error -> logger.error("Error fetching all movies: {}", error.getMessage(), error))
                .map(this::ensureCrewInfoExists)
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

                    if (existingMovie.getCrew() == null) {
                        existingMovie.setCrew(new CrewInfo());
                    }

                    logger.debug("Updated movie {} release status: {}", movieRequestDto.getTitle(),
                            isReleased ? "Released" : "Upcoming");

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
                    logger.debug("Updating rating for movie: {} from {} to {}",
                            movie.getTitle(), movie.getAverageRating(), newRating);
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
                .map(this::ensureCrewInfoExists)
                .map(MovieMapper::toDto);
    }

    @Override
    public Mono<MovieResponseDto> createMovieFromTmdbSearch(String query, Integer year) {
        logger.info("Creating movie from TMDB search - query: {}, year: {}", query, year);

        MovieRequestDto dto = new MovieRequestDto();
        dto.setTitle(query);
        dto.setGenre(List.of("Telugu"));
        dto.setReleaseYear(year != null ? year : LocalDate.now().getYear());

        return createMovie(dto);
    }

    @Scheduled(cron = "0 0 0 * * *") //daily sync up at mid night 12
    public void syncTeluguMoviesDaily() {
        if (tmdbApiKey == null || tmdbApiKey.isBlank()) {
            logger.warn("TMDb API key not configured, skipping Telugu movie sync");
            return;
        }

        logger.info("Starting comprehensive Telugu movie sync from 1990 to future");

        WebClient client = webClientBuilder.baseUrl(tmdbBaseUrl).build();
        int currentYear = LocalDate.now().getYear();
        int startYear = 1990;
        int endYear = currentYear + 15;

        List<Integer> yearsToSync = new ArrayList<>();
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
                                    .delayElement(Duration.ofMillis(250)))
                            .then();
                })
                .subscribe(
                        null,
                        error -> logger.error("Telugu movie sync error: {}", error.getMessage(), error),
                        () -> logger.info("Complete Telugu movie sync finished! (1990-{})", endYear)
                );
    }

    @Override
    public Mono<MovieResponseDto> enrichMovieWithTmdbData(String movieId) {
        logger.info("Enriching movie {} with TMDb data", movieId);

        return movieRepository.findById(movieId)
                .switchIfEmpty(Mono.error(new MovieNotFoundException("Movie not found with movieId: " + movieId)))
                .doOnNext(movie -> logger.debug("Found movie to enrich: {}", movie.getTitle()))
                .flatMap(movie -> {
                    logger.info("Movie enrichment completed for: {}", movie.getTitle());
                    return Mono.just(movie);
                })
                .map(this::ensureCrewInfoExists)
                .map(MovieMapper::toDto)
                .doOnError(error -> logger.error("Error enriching movie {}: {}", movieId, error.getMessage(), error));
    }

    private Movie ensureCrewInfoExists(Movie movie) {
        if (movie.getCrew() == null) {
            movie.setCrew(createEmptyCrewInfo());
        }

        CrewInfo crew = movie.getCrew();
        if (crew.getDirectors() == null) crew.setDirectors(Collections.emptyList());
        if (crew.getProducers() == null) crew.setProducers(Collections.emptyList());
        if (crew.getWriters() == null) crew.setWriters(Collections.emptyList());
        if (crew.getMusicDirectors() == null) crew.setMusicDirectors(Collections.emptyList());
        if (crew.getCinematographers() == null) crew.setCinematographers(Collections.emptyList());
        if (crew.getEditors() == null) crew.setEditors(Collections.emptyList());

        return movie;
    }

    private CrewInfo createEmptyCrewInfo() {
        CrewInfo crewInfo = new CrewInfo();
        crewInfo.setDirectors(Collections.emptyList());
        crewInfo.setProducers(Collections.emptyList());
        crewInfo.setWriters(Collections.emptyList());
        crewInfo.setMusicDirectors(Collections.emptyList());
        crewInfo.setCinematographers(Collections.emptyList());
        crewInfo.setEditors(Collections.emptyList());
        return crewInfo;
    }

    private Movie createMovieFromTmdbData(Map<String, Object> tmdbData) {
        Movie movie = new Movie();

        movie.setTitle((String) tmdbData.get("original_title"));
        movie.setOverview((String) tmdbData.get("overview"));
        movie.setTmdbId(String.valueOf(tmdbData.get("id")));
        movie.setImdbId((String) tmdbData.get("imdb_id"));
        movie.setRuntime((Integer) tmdbData.get("runtime"));

        String posterPath = (String) tmdbData.get("poster_path");
        if (posterPath != null) {
            movie.setPosterUrl("https://image.tmdb.org/t/p/w500" + posterPath);
        }

        String backdropPath = (String) tmdbData.get("backdrop_path");
        if (backdropPath != null) {
            movie.setBackdropUrl("https://image.tmdb.org/t/p/w1280" + backdropPath);
        }

        String releaseDate = (String) tmdbData.get("release_date");
        if (releaseDate != null && !releaseDate.isEmpty()) {
            try {
                LocalDate parsedDate = LocalDate.parse(releaseDate);
                movie.setReleaseDate(parsedDate);
                movie.setReleaseYear(parsedDate.getYear());
                movie.setReleased(parsedDate.isBefore(LocalDate.now()) || parsedDate.isEqual(LocalDate.now()));
            } catch (Exception e) {
                logger.warn("Error parsing release date: {}", e.getMessage());
            }
        }

        Map<String, Object> credits = (Map<String, Object>) tmdbData.get("credits");
        if (credits != null) {
            movie.setCast(extractCastMembers(credits));
            movie.setCrew(extractCrewInfo(credits));
        } else {
            movie.setCast(Collections.emptyList());
            movie.setCrew(createEmptyCrewInfo());
        }

        Map<String, Object> watchProviders = (Map<String, Object>) tmdbData.get("watch/providers");
        if (watchProviders != null) {
            movie.setOttPlatforms(extractOttPlatforms(watchProviders));
        } else {
            movie.setOttPlatforms(Collections.emptyList());
        }

        movie.setGenre(List.of("Telugu", "Indian Cinema"));
        movie.setAverageRating(0.0);

        return movie;
    }

    private List<CastMember> extractCastMembers(Map<String, Object> credits) {
        List<Map<String, Object>> cast = (List<Map<String, Object>>) credits.get("cast");
        if (cast == null) return Collections.emptyList();

        return cast.stream()
                .limit(20)
                .map(castData -> {
                    CastMember member = new CastMember();
                    member.setName((String) castData.get("name"));
                    member.setCharacter((String) castData.get("character"));
                    member.setOrder((Integer) castData.get("order"));

                    String profilePath = (String) castData.get("profile_path");
                    if (profilePath != null) {
                        member.setProfileUrl("https://image.tmdb.org/t/p/w185" + profilePath);
                    }

                    Integer gender = (Integer) castData.get("gender");
                    Integer order = (Integer) castData.get("order");

                    if (gender != null && order != null) {
                        if (order <= 2) {
                            if (gender == 2) {
                                member.setRole("Hero");
                            } else if (gender == 1) {
                                member.setRole("Heroine");
                            } else {
                                member.setRole("Supporting");
                            }
                        } else if (order <= 10) {
                            member.setRole("Supporting");
                        } else {
                            member.setRole("Other");
                        }
                    } else {
                        member.setRole("Supporting");
                    }

                    return member;
                })
                .collect(Collectors.toList());
    }


    private CrewInfo extractCrewInfo(Map<String, Object> credits) {
        List<Map<String, Object>> crew = (List<Map<String, Object>>) credits.get("crew");
        if (crew == null) return createEmptyCrewInfo();

        CrewInfo crewInfo = new CrewInfo();

        Map<String, List<Map<String, Object>>> crewByDept = crew.stream()
                .collect(Collectors.groupingBy(c -> (String) c.get("department")));

        crewInfo.setDirectors(extractCrewByJob(crewByDept.get("Directing"), "Director"));
        crewInfo.setProducers(extractCrewByJob(crewByDept.get("Production"), "Producer"));
        crewInfo.setWriters(extractCrewByJob(crewByDept.get("Writing"), null));
        crewInfo.setMusicDirectors(extractCrewByJob(crewByDept.get("Sound"), "Music"));
        crewInfo.setCinematographers(extractCrewByJob(crewByDept.get("Camera"), "Director of Photography"));
        crewInfo.setEditors(extractCrewByJob(crewByDept.get("Editing"), "Editor"));

        return crewInfo;
    }

    private List<CrewMember> extractCrewByJob(List<Map<String, Object>> deptCrew, String jobFilter) {
        if (deptCrew == null) return Collections.emptyList();

        return deptCrew.stream()
                .filter(c -> jobFilter == null || ((String) c.get("job")).contains(jobFilter))
                .map(crewData -> {
                    CrewMember member = new CrewMember();
                    member.setName((String) crewData.get("name"));
                    member.setJob((String) crewData.get("job"));
                    member.setDepartment((String) crewData.get("department"));

                    String profilePath = (String) crewData.get("profile_path");
                    if (profilePath != null) {
                        member.setProfileUrl("https://image.tmdb.org/t/p/w185" + profilePath);
                    }

                    return member;
                })
                .collect(Collectors.toList());
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
                                            .delayElement(Duration.ofMillis(100))
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
                .timeout(Duration.ofSeconds(10))
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
                    return Mono.just(new HashMap<>());
                });
    }

    private Mono<Void> saveTeluguMovieFromTmdb(Map<String, Object> movieData) {
        try {
            String title = (String) movieData.get("title");
            String originalTitle = (String) movieData.get("original_title");
            String tmdbId = String.valueOf(movieData.get("id"));
            String movieTitle = originalTitle != null ? originalTitle : title;

            if (movieTitle == null || movieTitle.isBlank()) {
                logger.debug("Skipping movie with empty title: TMDb ID {}", tmdbId);
                return Mono.empty();
            }

            return Mono.zip(
                            movieRepository.findByTitleIgnoreCase(movieTitle).hasElement(),
                            movieRepository.findByTmdbId(tmdbId).hasElement()
                    )
                    .flatMap(tuple -> {
                        boolean titleExists = tuple.getT1();
                        boolean tmdbIdExists = tuple.getT2();

                        if (titleExists || tmdbIdExists) {
                            logger.debug("Movie already exists - Title: {}, TMDb ID: {}", movieTitle, tmdbId);
                            return Mono.empty();
                        }

                        return fetchCompleteMovieDataFromTmdb(tmdbId)
                                .flatMap(completeData -> {
                                    Movie movie = createMovieFromTmdbData(completeData);
                                    movie.setTmdbId(tmdbId);

                                    if (movie.getTitle() == null || movie.getTitle().isBlank()) {
                                        logger.warn("Created movie has null/empty title, skipping save for TMDb ID: {}", tmdbId);
                                        return Mono.empty();
                                    }

                                    return movieRepository.save(movie);
                                })
                                .doOnSuccess(saved -> {
                                    if (saved != null && saved.getTitle() != null) {
                                        logger.debug("Successfully saved new movie: {}", saved.getTitle());
                                    } else {
                                        logger.warn("Save operation returned null for movie with TMDb ID: {}", tmdbId);
                                    }
                                })
                                .doOnError(error -> {
                                    logger.error("Failed to save movie with TMDb ID {}: {}", tmdbId, error.getMessage());
                                })
                                .onErrorResume(error -> {
                                    logger.debug("Continuing sync despite error for TMDb ID: {}", tmdbId);
                                    return Mono.empty();
                                })
                                .then();
                    })
                    .onErrorResume(error -> {
                        logger.error("Error in duplicate check for TMDb ID {}: {}", tmdbId, error.getMessage());
                        return Mono.empty();
                    });
        } catch (Exception e) {
            logger.error("Error processing Telugu movie data: {}", e.getMessage(), e);
            return Mono.empty();
        }
    }

    private Mono<Map<String, Object>> fetchCompleteMovieDataFromTmdb(String tmdbId) {
        WebClient client = webClientBuilder
                .baseUrl(tmdbBaseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();

        return client.get()
                .uri("/movie/{id}?api_key={apiKey}&append_to_response=credits,watch/providers,keywords", tmdbId, tmdbApiKey)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(Duration.ofSeconds(15))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(throwable ->
                                throwable instanceof SocketException ||
                                        throwable instanceof TimeoutException ||
                                        throwable instanceof IOException)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            logger.warn("Max retries exceeded for movie {}", tmdbId);
                            return new RuntimeException("Failed to fetch movie data after retries");
                        }))
                .doOnNext(data -> {
                    logger.debug("Successfully fetched TMDb data for movie ID: {}", tmdbId);
                })
                .onErrorResume(error -> {
                    logger.warn("Failed to fetch TMDb data for movie {} after all retries: {}", tmdbId, error.getMessage());
                    return Mono.empty();
                });
    }


    private List<OttPlatform> extractOttPlatforms(Map<String, Object> watchProviders) {
        Map<String, Object> results = (Map<String, Object>) watchProviders.get("results");
        if (results == null) {
            return Collections.emptyList();
        }

        List<OttPlatform> platforms = new ArrayList<>();

        for (String region : List.of("IN", "US")) {
            Map<String, Object> regionData = (Map<String, Object>) results.get(region);
            if (regionData != null) {
                platforms.addAll(extractPlatformsForRegion(regionData, region));
            }
        }

        return platforms;
    }

    private List<OttPlatform> extractPlatformsForRegion(Map<String, Object> regionData, String region) {
        List<OttPlatform> platforms = new ArrayList<>();

        List<Map<String, Object>> flatrate = (List<Map<String, Object>>) regionData.get("flatrate");
        if (flatrate != null) {
            platforms.addAll(flatrate.stream()
                    .map(provider -> createOttPlatform(provider, region, "Premium"))
                    .toList());
        }

        List<Map<String, Object>> rent = (List<Map<String, Object>>) regionData.get("rent");
        if (rent != null) {
            platforms.addAll(rent.stream()
                    .map(provider -> createOttPlatform(provider, region, "Rent"))
                    .toList());
        }

        List<Map<String, Object>> buy = (List<Map<String, Object>>) regionData.get("buy");
        if (buy != null) {
            platforms.addAll(buy.stream()
                    .map(provider -> createOttPlatform(provider, region, "Buy"))
                    .toList());
        }

        return platforms;
    }

    private OttPlatform createOttPlatform(Map<String, Object> provider, String region, String type) {
        OttPlatform platform = new OttPlatform();
        platform.setPlatformName((String) provider.get("provider_name"));
        platform.setAvailabilityRegion(region);
        platform.setSubscriptionType(type);
        platform.setAvailableFrom(LocalDate.now());
        return platform;
    }
}
