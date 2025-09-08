package com.moviehub.review.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;
import java.util.List;

@Document(collection = "movies")
@Data
public class Movie {
    @Id
    private String movieId;
    private String title;
    private String originalTitle;
    private List<String> genre;
    private Integer releaseYear;
    private LocalDate releaseDate;
    private Boolean released;
    private Double averageRating;
    private String overview;
    private String posterUrl;
    private String backdropUrl;
    private Integer runtime;
    private String language;
    private String country;
    private Double budget;
    private Double revenue;

    private List<CastMember> cast;
    private CrewInfo crew;

    private List<OttPlatform> ottPlatforms;
    private String distributionCompany;

    private List<String> singers;
    private List<String> lyricists;
    private List<String> musicDirectors;

    private String tmdbId;
    private String imdbId;
}