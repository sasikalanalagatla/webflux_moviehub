package com.moviehub.review.service.impl;

import com.moviehub.review.dto.MovieRequestDto;
import com.moviehub.review.dto.MovieResponseDto;
import com.moviehub.review.exception.MovieNotFoundException;
import com.moviehub.review.mapper.MovieMapper;
import com.moviehub.review.model.Movie;
import com.moviehub.review.repository.MovieRepository;
import com.moviehub.review.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class MovieServiceImpl implements MovieService {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Override
    public Mono<MovieResponseDto> createMovie(MovieRequestDto movieRequestDto) {
        Movie movie = MovieMapper.toEntity(movieRequestDto);
        movie.setAverageRating(0.0); // Initialize with 0.0
        return movieRepository.save(movie)
                .map(MovieMapper::toDto);
    }

    @Override
    public Mono<MovieResponseDto> getMovieById(String movieId) {
        return movieRepository.findById(movieId)
                .switchIfEmpty(Mono.error(new MovieNotFoundException("Movie not found with movieId: " + movieId)))
                .map(MovieMapper::toDto);
    }

    @Override
    public Flux<MovieResponseDto> getALlMovies() {
        return movieRepository.findAll()
                .map(MovieMapper::toDto)
                .switchIfEmpty(Flux.empty());
    }

    @Override
    public Mono<MovieResponseDto> updateMovie(String movieId, MovieRequestDto movieRequestDto) {
        return movieRepository.findById(movieId)
                .switchIfEmpty(Mono.error(new MovieNotFoundException("Movie not found with movieId: " + movieId)))
                .flatMap(existingMovie -> {
                    existingMovie.setTitle(movieRequestDto.getTitle());
                    existingMovie.setGenre(movieRequestDto.getGenre());
                    existingMovie.setReleaseYear(movieRequestDto.getReleaseYear());
                    // Keep existing rating
                    return movieRepository.save(existingMovie);
                })
                .map(MovieMapper::toDto);
    }

    @Override
    public Mono<Void> deleteMovie(String movieId) {
        return movieRepository.findById(movieId)
                .switchIfEmpty(Mono.error(new MovieNotFoundException("Movie not found with movieId: " + movieId)))
                .flatMap(movie -> movieRepository.deleteById(movieId));
    }

    @Override
    public Mono<MovieResponseDto> updateMovieRating(String movieId, Double newRating) {
        return movieRepository.findById(movieId)
                .switchIfEmpty(Mono.error(new MovieNotFoundException("Movie not found with movieId: " + movieId)))
                .flatMap(movie -> {
                    movie.setAverageRating(newRating);
                    return movieRepository.save(movie);
                })
                .map(MovieMapper::toDto);
    }

    @Override
    public Flux<MovieResponseDto> findMoviesByGenre(String genre) {
        return movieRepository.findAll()
                .filter(movie -> movie.getGenre() != null &&
                        movie.getGenre().stream().anyMatch(g ->
                                g.toLowerCase().contains(genre.toLowerCase())))
                .map(MovieMapper::toDto);
    }

    private Mono<Double> calculateMovieRating(String movieId) {
        return reviewRepository.findByMovieId(movieId)
                .collectList()
                .map(reviews -> {
                    if (reviews.isEmpty()) {
                        return 0.0;
                    }
                    double sum = reviews.stream()
                            .mapToInt(review -> review.getRating())
                            .sum();
                    return sum / reviews.size();
                });
    }
}
