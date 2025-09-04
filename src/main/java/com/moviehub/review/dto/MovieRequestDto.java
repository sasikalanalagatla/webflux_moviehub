package com.moviehub.review.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class MovieRequestDto {

    @NotBlank(message = "Movie title is required")
    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;

    @NotNull(message = "At least one genre is required")
    @Size(min = 1, message = "At least one genre must be specified")
    private List<String> genre;

    @NotNull(message = "Release year is required")
    @Min(value = 1900, message = "Release year must be after 1900")
    @Max(value = 2030, message = "Release year cannot be in the future")
    private Integer releaseYear;
}