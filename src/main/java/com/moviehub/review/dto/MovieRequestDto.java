package com.moviehub.review.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class MovieRequestDto {

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;

    private String originalTitle;

    @NotEmpty(message = "At least one genre is required")
    private List<String> genre;

    @NotNull(message = "Release year is required")
    private Integer releaseYear;

    private LocalDate releaseDate;

    @Size(max = 2000, message = "Overview must not exceed 2000 characters")
    private String overview;

    private String posterUrl;
    private String backdropUrl;

    @Min(value = 1, message = "Runtime must be at least 1 minute")
    @Max(value = 600, message = "Runtime must not exceed 600 minutes")
    private Integer runtime;

    private String language;
    private String country;
    private Double budget;
    private Double revenue;
    private String distributionCompany;

    private List<CastMemberDto> cast;
    private CrewInfoDto crew;

    private List<OttPlatformDto> ottPlatforms;

    private List<String> singers;
    private List<String> lyricists;
    private List<String> musicDirectors;

    private String tmdbId;
    private String imdbId;

}