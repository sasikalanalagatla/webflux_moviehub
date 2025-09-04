package com.moviehub.review.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document
@Data
public class Movie {

    @Id
    private String movieId;
    private String title;
    private List<String> genre;
    private Integer releaseYear;
    private Double averageRating;
}
