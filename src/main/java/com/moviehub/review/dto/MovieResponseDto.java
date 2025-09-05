package com.moviehub.review.dto;

import lombok.Data;

import java.util.List;
import java.time.LocalDate;

@Data
public class MovieResponseDto {
    private String movieId;
    private String title;
    private String genre;
    private Integer releaseYear;
    private Double averageRating;
    private LocalDate releaseDate;
    private Boolean released;
}