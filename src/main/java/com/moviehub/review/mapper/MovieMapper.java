package com.moviehub.review.mapper;

import com.moviehub.review.dto.MovieRequestDto;
import com.moviehub.review.dto.MovieResponseDto;
import com.moviehub.review.model.Movie;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MovieMapper {

    public static Movie toEntity(MovieRequestDto dto) {
        Movie movie = new Movie();
        movie.setTitle(dto.getTitle());

        if (dto.getGenre() != null && !dto.getGenre().trim().isEmpty()) {
            List<String> genreList = Arrays.stream(dto.getGenre().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            movie.setGenre(genreList);
        }

        movie.setReleaseYear(dto.getReleaseYear());
        movie.setAverageRating(0.0);
        return movie;
    }

    public static MovieResponseDto toDto(Movie movie) {
        MovieResponseDto dto = new MovieResponseDto();
        dto.setMovieId(movie.getMovieId());
        dto.setTitle(movie.getTitle());

        if (movie.getGenre() != null && !movie.getGenre().isEmpty()) {
            dto.setGenre(String.join(", ", movie.getGenre()));
        }

        dto.setReleaseYear(movie.getReleaseYear());
        dto.setAverageRating(movie.getAverageRating());
        dto.setReleaseDate(movie.getReleaseDate());
        dto.setReleased(movie.getReleased());
        return dto;
    }
}