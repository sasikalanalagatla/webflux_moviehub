package com.moviehub.review.service;

import com.moviehub.review.dto.MovieRequestDto;
import com.moviehub.review.dto.MovieResponseDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MovieService {
    Mono<MovieResponseDto> createMovie(MovieRequestDto movieRequestDto);
    Mono<MovieResponseDto> getMovieById(String movieId);
    Flux<MovieResponseDto> getALlMovies();
    Mono<MovieResponseDto> updateMovie(String movieId, MovieRequestDto movieRequestDto);
    Mono<Void> deleteMovie(String id);
    Mono<MovieResponseDto> updateMovieRating(String movieId, Double newRating); // New method
    Flux<MovieResponseDto> findMoviesByGenre(String genre); // New method
    Mono<MovieResponseDto> createMovieFromTmdbSearch(String query, Integer year);
}