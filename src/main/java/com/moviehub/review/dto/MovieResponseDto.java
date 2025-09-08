package com.moviehub.review.dto;

import lombok.Data;

import java.util.List;
import java.time.LocalDate;

@Data
public class MovieResponseDto {
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

    private List<CastMemberDto> cast;
    private CrewInfoDto crew;

    private List<OttPlatformDto> ottPlatforms;

    private List<String> singers;
    private List<String> lyricists;
    private List<String> musicDirectors;

    private String tmdbId;
    private String imdbId;
}