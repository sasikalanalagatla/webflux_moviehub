package com.moviehub.review.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.time.LocalDate;

@Document
@Data
public class Movie {

    @Id
    private String movieId;

    private String tmdbId;
    private String title;
    private List<String> genre;
    private Integer releaseYear;
    private Double averageRating;
    private LocalDate releaseDate;
    private Boolean released;
}
