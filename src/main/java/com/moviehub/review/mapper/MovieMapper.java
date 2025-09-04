package com.moviehub.review.mapper;

import com.moviehub.review.dto.MovieRequestDto;
import com.moviehub.review.dto.MovieResponseDto;
import com.moviehub.review.model.Movie;

public class MovieMapper {

    public static Movie toEntity(MovieRequestDto dto) {
        Movie movie = new Movie();
        movie.setTitle(dto.getTitle());
        movie.setGenre(dto.getGenre());
        movie.setReleaseYear(dto.getReleaseYear());
        movie.setAverageRating(0.0);
        return movie;
    }

    public static MovieResponseDto toDto(Movie movie) {
        MovieResponseDto dto = new MovieResponseDto();
        dto.setMovieId(movie.getMovieId());
        dto.setTitle(movie.getTitle());
        dto.setGenre(movie.getGenre());
        dto.setReleaseYear(movie.getReleaseYear());
        dto.setAverageRating(movie.getAverageRating());
        return dto;
    }
}